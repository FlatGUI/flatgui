; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Grouping table colum Vertical Feature"
      :author "Denys Lebediev"}
  flatgui.widgets.table.vfcgrouping (:use flatgui.base
                                           flatgui.theme
                                           flatgui.paint
                                           flatgui.widgets.table.commons
                                           flatgui.widgets.table.vfc
                                           clojure.test))

; V-Feature functionalilty

(defaccessorfn apply-grouping [contentpane prev-header-ids header-id prev-row-order modes]
  prev-row-order)

(defaccessorfn apply-grouping-feature [contentpane prev-row-order modes]
  (apply-vf-by-degree contentpane :grouping apply-grouping prev-row-order modes))

(defevolverfn :row-groups
  ; This is temporary. Need to know which evolvers to invoke on which input event.
  ; Then this heavy evolver will not be called on each mouse move/click
  (if (vector? ((:evolve-reason-provider component) (:id component)))
    (let [ mode (get-property component [:this] :mode)
           vfc-degree-headers (get-property component [:_ :_ :content-pane] :vfc-degree-headers)]
      (if (and (not= :none mode) vfc-degree-headers)
        (let [ degree (get-property component [:this] :degree)
               degrees (reverse (range 0 (inc degree)))
               row-order (get-property component [:_ :_ :content-pane] :row-order)
               header-ids (get-property component [:_ :_] :header-ids)
               value-provider (get-property component [:_ :_] :value-provider)
               get-from-col (fn [model-row header-id] (let [ model-col (.indexOf header-ids header-id)] (value-provider model-row model-col)))
               c (count row-order)
               sorted-headers (:grouping vfc-degree-headers)
               grouper (fn [screen-row] (for [d degrees] (get-from-col (nth row-order screen-row) (nth sorted-headers d))))]
          (if (> c 0)
            (loop [ i 0
                    v (grouper 0)
                    g 0
                    r (transient (vec (make-array java.lang.Long c)))]
              (if (< i c)
                (let [ new-i (inc i)
                       new-v (if (< new-i c) (grouper new-i))]
                  (recur
                    new-i
                    new-v
                    (if (= v new-v) (inc g) 0)
                    (assoc! r i g)))
                (let [ group-nums (loop [ j 0
                                          g (nth r 0)
                                          n 1
                                          cv (transient (vec (make-array java.lang.Long c)))]
                                    (if (< j c)
                                      (let [ new-n (if (= 0 g)
                                                     (loop [ k (inc j)
                                                             gn 1]
                                                       (if (and (< k c) (not= (nth r k) 0)) (recur (inc k) (inc gn)) gn))
                                                     1)
                                             new-j (+ j new-n)]
                                        (recur
                                          new-j
                                          (nth r j)
                                          new-n
                                            (loop [ tj j
                                                    tcv cv]
                                              (if (<= tj new-j)
                                                (recur
                                                  (inc tj)
                                                  (assoc! cv tj new-n))
                                                tcv))))
                                      (persistent! cv)))]
                    [(persistent! r)
                     group-nums])))))
        nil))
    old-row-groups))

(defevolverfn grouping-degree-evolver :degree
  (if (not= :none (get-property component [:this] :mode))
    (let [ header-id (get-property component [] :id)
           header-ids (get-property component [:_ :_] :header-ids)
           c (count header-ids)]
      (loop [ i 0
              d 0]
        (let [ hi (nth header-ids i)]
          (if (and (< i c) (not= hi header-id))
            (recur
              (inc i)
              (if (not= :none (get-property component [:_ :_ :header hi :grouping] :mode))
                (inc d)
                d))
            d))))
    -1))

; Params

(defwidget vfcgrouping
  { :apply-feature apply-grouping-feature
    :mode-vec [:none :g]
    :mode :none
    :row-groups nil
    :look grouping-look
    :evolvers { :degree grouping-degree-evolver
                :row-groups row-groups-evolver}}
  vfc)


