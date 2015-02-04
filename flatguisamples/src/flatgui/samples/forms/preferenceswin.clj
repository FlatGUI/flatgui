; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.preferenceswin
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.widgets.radiobutton :as radiobutton]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.label :as label]))

;(radiobutton/defradiogroupevolver skin-group [:some-other :flat])

(radiobutton/defradiogroupevolver theme-group [:dark :light])

(def preferences-window
  (fg/defcomponent window/window :preferences
    {:clip-size (m/defpoint 3.75 1.375)
     :position-matrix (m/transtation 10 7)
     :text "Preferences"}

    ;; TODO uncomment when one more skin is available
    ;(fg/defcomponent label/label :skin-label
    ;  {:text "Skin:"
    ;   :h-alignment :right
    ;   :clip-size (m/defpoint 0.75 0.25)
    ;   :position-matrix (m/transtation 0.125 0.875)})
    ;
    ;(fg/defcomponent radiobutton/radiobutton :oldschool
    ;  {:text "Oldschool"
    ;   :pressed true
    ;   :clip-size (m/defpoint 1.25 0.25)
    ;   :position-matrix (m/transtation 1.0 0.875)
    ;   :evolvers {:pressed skin-group}})
    ;
    ;(fg/defcomponent radiobutton/radiobutton :smooth
    ;  {:text "Smooth"
    ;   :clip-size (m/defpoint 1.0 0.25)
    ;   :position-matrix (m/transtation 2.375 0.875)
    ;   :evolvers {:pressed skin-group}})

    (fg/defcomponent label/label :theme-label
      {:text "Theme:"
       :h-alignment :right
       :clip-size (m/defpoint 0.75 0.25)
       :position-matrix (m/transtation 0.125 0.5)})

    (fg/defcomponent radiobutton/radiobutton :dark
      {:text "Dark"
       :pressed true
       :clip-size (m/defpoint 1.25 0.25)
       :position-matrix (m/transtation 1.0 0.5)
       :evolvers {:pressed theme-group}})

    (fg/defcomponent radiobutton/radiobutton :light
      {:text "Light"
       :clip-size (m/defpoint 1.0 0.25)
       :position-matrix (m/transtation 1.875 0.5)
       :evolvers {:pressed theme-group}})))