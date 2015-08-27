; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Window widget. Can be mouse-dragged, so attached
            widgets can be dragged together with it."
      :author "Denys Lebediev"}
  flatgui.widgets.window
  (:require [flatgui.base :as fg]
            [flatgui.widgets.component]
            [flatgui.widgets.label]
            [flatgui.widgets.floatingbar :as floatingbar]
            [flatgui.widgets.resizeablebar :as resizeablebar]
            [flatgui.util.matrix :as m]
            [flatgui.inputchannels.mouse :as mouse]))


(fg/defevolverfn window-capture-area-evolver :capture-area
  (let [ content-size (get-property component [:this] :content-size)]
    {:x 0 :y 0 :w (m/x content-size) :h (:header-h component)}))

(fg/defevolverfn window-position-matrix-evolver :position-matrix
  (if (mouse/is-mouse-event? component)
    (let [ mce (get-property component [:this] :mouse-capture-edges)]
      (if mce
        (condp = (:edge mce)
          :left (m/mx*
                  (:position-matrix mce)
                  (m/translation-matrix (- (mouse/get-mouse-x component) (:x mce)) 0))
          old-position-matrix)
        (flatgui.widgets.floatingbar/position-matrix-evolver component)))
    (flatgui.widgets.floatingbar/position-matrix-evolver component)))

(fg/defwidget "window"
  {:header-h 0.375
   :v-alignment :top
   :h-alignment :left
   :foreground :prime-4
   :text ""
   :closed-focus-root true
   :skin-key [:window]
   :evolvers {:capture-area window-capture-area-evolver
              :position-matrix window-position-matrix-evolver}}
  floatingbar/floatingbar
  resizeablebar/resizeablebar)