; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns compound
  (:require [flatgui.app]
            [flatgui.skins.flat]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.util.matrix :as m]
            [flatgui.widgets.label :as label]
            [flatgui.widgets.panel :as panel]
            [flatgui.widgets.radiobutton :as radiobutton]
            [flatgui.widgets.checkbox :as checkbox]
            [flatgui.samples.forms.hellowindow :as hw]
            [flatgui.samples.forms.colorchooserwin :as cc]
            [flatgui.samples.forms.orderticketwin :as ticket]
            [flatgui.samples.forms.blotterwin :as blotterwin]
            [flatgui.testsuite]))

(fg/defevolverfn main-theme-evolver :theme
  (if (get-property [:this :header-panel :dark] :pressed)
    flatgui.theme/dark
    flatgui.theme/light))

(radiobutton/defradiogroupevolver theme-group [:dark :light])

(def header-panel
  (fg/defcomponent panel/panel :header-panel
    {:clip-size (m/defpoint 16 0.5)
     :position-matrix (m/translation 0 0)
     :background :prime-3}

    (fg/defcomponent label/label :theme-label
      {:text "Theme:"
       :h-alignment :right
       :clip-size (m/defpoint 0.75 0.25)
       :position-matrix (m/translation 0.125 0.125)})

    (fg/defcomponent radiobutton/radiobutton :dark
      {:text "Dark"
       :pressed true
       :clip-size (m/defpoint 1.25 0.25)
       :position-matrix (m/translation 1.0 0.125)
       :evolvers {:pressed theme-group}})

    (fg/defcomponent radiobutton/radiobutton :light
      {:text "Light"
       :clip-size (m/defpoint 1.0 0.25)
       :position-matrix (m/translation 1.875 0.125)
       :evolvers {:pressed theme-group}})

    (fg/defcomponent label/label :app-label
      {:text "Show demo application:"
       :h-alignment :right
       :clip-size (m/defpoint 2.5 0.25)
       :position-matrix (m/translation 2.875 0.125)})

    (fg/defcomponent checkbox/checkbox :hello-world
      {:text "Hello World"
       :pressed true
       :clip-size (m/defpoint 1.5 0.25)
       :position-matrix (m/translation 5.5 0.125)})

    (fg/defcomponent checkbox/checkbox :color-chooser
      {:text "Color Chooser"
       :clip-size (m/defpoint 1.625 0.25)
       :position-matrix (m/translation 7.125 0.125)})

    (fg/defcomponent checkbox/checkbox :book
      {:text "Book"
       :clip-size (m/defpoint 1.0 0.25)
       :position-matrix (m/translation 9.0 0.125)})))

(fg/defevolverfn helloworld-visible-evolver :visible
  (get-property [:_ :header-panel :hello-world] :pressed))

(fg/defevolverfn colorchooser-visible-evolver :visible
  (get-property [:_ :header-panel :color-chooser] :pressed))

(fg/defevolverfn book-visible-evolver :visible
  (get-property [:_ :header-panel :book] :pressed))

(def app-panel
  (fg/defcomponent panel/panel :app-panel
    {:clip-size (m/defpoint 16 11.25)
     :position-matrix (m/translation 0 0.75)
     :background (awt/color 0 38 70)}

    (fg/defcomponent ticket/orderticket-window :ticket
      {:evolvers {:visible book-visible-evolver}})

    (fg/defcomponent blotterwin/tradeblotter-window :blotter
      {:evolvers {:visible book-visible-evolver}})

    (fg/defcomponent hw/hello-window :hello
      {:evolvers {:visible helloworld-visible-evolver}})

    (fg/defcomponent cc/color-chooser-window :chooser
      {:position-matrix (m/translation 5 1)
       :evolvers {:visible colorchooser-visible-evolver}})))

(def raw-compoundpanel
  (flatgui.app/defroot
    (fg/defcomponent panel/panel :main
      {:clip-size (m/defpoint 16 12)
       :background (awt/color 0 38 70)
       :evolvers {:theme main-theme-evolver}}
      header-panel
      app-panel)))

;;;
;;; We want :hello-world and :color-chooser to be checked initially. While generic way to assign
;;; initial property values is not implemented yet, let's simply simulate mouse click events into
;;; both check boxes - as if it was for UI testing
;;;

;;; raw-compoundpanel container with two simulated mouse clicks passed to it
(def compoundpanel
  (-> (flatgui.testsuite/simulate-mouse-click raw-compoundpanel [:main :header-panel :hello-world])
      (flatgui.testsuite/simulate-mouse-click [:main :header-panel :color-chooser])))