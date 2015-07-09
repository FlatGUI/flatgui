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
  (:require [flatgui.awt :as awt]
            [flatgui.paint :as fgp]
            [flatgui.base :as fg]
            [flatgui.widgets.component]
            [flatgui.widgets.table.columnheader]
            [flatgui.widgets.abstractmenu]
            [flatgui.widgets.combobox.dropdowncell]
            [flatgui.util.matrix :as m]))


(def dropdown-menu-columns [:text])

(def dropdown-default-row-height 0.25)


(fgp/deflookfn dropdown-content-look (:theme)
  (fgp/call-look flatgui.widgets.component/component-look)
  (awt/setColor (:prime-6 theme))
  (awt/drawRect 0 0 w- h-))

(fg/defevolverfn dropdown-contnent-row-count-evolver :row-count
  (let [items (get-property component [:_] :model)]
    (count items)))

(fg/defevolverfn menu-text-clip-size-evolver :clip-size
  (let [header-size (get-property component [] :clip-size)
        row-h (get-property component [:_ :content-pane] :row-height)]
    (m/defpoint (m/x header-size) row-h)))

(fg/defevolverfn dropdown-position-matrix-evolver :position-matrix
  (let [editor-size (get-property component [:editor] :clip-size)]
    (m/translation-matrix 0 (m/y editor-size))))

(fg/defevolverfn dropdown-clip-size-evolver :clip-size
  (let [combo-size (get-property component [] :clip-size)
        row-count (min
                     (get-property component [:this :content-pane] :row-count)
                     (get-property component [:this :content-pane] :maximum-visible-rows))
        row-height (get-property component [:this :content-pane] :row-height)]
    (m/defpoint (m/x combo-size) (* row-count row-height))))

(fg/defevolverfn dropdown-visible-evolver :visible
  (let [reason (fg/get-reason)]
    (cond
      (and
        (vector? reason)
        (= 3 (count reason))
        (flatgui.widgets.abstractbutton/button-pressed? (get-property component [:this :content-pane (nth reason 2)] :pressed-trigger)))
      false
      (and
        (= reason [:arrow-button])
        (flatgui.widgets.abstractbutton/button-pressed? (get-property component [:arrow-button] :pressed-trigger)))
      true
      (and
        (= reason [:editor])
        (not= :has-focus (:mode (get-property component [:editor] :focus-state))))
      false
      :else old-visible)))

(fg/defevolverfn dropdown-row-order-evolver :row-order
              (range 0 (get-property component [:this] :row-count)))

(fg/defwidget "dropdown"
  { :header-ids dropdown-menu-columns
    :value-provider (fn [model-row model-col] (str model-row "-" model-col))

   ;; TODO Decide: either non-focusable dropdown, or maybe focusable (accepts-focus? will be true) when open
   :focusable false

    :evolvers { :position-matrix dropdown-position-matrix-evolver
                :clip-size dropdown-clip-size-evolver
                :visible dropdown-visible-evolver}
    :children {:header (fg/defcomponent flatgui.widgets.abstractmenu/menuheader :header
                         {:children {:text (fg/defcomponent flatgui.widgets.table.columnheader/columnheader :text {:evolvers {:clip-size menu-text-clip-size-evolver}})}})
               :content-pane (fg/defcomponent flatgui.widgets.abstractmenu/menucontentpane :content-pane
                               {

                                ;; TODO
                                ;; the problem is that is merges ALL map values, so :default-cell-component inherits
                                ;; parameters from supertype's :default-cell-component, and this is BAD. Need to turn
                                ;; this merging off carefully for ALL params except for special ones like children.
                                ;; assoc is used here to work around this problem quickly.
                                :default-cell-component (assoc flatgui.widgets.combobox.dropdowncell/dropdowncell :rollover-notify-disabled false)

                                :maximum-visible-rows 10
                                :row-height dropdown-default-row-height
                                :wheel-rotation-step-y dropdown-default-row-height
                                :row-count 0
                                :row-order []
                                :look dropdown-content-look
                                :evolvers {:row-count dropdown-contnent-row-count-evolver
                                           ; TODO Find out why regular evolver reverses row order for dropdowns and then get rid of this one
                                           :row-order dropdown-row-order-evolver}})}}
  flatgui.widgets.abstractmenu/abstractmenu)