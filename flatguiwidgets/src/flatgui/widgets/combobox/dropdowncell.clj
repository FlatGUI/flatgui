; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Table cell implementation for menus"
      :author "Denys Lebediev"}
  flatgui.widgets.combobox.dropdowncell (:use flatgui.awt
                                            flatgui.comlogic
                                            flatgui.base
                                            flatgui.theme
                                            flatgui.paint
                                            flatgui.widgets.abstractbutton
                                            flatgui.widgets.menu.menucell
                                            flatgui.widgets.table.commons
                                            flatgui.inputchannels.mouse
                                            flatgui.inputchannels.keyboard
                                            clojure.test))


;
; @todo if selection changes because of keyboard - apply value, but do not close dropdown
;

(defevolverfn dropdowncell-pressed-evolver :pressed
  (regular-pressed-evolver component))


(defwidget "dropdowncell"
  {:evolvers {:pressed dropdowncell-pressed-evolver

                :text (accessorfn (let [ model-row (get-model-row component)
                                         model (get-property component [:_ :_] :model)]
                                   (if (>= model-row 0)
                                     (str (nth (get-property component [:_ :_] :model) model-row))
                                     ""
                                     )))
                }
    }
  menucell)

(println "!!!! dropdowncell rollover disabled " (:rollover-notify-disabled dropdowncell))

