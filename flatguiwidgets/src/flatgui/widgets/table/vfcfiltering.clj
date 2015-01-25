; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Filtering table colum Vertical Feature"
      :author "Denys Lebediev"}
  flatgui.widgets.table.vfcfiltering (:use flatgui.base
                                           flatgui.theme
                                           flatgui.paint
                                           flatgui.widgets.componentbase
                                           flatgui.widgets.table.commons
                                           flatgui.widgets.table.vfc
                                           clojure.test))

; V-Feature functionalilty

(defaccessorfn apply-filtering [contentpane prev-header-ids header-id prev-row-order modes]
  (let [ mode (get-from-header-vfc contentpane header-id :filtering :mode)]
    (if (= :f mode)
      (let [ value-provider (get-property contentpane [] :value-provider)
             header-ids (get-property contentpane [] :header-ids)
             ;@todo do not duplicate these .indexOf calls
             column-index (.indexOf header-ids header-id)
             key-fn (fn [row-order-item]
                      (value-provider row-order-item column-index))
             selection-model (get-property contentpane [:this] :selection-model)
             visible-values (map (fn [row] (key-fn row)) (get-selected-model-rows selection-model))]
          (filter
            (fn [row-order-item]
              (if (empty? visible-values) true (some #(= %1 (key-fn row-order-item)) visible-values)))
            prev-row-order))
        prev-row-order)))

(defaccessorfn apply-filtering-feature [contentpane prev-row-order modes]
  (apply-vf-by-degree contentpane :filtering apply-filtering prev-row-order modes))

; Params

(defwidget vfcfiltering
  { :apply-feature apply-filtering-feature
    :mode-vec [:none :f]
    :mode :none
    :look filtering-look}
  vfc)
