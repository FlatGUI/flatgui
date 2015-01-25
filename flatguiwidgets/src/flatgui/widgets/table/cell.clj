; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Regular table cell implementation. Supports row grouping"
      :author "Denys Lebediev"}
  flatgui.widgets.table.cell (:use flatgui.awt
                                   flatgui.comlogic
                                   flatgui.base
                                   flatgui.theme
                                   flatgui.paint
                                   flatgui.widgets.table.abstractcell
                                   flatgui.widgets.table.commons
                                   flatgui.widgets.componentbase
                                   flatgui.widgets.component
                                   flatgui.widgets.label
                                   flatgui.inputchannels.mouse
                                   flatgui.inputchannels.keyboard
                                   flatgui.util.matrix
                                   flatgui.util.circularbuffer
                                   clojure.test
                                   clojure.stacktrace))


;; TODO move to theme namespace
;;
(deflookfn tablecell-look (:theme :anchor :text :h-alignment :v-alignment :foreground)
  [(flatgui.awt/setColor (:prime-4 theme))
   (flatgui.awt/drawRect 0 0 w- h-)
   (flatgui.awt/setColor background)
   (flatgui.awt/fillRect 0 0 w- h-)
   (label-look-impl foreground text h-alignment v-alignment 0 0 w h)
   ;(setColor (:theme-border theme))
   ;(drawLine 0 h- w- h-)
   ;(drawLine w- 0 w- h-)
   (if anchor (flatgui.awt/setColor (:prime-2 theme)))
   (if anchor (drawRect 0 0 (-px w-) (-px h-)))])


(defevolverfn :row-grouping-state
  (let [ screen-row (get-property component [:this] :screen-row)
         visible-screen-rows (get-property component [] :visible-screen-rows)
         first-visible-row (nth (:rows visible-screen-rows) 0)
         header-id (:header-id component)
         mode (get-property component [:_ :header header-id :grouping] :mode)]
    (if (and (not (nil? mode)) (not= :none mode))
      (let [ groups (get-property component [:_ :header header-id :grouping] :row-groups)
             group-inds (nth groups 0)
             group-nums (nth groups 1)
             screen-row-out-of-groups (or (>= screen-row (count group-inds)) (< screen-row 0))
             index-in-group (if (or (nil? groups) screen-row-out-of-groups) 0 (nth group-inds screen-row))
             group-num (if (or (nil? groups) screen-row-out-of-groups) 1 (nth group-nums screen-row))]
        (cond
          (= 0 index-in-group) [group-num index-in-group]
          (= screen-row first-visible-row) [(- group-num index-in-group) index-in-group]))
      [1 0])))

(defevolverfn cell-visible-evolver :visible
  (let [ screen-row (get-property component [:this] :screen-row)
         visible-screen-rows (get-property component [] :visible-screen-rows)
         first-visible-row (nth (:rows visible-screen-rows) 0)
         last-visible-row (nth (:rows visible-screen-rows) 1)
         first-visible-col (nth (:cols visible-screen-rows) 0)
         last-visible-col (nth (:cols visible-screen-rows) 1)
         grouping (nth (get-property component [:this] :row-grouping-state) 0)]
    (and
      (visible-evolver component)
      (row-visible? screen-row first-visible-row last-visible-row)
      (col-visible? (get-property component [:this] :screen-col) first-visible-col last-visible-col)
      (or grouping (= screen-row first-visible-row)))))

(defwidget "tablecell"
  {
    :row-grouping-state nil
    :allowed-by-row-grouping true
    :look tablecell-look

   ;; Optimization for RIA. By default, do not send mouse move event for table cells.
   ;; Override this for some special cases, e.g. cells containing rollover buttons.
   :rollover-notify-disabled true
   :roll-test-1 true

    ;; TODO move out
   ;:foreground :prime-6
    :nonselected-background :prime-3

    :evolvers {
                :row-grouping-state row-grouping-state-evolver
                :visible cell-visible-evolver
                :text (accessorfn (let [ model-row (get-model-row component)]
                                    (if (>= model-row 0)
                                      (let [ value-provider (get-property component [:_] :value-provider)]
                                          (str (value-provider model-row (:screen-col component))))
                                      "")))}
    }
  abstractcell)

