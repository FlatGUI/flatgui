; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Base component for table cell"
      :author "Denys Lebediev"}
    flatgui.widgets.table.abstractcell
  (:use
        flatgui.comlogic




        ;flatgui.widgets.componentbase
        ;flatgui.widgets.component
        ;flatgui.widgets.label



        )
  (:require [flatgui.base :as fg]
            [flatgui.util.circularbuffer :as cb]
            [flatgui.util.matrix :as m]
            [flatgui.widgets.table.commons :as tcom]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.inputchannels.keyboard :as keyboard]
            [flatgui.widgets.component]))


;(defn cell-z-position-evolver [c]
;  (let [ super-z-position-evolver (get-in component [:initializers :z-position])]
;    (- (super-z-position-evolver c) 2)))

(fg/defevolverfn :screen-row
  (let [ visible-screen-rows (:cbuf (get-property component [] :visible-screen-rows))]
      (cb/get-element-position visible-screen-rows (:cbuf-index component))))

(fg/defevolverfn :position-matrix
  (let [ column-x-locations (get-property component [:_ :header] :column-x-locations)
         header-x (if column-x-locations (column-x-locations (:header-id component)) 0)
         visible-screen-rows (get-property component [] :visible-screen-rows)
         visible-screen-rows-index (- (get-property component [:this] :screen-row) (nth (:rows visible-screen-rows) 0))]
    ;; TODO use new invariant macro here
    ;;
    ;; Otherwise :screen-row is not up to date
    (if (and (>= visible-screen-rows-index 0) (< visible-screen-rows-index (count (:y-locations visible-screen-rows))))
      (let [ row-y (nth (:y-locations visible-screen-rows) visible-screen-rows-index)]
          (m/mx-set! (m/mx-set! old-position-matrix 1 3 row-y) 0 3 header-x))
      old-position-matrix)))

(fg/defevolverfn :content-size
  (let [column-widths (get-property component [:_ :header] :column-widths)
        header-w (if column-widths (column-widths (:header-id component)) 0)
        row-group-count (nth (get-property component [:this] :row-grouping-state) 0)
        row-h (:row-height component)
        h (if row-group-count (* row-group-count row-h) row-h)]
      (m/mx-set! (m/mx-set! old-content-size 1 0 h) 0 0 header-w)))

(fg/defevolverfn :clip-size
    (get-property component [:this] :content-size))

(fg/defevolverfn :selection
  (let [selection-model (get-property component [] :selection-model)]
    (if selection-model
      (let [anchor-model-row (tcom/get-anchor-model-row selection-model)
            selected-model-rows (tcom/get-selected-model-rows selection-model)
            this-model-row (tcom/get-model-row component)
            screen-row (get-property component [:this] :screen-row)
            row-grouping-state (get-property component [:this] :row-grouping-state)
            grouping (nth row-grouping-state 0)
            index-in-group (nth row-grouping-state 1)
            anchor-screen-col (tcom/get-anchor-screen-col selection-model)
            this-screen-col (get-property component [:this] :screen-col)
            this-screen-rows (if grouping (range (- screen-row index-in-group) (+ screen-row grouping)) [screen-row])
            this-model-rows (if (and grouping (> (count this-screen-rows) 1))
                              (let [ row-order (get-property component [] :row-order)]
                                (set (for [s this-screen-rows] (tcom/screen-row-to-model row-order s))))
                              [this-model-row])]
        (if (>= this-model-row 0)
          [(and (some #(= %1 anchor-model-row) this-model-rows) (= anchor-screen-col this-screen-col))
           (some (fn [sm] (some #(= %1 sm) this-model-rows)) selected-model-rows)]
          old-selection)))))

(fg/defevolverfn :anchor (nth (get-property component [:this] :selection) 0))

(fg/defevolverfn :background
  (let [selection (get-property component [:this] :selection)]
    (if (nth selection 1)
      (if (nth selection 0)
        (get-property component [:this] :anchor-background)
        (get-property component [:this] :selected-background))
      (get-property component [:this] :nonselected-background))))

(fg/defevolverfn :foreground
  (let [selection (get-property component [:this] :selection)]
    (if (nth selection 1)
      (if (nth selection 0)
        (get-property component [:this] :anchor-foreground)
        (get-property component [:this] :selected-foreground))
      (get-property component [:this] :nonselected-foreground))))

(fg/defevolverfn abstractcell-visible-evolver :visible
  (let [screen-row (get-property component [:this] :screen-row)
        visible-screen-rows (get-property component [] :visible-screen-rows)
        first-visible-row (nth (:rows visible-screen-rows) 0)
        last-visible-row (nth (:rows visible-screen-rows) 1)]
    (and
      (flatgui.widgets.component/visible-evolver component)
      (tcom/row-visible? screen-row first-visible-row last-visible-row))))

;;; TODO measure whether performance impact. It looks like it degrades actually rather than increase
;;;
;;;(fg/defevolverfn cell-visible-screen-rows-evolver :visible-screen-rows
;;;  (get-property component [] :visible-screen-rows))

(defn cell-consumes? [component]
  (if
    (or
      (mouse/is-mouse-event? component)
      ;;TODO consume key event when in cell editor mode
      (keyboard/key-event? component))
    false
    true))

(fg/defwidget "abstractcell"
  { ;;TODO move out
    :nonselected-background :prime-1
    :anchor-background :prime-2
    :selected-background :prime-2

    :nonselected-foreground :prime-6
    :anchor-foreground :prime-4
    :selected-foreground :prime-4

    :header-id nil
    :cbuf-index 0
    :screen-row -1
    :screen-col 0
    :selection nil
    :anchor false
    :position-matrix m/IDENTITY-MATRIX
    :content-size (m/defpoint 1 1 0)
    :clip-size (m/defpoint 1 1 0)
    :consumes? cell-consumes?
;    :visible-screen-rows {:rows [0 0 0 0]
;                          :cols [0 0 0 0]
;                          :cbuf (cbuf 0 0)
;                          :y-locations nil}
    :evolvers { ;:abs-position-matrix TODO either more optimal :abs-position-matrix for cell, or do not calculate it at all,
                                     ;TODO but rather let evolve-component deal with it. With this implementation, moving
                                     ;TODO a window that contains table is very slow. Now it relies on a workarpund: a property in
                                     ;TODO content pane that reacts on grouping state and therefore invalidates whole content pane

                :visible abstractcell-visible-evolver
                :screen-row screen-row-evolver
                :position-matrix position-matrix-evolver
                :content-size content-size-evolver
                :clip-size clip-size-evolver
                :selection selection-evolver
                :background background-evolver
                :foreground foreground-evolver
                :anchor anchor-evolver

                ; TODO measure whether performance impact. It looks like it degrades actually rather tham increase
                ;
                ;:visible-screen-rows cell-visible-screen-rows-evolver
                }}
  flatgui.widgets.component/componentbase)