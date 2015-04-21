; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.widgets.pinwindow
  (:require [flatgui.base :as fg]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.resizeablebar :as resizeablebar]
            [flatgui.util.matrix :as m]))


(fg/defevolverfn bounds-pct-evolver :bounds-pct
  (if (or
        (nil? old-bounds-pct)
        (and
          (= [:this] (fg/get-reason))
          (or
            (get-property component [:this] :mouse-capture)
            (get-property component [:this] :mouse-capture-edges))))
    (let [container-size (get-property [] :clip-size)       ;TODO :content-size should be here
          cw (m/x container-size)
          ch (m/y container-size)
          pm (get-property [:this] :position-matrix)
          cs (get-property [:this] :clip-size)]
      {:x (/ (m/mx-x pm) cw)
       :y (/ (m/mx-y pm) ch)
       :w (/ (m/x cs) cw)
       :h (/ (m/y cs) ch)})
    old-bounds-pct))

(fg/defevolverfn pinwindow-position-matrix-evolver :position-matrix
  (let [bounds-pct (get-property [:this] :bounds-pct)]
    (if (and bounds-pct (= nil (fg/get-reason)) (get-property [:this] :pinned?)) ; TODO why nil, not [] ? FlatGUI bug
      (let [container-size (get-property [] :clip-size)       ;TODO :content-size should be here
            cw (m/x container-size)
            ch (m/y container-size)]
        (m/translation (* (:x bounds-pct) cw) (* (:y bounds-pct) ch)))
      (window/window-position-matrix-evolver component))))

(fg/defevolverfn pinwindow-clip-size-evolver :clip-size
  (let [bounds-pct (get-property [:this] :bounds-pct)]
    (if (and bounds-pct (= nil (fg/get-reason)) (get-property [:this] :pinned?)) ; TODO why nil, not [] ? FlatGUI bug
      (let [container-size (get-property [] :clip-size)       ;TODO :content-size should be here
            cw (m/x container-size)
            ch (m/y container-size)]
        (m/defpoint (* (:w bounds-pct) cw) (* (:h bounds-pct) ch)))
      (resizeablebar/resizeablebar-clip-size-evolver component))))

(fg/defwidget "pinwindow"
 {:pinned? true
  :bounds-pct nil
  :evolvers {:bounds-pct bounds-pct-evolver
             :position-matrix pinwindow-position-matrix-evolver
             :clip-size pinwindow-clip-size-evolver}}
 window/window)