; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Scrollbar widget"
      :author "Denys Lebediev"}
  flatgui.widgets.scrollbar
  (:import [clojure zip$xml_zip])
  (:use flatgui.awt
                              flatgui.comlogic
                              flatgui.base
                              flatgui.theme
                              flatgui.paint
                              flatgui.widgets.componentbase
                              flatgui.widgets.component
                              flatgui.widgets.panel
                              flatgui.widgets.floatingbar
                              flatgui.util.matrix
                              clojure.test))




(defn- orient-fn [orientation]
  (if (= :vertical orientation) y x))

(def MIN-SCROLLER-LEN-DEFAULT 0.375)

(defn smaller-then-content? [orientation size parent-content-size]
  (if (= :vertical orientation)
    (< (y size) (y parent-content-size))
    (< (x size) (x parent-content-size))))

(defevolverfn :last-position-matrix
  ; Keeps last position matrix that scroller had before content size
  ; bacame small and therefore scroller position changed to 0.0
  (let [ orientation (get-property component [] :orientation)
         parent-content-size (get-property component [] :content-size)
         size (get-property component [:this] :clip-size)
         last-size (get-property component [:this] :last-size)
         prev-size (first last-size)
         new-size (second last-size)]
    (if (smaller-then-content? orientation size parent-content-size)
      (let [ current (get-property component [:this] :position-matrix)]
        (if
;          (or
;            (and (= :vertical orientation) (> (mx-y current) 0))
;            (and (not= :vertical orientation) (> (mx-x current) 0)))
          (and
            prev-size
            new-size
            (smaller-then-content? orientation prev-size parent-content-size)
            (smaller-then-content? orientation new-size parent-content-size))
          current
          (if old-last-position-matrix old-last-position-matrix current)))
      old-last-position-matrix)))

;@todo add protection from same new clip-size valuse like in :last-anchor
(defevolverfn :last-size
  [(second old-last-size)
   (get-property component [:this] :clip-size)])

(defevolverfn :clip-size
  (let [ scrolled-clip-rect (get-property component [:_ :content-pane] :clip-size)
         scrolled-content-rect (get-property component [:_ :content-pane] :content-size)
         min-scroller-len (:min-scroller-len component)]
    (if (or (nil? scrolled-clip-rect) (nil? scrolled-clip-rect))
      (defpoint min-scroller-len min-scroller-len 0)
      (let [ orientation (get-property component [] :orientation)
             clip-size (max ((orient-fn orientation) scrolled-clip-rect) 0)
             content-size ((orient-fn orientation) scrolled-content-rect)
             ratio (if (> content-size clip-size) (/ clip-size content-size) 1.0)
             bar-rect (get-property component [] :clip-size)
             bar-size ((orient-fn orientation) bar-rect)
             scroller-size (max (* bar-size ratio) min-scroller-len)]
        (if (= :vertical orientation)
          (defpoint (x bar-rect) scroller-size 0)
          (defpoint scroller-size (y bar-rect) 0))))))

(defevolverfn scroller-position-matrix-evolver :position-matrix
  ; Restores last position matrix that scroller had before content size
  ; bacame small and therefore scroller position changed to 0.0
  (let [
         orientation (get-property component [] :orientation)
         parent-content-size (get-property component [] :content-size)
         last-size (get-property component [:this] :last-size)
         prev-size (first last-size)
         new-size (second last-size)]
    (if (and
          prev-size
          new-size
          (smaller-then-content? orientation new-size parent-content-size)
          (not (smaller-then-content? orientation prev-size parent-content-size)))
      (let [ last-pos (get-property component [:this] :last-position-matrix)]
        (if last-pos
          last-pos
          (flatgui.widgets.floatingbar/position-matrix-evolver component)))
      (flatgui.widgets.floatingbar/position-matrix-evolver component))))

(defevolverfn :content-size
  (get-property component [:this] :clip-size))

(defwidget "scroller"
  (array-map
    :min-scroller-len MIN-SCROLLER-LEN-DEFAULT
    ;:look scroller-look
    :skin-key [:scrollbar :scroller]
    :evolvers {
                ;@todo :position-matrix evolver with additional functionality:
                ;  - process mouse wheel event
                :position-matrix scroller-position-matrix-evolver
                :last-position-matrix last-position-matrix-evolver
                :last-size last-size-evolver
                :clip-size clip-size-evolver
                :content-size content-size-evolver
                })
  floatingbar)



(defevolverfn scrollbar-visible-evolver :visible
  (let [ scrolled-clip-rect (get-property component [:content-pane] :clip-size)
         scrolled-content-rect (get-property component [:content-pane] :content-size)]
    (if (= (:orientation component) :vertical)
      (if (<= (- (y scrolled-content-rect) (y scrolled-clip-rect)) (* 2 (px)))
        false
        (flatgui.widgets.component/visible-evolver component))
      (if (<= (- (x scrolled-content-rect) (x scrolled-clip-rect)) (* 2 (px)))
        false
        (flatgui.widgets.component/visible-evolver component)))))

(defwidget "scrollbar"
  (array-map
    :orientation :vertical

    ;:look scrollbar-look
    :skin-key [:scrollbar :scrollbar]
    :evolvers { :visible scrollbar-visible-evolver }
    :children {
                :scroller (defcomponent scroller :scroller {})
                }
    )
  component)

;
; Tests
;