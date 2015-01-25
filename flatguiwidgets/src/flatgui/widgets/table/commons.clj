; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Table utilities "
      :author "Denys Lebediev"}
  flatgui.widgets.table.commons (:use flatgui.awt
                                   flatgui.comlogic
                                   flatgui.base
                                   flatgui.widgets.componentbase
                                   flatgui.widgets.component
                                   flatgui.inputchannels.awtbase
                                   flatgui.inputchannels.mouse
                                   flatgui.util.matrix
                                   flatgui.util.circularbuffer
                                   clojure.test))

;@todo these should be properties of components with these constants just as default values
;
(def VF_APPLYING_ORDER [:filtering :sorting :grouping])
(def VF_SET (set VF_APPLYING_ORDER))
(def VF_VISUAL_ORDER [:sorting :filtering :grouping])

;@todo
(def DFLT_COL_WIDTH 1.25)
(def DFLT_ROW_HEIGHT 0.375)


(defaccessorfn get-row-y [contentpane screen-row]
  (let [ row-height (get-property contentpane [:this] :row-height)]
    (* screen-row row-height)))

(defaccessorfn get-row-h [contentpane screen-row] (get-property contentpane [:this] :row-height))

(defaccessorfn get-screen-row-at [contentpane y-pos]
  ; only constant height rows are supported for now
  (let [ visible-row (int (/ y-pos (get-row-h contentpane nil)))
         row-order (get-property contentpane [:this] :row-order)]
    (if (< visible-row (count row-order))
      visible-row
      -1)))

(defn screen-row-to-model [row-order screen-row]
  (if (and (>= screen-row 0) (< screen-row (count row-order)))
    (nth row-order screen-row)
    -1))

(defn screen-col-to-model [screen-col]
  screen-col)

(defaccessorfn get-model-row [cell]
  (screen-row-to-model (get-property cell [] :row-order) (get-property cell [:this] :screen-row)))

(defaccessorfn get-screen-col-at [contentpane x-pos]
  (let [ column-x-locations (get-property contentpane [:header] :column-x-locations)
         header-ids (get-property contentpane [] :header-ids)
         cnt (count header-ids)]
    (loop [ i 0
            total-w 0]
      (if (<= i cnt)
        (if (> total-w x-pos)
          (dec i)
          (recur
            (inc i)
            (if (< i (dec cnt))
              (column-x-locations (nth header-ids (inc i)))
              Integer/MAX_VALUE)))
        -1))))

(defn get-model-col [cell]
  (:screen-col cell))

(defn row-visible? [screen-row first-visible-row last-visible-row]
  (and (>= screen-row first-visible-row) (<= screen-row last-visible-row)))

(defn col-visible? [screen-col first-visible-col last-visible-col]
  (and (>= screen-col first-visible-col) (<= screen-col last-visible-col)))


(defaccessorfn should-evolve-header [component]
  (and (not (get-property component [:this] :mouse-down)) (mouse-left? component)))

(defn clicked-no-shift? [component]
  (and (should-evolve-header component) (not (with-shift? component))))

(defn clicked-with-shift? [component]
  (and (should-evolve-header component) (with-shift? component)))

(defn clicked-with-ctrl? [component]
  (and (should-evolve-header component) (with-ctrl? component)))


(defevolverfn :clicked-no-shift
  (clicked-no-shift? component))

(defevolverfn :clicked-with-shift
  (clicked-with-shift? component))

(defevolverfn :clicked-with-ctrl
  (clicked-with-ctrl? component))


(defn get-anchor-model-row [selection-model] (nth selection-model 0))
(defn get-screen-row [selection-model] (nth selection-model 1));@todo rename to get-anchor-screen-row
(defn get-selected-model-rows [selection-model] (nth selection-model 2))
(defn get-selected-screen-rows [selection-model] (nth selection-model 3))
(defn get-anchor-model-col [selection-model] (nth selection-model 4))
(defn get-anchor-screen-col [selection-model] (nth selection-model 5))