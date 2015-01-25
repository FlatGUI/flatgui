; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Menu with placeholder for icon or check component on the left of the item"
      :author "Denys Lebediev"}
  flatgui.widgets.menu (:use
                           flatgui.comlogic
                           flatgui.base
                           flatgui.theme
                           flatgui.paint
                           flatgui.widgets.component
                           flatgui.widgets.table.columnheader
                           flatgui.widgets.abstractmenu
                           flatgui.inputchannels.mouse
                           clojure.test))

(def MENU_COLUMNS [:icon :text])

(defevolverfn menu-icon-clip-size-evolver :clip-size
  (let [ row-h (get-property component [:_ :content-pane] :row-height)]
    (defpoint row-h row-h)))

(defevolverfn menu-text-clip-size-evolver :clip-size
  (let [ header-size (get-property component [] :clip-size)
         row-h (get-property component [:_ :content-pane] :row-height)
         icon-size (get-property component [:icon] :clip-size)]
    (defpoint (- (x header-size) (x icon-size)) row-h)))

(defwidget "menu"
  { :header-ids MENU_COLUMNS
    :children (array-map
                :header (defcomponent menuheader
                          :header { :children { :icon (defcomponent columnheader :icon
                                                        { :evolvers {:clip-size menu-icon-clip-size-evolver}})
                                                :text (defcomponent columnheader :text
                                                        {:evolvers {:clip-size menu-text-clip-size-evolver}})}})
                )}
  abstractmenu)

;
; Tests
;