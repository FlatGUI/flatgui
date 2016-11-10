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

(defn- symbol->str-impl [smb]
  (if-let [smb-ns (:ns (meta (resolve smb)))]
    (str smb-ns "/" (name smb))
    (name smb)))

;;; TODO
;;; 1. this leads to error: see capture-area fn in floatingbar
;;; 2. could not handle Integer/MAX_VALUE in component
;;; 3. see uiapp-test: Unable to resolve symbol: b-text...
;;; x. see "strict"-related comments in Container
(defn- symbol->str [smb] (str fg (symbol->str-impl smb)))

(defn- str->symbol [str]
  (if
    ;(str/starts-with? str fg) (str/replace str fg "")
    (.startsWith str fg) (symbol (.replace str fg ""))
                              str))

(defn- accessor-call-form->accessor-body [form]
  (if-let [obj (resolve (first form))] (var-get obj)))

(defn- evolver-call-form->evolver-body [form]
  (if-let [obj (resolve (first form))] (var-get obj)))

(defn- accessor-body->params [ab] (:accessor_fn_params (meta ab)))

(defn- accessor-call-form->params [form] (accessor-body->params (accessor-call-form->accessor-body form)))

(defn accessor-call? [form]
  (and
    (seq? form)
    (symbol? (first form))
    (not (nil? (accessor-call-form->params form)))))

(defn evolver-call? [form]
  (and
    (seq? form)
    (symbol? (first form))
    (:evolver (meta (evolver-call-form->evolver-body form)))))

(declare replace-occur-seq)
(declare replace-occur-vec)
(declare replace-occur-map)

(defn- occur-replacer [% from-vec to-vec]
  (cond
    (seq? %) (replace-occur-seq % from-vec to-vec)
    (vector? %) (replace-occur-vec % from-vec to-vec)
    (map? %) (replace-occur-map % from-vec to-vec)
    (symbol? %) (let [i (.indexOf from-vec %)]
                  (if (>= i 0)
                    (nth to-vec i)
                    %))
    :else %))

