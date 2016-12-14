; Copyright (c) 2016 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.core
  (:require [clojure.algo.generic.functor :as functor]
            [flatgui.dependency])
  (:import (flatgui.core.engine GetPropertyStaticClojureFn GetDynPropertyDynPathClojureFn GetDynPropertyClojureFn GetPropertyDynPathClojureFn)))

(defn get-property-call? [form]
  (and
    (= 'get-property (first form))
    (or
      (vector? (second form))
      (and (= 'component (second form)) (vector? (first (next (next form))))))))

(defn get-reason-call? [form]
  (or
    (= 'get-reason (first form))
    ;; 'fg/get-reason is here for backward compatibility
    (= 'fg/get-reason (first form))))

;;; TODO add get-full-reason: include property also; maybe also actual node instead of wildcard

(def old-val-prefix "old-")

(defn old-value-ref? [e property]
  (and (not (nil? property)) (symbol? e) (.startsWith (name e) old-val-prefix) (.endsWith (name e) (name property))))

(declare replace-gp)
(declare replace-gpv)
(declare replace-gpmap)

(defn- gp-replacer [% property]
  (cond
    (and (seq? %) (get-property-call? %))
    (let [% (if (symbol? (second %)) (conj (drop 2 %) (first %)) %) ;backward compatibility
          % (replace-gp % property)
          path-&-prop (next %)
          path (first path-&-prop)
          dyn-path (some #(not (keyword? %)) path)
          property (last path-&-prop)
          dyn-property (not (keyword? property))
          get-property-fn (cond
                            (and dyn-path dyn-property) (GetDynPropertyDynPathClojureFn.)
                            dyn-path (GetPropertyDynPathClojureFn.)
                            dyn-property (GetDynPropertyClojureFn.)
                            :else (GetPropertyStaticClojureFn.))]
      (conj path-&-prop get-property-fn))
    (and (seq? %) (get-reason-call? %))
    (list '.getEvolveReason 'component)
    (old-value-ref? % property)
    (list (keyword (.substring (name %) (.length old-val-prefix))) 'component)
    (seq? %) (replace-gp % property)
    (vector? %) (replace-gpv % property)
    (map? %) (replace-gpmap % property)
    :else %))

(defn replace-gp [form property] (map #(gp-replacer % property) form))
(defn replace-gpv [v property] (mapv #(gp-replacer % property) v))
(defn replace-gpmap [form property] (functor/fmap #(gp-replacer % property) form))

;(defn- gen-evolver [body property] (flatgui.core/replace-gp (gp-replacer body property) property))
(defn- gen-evolver [body property] (gp-replacer body property))

(defn with-all-meta [obj m]
  (let [orig-meta (meta obj)]
    (with-meta obj (merge orig-meta m))))

(defn- gen-evolver-decl
  ([fnname property body]
    (let [result (list 'def fnname
                       (list 'flatgui.core/with-all-meta
                             (list 'fn ['component] (gen-evolver body property))
                             (list 'hash-map
                                    :input-channel-dependencies (conj (flatgui.dependency/get-input-dependencies body) 'list)
                                    :relative-dependencies (conj (flatgui.dependency/get-all-dependencies body) 'list))))
          ;_ (println "Generated evolver:\n" result)
          _ (if (= fnname 'r-spinner-evolver)
              (println "Evolver dependencies:\n" (flatgui.dependency/get-all-dependencies body)))
          ]
      result))
  ([property body]
   (gen-evolver-decl (symbol (str (name property) "-evolver")) property body)))

(defmacro defevolverfn [& args] (apply gen-evolver-decl args))

(defmacro accessorfn [body]
  (list 'flatgui.core/with-all-meta
        (list 'fn ['component] (gen-evolver body nil))
        (list 'hash-map
              :input-channel-dependencies (conj (flatgui.dependency/get-input-dependencies body) 'list)
              :relative-dependencies (conj (flatgui.dependency/get-all-dependencies body) 'list))))

(defmacro defaccessorfn [fnname params body]
  (list 'def fnname (list 'flatgui.core/with-all-meta
                          (list 'fn params (gen-evolver body nil))
                          (list 'hash-map
                                :input-channel-dependencies (conj (flatgui.dependency/get-input-dependencies body) 'list)
                                :relative-dependencies (conj (flatgui.dependency/get-all-dependencies body) 'list)))))

(defn defroot [container] container)

;;; TODO move below to dedicated namespace(s)

(defn properties-merger [a b]
  (if (and (map? a) (map? b))
    (merge-with properties-merger a b)
    (if (not (nil? b)) b a)))

(defmacro defwidget [widget-type dflt-properties & base-widget-types]
  "Creates widget property map and associates it with a symbol."
  `(def
     ~(symbol widget-type)
     (merge-with properties-merger
                 ~@base-widget-types
                 ~dflt-properties
                 {:widget-type ~widget-type})))

;;
;; TODO   For component inheritance, merge-properties should throw exception in case same property found in more than one parent,
;; TODO   and inheriting component does not declare its own value
;;
(defn defcomponent [type id properties & children]
  "Defines component of speficied type, with
   specified id and optionally with child components"
  (let [c (merge-with
            properties-merger
            ;; Do not inherit :skin-key from parent type if :look is defined explicitly
            (if (:look properties) (dissoc type :skin-key) type)
            properties)]
    (merge-with
      properties-merger
      c
      {:children (into {} (for [c children] [(:id c) c]))
       :id id
       :look-vec nil})))


;;;;;
;;;;; borrowed from flatgui.base
;;;;;

(def date-formatter (java.text.SimpleDateFormat. "MMM d HH:mm:ss.SSS Z"))
(defn- ts [] (.format date-formatter (java.util.Date. (java.lang.System/currentTimeMillis))))

(def log-info? true)
(defn log-info [& msg] (if log-info? (println (conj msg "[FlatGUI info] " (ts)))))

(def log-debug? true)
(defn log-debug [& msg] (if log-debug? (println (conj msg "[FlatGUI debug] " (ts)))))

(def log-warning? true)
(defn log-warning [& msg] (if log-warning? (println (conj msg "[FlatGUI warning] " (ts)))))

(def log-error? true)
(defn log-error [& msg] (if log-error? (println (conj msg "[FlatGUI ERROR] " (ts)))))

(defn log-timestamp [& msg] (log-debug msg " at " (str (java.lang.System/currentTimeMillis))))


(defn get-child-count [comp-property-map] (count (:children comp-property-map)))

;;;;;
;;;;; borrowed from flatgui.base but re-implemented
;;;;;
