; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Table utilities "
      :author "Denys Lebediev"}
    flatgui.widgets.table.commons
  (:require [flatgui.base :as fg]
            [flatgui.inputchannels.awtbase :as inputbase]
            [flatgui.inputchannels.mouse :as mouse]))


;;;TODO these should be properties of components with these constants just as default values
;;;
(def vf-apply-order [:filtering :sorting :grouping])
(def vf-visual-order [:sorting :filtering :grouping])
(def default-col-width 1.25)
(def default-row-height 0.375)

(fg/defaccessorfn get-row-y [component screen-row]
  (let [row-height (get-property component [:this] :row-height)]
    (* screen-row row-height)))

(fg/defaccessorfn get-row-h [component screen-row] (get-property component [:this] :row-height))

(fg/defaccessorfn get-screen-row-at [component y-pos]
  ; only constant height rows are supported for now
  (let [visible-row (int (/ y-pos (get-row-h component nil)))
        row-order (get-property component [:this] :row-order)]
    (if (< visible-row (count row-order))
      visible-row
      -1)))

(defn screen-row-to-model [row-order screen-row]
  (if (and (>= screen-row 0) (< screen-row (count row-order)))
    (nth row-order screen-row)
    -1))

(defn screen-col-to-model [screen-col]
  screen-col)

(fg/defaccessorfn get-model-row [component]
  (screen-row-to-model (get-property component [] :row-order) (get-property component [:this] :screen-row)))

(fg/defaccessorfn get-screen-col-at [component x-pos]
  (let [column-x-locations (get-property component [:header] :column-x-locations)
        header-ids (get-property component [] :header-ids)
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

(fg/defaccessorfn should-evolve-header [component] (mouse/mouse-released? component))

(fg/defaccessorfn clicked-no-shift? [component]
  (and (should-evolve-header component) (not (inputbase/with-shift? component))))

(fg/defaccessorfn clicked-with-shift? [component]
  (and (should-evolve-header component) (inputbase/with-shift? component)))

(fg/defaccessorfn clicked-with-ctrl? [component]
  (and (should-evolve-header component) (inputbase/with-ctrl? component)))

(fg/defevolverfn :clicked-no-shift (clicked-no-shift? component))

(fg/defevolverfn :clicked-with-shift
  (clicked-with-shift? component))

(fg/defevolverfn :clicked-with-ctrl
  (clicked-with-ctrl? component))

(defn get-anchor-model-row [selection-model] (nth selection-model 0))

(defn get-screen-row [selection-model] (nth selection-model 1));TODO rename to get-anchor-screen-row

(defn get-selected-model-rows [selection-model] (nth selection-model 2))

(defn get-selected-screen-rows [selection-model] (nth selection-model 3))

(defn get-anchor-model-col [selection-model] (nth selection-model 4))

(defn get-anchor-screen-col [selection-model] (nth selection-model 5))