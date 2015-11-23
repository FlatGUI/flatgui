; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.sliderdemowindow
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.theme]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.button :as button]
            [flatgui.widgets.slider :as slider]
            [flatgui.widgets.panel :as panel]))


(def slider-window
  (fg/defcomponent
    window/window
    :sliderdemo
    {:clip-size (m/defpoint 5.125 5.5)
     :position-matrix (m/translation 1 1)
     :text "Slider rendering test"
     :layout [[[:slider :---|] ]]}                          ;[:button :-|]

    (fg/defcomponent slider/slider :slider {})

    ;(fg/defcomponent button/button :button {})
    ))

(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:theme flatgui.theme/dark
     :clip-size (m/defpoint 16 10)
     :background (awt/color 9 17 26)
     :font "14px sans-serif"

     ;; TODO this should be a part defroot probably
     :closed-focus-root true
     :focus-state {:mode :has-focus
                   :focused-child nil}}
    slider-window))