(defn replace-occur-seq [form from-vec to-vec]
  (map #(occur-replacer % from-vec to-vec) form))

(defn replace-occur-vec [form from-vec to-vec]
  (mapv #(occur-replacer % from-vec to-vec) form))

(defn replace-occur-map [form from-vec to-vec]
  (functor/fmap #(occur-replacer % from-vec to-vec) form))

(declare shade-list)
(declare shade-vec)
(declare shade-map)

(defn- shader [%]
  (do
    ;(println "------------ % = " % "acc?" (accessor-call? %))
    (cond
      ;(accessor-call? %) (let [accessor-form (eval (first %))
      ;                         _ (println "accessor-form" accessor-form)]
      ;                     (replace-occur-seq
      ;                       accessor-form
      ;                       ["__flatgui_x"] ;(:accessor_fn_params (meta accessor-form))
      ;                       (next %)))
      (seq? %) (shade-list %)
      (vector? %) (shade-vec %)
      (map? %) (shade-map %)
      (symbol? %) (symbol->str %)
      :else %)))

(defn shade-vec [v] (mapv shader v))
(defn shade-list [form] (conj (map shader form) 'list))
(defn shade-map [m] (functor/fmap shader m))

(declare replace-gp)
(declare replace-gpv)
(declare replace-gpmap)

(defn- unshader [%]
  (cond
    (seq? %) (replace-gp %)
    (vector? %) (replace-gpv %)
    (map? %) (replace-gpmap %)
    (string? %) (str->symbol %)
    :else %))

(defn replace-gp [form] (map unshader form))
(defn replace-gpv [v] (mapv unshader v))
(defn replace-gpmap [form] (functor/fmap unshader form))

(defn- gen-evolver [body] (list 'flatgui.core/replace-gp (shade-list body)))

(defn- gen-evolver-decl
  ([fnname _property body]
    (list 'def fnname (list 'with-meta (gen-evolver body) (list 'hash-map :evolver true))))
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
  (list 'def fnname (list 'with-meta (gen-evolver body) (list 'hash-map :accessor_fn_params (shade-vec params)))))

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
      (list (build-abs-path component-path (second (next form))))
      (next (next (next form))))))

(declare replace-all-rel-paths)
(declare replace-all-rel-pathsv)
(declare replace-all-rel-pathsmap)

(defn- rel-replacer [e component-path]
  (cond
    (seq? e) (replace-all-rel-paths e component-path)
    (vector? e) (replace-all-rel-pathsv e component-path)
    (map? e) (replace-all-rel-pathsmap e component-path)
    :else e))

(defn replace-all-rel-pathsv [form component-path]
  (mapv #(rel-replacer % component-path) form))

(defn replace-all-rel-paths [form component-path]
  (if (get-property-call? form)
    (replace-rel-path form component-path)
    (map #(rel-replacer % component-path) form)))

(defn replace-all-rel-pathsmap [m component-path]
  (functor/fmap #(rel-replacer % component-path) m))

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
      (fn [evolver-map]
        (do
          (println "FUNCS: " (filter #(instance? clojure.lang.IFn (second %)) evolver-map))
          (functor/fmap (fn [form] (replace-all-rel-paths form component-path)) evolver-map))
        ))
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
    (filter seq (mapcat (fn [e] (if (coll? e) (collect-evolver-dependencies e))) form))))

(defn collect-all-evolver-dependencies [component]
  "Returns a map of property id to the collection of dependency paths"
  (functor/fmap (fn [evolver] (collect-evolver-dependencies evolver)) (:evolvers component)))

(declare replace-dependencies-with-indices)
(declare replace-dependencies-with-indicesv)
(declare replace-dependencies-with-indicesmap)

(defn- dep-replacer [e index-provider]
  (cond

    (and (seq? e) (get-property-call? e))
    (let [e (if (symbol? (second e)) (conj (drop 2 e) (first e)) e) ;backward compatibility
          path-&-prop (next e)
          prop-full-path (conj (first path-&-prop) (second path-&-prop))
          index (.apply index-provider prop-full-path)]
      ;See :input-channel-subscribers evolver in component: k is not known during dependency replacement with indices
      ; so need to
      ; 1) calculate proper dependecies - all possible nodes matching templete
      ; 2) put s-expr that will resolve in runtime instead of .getNodeValueByIndex
      (if index
        (list '.getNodeValueByIndex 'component index)
        ; Here replace-dependencies-with-indicesv is called for prop-full-path because
        ; prop-full-path may contain exsressions (e.g. calls to get-property)
        (list '.getValueByAbsPath 'component (replace-dependencies-with-indicesv prop-full-path index-provider))))

    (and (seq? e) (get-reason-call? e))
    (list '.getEvolveReason 'component)

    (seq? e)
    (replace-dependencies-with-indices e index-provider)

    (vector? e)
    (replace-dependencies-with-indicesv e index-provider)

    (map? e)
    (replace-dependencies-with-indicesmap e index-provider)

    (old-value-ref? e)
    (list (keyword (.substring (name e) (.length old-val-prefix))) 'component)

    :else
    e))

(defn replace-dependencies-with-indices [form index-provider]
  (map #(dep-replacer % index-provider) form))

(defn replace-dependencies-with-indicesv [form index-provider]
  (mapv #(dep-replacer % index-provider) form))

(defn replace-dependencies-with-indicesmap [m index-provider]
  (functor/fmap #(dep-replacer % index-provider) m))


(declare replace-param-value)
(declare replace-param-valuev)
(declare replace-param-valuemap)
(defn- param-value-replacer [e replace-map]
  (cond
    (symbol? e) (if-let [replacement (get replace-map e)] replacement e)
    (seq? e) (replace-param-value e replace-map)
    (vector? e) (replace-param-valuev e replace-map)
    (map? e) (replace-param-valuemap e replace-map)
    :else e))
(defn replace-param-value [form replace-map] (map #(param-value-replacer % replace-map) form))
(defn replace-param-valuev [form replace-map] (mapv #(param-value-replacer % replace-map) form))
(defn replace-param-valuemap [form replace-map] (functor/fmap #(param-value-replacer % replace-map) form))

(declare inline-accessors)
(declare inline-accessorsv)
(declare inline-accessorsmap)
(defn- inliner [form]
  (cond
    (accessor-call? form) (let [accessor-body (accessor-call-form->accessor-body form)
                                params (replace-gpv (accessor-body->params accessor-body))
                                params-values (vec (next form))
                                repace-map (into {} (map (fn [%] [(nth params %) (nth params-values %)]) (range (count params))))]
                            (replace-param-value accessor-body repace-map))
    (evolver-call? form) (evolver-call-form->evolver-body form)
    (seq? form) (inline-accessors form)
    (vector? form) (inline-accessorsv form)
    (map? form) (inline-accessorsmap form)
    :else form))
(defn inline-accessors [form] (map inliner form))
(defn inline-accessorsv [form] (mapv inliner form))
(defn inline-accessorsmap [form] (functor/fmap inliner form))

(defn eval-evolver [form]
  (eval (conj (list form) ['component] 'fn)))

(defn compile-evolver [form index-provider]
  (try
    (eval-evolver (replace-dependencies-with-indices (inline-accessors form) index-provider))
    (catch Exception ex
      (do
        (println "Error compiling evolver " form)
        (.printStackTrace ex)))))

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
      [])))

(defn properties-merger [a b]
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
       :id id
       :look-vec nil})))
