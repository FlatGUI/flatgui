; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Regular table cell implementation. Supports row grouping"
      :author "Denys Lebediev"}
  flatgui.widgets.table.cell
  (:require [flatgui.paint :as fgp]
            [flatgui.awt :as awt]
            [flatgui.base :as fg]
            [flatgui.widgets.table.commons :as tcom]
            [flatgui.widgets.label]
            [flatgui.widgets.table.abstractcell]
            [flatgui.widgets.component]))


;;; TODO move to theme namespace
;;;
(fgp/deflookfn tablecell-look (:theme :anchor :text :h-alignment :v-alignment :foreground)
  [(awt/setColor (:prime-4 theme))
   (awt/drawRect 0 0 w- h-)
   (awt/setColor background)
   (awt/fillRect 0 0 w- h-)
   (flatgui.widgets.label/label-look-impl foreground text h-alignment v-alignment 0 0 w h)
   (if anchor (awt/setColor (:prime-2 theme)))
   (if anchor (awt/drawRect 0 0 (awt/-px w-) (awt/-px h-)))])

(fg/defevolverfn :row-grouping-state
  (let [screen-row (get-property component [:this] :screen-row)
        visible-screen-rows (get-property component [] :visible-screen-rows)
        first-visible-row (nth (:rows visible-screen-rows) 0)
        header-id (:header-id component)
        mode (get-property component [:_ :header header-id :grouping] :mode)]
    (if (and (not (nil? mode)) (not= :none mode))
      (let [groups (get-property component [:_ :header header-id :grouping] :row-groups)
            group-inds (nth groups 0)
            group-nums (nth groups 1)
            screen-row-out-of-groups (or (>= screen-row (count group-inds)) (< screen-row 0))
            index-in-group (if (or (nil? groups) screen-row-out-of-groups) 0 (nth group-inds screen-row))
            group-num (if (or (nil? groups) screen-row-out-of-groups) 1 (nth group-nums screen-row))]
        (cond
          (= 0 index-in-group) [group-num index-in-group]
          (= screen-row first-visible-row) [(- group-num index-in-group) index-in-group]))
      [1 0])))

(fg/defevolverfn cell-visible-evolver :visible
  (let [screen-row (get-property component [:this] :screen-row)
        visible-screen-rows (get-property component [] :visible-screen-rows)
        first-visible-row (nth (:rows visible-screen-rows) 0)
        last-visible-row (nth (:rows visible-screen-rows) 1)
        first-visible-col (nth (:cols visible-screen-rows) 0)
        last-visible-col (nth (:cols visible-screen-rows) 1)
        grouping (nth (get-property component [:this] :row-grouping-state) 0)]
    (and
      (flatgui.widgets.component/visible-evolver component)
      (tcom/row-visible? screen-row first-visible-row last-visible-row)
      (tcom/col-visible? (get-property component [:this] :screen-col) first-visible-col last-visible-col)
      (or grouping (= screen-row first-visible-row)))))

(fg/defwidget "tablecell"
  {:row-grouping-state nil
   :allowed-by-row-grouping true
   :look tablecell-look
   ;; Optimization for RIA. By default, do not send mouse move event for table cells.
   ;; Override this for some special cases, e.g. cells containing rollover buttons.
   :rollover-notify-disabled true
   :roll-test-1 true
   ;; TODO move out
   ;:foreground :prime-6
   :nonselected-background :prime-3
   :evolvers {:row-grouping-state row-grouping-state-evolver
              :visible cell-visible-evolver
              :text (fg/accessorfn (let [model-row (tcom/get-model-row component)]
                                     (if (>= model-row 0)
                                       (let [value-provider (get-property component [:_] :value-provider)]
                                         (str (value-provider model-row (:screen-col component))))
                                     "")))}}
  flatgui.widgets.table.abstractcell/abstractcell)