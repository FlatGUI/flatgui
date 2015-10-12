; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "FlatGUI widget base routines"
      :author "Denys Lebediev"}
  flatgui.widgets.componentbase
  (:require [flatgui.dependency :as dep]
            [flatgui.base :as fg]
            [flatgui.ids :as ids]
            [flatgui.comlogic :as fgc]
            [flatgui.paint :as fgp]
            [flatgui.util.matrix :as m]))


(defn- get-evolver [component property]
  (get-in component [:evolvers property]))

(defn- get-initializer [component property]
  (get-in component [:initializers property]))

(defn- get-child [component child-id]
  (get-in component [:children child-id]))


(declare reinit-if-needed)

(declare initialize)

(declare flex-initialize)

(defn reinit-if-needed [container]
  (cond
    (:has-structure-changes container)
    (do
      (fg/log-info "Performing full reinit")
      (initialize container))
    (:flex-structure-changes container)
    (do
      (fg/log-info "Performing flexible structure init ")
      (println "FLEX: " (:flex-target-id-paths-added container))
      (flex-initialize container))
    :else container))

(defn- debug-prefix [debug-shift]
  (str debug-shift "[" (/ (.length debug-shift) 2) "]"))

(defn evolve-by-dependencies [original-container container original-target-id-path target-id-path reason properties-to-evolve debug-shift initialization issuer])

