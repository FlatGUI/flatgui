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
  (:require [flatgui.awt :as awt]
            [flatgui.base :as fg]
            [flatgui.paint :as fgp]
            [flatgui.widgets.component]
            [flatgui.widgets.label]
            [flatgui.widgets.floatingbar]
            [flatgui.util.matrix :as m]
            [flatgui.inputchannels.mouse :as mouse]))


(fgp/deflookfn window-look (:theme :header-h :text)
  [(fgp/call-look flatgui.widgets.component/component-look)
   (awt/setColor (:prime-4 theme))
   (awt/fillRect 0 0 w header-h)
   (flatgui.widgets.label/label-look-impl (:prime-1 theme) text :left :center 0 0 w header-h)])

(fg/defevolverfn window-capture-area-evolver :capture-area
  (let [ content-size (get-property component [:this] :content-size)]
    {:x 0 :y 0 :w (m/x content-size) :h (:header-h component)}))

(fg/defevolverfn :mouse-capture-edges
  ;; get-property in not used here intentionally, this evolver should not be invoked on :position-matrix or :clip-size change
  (let [pm (:position-matrix component)
        cs (:clip-size component)
        capture-area-left (flatgui.widgets.floatingbar/capture-area 0 0 0.125 (m/y cs))
        capture-area-right (flatgui.widgets.floatingbar/capture-area (- (m/x cs) 0.125) 0 0.125 (m/y cs))
        capture-area-top nil ;@todo this wil not conflict with window's capture area when there will be borders (capture-area 0 0 (m/x cs) 0.125)
        capture-area-bottom (flatgui.widgets.floatingbar/capture-area 0 (- (m/y cs) 0.125) (m/x cs) 0.125)]
    (if (mouse/mouse-left? component)
      (if (nil? old-mouse-capture-edges)
        (let [mabsx (mouse/get-mouse-x component)
              mabsy (mouse/get-mouse-y component)
              mrelx (mouse/get-mouse-rel-x component)
              mrely (mouse/get-mouse-rel-y component)]
          (cond
            (flatgui.widgets.floatingbar/mouse-in-area capture-area-left mrelx mrely) {:edge :left :x mabsx :y mabsy :position-matrix pm :clip-size cs}
            (flatgui.widgets.floatingbar/mouse-in-area capture-area-right mrelx mrely) {:edge :right :x mabsx :y mabsy :position-matrix pm :clip-size cs}
            (flatgui.widgets.floatingbar/mouse-in-area capture-area-top mrelx mrely) {:edge :top :x mabsx :y mabsy :position-matrix pm :clip-size cs}
            (flatgui.widgets.floatingbar/mouse-in-area capture-area-bottom mrelx mrely) {:edge :bottom :x mabsx :y mabsy :position-matrix pm :clip-size cs}
            :else old-mouse-capture-edges))
        old-mouse-capture-edges))))

(fg/defevolverfn window-clip-size-evolver :clip-size
  (if (mouse/is-mouse-event? component)
    (let [ mce (get-property component [:this] :mouse-capture-edges)]
      (if mce
        (condp = (:edge mce)

          ;; TODO temporarily disabled, because table does not behave well when _decreasing_ vertical size
          ;;
          ;:bottom (m/defpoint
          ;          (m/x old-clip-size)
          ;          (do ;(println " (get-mouse-y component) " (float (get-mouse-y component)) " (:y mce) " (float (:y mce)))
          ;            (+ (m/y (:clip-size mce)) (- (get-mouse-y component) (:y mce)))))
          :bottom (m/defpoint
                    (m/x old-clip-size)
                    (do ;(println " (get-mouse-y component) " (float (get-mouse-y component)) " (:y mce) " (float (:y mce)))
                      (+ (m/y (:clip-size mce)) (max 0 (- (mouse/get-mouse-y component) (:y mce))))))
          :left (m/defpoint
                  (- (m/x (:clip-size mce)) (- (mouse/get-mouse-x component) (:x mce)))
                  (m/y old-clip-size))
          :right (m/defpoint
                  (+ (m/x (:clip-size mce)) (- (mouse/get-mouse-x component) (:x mce)))
                  (m/y old-clip-size))
          old-clip-size)
        old-clip-size))
  old-clip-size))

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
   :mouse-capture-edges nil
   :look window-look
   :evolvers {:capture-area window-capture-area-evolver
              :mouse-capture-edges mouse-capture-edges-evolver
              :clip-size window-clip-size-evolver
              :position-matrix window-position-matrix-evolver}}
  flatgui.widgets.floatingbar/floatingbar)