; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc    "Utilities for arranging container component layout."
      :author "Denys Lebediev"}
  flatgui.layout
    (:require [flatgui.base :as fg]
      [flatgui.awt :as awt]
      [flatgui.util.matrix :as m] [flatgui.base :as fg])
  (:import (java.util.regex Pattern)))


(def gap 0.0625)

(def component-min-size (m/defpoint 0.375 0.375))

(def cmd->smile {:h-stretch :-
                 :v-stretch :|
                 :top-align :'
                 :btm-align :.
                 :l-align :<
                 :r-align :>})

(def axis-switch {\- \|   ; "stretcher by x flag" -> "stretcher by y flag"
                  \< \'}) ; "beginner by x flag" -> "beginner by y flag"

(def smile-pattern (Pattern/compile "(\\||\\-|\\.|'|\\<|\\>)+"))

(defn smile? [kw] (and (keyword? kw) (.matches (.matcher smile-pattern (name kw)))))

(defn- combine-flags
  ([] "")
  ([a] a)
  ([a b] (str a b)))

;;; TODO take into account button visibility
(fg/defaccessorfn get-child-preferred-size [component child-id]
  (let [abs-pref-size (if-let [own-pref-size (get-property component [:this child-id] :preferred-size)]
                        own-pref-size
                        (if-let [text (get-property component [:this child-id] :text)]
                          (let [interop (get-property component [:this child-id] :interop)]
                               (if (get-property component [:this child-id] :multiline)
                                 (awt/get-text-preferred-size
                                   (if-let [lines (:lines (get-property component [:this child-id] :multiline))] lines [text])
                                   interop)
                                 (awt/get-text-preferred-size [text] interop)))
                          component-min-size))
        container-size (get-property component [:this] :clip-size)]
    (m/defpoint
      (/ (m/x abs-pref-size) (m/x container-size))
      (/ (m/y abs-pref-size) (m/y container-size)))))

(fg/defaccessorfn get-child-minimum-size [component child-id]
  (let [abs-min-size (if-let [own-min-size (get-property component [:this child-id] :minimum-size)]
                       own-min-size
                       component-min-size)
        container-size (get-property component [:this] :clip-size)]
    (m/defpoint
      (/ (m/x abs-min-size) (m/x container-size))
      (/ (m/y abs-min-size) (m/y container-size)))))

(declare cfg->flags)

(defn- cfg->flags-mapper [v]
  (cond

    (keyword? v)
    {:element v :flags nil}

    (sequential? v)
    (let [grouped (group-by smile? v)
          elements (get grouped false)
          elem-count (count elements)
          smiles (get grouped true)
          flags (if smiles (reduce combine-flags (map name smiles)))]
        (cond
          (> elem-count 1) (cfg->flags (mapv (fn [e] (if flags [e (keyword flags)] [e])) elements))
          (and (= elem-count 1) (sequential? (nth elements 0))) (nth (cfg->flags elements) 0)
          (= elem-count 1) {:element (nth elements 0) :flags flags}
          (= elem-count 0) (throw (IllegalArgumentException. (str "Wrong layout cfg (nothing but flags specified): " v)))))

   :else
   (throw (IllegalArgumentException. (str "Wrong layout cfg: '" (if (nil? v) "<nil>" v) "'")))))

(defn cfg->flags [cfg]
  (if (and (sequential? cfg) (every? keyword? cfg))
    (cfg->flags-mapper cfg)
    (mapv cfg->flags-mapper cfg)))

(declare flattenmap)

(defn- node? [e] (and (coll? e) (not (map? e))))

(defn- flatten-mapper [e]
  (if (node? e) (flattenmap e) [e]))

(defn flattenmap [coll]
  (mapcat flatten-mapper coll))

(defn- remove-intermediate-data [m] (dissoc m :stch-weight :total-stch-weight))

(defn flagnestedvec->coordmap [flags]
  (into {} (map (fn [flg] [(:element flg) (remove-intermediate-data flg)]) (flattenmap flags))))

(fg/defaccessorfn assoc-constraints [component cfg-table stcher]
  (let [with-abs-weights (map
                           (fn [cfg-row] (map
                                           #(assoc % :stch-weight (count (filter (fn [f] (= f stcher)) (:flags %))))
                                           (flattenmap (cfg->flags cfg-row))))
                           cfg-table)
        row-w-mapper (fn [cfg-row] (reduce + (map #(:stch-weight %) cfg-row)))
        total-stch-weight (reduce + (map row-w-mapper with-abs-weights))
        with-total-weights (mapv
                             (fn [cfg-row] (map
                                             #(assoc %
                                                     :min (get-child-minimum-size component (:element %))
                                                     :pref (get-child-preferred-size component (:element %))
                                                     :stch-weight (if (pos? total-stch-weight)
                                                                    (/ (:stch-weight %) total-stch-weight)
                                                                    0))
                                             cfg-row))
                             with-abs-weights)
        stch-total-weights (mapv row-w-mapper with-total-weights)]
       (map
         (fn [row-index]
           (let [row (nth with-total-weights row-index)
                 row-stch-w (nth stch-total-weights row-index)]
             (map #(assoc % :stch-weight (if (pos? row-stch-w) (* (:stch-weight %) (/ 1 row-stch-w)) 0)) row)))
         (range 0 (count cfg-table)))))

(defn nth-if-present [coll index not-present] (if (< index (count coll)) (nth coll index) not-present))

(defn gen-vector-op [f]
  (fn
    ([a b]
      (let [gv (fn [v i] (nth-if-present v i 0))]
           (mapv #(f (gv a %) (gv b %)) (range 0 (max (count a) (count b))))))
    ([a] a)
    ([] [])))

(def vmax (gen-vector-op max))

(defn compute-x-dir [cfg-table]
  (let [weight-table (map (fn [cfg-row] (mapv #(:stch-weight %) cfg-row)) cfg-table)
        column-weights (vec (reduce vmax weight-table))]
    (mapv
      (fn [cfg-row] (mapv #(assoc (nth cfg-row %) :total-stch-weight (nth column-weights %)) (range 0 (count cfg-row))))
      cfg-table)))

(defn compute-y-dir [cfg-table]
  (let [row-weights (mapv (fn [cfg-row] (reduce max (map #(:stch-weight %) cfg-row))) cfg-table)]
    (mapv
      (fn [row-index]
        (let [cfg-row (nth cfg-table row-index)]
          (mapv #(assoc (nth cfg-row %) :total-stch-weight (nth row-weights row-index)) (range 0 (count cfg-row)))))
      (range 0 (count cfg-table)))))

(fg/defaccessorfn map-direction [component dir stcher bgner sfn coord-key size-key]
  (let [stretches? (fn [element] (true? (and (:flags element) (.contains (:flags element) (str stcher)))))
        grouped-by-stretch (group-by stretches? dir)
        ;_ (println "grouped-by-stretch" grouped-by-stretch)
        stretching (get grouped-by-stretch true)
        stable (get grouped-by-stretch false)
        stable-pref-total (reduce + (map #(sfn (:pref %)) stable))
        stretching-min-total (reduce + (map #(sfn (:min %)) stretching))]
       (if (< (+ stable-pref-total stretching-min-total) 1.0)
         (let [stretch-space (- 1.0 stable-pref-total)
               index-range (range 0 (count dir))
               sizes (mapv #(if (stretches? %) (* (:stch-weight %) stretch-space) (sfn (:pref %))) dir)
               total-sizes (mapv #(if (stretches? %) (* (:total-stch-weight %) stretch-space) (sfn (:pref %))) dir)
               coords (mapv #(reduce + (take % total-sizes)) index-range)]
           (map #(assoc (nth dir %) coord-key (nth coords %) size-key (nth sizes %)) index-range))

         ;; TODO maybe one of three container policies when there is lack of space
         ;;  1. shrink
         ;;  2. ignore
         ;;  3. line-wrap
         (let [_ (println " <<<<TODO space lack>>>>")]
              nil))))

(defn rotate-table [t]
  (let [row-count (count t)
        column-count (reduce max (map count t))]
    (mapv (fn [col-index] (mapv (fn [row] (nth-if-present row col-index nil)) t)) (range 0 column-count))))

(fg/defevolverfn :coord-map
  (if-let [usr-layout (get-property [:this] :layout)]
    (let [;TODO this does not work layout (if usr-layout (map cmd->smile usr-layout))
          layout usr-layout
          x-coord-map (map
                        #(map-direction component % \- \< m/x :x :w)
                        (compute-x-dir (assoc-constraints component layout \-)))
          y-coord-map (map
                        #(map-direction component % \| \' m/y :y :h)
                        (rotate-table (compute-y-dir (assoc-constraints component layout \|))))]
      (merge-with merge
        (flagnestedvec->coordmap x-coord-map)
        (flagnestedvec->coordmap y-coord-map)))))

(fg/defevolverfn :position-matrix
  (if-let [coord-map (get-property [] :coord-map)]
    (if-let [coord ((:id component) coord-map)]
      (let [ps (get-property [] :clip-size)]
        (m/translation (+ gap (* (:x coord) (m/x ps))) (+ gap (* (:y coord (m/y ps))))))
      old-position-matrix)
    old-position-matrix))

(fg/defevolverfn :clip-size
  (if-let [coord-map (get-property [] :coord-map)]
    (if-let [coord ((:id component) coord-map)]
      (let [ps (get-property [] :clip-size)]
        (m/defpoint (- (* (:w coord) (m/x ps)) gap gap) (- (* (:h coord (m/y ps))) gap gap)))
      old-clip-size)
    old-clip-size))


;;; 1. After map-direction is used for elements of each row, each row will
;;; receive it's own set of smile flags. Then map-direction will be used
;;; for rows
;;; 2. Split all elements of a direction into two sub-categories:
;;;   a) elements that do not want extra space
;;;   b) stretching elements that want extra space
;;; See how much space totally (a) elements plus minimum sizes of (b) elements want.
;;;   - if more than avaialble then
;;;      - either rescale everything accorrding to previous proportions
;;;      - or if there was nothing previously, distribute equally
;;;   - if less than or equal to available:
;;;      - each (a) component receives just what it needs
;;;      - what remains is distributed between (b) components
;;;          - either equally if nothing else specified
;;;          - or according to specified weights (may be specified with digits or
;;;            with smile lengths like :--- or :|||)
