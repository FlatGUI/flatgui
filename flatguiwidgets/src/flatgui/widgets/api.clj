; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.widgets.api
  (:require
    flatgui.access
    flatgui.widgets.component
    flatgui.widgets.label
    flatgui.widgets.panel
    flatgui.widgets.scrollpanel
    flatgui.widgets.table
    flatgui.widgets.table.cell
    flatgui.widgets.table.contentpane
    flatgui.widgets.table.header
    flatgui.widgets.table.columnheader
    flatgui.widgets.table.vfc
    flatgui.widgets.table.vfcsorting
    flatgui.widgets.table.vfcfiltering
    flatgui.widgets.table.vfcgrouping
    flatgui.widgets.floatingbar
    flatgui.widgets.window
    flatgui.widgets.toolbar
    flatgui.widgets.abstractbutton
    flatgui.widgets.button
    flatgui.widgets.textfield
    flatgui.widgets.slider
    flatgui.widgets.checkbox
    flatgui.widgets.radiobutton
    flatgui.widgets.spinner
    flatgui.widgets.abstractmenu
    flatgui.widgets.menu
    flatgui.widgets.combobox))


(def component flatgui.widgets.component/component)

(def label flatgui.widgets.label/label)

(def panel flatgui.widgets.panel/panel)

(def scrollpanel flatgui.widgets.scrollpanel/scrollpanel)

(def scrollpanelcontent flatgui.widgets.scrollpanel/scrollpanelcontent)

(def scrollpanelverticalbar flatgui.widgets.scrollpanel/scrollpanelverticalbar)

(def scrollpanelhorizontalbar flatgui.widgets.scrollpanel/scrollpanelhorizontalbar)

(def table flatgui.widgets.table/table)

(def tablecell flatgui.widgets.table.cell/tablecell)

(def tablecontentpane flatgui.widgets.table.contentpane/tablecontentpane)

(def tableheader flatgui.widgets.table.header/tableheader)

(def tablecolumnheader flatgui.widgets.table.columnheader/columnheader)

(def vfc flatgui.widgets.table.vfc/vfc)

(def vfcsorting flatgui.widgets.table.vfcsorting/vfcsorting)

(def vfcfiltering flatgui.widgets.table.vfcfiltering/vfcfiltering)

(def vfcgrouping flatgui.widgets.table.vfcgrouping/vfcgrouping)

(def floatingbar flatgui.widgets.floatingbar/floatingbar)

(def window flatgui.widgets.window/window)

(def toolbar flatgui.widgets.toolbar/toolbar)

(def abstractbutton flatgui.widgets.abstractbutton/abstractbutton)

(def button flatgui.widgets.button/button)

(def checkbutton flatgui.widgets.button/checkbutton)

(def rolloverbutton flatgui.widgets.button/rolloverbutton)

(def textfield flatgui.widgets.textfield/textfield)

(def slider flatgui.widgets.slider/slider)

(def checkbox flatgui.widgets.checkbox/checkbox)

(def radiobutton flatgui.widgets.radiobutton/radiobutton)

(def spinner flatgui.widgets.spinner/spinner)

(def spinnereditor flatgui.widgets.spinner/spinnereditor)

(def abstractmenu flatgui.widgets.abstractmenu/abstractmenu)

(def menucontentpane flatgui.widgets.abstractmenu/menucontentpane)

(def menuheader flatgui.widgets.abstractmenu/menuheader)

(def menu flatgui.widgets.menu/menu)

(def combobox flatgui.widgets.combobox/combobox)