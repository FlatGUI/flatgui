; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Slider widget"
      :author "Denys Lebediev"}
  flatgui.widgets.slider
  (:require [flatgui.base :as fg]
            [flatgui.util.matrix :as m]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.inputchannels.keyboard :as keyboard]
            [flatgui.widgets.component]
            [flatgui.widgets.floatingbar])
  (:import (java.awt.event KeyEvent)))

(def default-slider-bar-thikness 0.3125)

(fg/defevolverfn sliderhandle-clip-size-evolver :clip-size
  (let [ orientation (get-property component [:_] :orientation)
         base-size (get-property component [] :clip-size)
         bar-width (get-property component [:_] :bar-width)]
    (if (= :horizontal orientation)
      (m/defpoint bar-width (m/y base-size) 0)
      (m/defpoint (m/x base-size) bar-width 0))))

(fg/defevolverfn sliderhandle-position-matrix-evolver :position-matrix
  (cond
    (mouse/is-mouse-event? component)
      (flatgui.widgets.floatingbar/position-matrix-evolver component)
    (= [:_] ((:evolve-reason-provider component) (:id component)))
      (let [ orientation (get-property component [:_] :orientation)
             position (get-property component [:_] :position)
             bar-width (get-property component [:_] :bar-width)
             base-size (get-property component [] :clip-size)]
        (if (= :horizontal orientation)
          (m/transtation-matrix (* position (- (m/x base-size) bar-width)) 0 0)
          (m/transtation-matrix 0 (* position (- (m/y base-size) bar-width)) 0)))
    :else
      old-position-matrix))

(fg/defwidget "sliderhandle"
  { :focusable false
    :skin-key [:slider :handle]
    :evolvers { :position-matrix sliderhandle-position-matrix-evolver
                :clip-size sliderhandle-clip-size-evolver}}
  flatgui.widgets.floatingbar/floatingbar)

(fg/defevolverfn sliderhandlebase-clip-size-evolver :clip-size
  (let [ orientation (get-property component [] :orientation)
         slider-size (get-property component [] :clip-size)]
    (if (= :horizontal orientation)
      (m/defpoint (m/x slider-size) (* 0.625 (m/y slider-size)))
      (m/defpoint (* 0.625 (m/x slider-size)) (m/y slider-size)))))

(fg/defwidget "sliderhandlebase"
  { :focusable false
    :side-gap 0
    :orientation :horizontal
    :sliderhandle-position (m/transtation-matrix 0 0)
    :skin-key [:slider :base]
    :evolvers {:orientation (fg/accessorfn (get-property component [] :orientation))
               :clip-size sliderhandlebase-clip-size-evolver
               :side-gap (fg/accessorfn (/ (get-property component [] :bar-width) 2))
               :sliderhandle-position (fg/accessorfn (get-property component [:this :handle] :position-matrix))}
    :children {:handle (fg/defcomponent sliderhandle :handle {})}}
  flatgui.widgets.component/component)

(fg/defevolverfn slider-position-evolver :position
  (if (keyboard/key-event? component)
    (let [ticksize (get-property component [:this] :ticksize)
          key (keyboard/get-key component)]
      (condp = key
         KeyEvent/VK_LEFT (max (- old-position ticksize) 0.0)
         KeyEvent/VK_DOWN (max (- old-position ticksize) 0.0)
         KeyEvent/VK_RIGHT (min (+ old-position ticksize) 1.0)
         KeyEvent/VK_UP (min (+ old-position ticksize) 1.0)
         KeyEvent/VK_HOME 0.0
         KeyEvent/VK_END 1.0
        old-position))
    (let [orientation (get-property component [:this] :orientation)
          bar-width (get-property component [:this] :bar-width)
          clip-size (get-property component [:this] :clip-size)
          handlepm (get-property component [:this :base :handle] :position-matrix)
          handlecoord (if (= :horizontal orientation) (m/mx-x handlepm) (m/mx-y handlepm))
          handlespace (- (if (= :horizontal orientation) (m/x clip-size) (m/y clip-size)) bar-width)]
      (/ handlecoord handlespace))))

(fg/defwidget "slider"
  {:focusable true
   :orientation :horizontal
   :position 0
   :bar-width default-slider-bar-thikness
   :ticksize 0.125
   :precise-positioning false
   :evolvers {:position slider-position-evolver}
   :children {:base (fg/defcomponent sliderhandlebase :base {})}}
  flatgui.widgets.component/component)