;
;@todo Actually we don't need issuer-reason and debug-shift parameters
;
(defn evolve-out-dependents [original-container container original-target-id-path target-id-path issuer-reason reason dependents debug-shift initialization targret-access-key]
  (let [
         ;@todo Experiment1
         ;targret-access-key (fgc/get-access-key target-id-path)
        ;targret-visible-access-key (fgc/conjv targret-access-key :visible)

        ; TODO October 12, 2015 initialization check should not be needed since  remove the following comment, but Review Experiment1 for regular evolving
         ;properties (if initialization
         ;             (let [ target-properties-already-evovled (fgc/get-evolved-properties container targret-access-key)
         ;                    to-evolve (if target-properties-already-evovled
         ;                                (vec (remove target-properties-already-evovled (:properties dependents)))
         ;                                (:properties dependents))]
         ;               (if (empty? to-evolve) nil to-evolve))
         ;
         ;             ;
         ;             ;@todo Experiment1. Have some :_parameter in :content-pane that tells this is allowed. Not allowed by default
         ;             ;
         ;             ;
         ;             ;(if (get-in container targret-visible-access-key)
         ;             ;  (:properties dependents)
         ;             ;  (do ;(println " Limiting properties for " target-id-path " because it is not visible")
         ;             ;    (if (some #(= :visible %1) (:properties dependents)) (:properties dependents)))
         ;             ;  )
         ;             ;
         ;             ; It causes bugs with cell :visible :achor :selection when grouping/ungrouping and when
         ;             ; selecting a row and then scrolling down
         ;             ;
         ;             (:properties dependents)
         ;
         ;             )
        properties (:properties dependents)
        ;_ (if (= [:main :hello] target-id-path) (println "ev-out" properties))

         dependent-children1 (:children dependents)

         ; @todo  This causes huge slowness. Instead of this, go to each of abs-dependencies of the cloned component
         ; @todo  after cloning and add new one as a dependent there. Will have to introduce :_has_flexible_structure_changes
         ;
         ;
;         dependent-children (if (not (empty? dependent-children1))
;                              (if (get-in container (fgc/conjv targret-access-key :_flexible-childset))
;                                (let [ target-children (get-in container (fgc/conjv targret-access-key :children))]
;                                  (if (not= (count target-children) (count dependent-children1))
;                                    (let [r (mapv (fn [[c-id _]] [c-id (last (first dependent-children1))]) target-children)]
;                                      (do
;                                        ;(println "Adjusted dependent-children")
;                                        ;(println dependent-children1)
;                                        ;(println r)
;                                        r))
;                                    (vec dependent-children1)))
;                                (vec dependent-children1))
;                              [])
          dependent-children (vec dependent-children1)

         dc-count (count dependent-children)

         ;[GOOD DEBUG OUTPUT]_ (if (some #(= :should-evolve-header %1) properties) (println (debug-prefix debug-shift) ">>ENTERING>> CAUTION :should-evolve-header is among " properties " issuer now is " issuer-reason))
         with-evolved-target  (if (and properties (> (count properties) 0))
                                (evolve-by-dependencies original-container container original-target-id-path target-id-path reason properties
                                                        nil;(str debug-shift "  ")
                                                        initialization
                                                        nil;(str "evolve-out-dependents called by " (System/currentTimeMillis) issuer-reason)
                                                        )
                                container)
         ;[GOOD DEBUG OUTPUT]_ (if (some #(= :should-evolve-header %1) properties) (println (debug-prefix debug-shift) "<<EXITED<< CAUTION :should-evolve-header is among " properties " issuer now is " issuer-reason))
         ;(or initialization (get-in ret (fgc/conjv (fgc/get-access-key child-target-id-path) :visible)))
         ]
    (loop [ dci 0
            ret with-evolved-target]
      (if (< dci dc-count)
        (let [ child-info (nth dependent-children dci)
               child-id (first child-info)
               child-target-id-path (fgc/conjv target-id-path child-id)
               child-depenents (last child-info)
               ;todo prepare access keys for children
                ]
          (recur
            (inc dci)
            (do
              ;[GOOD DEBUG OUTPUT](println (debug-prefix debug-shift) "  >--evolve-out-dependents from " target-id-path  " to " child-target-id-path " child-depenents " child-depenents " issuer " issuer-reason  )
              (evolve-out-dependents
                original-container
                ret
                original-target-id-path
                child-target-id-path
                nil;(str issuer-reason "*" target-id-path properties "*")
                reason
                child-depenents
                debug-shift
                initialization
                (fgc/get-access-key child-target-id-path)))
          ))
          ret))))

(defn evolve-component [original-container container original-target-id-path target-id-path reason properties-to-evolve debug-shift initialization])

(defn- ensure-map! [m]
  (if m m (transient {})))

(defn update-in!
  ([m [k & ks] f & args]
    (if ks
      (assoc! (ensure-map! m) k (apply update-in! (get m k) ks f args))
      (assoc! (ensure-map! m) k (apply f (get m k) args)))))

(defn- combine-dirty-rect [r pm clip]
  (do ;(println " combine-dirty-rect " r pm clip)
    (if (and pm clip)
    (let [ pmx (m/mx-x pm)
           pmy (m/mx-y pm)
           clip-w (m/x clip)
           clip-h (m/y clip)]
      (if r
        (let [ x1 (min (:x r) pmx)
               y1 (min (:y r) pmy)
               x2 (max (+ (:x r) (:w r)) (+ pmx clip-w))
               y2 (max (+ (:y r) (:h r)) (+ pmy clip-h))]
          {:x x1 :y y1 :w (- x2 x1) :h (- y2 y1)})
        {:x pmx :y pmy :w clip-w :h clip-h}))
    r)))

;;;
;;; TODO Refactor into a set of smaller functions
;;;
(defn evolve-component [original-container container original-target-id-path target-id-path reason properties-to-evolve debug-shift initialization]
  (let [ k (fgc/get-access-key target-id-path)
         pre-target-component (get-in container k)]
    ;; Absence of :path-to-target means that target is a newly created component that has
    ;; not passed container initialization yet and hence cannot be evolved yet
    (if (and pre-target-component (:path-to-target pre-target-component))
      (let [;; During initialization, if a property appears among :evolved-properties then:
            ;; - it is aready initialized, i.e. transitioned from nil to explicitly specified initial value
            ;; - it is not necessarily entirely initialized and may be updated because of the dependencies being initialized
            evolved-properties-key (fgc/conjv k :evolved-properties)
             changed-properties-key (fgc/conjv k :changed-properties)
             latest-changed-properties-key (fgc/conjv k :latest-changed-properties)
             aux-container (update-in! (:aux-container container) latest-changed-properties-key (transient #{}))
             evolve-reason-provider (fn [_] (cond
                                              (vector? reason) (ids/get-relative-path reason target-id-path)
                                              (= original-target-id-path target-id-path) reason))
             target-component (assoc pre-target-component
                                :target-id-path-index (dec (count target-id-path))
                                :evolve-reason-provider evolve-reason-provider)
             ps-count (count properties-to-evolve)]
        (loop [ pi 0
                tgt target-component
                ret container
                aux aux-container]
          (if (< pi ps-count)
                  (let [ p (nth properties-to-evolve pi)
                         pk (fgc/conjv k p)
                         evolver (p (:evolvers tgt))]
                    (if (or initialization evolver)
                      (let [;; Remember: cannot use get in :evolved-properties check because of http://dev.clojure.org/jira/browse/CLJ-700
                            ;; We don track :children change during initialization. Current implementation of initialization
                            ;; that calls account-struc-changes runs into stack overflow if we do this here
                            old-value (if (and initialization (not= p :children))
                                        (let [evolved (get-in aux evolved-properties-key)]
                                          (if (or (nil? evolved) (not (evolved p)))
                                            nil
                                            (p tgt)))
                                        (p tgt))
                             aux-with-p (update-in! aux evolved-properties-key fgc/set-conj! p)
                             root-p (assoc ret :aux-container aux-with-p)
                             aux-with-evolved-dependencies (:aux-container root-p)
                             ;@todo maybe use original value from original-container for currently evolved property while up-to-date for other, probably this is more honest
                             new-value (if evolver (evolver (assoc tgt :root-container root-p :aux-container aux-with-evolved-dependencies)) (p tgt))
                             ;; TODO October 15, 2015: looks like it is the same from now on
                             has-changes-raw (not (= old-value new-value))
                             has-changes has-changes-raw ;(or initialization has-changes-raw)

;                             _ (if (and has-changes (= p :children)) (println " Evolved children count from " (count old-value) " to " (count new-value) " str change "
;                                                                       (and has-changes-raw (= p :children) (or (not (:_flexible-childset tgt)) (empty? old-value)))))
                             ;@todo for :content-pane has-structure-changes should be false in case new child count is LESS than previous child count?
                             ;
                             children-changed (= p :children)
                            ;_ (if children-changed (println (:widget-type tgt) (:id tgt) "------CHILDREN CHANGED--FROM-" (count old-value) " TO " (count new-value)))



                             has-structure-changes (and has-changes-raw children-changed (or (not (:_flexible-childset tgt)) (empty? old-value)))
                             flex-structure-changes (if (and has-changes-raw children-changed (:_flexible-childset tgt) (seq old-value))
                                                      (:_flexible-childset-added new-value))


                             flex-target-id-paths-added (if flex-structure-changes (:_flex-target-id-paths-added new-value))
                             new-value (if flex-structure-changes (dissoc new-value :_flexible-childset-added :_flex-target-id-paths-added) new-value)
                             new-aux-with-consume (if (evolve-reason-provider nil)
                                                    (assoc!
                                                      aux-with-evolved-dependencies
                                                      :consumed
                                                      (or (:consumed aux-with-evolved-dependencies) ((:consumes? tgt) tgt)))
                                                    aux-with-evolved-dependencies)

                             new-aux (if has-changes
                                       (->
                                         (update-in! new-aux-with-consume changed-properties-key fgc/set-conj! p)
                                         (update-in! latest-changed-properties-key fgc/set-conj! p)
                                         (update-in! [:_changed-paths] fgc/set-conj! target-id-path))
                                       new-aux-with-consume)

                            ;_ (if (and (= :hello (:id tgt)) (= p :position-bound))
                            ;    (println "evolve-component" target-id-path "Reason" reason " Property " p "old" old-value "new" new-value " has-changes " has-changes
                            ;             (if (get-in new-aux latest-changed-properties-key)
                            ;               (str "position-bound?" ((get-in new-aux latest-changed-properties-key) :position-bound))
                            ;                                 )))


                            new-ret (if has-changes
                                       (let [ fin-ret1 (assoc (assoc-in root-p pk new-value)
                                                         :has-structure-changes (or (:has-structure-changes root-p) has-structure-changes)
                                                         :flex-structure-changes (merge (:flex-structure-changes root-p) flex-structure-changes)

                                                         ; For some reason that is still not clear enough, removing vec from here causes
                                                         ; stack overflow related to lazy seq
                                                         :flex-target-id-paths-added (vec (concat (:flex-target-id-paths-added root-p) flex-target-id-paths-added))


                                                         :has-changes (or (:has-changes root-p) has-changes)
                                                         :aux-container new-aux)
                                              fin-ret (if (and (:popup tgt) (= p :visible))
                                                        (update-in fin-ret1 [:paths-having-visible-popups] (if new-value conj disj) (fgc/drop-lastv target-id-path))
                                                        fin-ret1)]
                                         (condp = p
                                           ;TODO 1. take into account viewport-matrix when combining?

                                           :abs-position-matrix (assoc fin-ret :dirty-rect
                                                                  (let [ cs (:clip-size tgt)
                                                                         r (:dirty-rect fin-ret)]
                                                                    (combine-dirty-rect
                                                                      (combine-dirty-rect r old-value cs)
                                                                      new-value
                                                                      cs)))
                                           :clip-size (assoc fin-ret :dirty-rect
                                                        (let [ pm (:abs-position-matrix tgt)
                                                               r (:dirty-rect fin-ret)]
                                                          (combine-dirty-rect
                                                            (combine-dirty-rect r pm old-value)
                                                            pm
                                                            new-value)))
                                            (assoc fin-ret :dirty-rect
                                                 (let [ pm (:abs-position-matrix tgt)
                                                        cs (:clip-size tgt)
                                                        r (:dirty-rect fin-ret)]
                                                   (combine-dirty-rect
                                                     r
                                                     pm
                                                     cs)))))
                                       (assoc root-p
                                         :aux-container new-aux))]
                          (recur
                            (inc pi)

                            ;
                            ; ACHTUNG! Experiment from 2014-10-31: get-property-private optimization
                            ;(assoc! tgt :root-container new-ret :aux-container new-aux)
                            (assoc tgt :root-container new-ret :aux-container new-aux p new-value)
                            ;

                            new-ret
                            new-aux))
                      (recur
                        (inc pi)
                        tgt
                        ret
                        aux)))
            (assoc ret
              :aux-container (assoc! aux :out-dependents (:out-dependents tgt))))))
      container)))

(defn evolve-by-dependencies [original-container container original-target-id-path target-id-path reason properties-to-evolve debug-shift initialization issuer]
    (let [ ;[GOOD DEBUG OUTPUT] _ (if (not= [:text :selection] properties-to-evolve) (println (debug-prefix debug-shift) ">>> entered evolve-by-dependencies function " target-id-path " for properties " properties-to-evolve))
           k (fgc/get-access-key target-id-path)
           evolved-properties-key (fgc/conjv k :evolved-properties)
          ;already-evolved (get-in (:aux-container container) evolved-properties-key)
           ;
           ;@todo If current reason in not a vector (which meands depencency), take only evolvers dependent on current reason
           ;
           remaining-to-evolve (if properties-to-evolve
                                 properties-to-evolve
                                 (if initialization
                                   (keys (get-in container (fgc/conjv k :abs-dependents)))
                                   (mapv (fn [[k v]] k) (get-in container (fgc/conjv k :evolvers))))
                                 )]
      (if (<= (count remaining-to-evolve) 0)
        (do
          ;[GOOD DEBUG OUTPUT](if (not= [:text :selection] properties-to-evolve) (println (debug-prefix debug-shift) "<<< exited(1) evolve-by-dependencies function for properties " properties-to-evolve))
          container)
        (let [ k (fgc/get-access-key target-id-path)
               prev-has-changes (:has-changes container)
               with-evolved-target-fresh (evolve-component original-container (assoc container :has-changes false) original-target-id-path target-id-path reason remaining-to-evolve debug-shift initialization)
               new-has-changes (:has-changes with-evolved-target-fresh)
               has-changes (or prev-has-changes new-has-changes)
               with-evolved-target (assoc with-evolved-target-fresh :has-changes has-changes)
               new-aux (:aux-container with-evolved-target)
              ; _ (if (and
              ;         (= [:main :hello] target-id-path)
              ;         (get-in new-aux (fgc/conjv k :latest-changed-properties))
              ;         ((get-in new-aux (fgc/conjv k :latest-changed-properties)) :position-bound))
              ;     (println " JUST EVOLVED target-id-path " target-id-path
              ;       " new-has-changes " new-has-changes
              ;       " init " initialization
              ;       (if (:position-bound (get-in new-aux (fgc/conjv k :latest-changed-properties)))
              ;                               ((get-in new-aux (fgc/conjv k :latest-changed-properties)) :position-bound))
              ;       " position-bound " (get-in container (fgc/conjv k :position-bound)) "->" (get-in with-evolved-target (fgc/conjv k :position-bound))
              ;       ))
               ]
              ;@todo Without this initialization check :children for table content pane do not get evolved on initialization. Why?
              (if (or new-has-changes (and initialization has-changes))
                (let [all-out-dependents (:out-dependents new-aux)
                      changed-properties (get-in new-aux (fgc/conjv k :latest-changed-properties))
                      properties (if changed-properties (vec (seq (filter changed-properties (for [[k v] all-out-dependents] k)))) [])
                      container-id (:id with-evolved-target)]
                  (loop [ pi (dec (count properties))
                          ret with-evolved-target]
                    (if (>= pi 0)
                      (recur
                        (dec pi)
                        (let [ property (nth properties pi)
                               out-dependents (property all-out-dependents)
                               reason-for-dependent target-id-path
                               ;[GOOD DEBUG OUTPUT]_ (println (debug-prefix debug-shift) "Evolving---- |" target-id-path property "| issuer " issuer " -------- calling evolve-out-dependents dependents: " (container-id out-dependents))
                               ]
                            (evolve-out-dependents original-container ret original-target-id-path [container-id] nil;(str " from |" target-id-path property "|");reason
                               reason-for-dependent (container-id out-dependents) debug-shift initialization (fgc/get-access-key [container-id]))))
                      (do
                        ;[GOOD DEBUG OUTPUT](if (not= [:text :selection] properties-to-evolve) (println (debug-prefix debug-shift) "<<< exited(2) evolve-by-dependencies function for properties " properties-to-evolve))
                        ret)
                      )))
                (do
                  ;[GOOD DEBUG OUTPUT](if (not= [:text :selection] properties-to-evolve) (println (debug-prefix debug-shift) "<<< exited(3) evolve-by-dependencies function for properties " properties-to-evolve))
                  with-evolved-target))))))


(defn rebuild-look [container target-id-path aux changed-only dirty-rect]
  (let [k (fgc/get-access-key target-id-path)
        this-container (if (or (not changed-only) (get-in aux (fgc/conjv k :changed-properties)))
                         (let [look-fn (:look container)]
                           (if look-fn
                             (try
                               (assoc container :look-vec (fgp/flatten-vector
                                                            [(fgp/font-look container)
                                                             (look-fn container dirty-rect)
                                                             (if (:has-trouble container) (fgp/trouble-look container dirty-rect))]))
                               (catch Exception ex
                                 (do
                                   (fg/log-error "Error painting " target-id-path ":" (.getMessage ex))
                                   (.printStackTrace ex))))
                             container))
                         container)]
    (assoc
      this-container
      :children (into (array-map) (for [[k v] (:children container)] [k (rebuild-look v (fgc/conjv target-id-path k) aux changed-only dirty-rect)])))))


; TODO this was one more attempt to optimize for web
;(defn rebuild-look [container target-id-path aux changed-only]
;  (let [ k (fgc/get-access-key target-id-path)
;        this-container (if (or (not changed-only) (get-in aux (fgc/conjv k :changed-properties)))
;                         (let [ look-fn (:look container)]
;                           (if look-fn
;                             (assoc container :look-vec (look-fn container (:dirty-rect container)) :needs-repaint true)
;                             container))
;                         (assoc container :needs-repaint false))]
;    (assoc
;      this-container
;      :children (into (array-map) (for [[k v] (:children container)] [k (rebuild-look v (fgc/conjv target-id-path k) aux changed-only)])))))
; TODO the function has been optimized for web
;
;(defn rebuild-look [container target-id-path aux changed-only]
;  (let [ k (fgc/get-access-key target-id-path)
;         changed-properties (get-in aux (fgc/conjv k :changed-properties))
;         this-container (if (or (not changed-only) changed-properties)
;                          (let [ look-fn (:look container)
;                                 old-look-vec (:look-vec container)
;                                 new-look-vec (look-fn container nil)]
;                            (if look-fn
;                              (assoc
;                                container
;                                :look-vec new-look-vec
;                                :needs-repaint (or
;                                                 (not= old-look-vec new-look-vec)
;                                                 (changed-properties :position-matrix)
;                                                 (changed-properties :viewport-matrix)))
;                              container))
;
;                          ; TODO 2. If position changes then :needs-repaint-children -> true into force param
;                          ;
;                          (assoc container :needs-repaint false))]
;    (assoc
;      this-container
;      :children (into (array-map) (for [[k v] (:children container)] [k (rebuild-look v (fgc/conjv target-id-path k) aux changed-only)])))))



(defn- get-empty-aux [] (transient {}))

(defn- clear-root [container]
  (assoc container
    :aux-container (get-empty-aux)
    :has-changes false
    :has-structure-changes false
    :flex-structure-changes nil
    :flex-target-id-paths-added nil
    ;:dirty-rect nil
    ))

(defn evolve-container-private [container target-path target-id-path reason]
  (let [ ;_ (println " evolve-container-private is called with target-id-path len = " (count target-id-path))
         result (reinit-if-needed
                  (evolve-by-dependencies
                    container
                    (clear-root container)
                    target-id-path
                    target-id-path
                    reason
                    nil
                    ""
                    false
                    "root"))
         pass-input-result (if (or (:consumed (:aux-container result)) (= 0 (count target-id-path)))
                             result
                             (evolve-container-private result nil (fgc/drop-lastv target-id-path) reason))]
    pass-input-result))

(defn evolve-container [container target-id-path reason]
  (let [ t (System/currentTimeMillis)]
    (do
      ;(println "\n\n======================--started evolving-----" reason " tgt: " target-id-path "\n\n")
      (let [ result (evolve-container-private (dissoc container :dirty-rect) nil target-id-path reason)
             ;@todo this if reason check is just a temporary solution to skip initialization cycle because of exception (NullPointerException: Font is null)
             with-look (if reason                      ;(and reason (:dirty-rect container))       ; OPTIMIZATION FOR WEB  reason
                         (rebuild-look result [(:id result)] (:aux-container result) true (:dirty-rect container))
                         result)
            changed (get-in result [:aux-container :_changed-paths])]
        (do
          ;(println "Evolved container in " (- (System/currentTimeMillis) t) " millis ")
          (if changed
            (assoc with-look :_changed-paths (persistent! changed))
            with-look)
          )))))

(defn- initialize-all
  ([root-container path-to-target component]
    (let [ target-path (conj path-to-target (:id component))
           evolved-target (assoc component :path-to-target path-to-target)
           fresh-root-container (if (> (count target-path) 1)
                                  (assoc-in root-container (fgc/get-access-key target-path) evolved-target)
                                  evolved-target)]
      (assoc
        evolved-target
        :children (into (array-map) (for [[k v] (:children evolved-target)] [k (initialize-all fresh-root-container (conj path-to-target (:id evolved-target)) v)])))))
  ([root-container] (initialize-all root-container [] root-container)))

(defn- evolve-all-component-only
  ([root-container target-id-path properties]
   (evolve-by-dependencies
     root-container
     root-container
     target-id-path
     target-id-path
     nil
     properties
     ""
     true
     "initialization"))
  ([root-container target-id-path] (evolve-all-component-only root-container target-id-path nil)))

(defn- evolve-all
  ([root-container target-id-path component properties]
    (let [ fresh-root-container (evolve-all-component-only root-container target-id-path properties)]
      (loop [ ret fresh-root-container
              children (seq (:children component))]
        (if children
          (let [ child-pair (first children)
                 child-id (first child-pair)
                 child-component (second child-pair)]
            (recur
              (evolve-all ret (fgc/conjv target-id-path child-id) child-component properties)
              (next children)))
          ret))))
  ([root-container] (evolve-all root-container [(:id root-container)] root-container nil)))

(declare initialize-internal)

(defn- account-struc-changes [container]
  (if (:has-structure-changes container)
    (initialize-internal (initialize-all (dep/setup-dependencies (assoc container :has-structure-changes false))))
    container))

(defn- account-flex-changes [container]
  (if (:flex-structure-changes container)
    (initialize-internal (initialize-all (flex-initialize container)))
    container))

(defn initialize-internal [container]
  (let [ vp (:paths-having-visible-popups container)
         pre (clear-root (assoc container :paths-having-visible-popups (if vp vp #{})))
         res (evolve-all pre)
         flex-changes (:flex-structure-changes res)
         flex-added (:flex-target-id-paths-added res)
        ; TODO use ->
         res1 (account-struc-changes res)
         res2 (update-in res1 [:flex-structure-changes] merge flex-changes)
         res3 (update-in res2 [:flex-target-id-paths-added] concat flex-added)
         re-res (account-flex-changes res3)]
    (rebuild-look re-res [(:id re-res)] (:aux-container re-res) false (:dirty-rect container))))

(defn initialize [container] (initialize-internal (initialize-all (dep/setup-dependencies container))))

(defn- evovle-newly-added [container added-target-id-paths]
  (loop [ ret container
         paths added-target-id-paths]
    (let [ p (first paths)]
      (if p
        (recur
          (let [ evolver-map (get-in ret (fgc/conjv (fgc/get-access-key p) :evolvers))
                properties (if evolver-map (map (fn [[k _]] k) evolver-map))]

            ;TODO 11/13/2014 today I resolved initialization issues and it looks like properties are not needed here
            ;(evolve-all-component-only ret p properties)
            (evolve-all-component-only ret p nil)

            )
          (next paths))
        ret))))

(defn flex-initialize [container]
  (dissoc (->
            (dep/apply-flex-changes container (:flex-structure-changes container))
            (dep/recompute-out-dependents (:flex-structure-changes container))
            (evovle-newly-added (:flex-target-id-paths-added container))) :flex-structure-changes))