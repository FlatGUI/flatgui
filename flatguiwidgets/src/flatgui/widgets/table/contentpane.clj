; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Table widget"
      :author "Denys Lebediev"}
  flatgui.widgets.table.contentpane
                        (:use flatgui.comlogic


                              ;flatgui.widgets.componentbase
                              ;flatgui.widgets.component










                              )
  (:require [flatgui.util.circularbuffer :as cb]
            [flatgui.base :as fg]
            [flatgui.util.matrix :as m]
            [flatgui.inputchannels.awtbase :as inputbase]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.inputchannels.keyboard :as keyboard]
            [flatgui.widgets.table.commons :as tcom]
            [flatgui.widgets.table.vfc :as vfc]
            [flatgui.widgets.scrollpanel]
            [flatgui.widgets.table.cell]
            [flatgui.widgets.table.vfcsorting]
            [flatgui.widgets.table.vfcfiltering]
            [flatgui.widgets.table.vfcgrouping]
            [flatgui.comlogic :as fgc])
  (:import [java.awt.event KeyEvent]))


;;; TODO implement gathering all VFs from all over the table
;;;
;;;
(defn get-all-table-vf-map [_]
  {:filtering flatgui.widgets.table.vfcfiltering/vfcfiltering
   :sorting flatgui.widgets.table.vfcsorting/vfcsorting
   :grouping flatgui.widgets.table.vfcgrouping/vfcgrouping})

(defn- get-cell-component-id [header-id cbuf-index]
  (keyword (str (name header-id) "-" cbuf-index)))

(fg/defevolverfn contentpane-position-matrix-evolver :position-matrix
  (let [header-size (get-property component [:header] :clip-size)
        header-h (m/y header-size)]
    (m/translation-matrix 0 header-h)))

(fg/defevolverfn contentpane-clip-size-evolver :clip-size
  (let [clip-size (flatgui.widgets.scrollpanel/scrollpanelcontent-clip-size-evolver component)
        header-size (get-property component [:header] :clip-size)
        header-h (m/y header-size)]
    (m/defpoint (m/x clip-size) (- (m/y clip-size) header-h))))

(defn- get-viewport-capacity [clip-h row-height] (+ 2 (int (/ clip-h row-height))))

