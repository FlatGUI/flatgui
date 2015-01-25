; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Base type for menu widget"
      :author "Denys Lebediev"}
  flatgui.widgets.abstractmenu (:use
                           flatgui.awt
                           flatgui.comlogic
                           flatgui.base
                           flatgui.theme
                           flatgui.paint
                           flatgui.widgets.component
                           flatgui.widgets.table
                           flatgui.widgets.table.commons
                           flatgui.widgets.table.contentpane
                           flatgui.widgets.table.header
                           flatgui.widgets.table.columnheader
                           flatgui.widgets.menu.menucell
                           flatgui.inputchannels.mouse
                           clojure.test))

;(deflookfn menu-look (:theme)
;  (call-look component-look)
;  (setColor (:dark theme))
;  (drawRect 0 0 w- h-))

(defwidget "menucontentpane"
  { :default-cell-component menucell
    :selection-mode :single
    :mouse-triggers-selection? (fn [component] (mouse-entered? component))
    }
  tablecontentpane)

(defwidget "menuheader"
  { :visible :false
    :default-height 0
    :evolvers { :visible (fn [_] false)}
    }
  tableheader)

(defwidget "abstractmenu"
  {
    :popup true
    ;:look menu-look
    :evolvers {:z-position flatgui.widgets.component/z-position-evolver }
    :children (array-map
                :header (defcomponent menuheader :header {})
                :content-pane (defcomponent menucontentpane :content-pane {}))}
  table)

;
; Tests
;