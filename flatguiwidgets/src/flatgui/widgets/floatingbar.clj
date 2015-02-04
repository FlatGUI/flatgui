; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Floating bar widget"
      :author "Denys Lebediev"}
  flatgui.widgets.floatingbar
  (:require [flatgui.base :as fg]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.util.matrix :as m]
            [flatgui.widgets.component]))

(defn mouse-in-area [area mouse-x mouse-y]
  (and
    area
    (> mouse-x (:x area))
    (> mouse-y (:y area))
    (<= mouse-x (+ (:x area) (:w area)))
    (<= mouse-y (+ (:y area) (:h area)))))

(defn- keep-range [v rmin rmax]
  (min rmax (max rmin v)))

(defn capture-area [ax ay aw ah]
  {:x ax :y ay :w aw :h ah})

(fg/defevolverfn :position-bound
  (let [ parent-content-size (get-property component [] :content-size)
         size (get-property component [:this] :clip-size)]
    (m/point-op - parent-content-size size)))

(fg/defevolverfn :capture-area
  (let [ content-size (get-property component [:this] :content-size)]
    ;{:x 0 :y 0 :w (m/x content-size) :h (m/y content-size)}
    (capture-area 0 0 (m/x content-size) (m/y content-size))))

(fg/defevolverfn :mouse-capture
  (let [ capture-area (get-property component [:this] :capture-area)]
    (if (mouse/mouse-left? component)
      (if
        (and
          (nil? old-mouse-capture)
          (mouse-in-area capture-area (mouse/get-mouse-rel-x component) (mouse/get-mouse-rel-y component)))
        {:x (mouse/get-mouse-x component) :y (mouse/get-mouse-y component) :position-matrix (:position-matrix component)}
        old-mouse-capture))))

(fg/defevolverfn :position-matrix
  (let [ mouse-capture (get-property component [:this] :mouse-capture)]
    (if (or (nil? mouse-capture) (not (mouse/is-mouse-event? component)))
      (let [ position-bound (get-property component [:this] :position-bound)
             x-bound (m/x position-bound)
             y-bound (m/y position-bound)
             size (get-property component [:this] :clip-size)]
        (m/mx-set
          (m/mx-set
            old-position-matrix
            0 3 (keep-range (m/mx-get old-position-matrix 0 3) 0 x-bound))
          1 3 (keep-range (m/mx-get old-position-matrix 1 3) 0 y-bound)))
      (let [ position-bound (get-property component [:this] :position-bound)
             x-bound (m/x position-bound)
             y-bound (m/y position-bound)
             size (get-property component [:this] :clip-size)
             new-pos-mx (m/mx*
                          (:position-matrix mouse-capture)
                          (m/transtation-matrix
                            ;(if (< (m/x size) x-bound) (- (get-mouse-x component) (:x mouse-capture)) 0.0)
                            (- (mouse/get-mouse-x component) (:x mouse-capture))
                            ;(if (< (m/y size) y-bound) (- (get-mouse-y component) (:y mouse-capture)) 0.0)
                            (- (mouse/get-mouse-y component) (:y mouse-capture))))]
        (m/mx-set
          (m/mx-set
            new-pos-mx
            0 3 (keep-range (m/mx-get new-pos-mx 0 3) 0 x-bound))
          1 3 (keep-range (m/mx-get new-pos-mx 1 3) 0 y-bound))))))


(fg/defwidget "floatingbar"
  {:position-bound (m/defpoint (Double/MAX_VALUE) (Double/MAX_VALUE) 0)
   :capture-area {:x 0 :y 0 :w 0 :h 0}
   :mouse-capture nil
   :position-matrix m/IDENTITY-MATRIX
   :compute-capture-area nil
   :evolvers {:capture-area capture-area-evolver
              :position-bound position-bound-evolver
              :mouse-capture mouse-capture-evolver
              :position-matrix position-matrix-evolver}}
  flatgui.widgets.component/component)