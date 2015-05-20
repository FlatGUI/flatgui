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

(def having-focus-state {:mode :has-focus
                         :focused-child nil})

(defn temporarily-taken-from-state [permanent-owner-child dir] {:mode :has-focus
                                                                :focused-child permanent-owner-child
                                                                :throw-mode dir})

(defn focus-child [child-id]
  (if (keyword? child-id)
    {:mode :parent-of-focused
     :focused-child child-id}
    (throw (IllegalArgumentException. (str "child-id must be a keyword but is: " child-id)))))

;; For :parent-of-focused and :has-focus the latest focus movent direction (if any) is tracked in :throw-mode
(defn focus-child-by-direction [child-id dir]
  (assoc (focus-child child-id) :throw-mode dir))

(defn accepts-focus? [component]
  (or
    ;; If the component is focusable itself
    (:focusable component)
    ;; If the component has any child to which it can pass focus
    (some (fn [[_ c]] (accepts-focus? c)) (:children component))))

(defn get-in-cycle
  ([component dir c-id]
    (let [accepting-children (filter accepts-focus? (for [[_ v] (:children component)] v))
          child-count (count accepting-children)
          child-ids (mapv :id accepting-children)
          closed (:closed-focus-root component)
          cycle-keeper (fn [i]
                         (cond
                           (>= i child-count) (if closed 0)
                           (< i 0) (if closed (dec child-count))
                           :else i))
          index-of-c (if c-id (.indexOf child-ids c-id))]
      (cond
        (= child-count 0) nil
        (and (= child-count 1) closed) (nth child-ids 0)
        :else (case dir
                :next (if-let [new-index (cycle-keeper (inc index-of-c))] (nth child-ids new-index))
                :prev (if-let [new-index (cycle-keeper (dec index-of-c))] (nth child-ids new-index))
                :first (nth child-ids 0)
                :last (nth child-ids (dec child-count))))))
  ([component dir] (get-in-cycle component dir nil)))

