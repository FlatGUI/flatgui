; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc    "Utilities for arranging container component layout."
      :author "Denys Lebediev"}
flatgui.layout
    (:require [flatgui.base :as fg]
      [flatgui.awt :as awt]
      [flatgui.util.matrix :as m] [flatgui.util.matrix :as m])
  (:import (java.util.regex Pattern)))


(def gap 0.125)

(def component-min-size (m/defpoint 0.375 0.375))

(def cmd->smile {:h-stretch :-
                 :v-stretch :|
                 :top-align :'
                 :btm-align :.
                 :l-align :<
                 :r-align :>})

(def axis-switch {\- \|   ; "stretcher by x flag" -> "stretcher by y flag"
                  \< \'}) ; "beginner by x flag" -> "beginner by y flag"

(def smile-pattern (Pattern/compile "(\\||\\-|\\.|'|\\<|\\>)+"))

(defn smile? [kw] (and (keyword? kw) (.matches (.matcher smile-pattern (name kw)))))

(defn- combine-flags
  ([] "")
  ([a] a)
  ([a b] (str a b)))

(fg/defaccessorfn get-child-preferred-size [component child-id]
  (let [abs-pref-size (if-let [own-pref-size (get-property component [:this child-id] :preferred-size)]
                        own-pref-size
                        (if-let [text (get-property component [:this child-id] :text)]
                          (let [interop (get-property component [:this child-id] :interop)]
                               (if (get-property component [:this child-id] :multiline)
                                 (awt/get-text-preferred-size
                                   (if-let [lines (:lines (get-property component [:this child-id] :multiline))] lines [text])
                                   interop)
                                 (awt/get-text-preferred-size [text] interop)))
                          component-min-size))
        container-size (get-property [:this] :clip-size)]
    (m/defpoint
      (/ (m/x abs-pref-size) (m/x container-size))
      (/ (m/y abs-pref-size) (m/y container-size)))))

(fg/defaccessorfn get-child-minimum-size [component child-id]
  (let [abs-min-size (if-let [own-min-size (get-property component [:this child-id] :minimum-size)]
                       own-min-size
                       component-min-size)
        container-size (get-property [:this] :clip-size)]
    (m/defpoint
      (/ (m/x abs-min-size) (m/x container-size))
      (/ (m/y abs-min-size) (m/y container-size)))))

(declare cfg->flags)

(defn- cfg->flags-mapper [v]
  (cond

    (keyword? v)
    {:element v :flags nil}

    (sequential? v)
    (let [grouped (group-by smile? v)
          _ (println "groupe2" grouped)
          elements (get grouped false)
          elem-count (count elements)
          smiles (get grouped true)
          flags (if smiles (reduce combine-flags (map name smiles)))]
        (cond
          (> elem-count 1) (cfg->flags (mapv (fn [e] (if flags [e (keyword flags)] [e])) elements))
          (and (= elem-count 1) (sequential? (nth elements 0))) (nth (cfg->flags elements) 0)
          (= elem-count 1) {:element (nth elements 0) :flags flags}
          (= elem-count 0) (throw (IllegalArgumentException. (str "Wrong layout cfg (nothing but flags specified): " v)))))

   :else
   (throw (IllegalArgumentException. (str "Wrong layout cfg: " v)))))

(defn cfg->flags [cfg]
  (let [_ (println "------" cfg)]
       (if (and (sequential? cfg) (every? keyword? cfg))
         (cfg->flags-mapper cfg)
         (mapv cfg->flags-mapper cfg))))

;(defn map-direction
;  ([cfg w stcher bgner sfn space-share])
;  ([cfg w space-share] (map-direction cfg w \- \< m/x 1.0)))

(fg/defaccessorfn map-direction [component cfg w stcher bgner sfn space-share]
  (let [stretches? (fn [element] (.contains (:flags element) stcher))
        dir (map #(assoc %
                         :min (get-child-minimum-size component (:element %))
                         :pref (get-child-preferred-size component (:element %))) (cfg->flags cfg))
        grouped-by-stretch (group-by stretches? dir)
        stretching (get grouped-by-stretch true)
        stable (get grouped-by-stretch false)
        stable-pref-total (reduce + (map #(sfn (:pref %)) stable))
        stretching-min-total (reduce + (map #(sfn (:min %)) stretching))]
    (if (< (+ stable-pref-total stretching-min-total) 1.0)
      ;; TODO distribute (- 1.0 stable-pref-total)
      nil
      ;; TODO
      nil)))

;(fg/defevolverfn :coord-map
; (if-let [layout (map cmd->smile (get-property [:this] :layout))]
;   (let [
;         ;No need in this, should contain rel coords ;cs (get-property [:this] :clip-size)
;         ;w (m/x cs)
;         ;h (m/y cs)
;         ])))

;;; 1. After map-direction is used for elements of each row, each row will
;;; receive it's own set of smile flags. Then map-direction will be used
;;; for rows
;;; 2. Split all elements of a direction into two sub-categories:
;;;   a) elements that do not want extra space
;;;   b) stretching elements that want extra space
;;; See how much space totally (a) elements plus minimum sizes of (b) elements want.
;;;   - if more than avaialble then
;;;      - either rescale everything accorrding to previous proportions
;;;      - or if there was nothing previously, distribute equally
;;;   - if less than or equal to available:
;;;      - each (a) component receives just what it needs
;;;      - what remains is distributed between (b) components
;;;          - either equally if nothing else specified
;;;          - or according to specified weights (may be specified with digits or
;;;            with smile lengths like :--- or :|||)



;(fg/defevolverfn :position-matrix)
;
;(fg/defevolverfn :clip-size)

