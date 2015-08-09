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
            [flatgui.inputchannels.keyboard :as keyboard]
            [flatgui.util.matrix :as m]
            [flatgui.util.rectmath :as rect])
  (:import (java.awt.event KeyEvent)
           (java.util Comparator)))

(def debug-logging-enabled false)

(defn log-debug [& msg]
  (if debug-logging-enabled (apply fg/log-debug msg)))

(fg/defevolverfn accepts-focus-evolver :accepts-focus?
  (and
    (get-property component [:this] :visible)
    (get-property component [:this] :enabled)
    (or
      ;; If the component is focusable itself
      (get-property component [:this] :focusable)
      ;; If the component has any child to which it can pass focus
      (some
        (fn [[_ c]] (get-property component [:this (:id c)] :accepts-focus?))
        (get-property component [:this] :children)))))

(fg/defaccessorfn get-accepting-children [component]
  (let [child-map (get-property component [:this] :children)]
    (filter
      (fn [c] (get-property component [:this (:id c)] :accepts-focus?))
      (for [[_ v] child-map] v))))

(def component-comparator
  (reify Comparator
    (compare [_this o1 o2]
      (let [x1 (m/mx-x (:position-matrix o1))
            x2 (m/mx-x (:position-matrix o2))]
        (if (< x1 x2)
          -1
          (if (> x1 x2)
            1
            (let [y1 (m/mx-y (:position-matrix o1))
                  y2 (m/mx-y (:position-matrix o2))]
              (if (< y1 y2) -1 (if (> y1 y2) 1 0)))))))))

(defn- sort-each-line-by-x [lines] (mapv (fn [l] (sort component-comparator l)) lines))

(def line-comparator
  (reify Comparator
    (compare [_this o1 o2]
      (if (< (:line-y1 o1) (:line-y1 o2))
        -1
        (if (> (:line-y1 o1) (:line-y1 o2)) 1 0)))))