(fg/defevolverfn :visible-screen-rows
  (let [clip-size (get-property component [:this] :clip-size)
        row-height (tcom/get-row-h component nil)
        vpm (get-property component [:this] :viewport-matrix)
        top (- (m/mx-y vpm))
        clip-h (m/y clip-size)
        btm (+ top clip-h)
        viewport-capacity (get-viewport-capacity clip-h row-height)
;        _ (if (= [:main :blotter :table] (:path-to-target component))
;           (println (:path-to-target component) "contentpane evolving :visible-screen-rows clip-h " clip-h " Viewport capacity = " viewport-capacity))
        row-order-count (count (get-property component [:this] :row-order))
        screen-row-count (min row-order-count viewport-capacity)
        top-screen-row (tcom/get-screen-row-at component top)
        first-fully-visible (if (>= (* top-screen-row row-height) top)
                              top-screen-row
                              (inc top-screen-row))
        inbound? (<= (+ top-screen-row screen-row-count) row-order-count)
        first-row (cond
                    (= -1 top-screen-row) 0
                    inbound? top-screen-row
                    :else (- row-order-count screen-row-count))
        ;bottom-bound (dec (+ first-row screen-row-count))
        last-row (dec (+ first-row screen-row-count));(if (> row-order-count screen-row-count) bottom-bound (dec bottom-bound))
        last-fully-visible (if (> (* (inc last-row) row-height) btm)
                             (dec last-row)
                             last-row)
        visible-rows [first-row last-row first-fully-visible last-fully-visible]
        left (- (m/mx-x vpm))
        right (+ left (m/x clip-size))
        header-ids (get-property component [] :header-ids)
        column-x-locations (get-property component [:header] :column-x-locations)
        column-count (count header-ids)
        visible-cols (if column-x-locations
                       (loop [c 0
                              first-col nil
                              last-col nil
                              first-fully-visible-col nil
                              last-fully-visible-col nil]
                         (if (< c column-count)
                           (let [h-id (nth header-ids c)
                                 cx (h-id column-x-locations)
                                 cw (if (< c (dec column-count)) (- ((nth header-ids (inc c)) column-x-locations) cx))]
                             (recur
                               (inc c)
                               (if (and (nil? first-col) (or (nil? cw) (>= (+ cx cw) left))) c first-col)
                               (if (< cx right) c last-col)
                               (if (and (nil? first-fully-visible-col) (>= cx left)) c first-fully-visible-col)
                               (if (not (nil? cw))
                                 (if (< (+ cx cw) right) c last-fully-visible-col)
                                 (if (< cx right) c last-fully-visible-col))))
                           [first-col last-col first-fully-visible-col last-fully-visible-col]))
                       [0 0 0 0])]
    {:rows visible-rows
     :cols visible-cols
     :cbuf (cb/move-cbuf (:cbuf old-visible-screen-rows) first-row last-row)
     :y-locations (mapv #(tcom/get-row-y component %1) (range (nth visible-rows 0) (inc (nth visible-rows 1))))}))


;;; ;;;;;;;;;;; TODO this should go into some generic widget-feature: a widget with flex child set ;;;;;;;;;;;;;;;;;;;;;;

(defn- get-cell-component [component header-id]
  (let [default-component (:default-cell-component component)]
    (if (fn? default-component)
      (default-component header-id)
      default-component)))

(fg/defaccessorfn create-cell-component [component id header-id cbuf-index clone]
                  (let [header-ids (get-property component [] :header-ids)
                        column-index (.indexOf header-ids header-id)
                        row-height (tcom/get-row-h component nil)]
                    (if clone
                      ;; TODO take into account possiblity of having a cell type per column
                      (assoc
                        (flatgui.dependency/clone-component (nth (first (:children component)) 1) id)
                        :header-id header-id
                        :cbuf-index cbuf-index
                        :row-height row-height
                        :screen-col column-index)
                      (fg/defcomponent
                        (get-cell-component component header-id)
                        id
                        {:header-id header-id
                         :cbuf-index cbuf-index
                         :row-height row-height
                         :screen-col column-index}))))

;;; Returns the list of :id,:header-id,cbuf-index maps (a map per each future component)
(defn- fill-child-info-for-rows [from to header-ids]
  (mapcat (fn [h] (for [c (range from to)] {:id (get-cell-component-id h c) :header-id h :cbuf-index c})) header-ids))

(defn- inv-merge
  ([a b] (merge-with concat a b))
  ([a] a)
  ([] {}))

(defn- invert-map [m]
  (if m (do
          ;(println ">>>>>> " (reduce #(merge-with concat %1 %2) (mapcat (fn [[k v]] (for [ve v] {ve [k]})) m)))
          (reduce inv-merge (mapcat (fn [[k v]] (for [ve v] {ve [k]})) m)))))

(defn- self-dependency? [c-path dependency]
  (and
    (= (count dependency) (inc (count c-path)))
    (= c-path (drop-lastv dependency))))

(defn- remove-self-dep-and-prepend-path [m c-path]
  (into {} (map (fn [[k v]] [(conjv c-path k) (filter #(not (self-dependency? c-path %)) v)]) m)))
;(defn- remove-self-dep-and-prepend-path [m c-path]
;  (into {} (map (fn [[k v]] [(conjv c-path k) v]) m)))

(defn- invert-dependencies [c]
  (let [ c-path (conjv (:path-to-target c) (:id c))
         r (->>
             (remove-self-dep-and-prepend-path (:evolver-abs-dependencies c) c-path)
             (invert-map))]
    (do
      ;(println "Single map before inversion: " (:evolver-abs-dependencies c))
      ;(println "Single map no self: " (remove-self-dep-and-prepend-path (:evolver-abs-dependencies c) c-path))
      ;(println "Single inverted map: " r)
      r)))

(defn- assign-cells-to-add [component old-children to-add should-clone]
  (let [new-children (mapcat
                       (fn [e] [(:id e) (create-cell-component
                                          component
                                          (:id e)
                                          (:header-id e)
                                          (:cbuf-index e)
                                          should-clone)])
                       to-add)
        supplied-children (apply assoc old-children new-children)]
    (if should-clone  ; TODO move to dependency namespace?
      (assoc
        supplied-children
        :_flexible-childset-added
        (merge-with
          concat
          (:_flexible-childset-added old-children)
          (do (let [ r (reduce
                         #(merge-with concat %1 %2)
                         (for [c (filter map? new-children)] (invert-dependencies c)))]
                ;(println ">>> " r)
                r))
          )
        :_flex-target-id-paths-added
        (map #(conjv (:path-to-target %) (:id %)) (filter (complement keyword?) new-children)))
      supplied-children)))

(fg/defevolverfn :children
  (let [header-ids (get-property component [] :header-ids)
        column-count (count header-ids)
        current-child-count (fg/get-child-count component)
        current-row-count (/ current-child-count column-count)
        row-height (tcom/get-row-h component nil)
        clip-h (m/y (get-property component [:this] :clip-size))
        new-row-count (get-viewport-capacity clip-h row-height)
        ;id-fn (fn [e] (get-cell-component-id (:header-id e) (:cbuf-index e)))
        should-clone (and (:_flexible-childset component) (> current-child-count 0))]
    (cond
      (> new-row-count current-row-count)
      (let [ to-add (fill-child-info-for-rows current-row-count new-row-count header-ids)]
        (assign-cells-to-add component old-children to-add should-clone)
        ;(apply assoc old-children (mapcat
        ;                            (fn [e] [(:id e) (create-cell-component
        ;                                                 component
        ;                                                 (:id e)
        ;                                                 (:header-id e)
        ;                                                 (:cbuf-index e)
        ;                                                 should-clone)])
        ;                            to-add))
        )
      (< new-row-count current-row-count)
      (let [to-remove (fill-child-info-for-rows new-row-count current-row-count header-ids)]
        (apply dissoc old-children (map (fn [e] (:id e)) to-remove)))
      :else
      old-children)))

;;;; End of TODO


(fg/defevolverfn :row-order
  ;; This is temporary. Need to know which evolvers to invoke on which input event.
  ;; Then this heavy evolver will not be called on each mouse move/click.
  ;; So we evolve either because of another component change (vector) or because of
  ;; inititalization (nil)
  (if (or (vector? (fg/get-reason)) (nil? (fg/get-reason)))
  ;;
  ;;
    (let [header-ids (get-property component [] :header-ids)
          vf-map (get-all-table-vf-map component)
          src-row-order (range 0 (get-property component [:this] :row-count))]
      (loop [cnt 0
             result src-row-order]
        (if (= cnt (count tcom/vf-apply-order))
          result
          (recur
            (inc cnt)
            (let [ vfc-id (nth tcom/vf-apply-order cnt)
                   vfc (vfc-id vf-map)
                   modes (into {} (for [header-id header-ids] [header-id (get-property component [:header header-id vfc-id] :mode)]))

                  ;; TODO !! Without this, :row-order evolver sometimes is not called for up-to-date degrees
                  degrees (into {} (for [header-id header-ids] [header-id (get-property component [:header header-id vfc-id] :degree)]))
                  ;;_ (println "--------------------- :row-order degrees = " degrees)

                   apply-feature (:apply-feature vfc)]
              (if (nil? apply-feature) result (apply-feature component result modes)))))))
    old-row-order))

(fg/defevolverfn :vfc-degree-headers
  ;; This is temporary. Need to know which evolvers to invoke on which input event.
  ;; Then this heavy evolver will not be called on each mouse move/click
  (if (vector? (get-reason))
    ;;
    ;;
    (let [header-ids (get-property component [] :header-ids)
          all-vf-ids (for [[k v] (get-all-table-vf-map component)] k)
          get-degree (fn [header-id vfc-id]
                       (let [ degree (get-property component [:header header-id vfc-id] :degree)]
                         (if (vfc/is-vfc-degree-active degree) degree (vfc/max-degree-value component))))]
      (into {} (for [vf-id all-vf-ids] [vf-id (vec (sort-by #(get-degree %1 vf-id) header-ids))])))
    old-vfc-degree-headers))

(defn- create-single-selection
  ([model-row screen-row model-col screen-col]
    [model-row screen-row (list model-row) (list screen-row) model-col screen-col])
  ([row-order screen-row screen-col]
    (let [anchor-model-row (tcom/screen-row-to-model row-order screen-row)
          anchor-model-col screen-col]
      (create-single-selection anchor-model-row screen-row anchor-model-col screen-col))))

(defn- keep-range [row count]
  (cond
    (< row 0) 0
    (>= row count) (dec count)
    :else row))

(defn- modify-single-selection [row-order rowfn colfn old-selection-model]
  (let [screen-row (tcom/get-screen-row old-selection-model)
        screen-col (tcom/get-anchor-screen-col old-selection-model)]
    (create-single-selection
      row-order
      (keep-range (rowfn screen-row) (count row-order))
      (keep-range (colfn screen-col) 11))));@todo column count

(fg/defaccessorfn get-visible-row-count [component]
  (let [visible-screen-rows (get-property component [:this] :visible-screen-rows)
        first-visible-row (nth (:rows visible-screen-rows) 0)
        last-visible-row (nth (:rows visible-screen-rows) 1)]
    (inc (- last-visible-row first-visible-row))))

;;;TODO use record for selection model?
;;;
(fg/defevolverfn :selection-model
  (cond
    ((get-property component [:this] :mouse-triggers-selection?) component)
    (let [with-ctrl (inputbase/with-ctrl? component)
          with-shift (inputbase/with-shift? component)
          selection-mode (get-property component [:this] :selection-mode)
          row-order (get-property component [:this] :row-order)
          screen-row (tcom/get-screen-row-at component (mouse/get-mouse-rel-y component))
          anchor-model-row (tcom/screen-row-to-model row-order screen-row)
          old-anchor-model-row (tcom/get-anchor-model-row old-selection-model)
          old-screen-row (tcom/get-screen-row old-selection-model)
          old-selected-model-rows (tcom/get-selected-model-rows old-selection-model)
          old-selected-screen-rows (tcom/get-selected-screen-rows old-selection-model)
          anchor-screen-col (tcom/get-screen-col-at component (mouse/get-mouse-rel-x component))
          anchor-model-col anchor-screen-col]
       (cond
         (and with-ctrl (= selection-mode :multiple-interval))
            (if (some #{anchor-model-row} old-selected-model-rows)
              [anchor-model-row screen-row (remove #{anchor-model-row} old-selected-model-rows) (remove #{screen-row} old-selected-screen-rows) anchor-model-col anchor-screen-col]
              [anchor-model-row screen-row (conj old-selected-model-rows anchor-model-row) (conj old-selected-screen-rows screen-row) anchor-model-col anchor-screen-col])
         (and with-shift old-screen-row (= selection-mode :multiple-interval))
            (let [r1 old-screen-row
                  r2 screen-row
                  screen-range (range (min r1 r2) (inc (max r1 r2)))
                  model-range (map #(tcom/screen-row-to-model row-order %1) screen-range)]
              [anchor-model-row screen-row (distinct (into old-selected-model-rows model-range)) (distinct (into old-selected-screen-rows screen-range)) anchor-model-col anchor-screen-col])
         :else (create-single-selection anchor-model-row screen-row anchor-model-col anchor-screen-col)
            ))
    (keyboard/key-event? component)
    (let [row-order (get-property component [:this] :row-order)
          key (keyboard/get-key component)]
      ;;TODO add shift and ctrl support
      (condp = key
        KeyEvent/VK_UP (modify-single-selection row-order dec identity old-selection-model)
        KeyEvent/VK_DOWN (modify-single-selection row-order inc identity old-selection-model)
        KeyEvent/VK_PAGE_UP (modify-single-selection row-order #(- %1 (get-visible-row-count component)) identity old-selection-model)
        KeyEvent/VK_PAGE_DOWN (modify-single-selection row-order #(+ %1 (get-visible-row-count component)) identity old-selection-model)
        KeyEvent/VK_HOME (modify-single-selection row-order (fn [_] 0) identity old-selection-model)
        KeyEvent/VK_END (modify-single-selection row-order (fn [_] (dec (count row-order))) identity old-selection-model)
        KeyEvent/VK_LEFT (modify-single-selection row-order identity dec old-selection-model)
        KeyEvent/VK_RIGHT (modify-single-selection row-order identity inc old-selection-model)
        old-selection-model))
    :else old-selection-model))

(fg/defevolverfn contentpane-size-evolver :content-size
  (let [visible-row-count-dec (dec (count (get-property component [:this] :row-order)))
        column-widths (get-property component [:header] :column-widths)]
    (m/defpoint
      (reduce + (map (fn [[_ v]] v) column-widths))
      (+ (tcom/get-row-y component visible-row-count-dec) (tcom/get-row-h component visible-row-count-dec)))))

(def NO_VAL -1)
(def ABSENT_ANCHOR_REMOTNESS [NO_VAL NO_VAL])

(fg/defevolverfn :anchor-remoteness
  (if (keyboard/key-event? component)
    (let [selection-model (get-property component [:this] :selection-model)
          anchor-row (tcom/get-screen-row selection-model)
          anchor-col (tcom/get-anchor-screen-col selection-model)
          visible-screen-rows (get-property component [:this] :visible-screen-rows)
          first-fully-visible-row (nth (:rows visible-screen-rows) 2)
          last-fully-visible-row (nth (:rows visible-screen-rows) 3)
          first-fully-visible-col (nth (:cols visible-screen-rows) 2)
          last-fully-visible-col (nth (:cols visible-screen-rows) 3)]
      (if anchor-row
        [(cond
           (< anchor-col first-fully-visible-col) anchor-col
           (> anchor-col last-fully-visible-col) (- anchor-col (- last-fully-visible-col first-fully-visible-col))
           :else NO_VAL)
         (cond
           (< anchor-row first-fully-visible-row) anchor-row
           (> anchor-row last-fully-visible-row) (- anchor-row (- last-fully-visible-row first-fully-visible-row))
           :else NO_VAL)]
        ABSENT_ANCHOR_REMOTNESS))
    ABSENT_ANCHOR_REMOTNESS))

(fg/defevolverfn tablecontentpane-viewport-matrix-evolver :viewport-matrix
  (let [anchor-remoteness (get-property component [:this] :anchor-remoteness)]
    (if (and anchor-remoteness (or (not= NO_VAL (nth anchor-remoteness 0)) (not= NO_VAL (nth anchor-remoteness 1))))
      (let [row-height (tcom/get-row-h component nil)
            xr (nth anchor-remoteness 0)
            yr (nth anchor-remoteness 1)
            old-x (m/mx-x old-viewport-matrix)
            old-y (m/mx-y old-viewport-matrix)
            mxx (if (= NO_VAL xr) old-x (let [model-col (tcom/screen-col-to-model xr)
                                              column-x-locations (get-property component [:header] :column-x-locations)
                                              header-ids (get-property component [] :header-ids)
                                              header-id (nth header-ids model-col)]
                                          (- (header-id column-x-locations))))
            mxy (if (= NO_VAL yr) old-y (fgc/round-to (- (* yr row-height)) row-height))]
        (m/translation-matrix mxx mxy))
      (flatgui.widgets.scrollpanel/scrollpanelcontent-viewport-matrix-evolver component))))

(fg/defevolverfn :column-grouping-state
  (let [header-ids (get-property component [] :header-ids)]
    (map #(get-property component [:header %1 :grouping] :degree) header-ids)))

(fg/defwidget "tablecontentpane"
  { ; Vector where each screen row index element contains its model row index
    :row-order nil
    ; Map of VF id to vector of header ids, sorted by corresponding VF degree
    :vfc-degree-headers {}
    ; One of :single, :multiple-interval. TODO implement :single-interval
    :selection-mode :multiple-interval
    ; Function that returns true in case component is processing mouse event that triggers selection change
    :mouse-triggers-selection? (fn [component] (mouse/mouse-left? component))
    ; Model row count
    :row-count 0
    :row-height tcom/default-row-height
    :wheel-rotation-step-y tcom/default-row-height
    :default-cell-component flatgui.widgets.table.cell/tablecell
    :visible-screen-rows {:rows [0 0 0 0]
                          :cols [0 0 0 0]
                          :cbuf (cb/cbuf 0 0)
                          :y-locations nil}
    :selection-model nil
    :column-grouping-state nil
    :background :prime-1
    ; Engine-recorgnized property that allow optimization for components with variable child set.
    ; When true, engine does not recompute dependencies each time a child is added, but rather
    ; clones dependency-related infromation from any existing child, assuming all children are the
    ; same.
    :_flexible-childset true
    :children {
;;;TODO It looks like with :_flexible-childset this is not needed any more
;                :refcell (defcomponent tablecell :refcell {;:header-id nil
;                                                           ;:cbuf-index cbuf-index
;                                                           :row-height 0
;                                                           ;:screen-col column-index
;                                                           })
                }
    :evolvers {:viewport-matrix tablecontentpane-viewport-matrix-evolver
               :row-order row-order-evolver
               :vfc-degree-headers vfc-degree-headers-evolver
               :content-size contentpane-size-evolver
               :position-matrix contentpane-position-matrix-evolver
               :clip-size contentpane-clip-size-evolver
               :visible-screen-rows visible-screen-rows-evolver
               :selection-model selection-model-evolver
               :anchor-remoteness anchor-remoteness-evolver
               :children children-evolver
               :column-grouping-state column-grouping-state-evolver}}
  flatgui.widgets.scrollpanel/scrollpanelcontent)

(defmacro deftablecontent
  ([row-count params]
  `(flatgui.base/defcomponent flatgui.widgets.table.contentpane/tablecontentpane
                              :content-pane (merge-with flatgui.base/properties-merger
                                                        ~params
                                                        {:row-count ~row-count
                                  :row-order (vec (range 0 ~row-count))})))
  ([row-count]
   `(flatgui.base/defcomponent flatgui.widgets.table.contentpane/tablecontentpane
                               :content-pane {:row-count ~row-count
                                 :row-order (vec (range 0 ~row-count))})))