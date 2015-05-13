; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.focus
    (:require [flatgui.base :as fg]
      [flatgui.comlogic :as fgc]
      [flatgui.inputchannels.awtbase :as inputbase]
      [flatgui.inputchannels.mouse :as mouse]
      [flatgui.inputchannels.keyboard :as keyboard])
  (:import (java.awt.event KeyEvent)))


(def clean-state {:mode :none
                  :focused-child nil})

(defn focus-child [child-id]
  (if (keyword? child-id)
    {:mode :parent-of-focused
     :focused-child child-id}
    (throw (IllegalArgumentException. (str "child-id must be a keyword but is: " child-id)))))


; TODO
;(defn accepts-focus? [component]
;  (or
;    ;; If the component is focusable itself
;    (:focusable component)
;    ;; If the component has :focus-policy and therefore is a focus cycle root
;    (:focus-policy component)))

(defn get-in-cycle
  ([component dir c-id]
    (let [child-count (count (:children component))
          child-ids (mapv (fn [[k _]] k) (:children component))
          _ (println " get-in-cycle c-id " c-id "; child-ids " child-ids)
          closed (:closed-focus-root component)
          cycle-keeper (fn [i]
                         (cond
                           (>= i child-count) (if closed 0)
                           (< i 0) (if closed (dec child-count))
                           :else i))
          index-of-c (if c-id (.indexOf child-ids c-id))]
      (cond
        (= child-count 0) nil
        (= child-count 1) (nth child-ids 0)
        :else (case dir
                :next (nth child-ids (cycle-keeper (inc index-of-c)))
                :prev (nth child-ids (cycle-keeper (dec index-of-c)))
                :first (nth child-ids 0)
                :last (nth child-ids (dec child-count))))))
  ([component dir] (get-in-cycle component dir nil)))


(fg/defevolverfn :focus-state
  (let [reason (fg/get-reason)
        this-focused-child (:focused-child old-focus-state)
        this-mode (:mode old-focus-state)
        parent-focus-state (get-property [] :focus-state)
        parent-focused-child (:focused-child parent-focus-state)
        parent-mode (:mode parent-focus-state)
        ;_ (println "FOCUS EVOLVER CALLED - " (:id component) " -- " reason)
        ;_ (println " old-focus-state = " old-focus-state)
        ]
    (cond

      (keyboard/key-event? component)
      (if (and
            (= :has-focus this-mode)
            (keyboard/key-pressed? component)
            (= (keyboard/get-key component) KeyEvent/VK_TAB))
        {:mode :throws-focus
         :focused-child nil
         :throw-mode (cond
                       (and (inputbase/with-alt? component) (inputbase/with-shift? component)) :out-of-cycle-prev
                       (inputbase/with-alt? component) :out-of-cycle-next
                       (inputbase/with-shift? component) :prev
                       :else :next)}
        old-focus-state)

      (mouse/mouse-left? component)
      (if (and (:focusable component) (#{:none :parent-of-focused :throws-focus} this-mode))
        {:mode :requests-focus
         :focused-child this-focused-child}
        old-focus-state)

      (fgc/parent-reason? reason)
      (case parent-mode
        :has-focus clean-state
        :none clean-state
        :parent-of-focused (if (= parent-focused-child (:id component))
                             ;; My parent is ready to give me focus. If I have any child requesting focus then I
                             ;; give focus to it. Otherwise I leave focus with myself. (*1)
                             ;(if (= child-mode :requests-focus)
                             ;  (focus-child child-id)
                             ;  {:mode :has-focus
                             ;   :focused-child nil})
                             (if-let [child-id (:id (first (filter
                                                             #(= :requests-focus (:mode (:focus-state %)))
                                                             (for [[_ c] (:children component)] c))))]
                               (focus-child child-id)
                               {:mode :has-focus
                                :focused-child nil})

                             ;; Another component next to me has focus, therefore my state is clean
                             clean-state)

        ;; My parent is requesting focus. No changes for my state yet
        :requests-focus old-focus-state

        ;; My parent is going to throw focus. No changes for my state yet
        :throws-focus old-focus-state)

      (fgc/child-reason? reason)
      (let [child-id (nth reason 1)
            child-focus-state (get-property [:this child-id] :focus-state)
            child-mode (:mode child-focus-state)
            child-throw-mode (:throw-mode child-focus-state)]
        (case child-mode
          ;; Nothing to change in this case
          :none old-focus-state

          ;; If my child is the parent of focused component (or parent of parent ...)
          ;; then I'm :parent-of-focused as well
          :parent-of-focused (focus-child child-id)

          ;; My child has focus itself - same thing
          :has-focus (focus-child child-id)

          :requests-focus (case this-mode
                            ;; I'm the parent of focused component (no matter which exactly) and my child requests
                            ;; focus - then I note that this child becomes the new focus owner. My :mode stays the same
                            :parent-of-focused (focus-child child-id)

                            ;; If I have focus and my child requests it - I give focus to it
                            :has-focus (focus-child child-id)

                            ;; If my child wants focus but I don't have it - I start requesting focus in order to give
                            ;; it to my child as soon as I have it (*1)
                            :none {:mode :requests-focus
                                   :focused-child nil})
          :throws-focus (case child-throw-mode
                          ;; My child throws focus in :prev direction. Give focus to previous child.
                          ;; But if I'm not a closed root then there may be no previous child,
                          ;; get-in-cycle returns nil in this case. Rethrow it out in this case
                          :prev (if-let [prev-child (get-in-cycle component :prev this-focused-child)]
                                  (focus-child prev-child)
                                  {:mode :throws-focus
                                   :throw-mode :prev
                                   :focused-child this-focused-child})

                          ;; My child throws focus in :next direction. Give focus to next child.
                          ;; But if I'm not a closed root then there may be no next child,
                          ;; get-in-cycle returns nil in this case. Rethrow it out in this case
                          :next (if-let [next-child (get-in-cycle component :next this-focused-child)]
                                  (focus-child next-child)
                                  {:mode :throws-focus
                                   :throw-mode :next
                                   :focused-child this-focused-child})

                          ;; Just rethrow out in :prev direction
                          :out-of-cycle-prev {:mode :throws-focus
                                              :throw-mode :prev
                                              :focused-child this-focused-child}

                          ;; Just rethrow out in :next direction
                          :out-of-cycle-next {:mode :throws-focus
                                              :throw-mode :next
                                              :focused-child this-focused-child})))

      :else ; Neither event type - ignore
      old-focus-state)))