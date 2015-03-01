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
            [flatgui.widgets.spinner :as spinner]
            [flatgui.widgets.slider :as slider]))


;;;
;;; Spinner evolvers are overriden to receive values from sliders when needed
;;;

(fg/defevolverfn r-spinner-evolver :model
  (if (= (fg/get-reason) [:_ :r-slider])
    (let [num->str (get-property [:this] :num->str)
          slider-val-str (num->str component (int (* 255.0 (- 1.0 (get-property [:_ :r-slider] :position)))))]
      {:text slider-val-str :caret-pos 0 :selection-mark 0})
    (spinner/spinner-model-evovler component)))

(fg/defevolverfn g-spinner-evolver :model
  (if (= (fg/get-reason) [:_ :g-slider])
    (let [num->str (get-property [:this] :num->str)
          slider-val-str (num->str component (int (* 255.0 (- 1.0 (get-property [:_ :g-slider] :position)))))]
      {:text slider-val-str :caret-pos 0 :selection-mark 0})
    (spinner/spinner-model-evovler component)))

(fg/defevolverfn b-spinner-evolver :model
 (if (= (fg/get-reason) [:_ :b-slider])
   (let [num->str (get-property [:this] :num->str)
         slider-val-str (num->str component (int (* 255.0 (- 1.0 (get-property [:_ :b-slider] :position)))))]
     {:text slider-val-str :caret-pos 0 :selection-mark 0})
   (spinner/spinner-model-evovler component)))

;;;
;;; Slider position evolvers are overriden here to "lock"
;;; sliders when 'Gray' check box is checked; and also
;;; to receive manually typed values from spinners
;;;

(defn- str->num [s]
  (if (> (.length s) 0) (Double/valueOf s) 0))

;;; NOTE: rounding is used in slider evolver because otherwise double math inaccuracy
;;; produces false change which triggers re-entrant evolving, which in turn makes
;;; slider/spinner value fall back to previous value when in certain positions.
;;; Presicion of 1000 is used there according to the 1/255 value.
(defn- round [n p] (/ (- (* n p) (mod n p)) p))

(fg/defevolverfn r-slider-evolver :position
  (if (and
        (#{[:g-slider] [:b-slider] [:gray]} (fg/get-reason))
        (get-property [:gray] :pressed))
    (get-property [:g-slider] :position)
    (if (= [:r-spinner :editor] (fg/get-reason))
      (let [spinner-text (:text (get-property [:r-spinner :editor] :model))
            spinner-val (str->num spinner-text)]
        (round (- 1.0 (/ spinner-val 255.0)) 1000))
      (flatgui.widgets.slider/slider-position-evolver component))))

(fg/defevolverfn g-slider-evolver :position
  (if (and
        (#{[:b-slider] [:r-slider] [:gray]} (fg/get-reason))
        (get-property [:gray] :pressed))
    (get-property [:b-slider] :position)
    (if (= [:g-spinner :editor] (fg/get-reason))
      (let [spinner-text (:text (get-property [:g-spinner :editor] :model))
            spinner-val (str->num spinner-text)]
        (round (- 1.0 (/ spinner-val 255.0)) 1000))
      (flatgui.widgets.slider/slider-position-evolver component))))

(fg/defevolverfn b-slider-evolver :position
  (if (and
        (#{[:r-slider] [:g-slider] [:gray]} (fg/get-reason))
        (get-property [:gray] :pressed))
    (get-property [:r-slider] :position)
    (if (= [:b-spinner :editor] (fg/get-reason))
      (let [spinner-text (:text (get-property [:b-spinner :editor] :model))
            spinner-val (str->num spinner-text)]
        (round (- 1.0 (/ spinner-val 255.0)) 1000))
      (flatgui.widgets.slider/slider-position-evolver component))))

;; Takes r g b values from spinners for the color for indicator
(fg/defevolverfn indicator-color-evolver :background
  (let [r (str->num (:text (get-property [:r-spinner :editor] :model)))
        g (str->num (:text (get-property [:g-spinner :editor] :model)))
        b (str->num (:text (get-property [:b-spinner :editor] :model)))]
    (awt/color (int r) (int g) (int b))))

(def color-chooser-window
  (fg/defcomponent window/window :chooser
    {:clip-size (m/defpoint 3.0 7.5)
     :position-matrix (m/translation 1 1)
     :text "Color Chooser"}

  (fg/defcomponent panel/panel :indicator
    {:clip-size (m/defpoint 2.5 2.0)
     :position-matrix (m/translation 0.25 0.5)
     :background (awt/color 0 0 0)
     :evolvers {:background indicator-color-evolver}})

  (fg/defcomponent checkbox/checkbox :gray
    {:clip-size (m/defpoint 1.5 0.25)
     :position-matrix (m/translation 0.25 2.75)
     :text "Gray"})

  (fg/defcomponent label/label :r-label
    {:clip-size (m/defpoint 0.25 0.25 0)
     :position-matrix (m/translation 0.25 3.125)
     :text "R"})

  (fg/defcomponent label/label :g-label
    {:clip-size (m/defpoint 0.25 0.25 0)
     :position-matrix (m/translation 1.375 3.125)
     :text "G"})

  (fg/defcomponent label/label :b-label
    {:clip-size (m/defpoint 0.25 0.25 0)
     :position-matrix (m/translation 2.5 3.125)
     :text "B"})

  (fg/defcomponent slider/slider :r-slider
    {:clip-size (m/defpoint 0.5 3.0 0)
     :orientation :vertical
     :position-matrix (m/translation 0.25 3.5)
     :evolvers {:position r-slider-evolver}})

  (fg/defcomponent slider/slider :g-slider
    {:clip-size (m/defpoint 0.5 3.0 0)
     :orientation :vertical
     :position-matrix (m/translation 1.375 3.5)
     :evolvers {:position g-slider-evolver}})

  (fg/defcomponent slider/slider :b-slider
    {:clip-size (m/defpoint 0.5 3.0 0)
     :orientation :vertical
     :position-matrix (m/translation 2.5 3.5)
     :evolvers {:position b-slider-evolver}})

  (fg/defcomponent spinner/spinner :r-spinner
    {:clip-size (m/defpoint 0.75 0.375 0)
     :position-matrix (m/translation 0.125 6.75)
     :min 0
     :max 255
     :step 1}
    (fg/defcomponent spinner/spinnereditor :editor {:evolvers {:model r-spinner-evolver}}))

  (fg/defcomponent spinner/spinner :g-spinner
    {:clip-size (m/defpoint 0.75 0.375 0)
     :position-matrix (m/translation 1.125 6.75)
     :min 0
     :max 255
     :step 1}
    (fg/defcomponent spinner/spinnereditor :editor {:evolvers {:model g-spinner-evolver}}))

  (fg/defcomponent spinner/spinner :b-spinner
    {:clip-size (m/defpoint 0.75 0.375 0)
     :position-matrix (m/translation 2.125 6.75)
     :min 0
     :max 255
     :step 1}
    (fg/defcomponent spinner/spinnereditor :editor {:evolvers {:model b-spinner-evolver}}))))

(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:clip-size (m/defpoint 40 23 0)
     :background (awt/color 9 17 26)}
    color-chooser-window))