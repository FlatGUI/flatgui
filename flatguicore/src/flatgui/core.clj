; Copyright (c) 2016 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.core
  (:require [clojure.algo.generic.functor :as functor]
            [clojure.string :as str]
            [flatgui.base]
            [flatgui.paint :as fgp]))

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

(defn- gen-evolver [body]
  (list 'flatgui.core/replace-gp (shade-list body)))

(defn- gen-evolver-decl
  ([fnname _property body]
   (list 'def fnname (gen-evolver body)))
  ([property body]
   (gen-evolver-decl (symbol (str (name property) "-evolver")) property body)))

(defmacro defevolverfn [& args]
  "Defines deferred evolver. It is supposed to be compiled
   when parsing the container so component absolute location
   in the hierarchy is already known and relative paths in
   get-property calls are replaced with absolute paths"
  (apply gen-evolver-decl args))

(defmacro accessorfn [body] (gen-evolver body))

(defmacro defaccessorfn [fnname params body]
  (list 'def fnname params (gen-evolver body)))

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

(defn get-reason-call? [form]
  (= 'get-reason (first form)))

(def old-val-prefix "old-")

(defn old-value-ref? [e]
  (and (symbol? e) (.startsWith (name e) old-val-prefix)))

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
  (parse-component container [(:id container)]))

(defn collect-evolver-dependencies [form]
  (if (get-property-call? form)
    (let [path-&-prop (next form)]
      (list (conj (first path-&-prop) (second path-&-prop))))
    (filter seq (mapcat (fn [e] (if (seq? e) (collect-evolver-dependencies e))) form))))

(defn collect-all-evolver-dependencies [component]
  "Returns a map of property id to the collection of dependency paths"
  (functor/fmap (fn [evolver] (collect-evolver-dependencies evolver)) (:evolvers component)))

(defn replace-dependencies-with-indices [form index-provider]
  (map
    (fn [e]
      (cond

        (and (seq? e) (get-property-call? e))
        (let [path-&-prop (next e)
              prop-full-path (conj (first path-&-prop) (second path-&-prop))
              index (.apply index-provider prop-full-path)]
          (list '.getNodeValueByIndex 'component index))

        (and (seq? e) (get-reason-call? e))
        (list '.getEvolveReason 'component)

        (seq? e)
        (replace-dependencies-with-indices e index-provider)

        (old-value-ref? e)
        (list (keyword (.substring (name e) (.length old-val-prefix))) 'component)

        :else
        e))
    form))

(defn eval-evolver [form]
  (eval (conj (list form) ['component] 'fn)))

(defn compile-evolver [form index-provider]
  (eval-evolver (replace-dependencies-with-indices form index-provider)))

;;; TODO move below to dedicated namespace(s)

(defn rebuild-look [component]
  (let [look-fn (:look component)
        font (:font component)]
    (if look-fn
      (try

        (do
          (if font
            (.setReferenceFont (:interop component) font (flatgui.awt/str->font font)))
          (let [lv (fgp/flatten-vector
                     [(fgp/font-look component)
                      (look-fn component nil)
                      (if (:has-trouble component) (fgp/trouble-look component nil))])]
            lv))

        (catch Exception ex
          (do
            ;(fg/log-error "Error painting " target-id-path ":" (.getMessage ex))
            (.printStackTrace ex))))
      component)))

(defn- properties-merger [a b]
  (if (and (map? a) (map? b))
    (merge-with properties-merger a b)
    b))

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
       :id id})))
