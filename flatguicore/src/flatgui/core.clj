; Copyright (c) 2016 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.core
  (:require [clojure.algo.generic.functor :as functor]))

(defmacro defevolverfn [fnname body]
  "Defined deferred evolver. It is supposed to be compiled
   when parsing the container so component absolute location
   in the hierarchy is already known and relative paths in
   get-property calls are replaced with absolute paths"
  (list 'def fnname (list
                      'with-meta
                      (list 'let ['get-property (list 'fn ['a 'b] (concat '(list 'get-property) (list 'a 'b)))] (conj body 'list))
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
      (fn [e] (if (list? e) (replace-all-rel-paths e component-path) e))
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