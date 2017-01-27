; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Filtering table colum Vertical Feature"
      :author "Denys Lebediev"}
    flatgui.widgets.table.vfcfiltering
  (:require [flatgui.base :as fg]
            [flatgui.widgets.table.vfc :as vfc]
            [flatgui.widgets.table.commons :as tcom]))

;;; V-Feature functionalilty

(fg/defaccessorfn apply-filtering [component prev-header-ids header-id prev-row-order modes]
  (let [mode (vfc/get-from-header-vfc component header-id :filtering :mode)]
    (if (= :f mode)
      (let [value-provider (get-property component [] :value-provider)
            header-ids (get-property component [] :header-ids)
            ;;TODO do not duplicate these .indexOf calls
            column-index (.indexOf header-ids header-id)
            key-fn (fn [row-order-item]
                      (value-provider row-order-item column-index))
            selection-model (get-property component [:this] :selection-model)
            visible-values (map (fn [row] (key-fn row)) (tcom/get-selected-model-rows selection-model))]
          (filter
            (fn [row-order-item]
              (if (empty? visible-values) true (some #(= %1 (key-fn row-order-item)) visible-values)))
            prev-row-order))
        prev-row-order)))

(fg/defaccessorfn apply-filtering-feature [component prev-row-order modes]
  (vfc/apply-vf-by-degree component :filtering apply-filtering prev-row-order modes))

;;; Params

(fg/defwidget vfcfiltering
  { :apply-feature apply-filtering-feature
    :mode-vec [:none :f]
    :mode :none
    :skin-key [:table :filtering]}
  vfc/vfc)
