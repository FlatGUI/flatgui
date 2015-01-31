; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Button widget"
      :author "Denys Lebediev"}
  flatgui.widgets.button
  (:require [flatgui.base :as fg]
            [flatgui.widgets.abstractbutton]))

(fg/defwidget "button"
           {:skin-key [:button :regular]
            :evolvers {:pressed flatgui.widgets.abstractbutton/regular-pressed-evolver}
            ;; TODO move out
            :foreground :prime-4}
           flatgui.widgets.abstractbutton/abstractbutton)

(fg/defwidget "checkbutton"
           {:skin-key [:button :regular]
            ;; TODO move out
            :foreground :prime-4
            :evolvers {:pressed flatgui.widgets.abstractbutton/check-pressed-evolver}}
           flatgui.widgets.abstractbutton/abstractbutton)

(fg/defwidget "rolloverbutton"
           {:skin-key [:button :rollover]
            ;; TODO move out
            :foreground :prime-4
            :evolvers {:pressed flatgui.widgets.abstractbutton/regular-pressed-evolver
                       :foreground (fg/accessorfn (if (get-property component [:this] :has-mouse)
                                                    (:prime-4 (get-property component [:this] :theme))
                                                    (:prime-6 (get-property component [:this] :theme))))}}
           flatgui.widgets.abstractbutton/abstractbutton)