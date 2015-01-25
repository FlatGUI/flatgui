; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Drop down menu for combo box"
      :author "Denys Lebediev"}
  flatgui.widgets.combobox.dropdown
                       (:use
                           flatgui.awt
                           flatgui.comlogic
                           flatgui.base
                           flatgui.theme
                           flatgui.paint
                           flatgui.widgets.component
                           flatgui.widgets.abstractbutton
                           flatgui.widgets.table.columnheader
                           flatgui.widgets.abstractmenu
                           flatgui.widgets.combobox.dropdowncell
                           flatgui.inputchannels.mouse
                           flatgui.util.matrix
                           clojure.test))

(def DROPDOWN_MENU_COLUMNS [:text])
(def DROPDOWN_DEFAULT_ROW_HEIGHT 0.25)


(deflookfn dropdown-content-look (:theme)
  (call-look component-look)
  (setColor (:prime-6 theme))
  (drawRect 0 0 w- h-))



(defevolverfn dropdown-contnent-row-count-evolver :row-count
  (let [ items (get-property component [:_] :model)]
    (count items)))

(defevolverfn menu-text-clip-size-evolver :clip-size
  (let [ header-size (get-property component [] :clip-size)
         row-h (get-property component [:_ :content-pane] :row-height)]
    (defpoint (x header-size) row-h)))

(defevolverfn dropdown-position-matrix-evolver :position-matrix
  (let [ editor-size (get-property component [:editor] :clip-size)]
    (transtation-matrix 0 (y editor-size))))

(defevolverfn dropdown-clip-size-evolver :clip-size
  (let [ combo-size (get-property component [] :clip-size)
         row-count (min
                     (get-property component [:this :content-pane] :row-count)
                     (get-property component [:this :content-pane] :maximum-visible-rows))
         row-height (get-property component [:this :content-pane] :row-height)]
    (defpoint (x combo-size) (* row-count row-height))))

(defevolverfn dropdown-visible-evolver :visible
  (let [ reason (get-reason)]
    (cond
      (and
        (vector? reason)
        (= 3 (count reason))
        (button-pressed? (get-property component [:this :content-pane (nth reason 2)] :pressed-trigger)))
      false
      (and
        (= reason [:arrow-button])
        (button-pressed? (get-property component [:arrow-button] :pressed-trigger)))
      true
      :else old-visible)))

(defevolverfn dropdown-row-order-evolver :row-order
              (range 0 (get-property component [:this] :row-count)))

(defwidget "dropdown"
  { :header-ids DROPDOWN_MENU_COLUMNS
    :value-provider (fn [model-row model-col] (str model-row "-" model-col))
    :evolvers { :position-matrix dropdown-position-matrix-evolver
                :clip-size dropdown-clip-size-evolver
                :visible dropdown-visible-evolver}
    :children {
                :header (defcomponent menuheader :header
                          {:children {:text (defcomponent columnheader :text {:evolvers {:clip-size menu-text-clip-size-evolver}})}})
                :content-pane (defcomponent menucontentpane :content-pane
                                {

                                  ;; TODO
                                  ;; the problem is that is merges ALL map values, so :default-cell-component inherits
                                  ;; parameters from supertype's :default-cell-component, and this is BAD. Need to turn
                                  ;; this merging off carefully for ALL params except for special ones like children.
                                  ;; assoc is used here to work around this problem quickly.
                                  :default-cell-component (assoc dropdowncell :rollover-notify-disabled false)

                                  :maximum-visible-rows 10
                                  :row-height DROPDOWN_DEFAULT_ROW_HEIGHT
                                  :wheel-rotation-step-y DROPDOWN_DEFAULT_ROW_HEIGHT
                                  :row-count 0
                                  :row-order []
                                  :look dropdown-content-look
                                  :evolvers {:row-count dropdown-contnent-row-count-evolver

                                             ; TODO Find out why regular evolver reverses row order for dropdowns and then get rid of this one
                                             :row-order dropdown-row-order-evolver
                                             }})
                }}
  abstractmenu)

(println "!!!! dropdown [][] rollover disabled " (get-in dropdown [:children :content-pane :default-cell-component :rollover-notify-disabled] ))

;
; Tests
;