; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns bookpanelmain
  (:require [flatgui.skins.flat]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.util.matrix :as m]
            [flatgui.widgets.panel :as panel]
            [flatgui.samples.forms.preferenceswin :as preferences]
            [flatgui.samples.forms.orderticketwin :as ticket]
            [flatgui.samples.forms.blotterwin :as blotterwin]))

(fg/defevolverfn main-theme-evolver :theme
  (if (get-property [:this :preferences :dark] :pressed)
    flatgui.theme/dark
    flatgui.theme/light))

;; TODO uncomment when one more skin is available
;(fg/defevolverfn main-skin-evolver :skin
;  (if (get-property [:this :preferences :oldschool] :pressed)
;    "flatgui.skins.oldschool"
;    "flatgui.skins.smooth"))

(def bookpanel
  (flatgui._old_app/defroot
    (fg/defcomponent panel/panel :main
      {:clip-size (m/defpoint 25 19 0)
       :background (awt/color 0 38 70)
       :evolvers {:theme main-theme-evolver
                  ;:skin main-skin-evolver
                  }}
      ticket/orderticket-window
      blotterwin/tradeblotter-window
      preferences/preferences-window)))