; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Sorting table colum Vertical Feature"
      :author "Denys Lebediev"}
  flatgui.widgets.table.vfcsorting
  (:require [flatgui.base :as fg]
            [flatgui.widgets.table.commons :as tcom]
            [flatgui.widgets.table.vfc :as vfc])
  (:import [java.util Collections Comparator]))


(fg/defevolverfn sorting-header-should-evolve :should-evolve-header
  (let [current old-should-evolve-header
        new (get-property component [] :mouse-down)]
      (and (not current) new)))

(fg/defevolverfn sorting-mode-evolver :mode
  (let [vfc-clicked (tcom/should-evolve-header component)
        column-header-clicked (get-property component [:this] :should-evolve-header)]
    (if (and
          (or vfc-clicked column-header-clicked)
          ;; This condition is needed to prevent get-new-mode to be evaluated twice
          ;; (second time because of [:_] :active-headers dependency)
          (not= [:_] ((:evolve-reason-provider component) (:id component))))
      (vfc/get-new-mode component)
      (let [ header-id (get-property component [] :id)
             active-headers (get-property component [:_] :active-headers)]
        (if (some #(= header-id %1) active-headers)
          old-mode
          :none)))))

(fg/defevolverfn sorting-degree-evolver :degree
  (let [r (if (= :none (get-property component [:this] :mode))
            -1
            (let [header-id (get-property component [] :id)
                  active-headers (vec (get-property component [:_] :active-headers))]
              (.indexOf active-headers header-id)))
        ;_ (println " sorting-degree-evolver called with reason " (get-reason) old-degree " -> " r)
        ]
    r))

;;;
;;; V-Feature functionalilty
;;;

(defn- mask-nil [o] (if (nil? o) "" o))

(def asc-comparator
  (proxy [Comparator] []
    (compare [o1 o2] (.compareTo (mask-nil o1) o2))))

(def desc-comparator (Collections/reverseOrder asc-comparator))

(fg/defaccessorfn apply-sorting [contentpane prev-header-ids header-id prev-row-order modes]
  (let [ mode (header-id modes)
        ;_ (println "apply-sorting called for " header-id " mode = " mode)
        ]
    (if (= :none mode)
      prev-row-order
      (let [row-count (count prev-row-order)
            key-fn (fn [row-order-item]
                     (vfc/get-value-from-col contentpane row-order-item header-id))
            sub-ranges (if (nil? prev-header-ids)
                         [[0 row-count]]
                           (vfc/find-subranges
                             (for [screen-row (range 0 row-count)]
                               (map
                                 (fn [prev-header-id] (vfc/get-value-from-col
                                                        contentpane
                                                        (nth prev-row-order screen-row)
                                                        prev-header-id))
                               prev-header-ids))))]
        (loop [sr 0
               prev-row-order-rest prev-row-order
               result-row-order []]
          (if (= sr (count sub-ranges))
            result-row-order
            (recur
              (inc sr)
              (take-last
                (-
                  (count prev-row-order-rest)
                  (last (nth sub-ranges sr)))
                prev-row-order-rest)
              (into result-row-order
                (sort-by
                  key-fn
                  (if (= :asc mode) asc-comparator desc-comparator)
                  (take (last (nth sub-ranges sr)) prev-row-order-rest))))))))))

(fg/defaccessorfn apply-sorting-feature [contentpane prev-row-order modes]
  (vfc/apply-vf-by-degree contentpane :sorting apply-sorting prev-row-order modes))

; Params

(fg/defwidget vfcsorting
  {:last-active-headers nil
   :apply-feature apply-sorting-feature
   :mode-vec [:none :asc :desc]
   :mode :none
   :degree 0
   :should-evolve-header false
   :skin-key [:table :sorting]
   :evolvers {:should-evolve-header sorting-header-should-evolve
              :mode sorting-mode-evolver
              :degree sorting-degree-evolver}}
  vfc/vfc)