; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Slider widget"
      :author "Denys Lebediev"}
  flatgui.widgets.slider (:use flatgui.awt
                               flatgui.comlogic
                               flatgui.base
                               flatgui.theme
                               flatgui.paint
                               flatgui.widgets.component
                               flatgui.widgets.floatingbar
                               flatgui.inputchannels.mouse
                               flatgui.inputchannels.keyboard
                               flatgui.util.matrix
                               clojure.test))

(def SLIDER_BAR_WIDTH 0.3125)

(defevolverfn sliderhandle-clip-size-evolver :clip-size
  (let [ orientation (get-property component [:_] :orientation)
         base-size (get-property component [] :clip-size)
         bar-width (get-property component [:_] :bar-width)]
    (if (= :horizontal orientation)
      (defpoint bar-width (y base-size) 0)
      (defpoint (x base-size) bar-width 0))))

(defevolverfn sliderhandle-position-matrix-evolver :position-matrix
  (cond
    (is-mouse-event? component)
      (flatgui.widgets.floatingbar/position-matrix-evolver component)
    (= [:_] ((:evolve-reason-provider component) (:id component)))
      (let [ orientation (get-property component [:_] :orientation)
             position (get-property component [:_] :position)
             bar-width (get-property component [:_] :bar-width)
             base-size (get-property component [] :clip-size)]
        (if (= :horizontal orientation)
          (transtation-matrix (* position (- (x base-size) bar-width)) 0 0)
          (transtation-matrix 0 (* position (- (y base-size) bar-width)) 0)))
    :else
      old-position-matrix))

(defwidget "sliderhandle"
  { :focusable false
   ;:look sliderhandle-look
    :skin-key [:slider :handle]
    :evolvers { :position-matrix sliderhandle-position-matrix-evolver
                :clip-size sliderhandle-clip-size-evolver
               }}
  floatingbar)


(defevolverfn sliderhandlebase-clip-size-evolver :clip-size
  (let [ orientation (get-property component [] :orientation)
         slider-size (get-property component [] :clip-size)]
    (if (= :horizontal orientation)
      (defpoint (x slider-size) (* 0.625 (y slider-size)))
      (defpoint (* 0.625 (x slider-size)) (y slider-size)))))


(defwidget "sliderhandlebase"
  { :focusable false
    :side-gap 0
    :orientation :horizontal
   ;:look sliderhandlebase-look
    :sliderhandle-position (transtation-matrix 0 0)
    :skin-key [:slider :base]
    :evolvers { :orientation (accessorfn (get-property component [] :orientation))
                :clip-size sliderhandlebase-clip-size-evolver
                :side-gap (accessorfn (/ (get-property component [] :bar-width) 2))
                :sliderhandle-position (accessorfn (get-property component [:this :handle] :position-matrix))}
    :children { :handle (defcomponent sliderhandle :handle {})}}
  component)



;(deflookfn slider-look (:theme :ticksize :orientation :bar-width)
;  (call-look component-look)
;  (setColor (:theme-border theme))
;  (for [ tick (range 0 (+ 1 ticksize) ticksize)]
;    (if (= :horizontal orientation)
;      (let [ gap (/ bar-width 2)
;             tickw (- w (* 2 gap))
;             tx (+ (* tick tickw) gap)]
;        (drawLine tx h- tx (- h- gap)))
;      (let [ gap (/ bar-width 2)
;             tickh (- h (* 2 gap))
;             ty (+ (* tick tickh) gap)]
;        (drawLine w- ty (- w- gap) ty)))))

(defevolverfn slider-position-evolver :position
  (if (key-event? component)
    (let [ ticksize (get-property component [:this] :ticksize)
           key (get-key component)]
      (condp = key
         java.awt.event.KeyEvent/VK_LEFT (max (- old-position ticksize) 0.0)
         java.awt.event.KeyEvent/VK_DOWN (max (- old-position ticksize) 0.0)
         java.awt.event.KeyEvent/VK_RIGHT (min (+ old-position ticksize) 1.0)
         java.awt.event.KeyEvent/VK_UP (min (+ old-position ticksize) 1.0)
         java.awt.event.KeyEvent/VK_HOME 0.0
         java.awt.event.KeyEvent/VK_END 1.0
        old-position))
    (let [ orientation (get-property component [:this] :orientation)
           bar-width (get-property component [:this] :bar-width)
           clip-size (get-property component [:this] :clip-size)
           handlepm (get-property component [:this :base :handle] :position-matrix)
           handlecoord (if (= :horizontal orientation) (mx-x handlepm) (mx-y handlepm))
           handlespace (- (if (= :horizontal orientation) (x clip-size) (y clip-size)) bar-width)]
      (/ handlecoord handlespace))))

(defwidget "slider"
  { :focusable true
    :orientation :horizontal
    :position 0
    :bar-width SLIDER_BAR_WIDTH
    :ticksize 0.125
    :precise-positioning false
   ;:look slider-look
    :evolvers {:position slider-position-evolver}
    :children { :base (defcomponent sliderhandlebase :base {})}
    }
  component)

;
; Tests
;