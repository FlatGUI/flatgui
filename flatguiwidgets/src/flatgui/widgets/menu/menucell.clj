; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Table cell implementation for menus"
      :author "Denys Lebediev"}
  flatgui.widgets.menu.menucell
  (:require [flatgui.base :as fg]
            [flatgui.widgets.abstractbutton]
            [flatgui.widgets.label]
            [flatgui.widgets.table.commons :as tcom]))


(fg/defwidget "menucell"
  {;TODO move out
   :nonselected-background :prime-3
   :h-alignment :right
   :skin-key [:menu :menucell]
   :evolvers {:pressed flatgui.widgets.abstractbutton/regular-pressed-evolver
              :text (fg/accessorfn (let [model-row (tcom/get-model-row component)]
                                     (if (>= model-row 0)
                                       (let [value-provider (get-property component [:_] :value-provider)]
                                         (value-provider
                                           model-row
                                           (:screen-col component)))
                                      (str (:id component) model-row))))}}
  flatgui.widgets.abstractbutton/abstractbutton,
  flatgui.widgets.table.abstractcell/abstractcell)