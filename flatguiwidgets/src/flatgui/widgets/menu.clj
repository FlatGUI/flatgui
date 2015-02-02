; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Menu with placeholder for icon or check component on the left of the item"
      :author "Denys Lebediev"}
  flatgui.widgets.menu
  (:require [flatgui.base :as fg]
            [flatgui.widgets.abstractmenu]
            [flatgui.widgets.table.columnheader]
            [flatgui.util.matrix :as m]))

(def menu-columns [:icon :text])

(fg/defevolverfn menu-icon-clip-size-evolver :clip-size
  (let [row-h (get-property component [:_ :content-pane] :row-height)]
    (m/defpoint row-h row-h)))

(fg/defevolverfn menu-text-clip-size-evolver :clip-size
  (let [header-size (get-property component [] :clip-size)
        row-h (get-property component [:_ :content-pane] :row-height)
        icon-size (get-property component [:icon] :clip-size)]
    (m/defpoint (- (m/x header-size) (m/x icon-size)) row-h)))

(fg/defwidget "menu"
  {:header-ids menu-columns
   :children (array-map
               :header (fg/defcomponent flatgui.widgets.abstractmenu/menuheader
                         :header { :children { :icon (fg/defcomponent flatgui.widgets.table.columnheader/columnheader :icon
                                                       {:evolvers {:clip-size menu-icon-clip-size-evolver}})
                                               :text (fg/defcomponent flatgui.widgets.table.columnheader/columnheader :text
                                                       {:evolvers {:clip-size menu-text-clip-size-evolver}})}}))}
   flatgui.widgets.abstractmenu/abstractmenu)