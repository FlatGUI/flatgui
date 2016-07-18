; Copyright (c) 2016 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.core
  (:require [clojure.algo.generic.functor :as functor]
            [clojure.string :as str]))

(def fg "__flatgui_")

(defn- symbol->str [smb] (str fg (name smb)))

(defn- str->symbol [str]
  (if
    ;(str/starts-with? str fg) (str/replace str fg "")
    (.startsWith str fg) (symbol (.replace str fg ""))
                              str))

(defn shade-list [form]
  (conj (map #(if (seq? %) (shade-list %) (if (symbol? %) (symbol->str %) %)) form) 'list))

(defn replace-gp [form]
  (map #(if (seq? %) (replace-gp %) (if (string? %) (str->symbol %) %)) form))

(defmacro defevolverfn [fnname body]
  "Defines deferred evolver. It is supposed to be compiled
   when parsing the container so component absolute location
   in the hierarchy is already known and relative paths in
   get-property calls are replaced with absolute paths"
  (list 'def fnname (list
                      'with-meta
                      (list 'flatgui.core/replace-gp (shade-list body))
                      {:type :evolver})))

;;; TODO initialize

(defn build-abs-path [component-path rel-path]
  (cond

    (= [] rel-path)
    (vec (drop-last component-path))

    (= :this (first rel-path))
    (vec (concat component-path (next rel-path)))

    (= :_ (first rel-path))
    (let [split-rel (split-with #(= % :_) rel-path)
          pre-path (take (- (count component-path) (count (first split-rel)) 1) component-path)]
      (vec (concat pre-path (second split-rel))))

    :else
    (vec (concat (drop-last component-path) rel-path))))

(defn get-property-call? [form]
  (and
    (= 'get-property (first form))
    (or
      (vector? (second form))
      (and (= 'component (second form)) (vector? (first (next (next form))))))))

;; NOTE: in case of '(get-property component [:x] :y)' from, this fn
;; omits 'component' part (as obsolete) and further processings rely on that
(defn replace-rel-path [form component-path]
  (cond

    (vector? (second form))
    (concat
      (list 'get-property)
      (list (build-abs-path component-path (second form)))
      (next (next form)))

    (= 'component (second form))
    (concat
      (list 'get-property)
      (list (build-abs-path component-path (second form)))
      (next (next (next form))))))

(defn replace-all-rel-paths [form component-path]
  (if (get-property-call? form)
    (replace-rel-path form component-path)
    (map
      (fn [e] (if (seq? e) (replace-all-rel-paths e component-path) e))
      form)))

(defn- dissoc-nil-properties [c]
  (let [properties [:children :evolvers]]
    (loop [r c
           i (dec (count properties))]
      (if (>= i 0)
        (recur
          (let [p (nth properties i)]
            (if (nil? (p r)) (dissoc r p) r))
          (dec i))
        r))))

(defn parse-component [component component-path]
  (->
    (update-in
      component
      [:evolvers]
      (fn [evolver-map] (functor/fmap (fn [form] (replace-all-rel-paths form component-path)) evolver-map)))
    (update-in
      [:children]
      (fn [child-map] (functor/fmap (fn [child] (parse-component child (conj component-path (:id child)))) child-map)))
    dissoc-nil-properties))

(defn defroot [container]
  "Parses container, compiles all evolvers having relative paths
   replaced with absolute paths"
  (parse-component container []))

(defn collect-evolver-dependencies [form]
  (if (get-property-call? form)
    (let [path-&-prop (next form)]
      (list (conj (first path-&-prop) (second path-&-prop))))
    (filter seq (mapcat (fn [e] (if (seq? e) (collect-evolver-dependencies e))) form))))

(defn collect-all-evolver-dependencies [component]
  "Returns a map of property id to the collection of dependency paths"
  (functor/fmap (fn [evolver] (collect-evolver-dependencies evolver)) (:evolvers component)))

(defn replace-dependencies-with-indices [form property-value-vec index-provider]
  (map
    (fn [e]
      (cond

        (and (seq? e) (get-property-call? e))
        (let [path-&-prop (next e)
              prop-full-path (conj (first path-&-prop) (second path-&-prop))
              index (.apply index-provider prop-full-path)]
          (list 'nth property-value-vec index))

        (seq? e)
        (replace-dependencies-with-indices e property-value-vec index-provider)

        :else
        e))
    form))

(defn eval-evolver [form]
  (do
    (println "Eval: form =" form)
    (eval (conj (list form) ['component] 'fn))))

(defn compile-evolver [form property-value-vec index-provider]
  (eval-evolver (replace-dependencies-with-indices form property-value-vec index-provider)))

;(defn compile-all-evovlers [component property-value-vec index-provider]
;  (functor/map
;    (fn [evolver] (eval-evolver (replace-dependencies-with-indices evolver property-value-vec index-provider)))
;    (:evolvers component)))