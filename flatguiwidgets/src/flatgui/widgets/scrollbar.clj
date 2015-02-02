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
  (:require [flatgui.awt :as awt]
            [flatgui.base :as fg]
            [flatgui.widgets.component]
            [flatgui.widgets.floatingbar]
            [flatgui.util.matrix :as m]))


(defn- orient-fn [orientation]
  (if (= :vertical orientation) m/y m/x))

(def default-min-scroller-len 0.375)

(defn smaller-then-content? [orientation size parent-content-size]
  (if (= :vertical orientation)
    (< (m/y size) (m/y parent-content-size))
    (< (m/x size) (m/x parent-content-size))))

(fg/defevolverfn :last-position-matrix
  ; Keeps last position matrix that scroller had before content size
  ; bacame small and therefore scroller position changed to 0.0
  (let [orientation (get-property component [] :orientation)
        parent-content-size (get-property component [] :content-size)
        size (get-property component [:this] :clip-size)
        last-size (get-property component [:this] :last-size)
        prev-size (first last-size)
        new-size (second last-size)]
    (if (smaller-then-content? orientation size parent-content-size)
      (let [current (get-property component [:this] :position-matrix)]
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
(fg/defevolverfn :last-size
  [(second old-last-size)
   (get-property component [:this] :clip-size)])

(fg/defevolverfn :clip-size
  (let [ scrolled-clip-rect (get-property component [:_ :content-pane] :clip-size)
         scrolled-content-rect (get-property component [:_ :content-pane] :content-size)
         min-scroller-len (:min-scroller-len component)]
    (if (or (nil? scrolled-clip-rect) (nil? scrolled-clip-rect))
      (m/defpoint min-scroller-len min-scroller-len 0)
      (let [ orientation (get-property component [] :orientation)
             clip-size (max ((orient-fn orientation) scrolled-clip-rect) 0)
             content-size ((orient-fn orientation) scrolled-content-rect)
             ratio (if (> content-size clip-size) (/ clip-size content-size) 1.0)
             bar-rect (get-property component [] :clip-size)
             bar-size ((orient-fn orientation) bar-rect)
             scroller-size (max (* bar-size ratio) min-scroller-len)]
        (if (= :vertical orientation)
          (m/defpoint (m/x bar-rect) scroller-size 0)
          (m/defpoint scroller-size (m/y bar-rect) 0))))))

(fg/defevolverfn scroller-position-matrix-evolver :position-matrix
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

(fg/defevolverfn :content-size
  (get-property component [:this] :clip-size))

(fg/defwidget "scroller"
  {:min-scroller-len default-min-scroller-len
   :skin-key [:scrollbar :scroller]
   :evolvers {:position-matrix scroller-position-matrix-evolver
              :last-position-matrix last-position-matrix-evolver
              :last-size last-size-evolver
              :clip-size clip-size-evolver
              :content-size content-size-evolver}}
  flatgui.widgets.floatingbar/floatingbar)

(fg/defevolverfn scrollbar-visible-evolver :visible
  (let [ scrolled-clip-rect (get-property component [:content-pane] :clip-size)
         scrolled-content-rect (get-property component [:content-pane] :content-size)]
    (if (= (:orientation component) :vertical)
      (if (<= (- (m/y scrolled-content-rect) (m/y scrolled-clip-rect)) (* 2 (awt/px)))
        false
        (flatgui.widgets.component/visible-evolver component))
      (if (<= (- (m/x scrolled-content-rect) (m/x scrolled-clip-rect)) (* 2 (awt/px)))
        false
        (flatgui.widgets.component/visible-evolver component)))))

(fg/defwidget "scrollbar"
  {:orientation :vertical
   :skin-key [:scrollbar :scrollbar]
   :evolvers {:visible scrollbar-visible-evolver}
   :children {:scroller (fg/defcomponent scroller :scroller {})}}
  flatgui.widgets.component/component)