; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Base component for table header cell"
      :author "Denys Lebediev"}
  flatgui.widgets.table.columnheader (:use flatgui.awt
                                           flatgui.comlogic
                                           flatgui.base
                                           flatgui.theme
                                           flatgui.paint
                                           flatgui.widgets.componentbase
                                           flatgui.widgets.component
                                           flatgui.widgets.label
                                           flatgui.widgets.table.commons
                                           flatgui.util.matrix
                                           flatgui.inputchannels.mouse
                                           clojure.test))


;(deflookfn columnheader-look (:theme :mouse-down)
;  (call-look label-look)
;  [(setColor (if mouse-down (:dark theme) (:light theme)))
;   (drawLine 0 0 w- 0)
;   (drawLine w- 0 w- h-)
;   (setColor (if mouse-down (:light theme) (:dark theme)))
;   (drawLine 0 h- w-2 h-)
;   (drawLine w-2 0 w-2 h-)])


(defevolverfn columnheader-position-matrix-evolver :position-matrix
  (let [ header-ids (get-property component [:_] :header-ids)
         column-width (get-property component [] :column-widths)
         id (:id component)]
    (loop [ totalw 0
            i 0]
      (if (< i (count column-width))
        (if (= (nth header-ids i) id)
          (if (not= totalw (mx-x old-position-matrix))
            (mx-setx old-position-matrix totalw)
            old-position-matrix)
          (recur
            (+ totalw (column-width (nth header-ids i)))
            (inc i)))
        old-position-matrix))))


(defevolverfn columnheader-clip-size-evolver :clip-size
  (if (get-property component [] :fit-width)
    (defpoint
      (/ (x (get-property component [] :clip-size)) (count (get-property component [:_] :header-ids)))
      (y old-clip-size))
    old-clip-size))

;:header-aliases

(defevolverfn columnheader-text-evolver :text
 (let [id (get-property component [:this] :id)
       aliases (get-property component [:_] :header-aliases)]
   (if aliases
     (id aliases)
     (name id))))

(defwidget "columnheader"
  { :clip-size (defpoint DFLT_COL_WIDTH DFLT_ROW_HEIGHT)
    :clicked-no-shift false
    :clicked-with-shift false
    :mouse-down false
    :vf-visual-order VF_VISUAL_ORDER
   ;:look columnheader-look
    :skin-key [:table :columnheader]

    ;; TODO move out
    :foreground :prime-1

    :evolvers { :text columnheader-text-evolver
                :clicked-no-shift clicked-no-shift-evolver
                :clicked-with-shift clicked-with-shift-evolver
                :mouse-down mouse-down-evolver
                :clip-size columnheader-clip-size-evolver
                :position-matrix columnheader-position-matrix-evolver}}
  label)

(defmacro defcolumn
  ([id]
    `(defcomponent columnheader ~id {}))
  ([id v-features]
    (let [ begindef (list 'defcomponent 'columnheader id {})
           vfdef (map (fn [vf] (list 'defcomponent (symbol (str "vfc" (name vf))) vf {})) v-features)]
      (concat begindef vfdef)))
  ([id v-features props]
    (let [ begindef (list 'defcomponent 'columnheader id props)
          vfdef (map (fn [vf] (list 'defcomponent (symbol (str "vfc" (name vf))) vf {})) v-features)]
      (concat begindef vfdef))))
