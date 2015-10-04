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
      [flatgui.util.matrix :as m]
      [flatgui.util.rectmath :as r]
      [flatgui.util.decimal :as d])
  (:import (java.util.regex Pattern)))


;(def gap 0.0625)
(def gap (* (awt/px) 2))

(def component-min-size (m/defpoint 0.375 0.375))

(def component-no-size (m/defpoint 0 0))

;; TODO ' . < >  should work only when there are no stcheching component in the row. Otherwise stretching component(s) take all space

(def cmd->smile {:h-stretch :-
                 :v-stretch :|
                 :top-align :'
                 :btm-align :.
                 :l-align :<
                 :r-align :>})

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

(defn- gen-v-reducer [xfn yfn]
  (fn ([] (m/defpoint 0 0))
      ([v] v)
      ([a b] (m/defpoint (xfn (m/x a) (m/x b)) (yfn (m/y a) (m/y b))))))

(fg/defaccessorfn get-element-preferred-size [component element xfn yfn]
  (if (keyword? element)
    (get-child-preferred-size component element)
    (let [;_ (println "sizes: " (map #(get-element-preferred-size component (:element %) xfn yfn) element) )
          ]
      (reduce (gen-v-reducer xfn yfn) (map #(get-element-preferred-size component (:element %) xfn yfn) element)))))

(fg/defaccessorfn get-element-minimum-size [component element xfn yfn]
  (if (keyword? element)
    (get-child-minimum-size component element)
    (reduce (gen-v-reducer xfn yfn) (map #(get-element-minimum-size component (:element %) xfn yfn) element))))

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
          (> elem-count 1) {:element (cfg->flags (mapv (fn [e] (if flags [e (keyword flags)] [e])) elements)) :flags flags}
          (and (= elem-count 1) (sequential? (nth elements 0))) (nth (cfg->flags elements) 0)
          (= elem-count 1) {:element (nth elements 0) :flags flags}
          (= elem-count 0) (throw (IllegalArgumentException. (str "Wrong layout cfg (nothing but flags specified): " v)))))

   :else
   (throw (IllegalArgumentException. (str "Wrong layout cfg: '" (if (nil? v) "<nil>" v) "'")))))

(defn cfg->flags [cfg]
  (cond

    (and (sequential? cfg) (every? keyword? cfg) (not (some smile? cfg)))
    (cfg->flags (mapv (fn [kw] [kw]) cfg))

    (and (sequential? cfg) (every? keyword? cfg))
    (cfg->flags-mapper cfg)

    (and (sequential? cfg) (some smile? cfg))
    (let [grouped (group-by smile? cfg)
          elements (get grouped false)
          smiles (get grouped true)
          flags (if smiles (reduce combine-flags (map name smiles)))]
         {:element (cfg->flags (mapv (fn [e]
                                         (if (keyword? e)
                                           (if flags [e (keyword flags)] [e])
                                           e))
                                     elements))
          :flags flags})

    :else
    (mapv cfg->flags-mapper cfg)))

(declare flattenmap)

(defn- node? [e] (and (coll? e) (not (map? e))))

(defn- flatten-mapper [e]
  (if (node? e) (flattenmap e) [e]))

(defn flattenmap [coll]
  (mapcat flatten-mapper coll))

(defn- remove-intermediate-data [m] (dissoc m :stch-weight :total-stch-weight :total-stable-pref))

(defn flagnestedvec->coordmap [flags]
      (let [fm (flattenmap flags)
            grouped (group-by :element fm)]
           ;; In addition to storing all data to map by component id, combine multiple cells
           ;; possibly taken by single component: compute one big area from them
           (into {}
                 (map
                   (fn [[k v]]
                       (let [cm (remove-intermediate-data (first v))]
                            (if (:x cm)
                              (let [cx (apply min (map :x v))
                                    rmost (apply max-key (fn [e] (+ (:x e) (:w e))) v)]
                                   [k (assoc cm :x cx :w (- (+ (:x rmost) (:w rmost)) cx))])
                              (let [cy (apply min (map :y v))
                                    bmost (apply max-key (fn [e] (+ (:y e) (:h e))) v)]
                                   [k (assoc cm :y cy :h (- (+ (:y bmost) (:h bmost)) cy))]))))
                   grouped))))

(fg/defaccessorfn assoc-row-constraints [component cfg-row stcher xfn yfn]
  (map
    #(assoc
      %
      :stch-weight (count (filter (fn [f] (= f stcher)) (:flags %)))
      :min (get-element-minimum-size component (:element %) xfn yfn)
      :pref (get-element-preferred-size component (:element %) xfn yfn))
    cfg-row))

(fg/defaccessorfn assoc-constraints [component cfg-table stcher xfn yfn]
  (map (fn [cfg-row] (assoc-row-constraints component (cfg->flags cfg-row) stcher xfn yfn)) cfg-table))

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
        column-weights (vec (reduce vmax weight-table))
        column-count (count column-weights)
        stable-size-table (map (fn [cfg-row] (mapv #(m/x (if (:pref %) (:pref %) component-no-size)) cfg-row)) cfg-table)
        column-stable-sizes (vec (reduce vmax stable-size-table))
        total-column-weight (reduce + column-weights)
        coeff (if (pos? total-column-weight) (/ 1 total-column-weight) 1)
        norm-column-weights (if (pos? total-column-weight) (map #(* % coeff) column-weights) column-weights)]
    (mapv
      (fn [cfg-row] (mapv
                      #(assoc
                        (if (< % (count cfg-row)) (nth cfg-row %) {:min component-no-size :pref component-no-size})
                        :total-stch-weight (nth norm-column-weights %)
                        :total-stable-pref (nth column-stable-sizes %)
                        :stch-weight (if (< % (count cfg-row)) (* coeff (:stch-weight (nth cfg-row %))) 0))
                      (range 0 column-count)))
      cfg-table)))

(defn compute-y-dir [cfg-table]
  (let [row-weights (mapv (fn [cfg-row] (reduce max (map #(:stch-weight %) cfg-row))) cfg-table)
        row-stable-sizes (mapv (fn [cfg-row] (reduce max (map #(m/y (if (:pref %) (:pref %) component-min-size)) cfg-row))) cfg-table)
        column-count (reduce max (map count cfg-table))
        total-row-weight (reduce + row-weights)
        coeff (if (pos? total-row-weight) (/ 1 total-row-weight) 1)]
    (mapv
      (fn [row-index]
        (let [cfg-row (nth cfg-table row-index)
              row-size (count cfg-row)]
          (mapv
            #(assoc
              (if (< % row-size) (nth cfg-row %) {:min component-no-size :pref component-no-size})
              :total-stch-weight (* coeff (nth row-weights row-index))
              :total-stable-pref (nth row-stable-sizes row-index)
              :stch-weight (* coeff (if (< % row-size) (:stch-weight (nth cfg-row %)) 0)))
            (range 0 column-count))))
      (range 0 (count cfg-table)))))

(defn has-flag? [element flg] (true? (and (:flags element) (.contains (:flags element) (str flg)))))

(fg/defaccessorfn map-direction [component dir stcher bgner ender sfn coord-key size-key]
  (let [stretches? (fn [element] (has-flag? element stcher))
        begins? (fn [element] (has-flag? element bgner))
        ends? (fn [element] (has-flag? element ender))
        grouped-by-stretch (group-by (fn [e] (pos? (:total-stch-weight e))) dir) ; Meaning whole column/row stretch (if there is at least one stretching element)
        stretching (get grouped-by-stretch true)
        stable (get grouped-by-stretch false)
        stable-pref-total (reduce + (map #(:total-stable-pref %) stable))
        stretching-min-total (reduce + (map #(sfn (:min %)) stretching))]
       (if (< (+ stable-pref-total stretching-min-total) 1.0)
         (let [stretch-space (- 1.0 stable-pref-total)
               index-range (range 0 (count dir))
               sizes (mapv #(if (stretches? %) (* (:stch-weight %) stretch-space) (sfn (:pref %))) dir)
               total-sizes (mapv #(if (pos? (:total-stch-weight %)) (* (:total-stch-weight %) stretch-space) (:total-stable-pref %)) dir)
               coords (mapv #(reduce + (take % total-sizes)) index-range)
               shifts (mapv
                        #(let [extra (- (nth total-sizes %) (nth sizes %))]
                           (if (pos? extra)
                             (cond
                               (ends? (nth dir %)) extra
                               (not (begins? (nth dir %))) (/ extra 2)
                               :esle 0)
                             0))
                        index-range)
               _ (println "shifts = " (mapv #(str stcher (:element (nth dir %)) "-" (nth shifts %)) index-range))]
              (map #(assoc (nth dir %) coord-key (+ (nth coords %) (nth shifts %)) size-key (nth sizes %)) index-range)
              ;(map #(assoc (nth dir %) coord-key (nth coords %) size-key (nth sizes %)) index-range)
              )

         ;; TODO maybe one of three container policies when there is lack of space
         ;;  1. shrink
         ;;  2. ignore
         ;;  3. line-wrap
         (let [_ (println " <<<<TODO space lack>>>>")]
              nil))))

(defn rotate-table [t]
  (let [column-count (reduce max (map count t))]
    (mapv (fn [col-index] (mapv (fn [row] (nth-if-present row col-index nil)) t)) (range 0 column-count))))

(fg/defaccessorfn map-row-nested-x [component cfg-row]
  (map
    #(let [e (:element %)]
      (if (coll? e)
        (assoc
          %
          :element
          (map-row-nested-x
            component
            (let [mdir (vec (map-direction component (first (compute-x-dir [(assoc-row-constraints component e \- + max)])) \- \< \> m/x :x :w))
                  irange (range 0 (count mdir))
                  pref-widths (mapv (fn [nested-e] (if (pos? (:stch-weight nested-e)) (* (:w nested-e) (:w %)) (:w nested-e))) mdir)
                  total-pref-w (reduce + pref-widths)
                  min-widths (if (> total-pref-w (:w %))
                               (let [n-stretching (count (filter (fn [nested-e] (pos? (:stch-weight nested-e))) mdir))
                                     cut-share (if (pos? n-stretching) (max 0 (/ (- total-pref-w (:w %)) n-stretching)) 0)]
                                 (mapv (fn [i] (if (pos? (:stch-weight (nth mdir i))) (- (nth pref-widths i) cut-share) (nth pref-widths i)) ) irange))
                               pref-widths)
                  min-preceding-width (mapv (fn [i] (reduce + (take i min-widths))) irange)]
              (map
                (fn [i]
                  ;; Scale nested components sizes to the width of column in which they are placed, but scale only ones that are stretchable.
                  ;; Shift all components to the beginning of their column.
                  ;; Apply scaled shift only if there are stretchable components to the left of the processed one.
                  (let [nested-e (nth mdir i)]
                    (if (pos? (:stch-weight nested-e))
                      (assoc
                        nested-e
                        :x (+ (nth min-preceding-width i) (:x %))
                        :w (nth min-widths i))
                      (assoc
                        nested-e
                        :x (+ (nth min-preceding-width i) (:x %))))))
                irange))))
        %))
    cfg-row))

(fg/defaccessorfn map-nested-x [component cfg-table]
  (map (fn [cfg-row] (map-row-nested-x component cfg-row)) cfg-table))

(defn- flatten-mapped-nested [cfg-table]
  (map
    (fn [cfg-row] (mapcat #(if (coll? (:element %)) (:element %) [%]) cfg-row))
    cfg-table))

(defn- process-y-compound-keys [cfg-map]
  (let [compounds (filter (fn [[k _]] (coll? k)) cfg-map)
        unrolled (into {}
                   (mapcat
                     (fn [[k v]]
                         (map (fn [e] [(:element e) v]) k))
                     compounds))
        ;_ (println "unrolled:" unrolled)
        ;_ (println "cfg-map" cfg-map)
        ]
    (into {}
      (map
        (fn [[k v]] [k (if-let [kdata (get unrolled k)] (assoc v :y (:y kdata) :h (:h kdata))  v)])
        cfg-map))))

(fg/defevolverfn :coord-map
  (if-let [usr-layout (get-property [:this] :layout)]
    (let [;TODO this does not work layout (if usr-layout (map cmd->smile usr-layout))
          layout usr-layout

          ;_ (println "X-constr" (assoc-constraints component layout \- + max))
          ;_ (println "X-compute" (compute-x-dir (assoc-constraints component layout \- + max)))

          x-coord-map (map
                        #(map-direction component % \- \< \> m/x :x :w)
                        (compute-x-dir (assoc-constraints component layout \- + max)))

          ;_ (println "X-coord-map" x-coord-map)
          ;_ (println "X-map-nested-x" (map-nested-x component x-coord-map))
          ;_ (println "X-map-nested-x-fl" (flatten-mapped-nested (map-nested-x component x-coord-map)))

          x-map-with-nested (flatten-mapped-nested (map-nested-x component x-coord-map))
          y-coord-map (map
                        #(map-direction component % \| \' \. m/y :y :h)
                        (rotate-table (compute-y-dir (assoc-constraints component layout \| + max))))]
      (process-y-compound-keys
        (merge-with merge
                    (flagnestedvec->coordmap x-map-with-nested)
                    (flagnestedvec->coordmap y-coord-map))))))

(fg/defevolverfn :position-matrix
  (if-let [coord-map (get-property [] :coord-map)]
    (if-let [coord ((:id component) coord-map)]
      (let [ps (get-property [] :clip-size)
            btop 0.375;TODO see 0.375 - window border
            bbtm 0.0
            bleft 0.0
            bright 0.0]
        (m/translation
          (d/round-granular (+ bleft gap (* (:x coord) (- (m/x ps) bleft bright))) (awt/px))
          (d/round-granular (+ btop gap (* (:y coord) (- (m/y ps) btop bbtm))) (awt/px))))
      old-position-matrix)
    old-position-matrix))

(fg/defevolverfn :clip-size
  (if-let [coord-map (get-property [] :coord-map)]
    (if-let [coord ((:id component) coord-map)]
      (let [ps (get-property [] :clip-size)
            btop 0.375;TODO see 0.375 - window border
            bbtm 0.0
            bleft 0.0
            bright 0.0]
        (m/defpoint
          (d/round-granular (- (* (:w coord) (- (m/x ps) bleft bright)) gap gap) (awt/px))
          (d/round-granular (- (* (:h coord) (- (m/y ps) btop bbtm)) gap gap) (awt/px))))
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
