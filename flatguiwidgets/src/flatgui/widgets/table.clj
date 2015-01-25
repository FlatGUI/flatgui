; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Table widget"
      :author "Denys Lebediev"}
  flatgui.widgets.table (:use flatgui.comlogic
                              flatgui.base
                              flatgui.theme
                              flatgui.paint
                              flatgui.widgets.component
                              flatgui.widgets.panel
                              flatgui.widgets.scrollpanel
                              flatgui.widgets.label
                              flatgui.widgets.table.commons
                              flatgui.widgets.table.contentpane
                              flatgui.inputchannels.mouse
                              clojure.test))




;; TODO sorting only by side (which is second column does not work, i.e. it works only for descending mode)

;; TODO deftable macro that would compute :header-ids from columns

(defwidget "table"
  (array-map
    :header-ids nil
    :header-aliases nil
    :value-provider nil
    :children (array-map
                :header panel
                :content-pane tablecontentpane)
   )
  scrollpanel)