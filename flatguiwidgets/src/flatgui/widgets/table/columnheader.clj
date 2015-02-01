; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Base component for table header cell"
      :author "Denys Lebediev"}
    flatgui.widgets.table.columnheader
  (:use flatgui.comlogic)
  (:require [flatgui.base :as fg]
            [flatgui.util.matrix :as m]
            [flatgui.widgets.table.commons :as tcom]
            [flatgui.widgets.component]
            [flatgui.widgets.label]))


(fg/defevolverfn columnheader-position-matrix-evolver :position-matrix
  (let [header-ids (get-property component [:_] :header-ids)
        column-width (get-property component [] :column-widths)
        id (:id component)]
    (loop [totalw 0
           i 0]
      (if (< i (count column-width))
        (if (= (nth header-ids i) id)
          (if (not= totalw (m/mx-x old-position-matrix))
            (m/mx-setx old-position-matrix totalw)
            old-position-matrix)
          (recur
            (+ totalw (column-width (nth header-ids i)))
            (inc i)))
        old-position-matrix))))

(fg/defevolverfn columnheader-clip-size-evolver :clip-size
  (if (get-property component [] :fit-width)
    (defpoint
      (/ (x (get-property component [] :clip-size)) (count (get-property component [:_] :header-ids)))
      (y old-clip-size))
    old-clip-size))

(fg/defevolverfn columnheader-text-evolver :text
 (let [id (get-property component [:this] :id)
       aliases (get-property component [:_] :header-aliases)]
   (if aliases
     (id aliases)
     (name id))))

(fg/defwidget "columnheader"
  {:clip-size (defpoint tcom/default-col-width tcom/default-row-height)
   :clicked-no-shift false
   :clicked-with-shift false
   :mouse-down false
   :vf-visual-order tcom/vf-visual-order
   :skin-key [:table :columnheader]
   ;; TODO move out
   :foreground :prime-1
   :evolvers {:text columnheader-text-evolver
              :clicked-no-shift tcom/clicked-no-shift-evolver
              :clicked-with-shift tcom/clicked-with-shift-evolver
              :mouse-down flatgui.widgets.component/mouse-down-evolver
              :clip-size columnheader-clip-size-evolver
              :position-matrix columnheader-position-matrix-evolver}}
  flatgui.widgets.label/label)

(defmacro defcolumn
  ([id]
    `(flatgui.base/defcomponent flatgui.widgets.table.columnheader/columnheader ~id {}))
  ([id v-features]
    (let [begindef (list 'flatgui.base/defcomponent 'flatgui.widgets.table.columnheader/columnheader id {})
          vfdef (map (fn [vf] (list 'flatgui.base/defcomponent (symbol (str "vfc" (name vf))) vf {})) v-features)]
      (concat begindef vfdef)))
  ([id v-features props]
    (let [begindef (list 'flatgui.base/defcomponent 'flatgui.widgets.table.columnheader/columnheader id props)
          vfdef (map (fn [vf] (list 'flatgui.base/defcomponent (symbol (str "vfc" (name vf))) vf {})) v-features)]
      (concat begindef vfdef))))