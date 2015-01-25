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
  flatgui.widgets.window (:use
                           flatgui.awt
                           flatgui.comlogic
                           flatgui.base
                           flatgui.theme
                           flatgui.paint
                           flatgui.widgets.component
                           flatgui.widgets.floatingbar
                           flatgui.inputchannels.mouse
                           flatgui.util.matrix
                           clojure.test)
                            (:require flatgui.widgets.label))

;(deflookfn window-look (:theme :header-h :text :h-alignment :v-alignment)
;  (call-look component-look)
;  (setColor (:dark theme))
;  (drawRect 0 0 w- h-)
;  (setColor (:light theme))
;  (drawLine 0 header-h 0 h-)
;  (setColor (:inactive-selection theme))
;  (fillRect 0 0 w- header-h)
;  (setColor (:mid-light-selection theme))
;  (drawLine 0 0 0 (-px header-h))
;  (drawLine 0 0 w- 0)
;  (setColor (:dark-selection theme))
;  (drawLine w- 0 w- (-px header-h))
;  (call-look label-look))

(deflookfn window-look (:theme :header-h :text)
  [
   (call-look component-look)
   (setColor (:prime-4 theme))
   (fillRect 0 0 w header-h)
   (flatgui.widgets.label/label-look-impl (:prime-1 theme) text :left :center 0 0 w header-h)
   ])

(defevolverfn window-capture-area-evolver :capture-area
  (let [ content-size (get-property component [:this] :content-size)]
    {:x 0 :y 0 :w (x content-size) :h (:header-h component)}))

(defevolverfn :mouse-capture-edges
  ; get-property in not used here intentionally, this evolver should not be invoked on :position-matrix or :clip-size change
  (let [ pm (:position-matrix component)
         cs (:clip-size component)
         capture-area-left (capture-area 0 0 0.125 (y cs))
         capture-area-right (capture-area (- (x cs) 0.125) 0 0.125 (y cs))
         capture-area-top nil ;@todo this wil not conflict with window's capture area when there will be borders (capture-area 0 0 (x cs) 0.125)
         capture-area-bottom (capture-area 0 (- (y cs) 0.125) (x cs) 0.125)
;         _ (println " :mouse-capture-edges mleft = " (mouse-left? component))
;         _ (if (mouse-left? component) (println "   "
;             (mouse-in-area capture-area-left (get-mouse-rel-x component) (get-mouse-rel-y component))
;             (mouse-in-area capture-area-right (get-mouse-rel-x component) (get-mouse-rel-y component))
;             (mouse-in-area capture-area-top (get-mouse-rel-x component) (get-mouse-rel-y component))
;             (mouse-in-area capture-area-bottom (get-mouse-rel-x component) (get-mouse-rel-y component))))
         ]
    (if (mouse-left? component)
      (if (nil? old-mouse-capture-edges)
        (let [ mabsx (get-mouse-x component)
               mabsy (get-mouse-y component)
               mrelx (get-mouse-rel-x component)
               mrely (get-mouse-rel-y component)]
          (cond
            (mouse-in-area capture-area-left mrelx mrely) {:edge :left :x mabsx :y mabsy :position-matrix pm :clip-size cs}
            (mouse-in-area capture-area-right mrelx mrely) {:edge :right :x mabsx :y mabsy :position-matrix pm :clip-size cs}
            (mouse-in-area capture-area-top mrelx mrely) {:edge :top :x mabsx :y mabsy :position-matrix pm :clip-size cs}
            (mouse-in-area capture-area-bottom mrelx mrely) {:edge :bottom :x mabsx :y mabsy :position-matrix pm :clip-size cs}
            :else old-mouse-capture-edges))
        old-mouse-capture-edges))))

(defevolverfn window-clip-size-evolver :clip-size
  (if (is-mouse-event? component)
    (let [ mce (get-property component [:this] :mouse-capture-edges)]
      (if mce
        (condp = (:edge mce)


          ;; TODO temporarily disabled, because table does not behave well when _decreasing_ vertical size
          ;;
          ;:bottom (defpoint
          ;          (x old-clip-size)
          ;          (do ;(println " (get-mouse-y component) " (float (get-mouse-y component)) " (:y mce) " (float (:y mce)))
          ;            (+ (y (:clip-size mce)) (- (get-mouse-y component) (:y mce)))))
          :bottom (defpoint
                    (x old-clip-size)
                    (do ;(println " (get-mouse-y component) " (float (get-mouse-y component)) " (:y mce) " (float (:y mce)))
                      (+ (y (:clip-size mce)) (max 0 (- (get-mouse-y component) (:y mce))))))

          :left (defpoint
                  (- (x (:clip-size mce)) (- (get-mouse-x component) (:x mce)))
                  (y old-clip-size))
          :right (defpoint
                  (+ (x (:clip-size mce)) (- (get-mouse-x component) (:x mce)))
                  (y old-clip-size))
          old-clip-size)
        old-clip-size))
  old-clip-size))

(defevolverfn window-position-matrix-evolver :position-matrix
  (if (is-mouse-event? component)
    (let [ mce (get-property component [:this] :mouse-capture-edges)]
      (if mce
        (condp = (:edge mce)
          :left (mx*
                  (:position-matrix mce)
                  (transtation-matrix (- (get-mouse-x component) (:x mce)) 0))
          old-position-matrix)
        (flatgui.widgets.floatingbar/position-matrix-evolver component)))
    (flatgui.widgets.floatingbar/position-matrix-evolver component)))



;(flatgui.widgets.floatingbar/position-matrix-evolver component)

(defwidget "window"
  { :header-h 0.375
    :v-alignment :top
    :h-alignment :left
    :foreground :prime-4
    :text ""
    :mouse-capture-edges nil
    :look window-look
    :evolvers {
                :capture-area window-capture-area-evolver
                :mouse-capture-edges mouse-capture-edges-evolver
                :clip-size window-clip-size-evolver
                :position-matrix window-position-matrix-evolver
               }}
  floatingbar)



;
; Tests
;