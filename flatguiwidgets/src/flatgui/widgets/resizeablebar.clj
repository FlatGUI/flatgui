; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.widgets.resizeablebar
  (:require [flatgui.base :as fg]
            [flatgui.layout :as layout]
            [flatgui.widgets.component]
            [flatgui.widgets.floatingbar]
            [flatgui.util.matrix :as m]
            [flatgui.inputchannels.mouse :as mouse]))

(fg/defaccessorfn get-edges [component]
  (let [pm (:position-matrix component)
        cs (:clip-size component)
        capture-area-left (flatgui.widgets.floatingbar/capture-area 0 0 0.125 (m/y cs))
        capture-area-right (flatgui.widgets.floatingbar/capture-area (- (m/x cs) 0.125) 0 0.125 (m/y cs))
        capture-area-top nil ;@todo this wil not conflict with window's capture area when there will be borders (capture-area 0 0 (m/x cs) 0.125)
        capture-area-bottom (flatgui.widgets.floatingbar/capture-area 0 (- (m/y cs) 0.125) (m/x cs) 0.125)
        mabsx (mouse/get-mouse-x component)
        mabsy (mouse/get-mouse-y component)
        mrelx (mouse/get-mouse-rel-x component)
        mrely (mouse/get-mouse-rel-y component)
        left (flatgui.widgets.floatingbar/mouse-in-area capture-area-left mrelx mrely)
        right (flatgui.widgets.floatingbar/mouse-in-area capture-area-right mrelx mrely)
        top (flatgui.widgets.floatingbar/mouse-in-area capture-area-top mrelx mrely)
        bottom (flatgui.widgets.floatingbar/mouse-in-area capture-area-bottom mrelx mrely)]
    {:edges {:left left :right right :top top :bottom bottom} :x mabsx :y mabsy :position-matrix pm :clip-size cs}))

(fg/defevolverfn :mouse-capture-edges
  ;; get-property in not used in get-edges intentionally: this evolver should not be invoked on :position-matrix or :clip-size change
  (if (mouse/mouse-left? component)
    (if (nil? old-mouse-capture-edges)
      (let [edges-data (get-edges component)
            edges (:edges edges-data)]
        (if (or (:left edges) (:right edges) (:top edges) (:bottom edges))
          edges-data
          old-mouse-capture-edges))
      old-mouse-capture-edges)))

(fg/defevolverfn resizeablebar-clip-size-evolver :clip-size
  (if (mouse/is-mouse-event? component)
    (if-let [mce (get-property component [:this] :mouse-capture-edges)]
      (let [new-size (m/defpoint
                       (cond
                         (:left (:edges mce)) (- (m/x (:clip-size mce)) (- (mouse/get-mouse-x component) (:x mce)))
                         (:right (:edges mce)) (+ (m/x (:clip-size mce)) (- (mouse/get-mouse-x component) (:x mce)))
                         :else (m/x old-clip-size))
                       (cond
                         (:bottom (:edges mce)) (+ (m/y (:clip-size mce)) (- (mouse/get-mouse-y component) (:y mce)))
                         (:top (:edges mce)) (+ (m/y (:clip-size mce)) (max 0 (- (mouse/get-mouse-y component) (:y mce))))
                         :else (m/y old-clip-size)))]
        (m/defpoint
          (if (< (m/x new-size) (m/x old-clip-size)) (if (layout/can-shrink-x component) (m/x new-size) (m/x old-clip-size)) (m/x new-size))
          (if (< (m/y new-size) (m/y old-clip-size)) (if (layout/can-shrink-y component) (m/y new-size) (m/y old-clip-size)) (m/y new-size))))
      old-clip-size)
    old-clip-size))

(fg/defevolverfn :cursor
   (if (and (mouse/is-mouse-event? component) (get-property [:this] :has-mouse))
     (if-let [mce (get-edges component)]
       (let [edges (:edges mce)]
         (cond
           (and (:top edges) (:left edges)) :nwse-resize
           (and (:bottom edges) (:right edges)) :nwse-resize
           (and (:top edges) (:right edges)) :nesw-resize
           (and (:bottom edges) (:left edges)) :nesw-resize
           (or (:top edges) (:bottom edges)) :ns-resize
           (or (:left edges) (:right edges)) :ew-resize
           :else nil)))))

(fg/defwidget "resizeablebar"
  {:mouse-capture-edges nil
   :evolvers {:mouse-capture-edges mouse-capture-edges-evolver
              :clip-size resizeablebar-clip-size-evolver
              :cursor cursor-evolver}}
  flatgui.widgets.component/component)