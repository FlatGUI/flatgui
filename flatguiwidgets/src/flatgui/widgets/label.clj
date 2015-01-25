; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Label widget"
      :author "Denys Lebediev"}
  flatgui.widgets.label (:use flatgui.awt
                              flatgui.comlogic
                              flatgui.base
                              flatgui.paint
                              flatgui.widgets.component
                              clojure.test))

(defn label-look-impl [foreground text h-alignment v-alignment left top w h]
  [(flatgui.awt/setColor foreground)
   (let [ dx (condp = h-alignment
               :left (flatgui.awt/halfstrh)
               :right (- w (flatgui.awt/strw text) (flatgui.awt/halfstrh))
               (/ (- w (flatgui.awt/strw text)) 2))
          dy (condp = v-alignment
               :top (+ (flatgui.awt/halfstrh) (flatgui.awt/strh))
               :bottom (- h (flatgui.awt/halfstrh))
               (+ (/ h 2) (flatgui.awt/halfstrh)))]
     (flatgui.awt/drawString text (+ left dx) (+ top dy)))])

(deflookfn label-look (:text :h-alignment :v-alignment)
  (label-look-impl foreground text h-alignment v-alignment 0 0 w h))

(defwidget "label"
  { :v-alignment :center
    :h-alignment :center
    :text ""
    :look label-look}
  component)

;
; Tests
;