(fg/defevolverfn :focus-state
  (let [reason (fg/get-reason)
        this-focused-child (:focused-child old-focus-state)
        this-mode (:mode old-focus-state)
        parent-focus-state (get-property [] :focus-state)
        parent-focused-child (:focused-child parent-focus-state)
        parent-mode (:mode parent-focus-state)]
    (cond

      (keyboard/key-event? component)
      (if (and
            (= :has-focus this-mode)
            (keyboard/key-pressed? component)
            (= (keyboard/get-key component) KeyEvent/VK_TAB))
        {:mode :throws-focus
         :focused-child nil
         :throw-mode (cond
                       (and (inputbase/with-ctrl? component) (inputbase/with-shift? component)) :out-of-cycle-prev
                       (inputbase/with-ctrl? component) :out-of-cycle-next
                       (inputbase/with-shift? component) :prev
                       :else :next)}
        old-focus-state)

      (mouse/mouse-left? component)
      (cond
        (and (:focusable component) (#{:none :parent-of-focused :throws-focus} this-mode))
        {:mode :requests-focus
         :focused-child this-focused-child}
        (and (:closed-focus-root component) (#{:none :throws-focus :requests-focus} this-mode))
        having-focus-state
        :else old-focus-state)

      (fgc/parent-reason? reason)
      (case parent-mode
        ;; If parent has focus temporarily, I take focus back (*2)
        ;; Otherwise my parent has focus permanently and my state is clean
        :has-focus (if (= parent-focused-child (:id component)) ; This condition means that parent has focus temporarily
                     ;; Parent keeps latest throwing direction in :throw-mode in this case. Track it for further use
                     (assoc having-focus-state :throw-mode (:throw-mode parent-focus-state))
                     clean-state)
        ;:has-focus clean-state
        :none clean-state
        :parent-of-focused (if (= parent-focused-child (:id component))
                             ;; If my parent is :parent-of-focused and I am :parent-of-focused,
                             ;; then no changes needed here.
                             (if (not= this-mode :parent-of-focused)
                               ;; My parent is ready to give me focus.
                               ;;   If I have any child requesting focus then I give focus to it.
                               ;;   Otherwise I leave focus with myself. (*1)
                               ;;   Else, if I am acutally not focusable then I pass focus to the first
                               ;;   child in my cycle
                               (if-let [child-id (if-let [requesting-child-id
                                                          (:id (first (filter
                                                                        #(= :requests-focus (:mode (:focus-state %)))
                                                                        (for [[_ c] (:children component)] c))))]
                                                   requesting-child-id
                                                   (if (not (:focusable component))
                                                     (get-in-cycle
                                                       component
                                                       ;; In case latest focus movent direction is tracked
                                                       ;; in :throw-mode - use it to determine firt or last
                                                       (if (= :prev (:throw-mode parent-focus-state))
                                                         :last
                                                         :first))))]
                                 (focus-child-by-direction child-id (:throw-mode parent-focus-state))
                                 having-focus-state)
                               ;; No change in state but track focus movent direction
                               (assoc old-focus-state :throw-mode (:throw-mode parent-focus-state)))

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
          :parent-of-focused (focus-child-by-direction child-id child-throw-mode)

          ;; My child has focus itself - same thing
          :has-focus (focus-child-by-direction child-id child-throw-mode)

          :requests-focus (case this-mode
                            ;; I'm the parent of focused component (no matter which exactly) and my child requests
                            ;; focus - then I note that this child becomes the new focus owner. My :mode stays the same
                            :parent-of-focused (focus-child-by-direction child-id (:throw-mode old-focus-state))

                            ;; If I have focus and my child requests it - I give focus to it
                            :has-focus (focus-child child-id)

                            ;; If my child wants focus but I don't have it - I start requesting focus in order to give
                            ;; it to my child as soon as I have it (*1)
                            :none {:mode :requests-focus
                                   :focused-child nil}

                            ;; If my child request focus and I request focus as well - there is nothing that can be
                            ;; changed about this
                            :requests-focus old-focus-state

                            ;; My child request focus and I throw focus - then I let this child get focus
                            :throws-focus (focus-child child-id))
          :throws-focus (case child-throw-mode
                          ;; My child throws focus in :prev direction. Give focus to previous child.
                          ;; But if I'm not a closed root then there may be no previous child,
                          ;; get-in-cycle returns nil in this case. Rethrow it out in this case
                          :prev (if-let [prev-child (get-in-cycle component :prev this-focused-child)]
                                  (if (= prev-child this-focused-child)
                                    ;; My child throws focus, but there is no other component to take it.
                                    ;; So I take it temporarily and child will take back within the next step. (*2)
                                    ;; Latest movement direction is tracked to be used within the next step
                                    (temporarily-taken-from-state this-focused-child :prev)
                                    (focus-child-by-direction prev-child :prev))
                                  {:mode :throws-focus
                                   :throw-mode :prev
                                   :focused-child this-focused-child})

                          ;; My child throws focus in :next direction. Give focus to next child.
                          ;; But if I'm not a closed root then there may be no next child,
                          ;; get-in-cycle returns nil in this case. Rethrow it out in this case
                          :next (if-let [next-child (get-in-cycle component :next this-focused-child)]
                                  (if (= next-child this-focused-child)
                                    ;; My child throws focus, but there is no other component to take it.
                                    ;; So I take it temporarily and child will take back within the next step. (*2)
                                    ;; Latest movement direction is tracked to be used within the next step
                                    (temporarily-taken-from-state this-focused-child :next)
                                    (focus-child-by-direction next-child :next))
                                  {:mode :throws-focus
                                   :throw-mode :next
                                   :focused-child this-focused-child})

                          ;; Rethrow up to nearest closed cycle root
                          :out-of-cycle-prev {:mode :throws-focus
                                              :throw-mode (if (:closed-focus-root component)
                                                            :prev
                                                            :out-of-cycle-prev)
                                              :focused-child this-focused-child}

                          ;; Rethrow up to nearest closed cycle root
                          :out-of-cycle-next {:mode :throws-focus
                                              :throw-mode (if (:closed-focus-root component)
                                                            :next
                                                            :out-of-cycle-next)
                                              :focused-child this-focused-child})))

      :else ; Neither event type - ignore
      old-focus-state)))