(defn- sort-lines-by-y [lines line-y1]
  (let [line-maps (map (fn [i] {:line (nth lines i) :line-y1 (nth line-y1 i)}) (range 0 (count lines)))]
    (mapv #(:line %) (sort line-comparator line-maps))))

(fg/defaccessorfn group-by-lines [component accepting-children]
  (let [cnt (count accepting-children)]
    (loop [line-y1 []
           line-y2 []
           lines []
           i 0]
      (if (< i cnt)
        (let [c (nth accepting-children i)
              cy1 (m/mx-y (get-property component [:this (:id c)] :position-matrix))
              cy2 (+ cy1 (m/y (get-property component [:this (:id c)] :clip-size)))
              line-index (if-let [matching (some
                                             (fn [j] (if (rect/line& cy1 cy2 (nth line-y1 j) (nth line-y2 j)) j))
                                             (range 0 (count lines)))]
                           matching
                           (count lines))
              existing-line (< line-index (count lines))
              line (if existing-line (nth lines line-index) #{})]
          (recur
            (assoc line-y1 line-index (if existing-line (min cy1 (nth line-y1 line-index)) cy1))
            (assoc line-y2 line-index (if existing-line (max cy2 (nth line-y2 line-index)) cy2))
            (assoc lines line-index (conj line c))
            (inc i)))
        (sort-lines-by-y (sort-each-line-by-x lines) line-y1)))))

;;; Sorts focusable components so that they are traversed in left->right, up->down direction.
;;; To make such ad order, arranges components in a vector of horizontal "lines"
(fg/defevolverfn :focus-traversal-order
  (let [accepting-children (get-accepting-children component)]
    (if (pos? (count accepting-children))
      (let [lines (group-by-lines component accepting-children)
            result (mapcat (fn [i] (map :id (nth lines i))) (range 0 (count lines)))]
        result)
      [])))

(fg/defaccessorfn get-child-ids-in-traversal-order [component]
  (if-let [focus-traversal-order (get-property component [:this] :focus-traversal-order)]
    focus-traversal-order
    (let [accepting-children (get-accepting-children component)]
      (mapv :id accepting-children))))

(fg/defaccessorfn get-in-cycle [component dir c-id]
  (let [child-ids (get-child-ids-in-traversal-order component)
        child-count (count child-ids)
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

(def clean-state {:mode :none
                  :focused-child nil})

(defn having-focus-state [component comment]
  (do
    (log-debug "Permanent focus (" comment ") received by" (:id component))
    {:mode :has-focus
     :focused-child nil}))

(defn temporarily-taken-from-state [component comment permanent-owner-child dir]
  (do
    (log-debug "Temporary focus (" comment ") from" permanent-owner-child "received by" (:id component))
    {:mode :has-focus
     :focused-child permanent-owner-child
     :throw-mode dir}))

(defn got-trigger []
  {:mode :got-trigger
   :focused-child nil})

(defn focus-child [child-id]
  (if (keyword? child-id)
    {:mode :parent-of-focused
     :focused-child child-id}
    (throw (IllegalArgumentException. (str "child-id must be a keyword but is: " child-id)))))

;;; For :parent-of-focused and :has-focus the latest focus movent direction (if any) is tracked in :throw-mode
(defn focus-child-by-direction [child-id dir]
  (assoc (focus-child child-id) :throw-mode dir))

(defn focus-uninterested? [component]
  (and (not (:closed-focus-root component)) (not (:focusable component))))

(fg/defevolverfn :focus-state
  (let [reason (fg/get-reason)
        this-focused-child (:focused-child old-focus-state)
        this-mode (:mode old-focus-state)
        parent-id (get-property [] :id)
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
        (having-focus-state component "closed root by mouse")
        (and (focus-uninterested? component) (not= :parent-of-focused this-mode))
        (do
          (log-debug "Trigger received by focus-uninterested" (:id component))
          (got-trigger))
        :else old-focus-state)

      (and (fgc/parent-reason? reason) parent-mode)
      (case parent-mode
        ;; If parent has focus temporarily, I take focus back (*2)
        ;; Otherwise my parent has focus permanently and my state is clean
        :has-focus (if (= parent-focused-child (:id component)) ; This condition means that parent has focus temporarily
                     ;; Parent keeps latest throwing direction in :throw-mode in this case. Track it for further use
                     (assoc (having-focus-state component (str "from parent " parent-id " that kept temporary")) :throw-mode (:throw-mode parent-focus-state))
                     clean-state)
        :got-trigger (if (= this-mode :got-trigger)
                       clean-state
                       (if (:focusable component)
                         having-focus-state
                         (if-let [child-id (get-in-cycle component :first nil)]
                           (focus-child-by-direction child-id :next)
                           old-focus-state)))
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
                                                         :first)
                                                       nil)))]
                                 (focus-child-by-direction child-id (:throw-mode parent-focus-state))
                                 (if (:accepts-focus? component)
                                   (having-focus-state component (str "from parent " parent-id " that requested"))
                                   {:mode :throws-focus
                                    :throw-mode :out-of-cycle-next}))
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
                                    (temporarily-taken-from-state component (str "prev, reason: " child-id) this-focused-child :prev)
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
                                    (temporarily-taken-from-state component (str "next, reason: " child-id) this-focused-child :next)
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
                                              :focused-child this-focused-child})

          :got-trigger (cond
                         ;; Trigger finally made it to focusable
                         (and (:focusable component) (not= :parent-of-focused this-mode)) ;maybe accepts-focus? instead of focusable
                         (having-focus-state component "trigger")
                         ;; Trigger reached non focusable, but one that possibly has where to give focus
                         (or
                           (:closed-focus-root component)
                           (get-in-cycle component :first nil))
                         (if-let [first-to-give (get-in-cycle component :first nil)]
                           (temporarily-taken-from-state component "trigger" first-to-give nil)
                           (got-trigger))
                         ;; Just give chance further
                         :else
                         (got-trigger))))

      :else ; Neither event type - ignore
      old-focus-state)))