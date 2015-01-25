; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Panel widget"
      :author "Denys Lebediev"}
  flatgui.widgets.panel (:use flatgui.awt
                              flatgui.comlogic
                              flatgui.base
                              flatgui.theme
                              flatgui.paint
                              flatgui.widgets.componentbase
                              flatgui.widgets.component
                              clojure.test))

(deflookfn panel-look (:theme)
    (call-look component-look)
;     (setColor (:light theme))
;     (drawLine 0 0 w- 0)
;     (drawLine 0 h- w- h-)
;     (drawLine 0 0 0 h-)
;     (drawLine w- 0 w- h-)
    )

(defwidget "panel"
  (array-map
    :look panel-look)
  component)

;
; Tests
;