; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Toolbar widget. Can be mouse-dragged, so attached
            widgets can be dragged together with it."
      :author "Denys Lebediev"}
    flatgui.widgets.toolbar
  (:use flatgui.comlogic)
  (:require [flatgui.base :as fg]
            [flatgui.widgets.floatingbar]))


(fg/defevolverfn toolbar-capture-area-evolver :capture-area
  (let [ content-size (get-property component [:this] :content-size)]
    {:x 0 :y 0 :w (:header-h component) :h (y content-size)}))

(fg/defwidget "toolbar"
  {:header-h 0.140625
   :skin-key [:toolbar]
   :evolvers {:capture-area toolbar-capture-area-evolver}}
  flatgui.widgets.floatingbar/floatingbar)