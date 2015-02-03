; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.colorchooserwin
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.widgets.panel :as panel]
            [flatgui.widgets.checkbox :as checkbox]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.label :as label]
            [flatgui.widgets.textfield :as textfield]
            [flatgui.widgets.spinner :as spinner]
            [flatgui.widgets.slider :as slider])
  (:import (java.text DecimalFormat)))


(def label-format (DecimalFormat. "###"))


(def color-chooser-window
  (fg/defcomponent window/window :chooser
    {:clip-size (m/defpoint 3.0 7.5)
     :position-matrix (m/transtation 1 1)
     :text "Color Chooser"}

  (fg/defcomponent panel/panel :indicator
    {:clip-size (m/defpoint 2.5 2.0)
     :position-matrix (m/transtation 0.25 0.5)
     :background (awt/color 0 0 0)})

  (fg/defcomponent checkbox/checkbox :gray
    {:clip-size (m/defpoint 1.5 0.25)
     :position-matrix (m/transtation 0.25 2.75)
     :text "Gray"})

  (fg/defcomponent label/label :r-label
    {:clip-size (m/defpoint 0.25 0.25 0)
     :position-matrix (m/transtation 0.25 3.125)
     :text "R"})

  (fg/defcomponent label/label :g-label
    {:clip-size (m/defpoint 0.25 0.25 0)
     :position-matrix (m/transtation 1.375 3.125)
     :text "G"})

  (fg/defcomponent label/label :b-label
    {:clip-size (m/defpoint 0.25 0.25 0)
     :position-matrix (m/transtation 2.5 3.125)
     :text "B"})

  (fg/defcomponent slider/slider :r-slider
    {:clip-size (m/defpoint 0.5 3.0 0)
     :orientation :vertical
     :position-matrix (m/transtation 0.25 3.5)})

  (fg/defcomponent slider/slider :g-slider
    {:clip-size (m/defpoint 0.5 3.0 0)
     :orientation :vertical
     :position-matrix (m/transtation 1.375 3.5)})

  (fg/defcomponent slider/slider :b-slider
    {:clip-size (m/defpoint 0.5 3.0 0)
     :orientation :vertical
     :position-matrix (m/transtation 2.5 3.5)})

  (fg/defcomponent spinner/spinner :r-spinner
    {:clip-size (m/defpoint 0.75 0.375 0)
     :position-matrix (m/transtation 0.125 6.75)
     :step 1}
    ;(fg/defcomponent spinner/spinnereditor :editor {:evolvers {:model r-spinner-evolver}})
    )

  (fg/defcomponent spinner/spinner :g-spinner
    {:clip-size (m/defpoint 0.75 0.375 0)
     :position-matrix (m/transtation 1.125 6.75)
     :step 1})

  (fg/defcomponent spinner/spinner :b-spinner
    {:clip-size (m/defpoint 0.75 0.375 0)
     :position-matrix (m/transtation 2.125 6.75)
     :step 1})

    ))

(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:clip-size (m/defpoint 40 23 0)
     :background (awt/color (float (/ 9 255)) (float (/ 17 255)) (float (/ 26 255)))}
    color-chooser-window))

;
;;
;; Evolvers for Debug Window
;;
;
;
;(fg/defevolverfn s1text :text
;                 (let [ pos (get-property [:slider1] :position)]
;                   (.format label-format pos)))
;
;(fg/defevolverfn s2text :text
;                 (let [ pos (get-property [:slider2] :position)]
;                   (.format label-format pos)))
;
;; @todo
;; super-evovler marco that would take evolver from parent type.
;; Will have to keep the list of parent types in component.
;; Throw exception if more than one found.
;; One more way to call this macro - specifying exactly which type
;; to take evovler from
;
;(fg/defevolverfn s1pos :position
;                 (if (and
;                       (#{[:slider2] [:lock-checkbox]} (fg/get-reason))
;                       (get-property [:lock-checkbox] :pressed))
;                   (get-property [:slider2] :position)
;                   (flatgui.widgets.slider/slider-position-evolver component)))
;
;(fg/defevolverfn s2pos :position
;                 (if (and
;                       (#{[:slider1] [:lock-checkbox]} (fg/get-reason))
;                       (get-property [:lock-checkbox] :pressed))
;                   (get-property [:slider1] :position)
;                   (flatgui.widgets.slider/slider-position-evolver component)))
;
;;
;; Debug Window
;;
;
;(def debug-window (fg/defcomponent w/window :debug {:clip-size (m/defpoint 3.5 5.5)
;                                                    :position-matrix (m/transtation-matrix 10 7)
;                                                    :text "Debug Panel"}
;
;                                   (fg/defcomponent w/label :s1-text
;                                                    { :text "Slider 1:"
;                                                     :clip-size (m/defpoint 1.0 0.5 0)
;                                                     :position-matrix (m/transtation-matrix 0.25 0.5)})
;
;                                   (fg/defcomponent w/label :s2-text
;                                                    { :text "Slider 2:"
;                                                     :clip-size (m/defpoint 1.0 0.5 0)
;                                                     :position-matrix (m/transtation-matrix 0.25 1.0)})
;
;                                   (fg/defcomponent w/label :s1-label
;                                                    { :clip-size (m/defpoint 3 0.5 0)
;                                                     :h-alignment :left
;                                                     :position-matrix (m/transtation-matrix 1.28125 0.5)
;                                                     :evolvers {:text s1text}})
;
;                                   (fg/defcomponent w/label :s2-label
;                                                    { :clip-size (m/defpoint 3 0.5 0)
;                                                     :h-alignment :left
;                                                     :position-matrix (m/transtation-matrix 1.28125 1.0)
;                                                     :evolvers {:text s2text}})
;
;
;                                   (fg/defcomponent w/checkbox :lock-checkbox { :clip-size (m/defpoint 1.5 0.25 0)
;                                                                               :text "Lock sliders"
;                                                                               :position-matrix (m/transtation-matrix 0.25 1.75)})
;
;                                   (fg/defcomponent w/slider :slider1 { :clip-size (m/defpoint 0.5 3.0 0)
;                                                                       :orientation :vertical
;                                                                       :position-matrix (m/transtation-matrix 0.25 2.25)
;                                                                       :evolvers {:position s1pos}})
;
;                                   (fg/defcomponent w/slider :slider2 { :clip-size (m/defpoint 0.5 3.0 0)
;                                                                       :orientation :vertical
;                                                                       :position-matrix (m/transtation-matrix 1.0 2.25)
;                                                                       :evolvers {:position s2pos}})
;
;                                   (fg/defcomponent w/spinner :spn { :clip-size (m/defpoint 1.375 0.375 0)
;                                                                    :position-matrix (m/transtation-matrix 1.625 2.25)})
;
;                                   ;  (fg/defcomponent w/menu :ctx { :clip-size (m/defpoint 4 6 0)
;                                   ;                            :position-matrix (m/transtation-matrix 3.625 2.75)})
;                                   ))
