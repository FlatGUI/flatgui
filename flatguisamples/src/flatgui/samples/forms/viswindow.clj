; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.viswindow
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.widgets.panel :as panel]
            [flatgui.widgets.floatingbar :as floatingbar]
            [flatgui.paint :as fgp]))

(fgp/deflookfn bar-look (:theme :focus-state :id)
 [(fgp/call-look flatgui.skins.flat/component-look)
  (awt/setColor (:prime-2 theme))
  (awt/drawRect 0 0 w- h-)])

(fg/defevolverfn :click-happened
  (if (mouse/mouse-clicked? component)
    (not old-click-happened)
    old-click-happened))

(fg/defevolverfn :visible
  (get-property [] :click-happened))

(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:clip-size (m/defpoint 40 20)
     :background (awt/color 9 17 26)
     :click-happened false
     :evolvers {:click-happened click-happened-evolver}
     :children {:c1_1 (fg/defcomponent
                      floatingbar/floatingbar
                      :c1_1
                      {:position-matrix (m/translation 0.25 0.25)
                       :clip-size (m/defpoint 0.5 0.5)
                       :background (awt/color 26 86 17)
                       :look bar-look
                       })}}

    (fg/defcomponent
      floatingbar/floatingbar
      :c1
      {:position-matrix (m/translation 1 1)
       :clip-size (m/defpoint 2 2)
       :background (awt/color 86 26 17)
       :look bar-look
       :visible false
       :evolvers {:visible visible-evolver}
       })))