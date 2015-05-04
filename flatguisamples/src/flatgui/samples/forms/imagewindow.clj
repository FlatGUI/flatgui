; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.imagewindow
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.widgets.component :as component]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.radiobutton :as radiobutton]
            [flatgui.widgets.panel :as panel]
            [flatgui.paint :as fgp]))

(fgp/deflookfn img-look (:method :image-url :w :h)
  [(fgp/call-look component/component-look)
   (if method
     (case method
       :draw (awt/drawImage image-url 0 0)
       :fit (awt/fitImage image-url 0 0 w h)
       :fill (awt/fillImage image-url 0 0 w h)))])

(radiobutton/defradiogroupevolver radio-group [:draw :fit :fill])

(fg/defevolverfn :method
  (cond
    (get-property [:draw] :pressed) :draw
    (get-property [:fit] :pressed) :fit
    (get-property [:fill] :pressed) :fill
    :else nil))

(def image-window
  (fg/defcomponent
    window/window
    :hello
    {:clip-size (m/defpoint 5.125 5)
     :position-matrix (m/translation 1 1)
     :text "Image Example"}

    (fg/defcomponent
      radiobutton/radiobutton
      :draw
      {:clip-size (m/defpoint 1.75 0.25)
       :text "Draw"
       :position-matrix (m/translation 0.125 0.75)
       :evolvers {:pressed radio-group}})

    (fg/defcomponent
      radiobutton/radiobutton
      :fit
      {:clip-size (m/defpoint 1.75 0.25)
       :text "Fit"
       :position-matrix (m/translation 0.125 1.25)
       :evolvers {:pressed radio-group}})

    (fg/defcomponent
      radiobutton/radiobutton
      :fill
      {:clip-size (m/defpoint 1.75 0.25)
       :text "Fill"
       :position-matrix (m/translation 0.125 1.75)
       :evolvers {:pressed radio-group}})

    (fg/defcomponent
      panel/panel
      :img-holder
      {:image-url "classpath://flatgui/samples/images/icon_FlatGUI_32x32.png"
       :method nil
       :background (awt/color 0 0 0)
       :clip-size (m/defpoint 4.0 4.0)
       :position-matrix (m/translation 1.0 0.5)
       :look img-look
       :evolvers {:method method-evolver}})))

(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:clip-size (m/defpoint 40 20)
     :background (awt/color 9 17 26)}
    image-window))