; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Table cell implementation for menus"
      :author "Denys Lebediev"}
  flatgui.widgets.menu.menucell (:use flatgui.awt
                                   flatgui.comlogic
                                   flatgui.base
                                   flatgui.theme
                                   flatgui.paint
                                   flatgui.widgets.table.abstractcell
                                   flatgui.widgets.table.commons
                                   flatgui.widgets.componentbase
                                   flatgui.widgets.component
                                   flatgui.widgets.label
                                   flatgui.widgets.abstractbutton
                                   flatgui.inputchannels.mouse
                                   flatgui.inputchannels.keyboard
                                   flatgui.util.matrix
                                   flatgui.util.circularbuffer
                                   clojure.test
                                   clojure.stacktrace))



(deflookfn menucell-look (:theme :anchor :id)
  [(flatgui.awt/setColor background)

   ; @todo 1 px is cut temporarily: until borders are introduced
   ;(flatgui.awt/fillRect 0 0 (x content-size) (y content-size))
   (flatgui.awt/fillRect (px) 0 (-px (x content-size) 2) (-px (y content-size)))

   (call-look label-look)
   ])


(defwidget "menucell"
  {                                                         ;:anchor-background :active-selection
   :nonselected-background :prime-3
   :h-alignment :right
    :look menucell-look
    :evolvers { :pressed regular-pressed-evolver
                :text (accessorfn (let [ model-row (get-model-row component)]
                                    (if (>= model-row 0)
                                      (let [ value-provider (get-property component [:_] :value-provider)]
                                          (value-provider
                                            model-row
                                            (:screen-col component)))
                                      (str (:id component) model-row);""
                                      )))}
    }
  abstractbutton, abstractcell)

