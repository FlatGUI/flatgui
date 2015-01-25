; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Floating bar widget"
      :author "Denys Lebediev"}
  flatgui.widgets.floatingbar (:use flatgui.comlogic
                                    flatgui.base
                                    flatgui.paint
                                    flatgui.widgets.componentbase
                                    flatgui.widgets.component
                                    flatgui.inputchannels.mouse
                                    flatgui.util.matrix
                                    clojure.test))

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

(defevolverfn :position-bound
  (let [ parent-content-size (get-property component [] :content-size)
         size (get-property component [:this] :clip-size)]
    (point-op - parent-content-size size)))

(defevolverfn :capture-area
  (let [ content-size (get-property component [:this] :content-size)]
    ;{:x 0 :y 0 :w (x content-size) :h (y content-size)}
    (capture-area 0 0 (x content-size) (y content-size))
    ))

(defevolverfn :mouse-capture
  (let [ capture-area (get-property component [:this] :capture-area)]
    (if (mouse-left? component)
      (if
        (and
          (nil? old-mouse-capture)
          (mouse-in-area capture-area (get-mouse-rel-x component) (get-mouse-rel-y component)))
        {:x (get-mouse-x component) :y (get-mouse-y component) :position-matrix (:position-matrix component)}
        old-mouse-capture))))

(defevolverfn :position-matrix
  (let [ mouse-capture (get-property component [:this] :mouse-capture)]
    (if (or (nil? mouse-capture) (not (is-mouse-event? component)))
      (let [ position-bound (get-property component [:this] :position-bound)
             x-bound (x position-bound)
             y-bound (y position-bound)
             size (get-property component [:this] :clip-size)]
        (mx-set
          (mx-set
            old-position-matrix
            0 3 (keep-range (mx-get old-position-matrix 0 3) 0 x-bound))
          1 3 (keep-range (mx-get old-position-matrix 1 3) 0 y-bound)))
      (let [ position-bound (get-property component [:this] :position-bound)
             x-bound (x position-bound)
             y-bound (y position-bound)
             size (get-property component [:this] :clip-size)
             new-pos-mx (mx*
                          (:position-matrix mouse-capture)
                          (transtation-matrix
                            ;(if (< (x size) x-bound) (- (get-mouse-x component) (:x mouse-capture)) 0.0)
                            (- (get-mouse-x component) (:x mouse-capture))
                            ;(if (< (y size) y-bound) (- (get-mouse-y component) (:y mouse-capture)) 0.0)
                            (- (get-mouse-y component) (:y mouse-capture))
                            ))]
        (mx-set
          (mx-set
            new-pos-mx
            0 3 (keep-range (mx-get new-pos-mx 0 3) 0 x-bound))
          1 3 (keep-range (mx-get new-pos-mx 1 3) 0 y-bound))))))


(defwidget "floatingbar"
  (array-map
    :position-bound (defpoint (Double/MAX_VALUE) (Double/MAX_VALUE) 0)
    :capture-area {:x 0 :y 0 :w 0 :h 0}
    :mouse-capture nil
    :position-matrix IDENTITY-MATRIX
    :compute-capture-area nil
    :evolvers { :capture-area capture-area-evolver
                :position-bound position-bound-evolver
                :mouse-capture mouse-capture-evolver
                :position-matrix position-matrix-evolver
                }
    )
  component)