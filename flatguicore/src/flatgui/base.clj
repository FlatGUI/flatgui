; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Basic means for defining and implementing FlatGUI widget types"
      :author "Denys Lebediev"}
  flatgui.base
  (:require flatgui.comlogic
            flatgui.responsefeed
            flatgui.util.matrix)
  (:import [clojure.lang Keyword])
  ;;; TODO Get rid of :use
  (:use
    flatgui.dependency
    clojure.test))

;;;
;;; Default implementation of logging. To be overriden by client application
;;;

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

;
; Logging utilities
;

(defn- get-indent [str] (clojure.string/join (map (fn [e] " ") str)))

(defn- key-to-str [k] (str k " "))

(defn- join-with-break [coll]
  (clojure.string/join (System/getProperty "line.separator") coll))

(defn container-to-str
  ([indent container]
    (join-with-break
      (sort (for [[k v] container] (if (map? v)
                                     (join-with-break [(str indent (key-to-str k)) (container-to-str (str indent (get-indent (key-to-str k))) v)])
                                     (str indent (key-to-str k) v))))))
  ([container] (container-to-str "" container)))

;
; Utilities for defining component type
;

(defn get-path-components [container path]
  (if (seq path)
    (let [ first-component ((first path) (:children container))
           next-path (next path)]
      (if next-path
        (concat [first-component] (get-path-components first-component next-path))
        [first-component]))))

(defn- count-levels-up [path]
  (let [ path-len (count path)]
    (loop [ i 0]
      (if (and (< i path-len) (= :_ (nth path i)))
        (recur (inc i))
        i))))

(def self-dependency-path [:this])

;;; TODO should be private, but need to fix utest then
(defn get-property-private [component path property]
  "Returns property from component or any of its children
   looking up for it by path. Path is a vector which is
   used to navigate component hierachy. Special path element
   :_ means go 1 level up; any other element is treated as
   child component id to look in."
  (if (= path self-dependency-path)
    (property component)
    (try (let [ ;_ (println " ---------- path = " path)
           path-to-component (:path-to-target component)]
      (if (not (and (= 0 (count path)) (and (= 0 (count path-to-component)))))
        (let [  levels-up (count-levels-up path)
                path-to-cnt (- (count path-to-component) 1 levels-up)
                path-down-cnt (- (count path) levels-up)
                total-cnt (+ path-to-cnt path-down-cnt)
                k (loop [ i 0
                          key (transient (vec (make-array Keyword total-cnt)))]
                    (if (< i total-cnt)
                      (recur
                        (inc i)
                        (let [ p (* 2 i)
                               pn (inc p)]
                            (assoc! (assoc! key p :children) pn (if (< i path-to-cnt)
                                                                  (nth path-to-component (inc i))
                                                                  (let [ pde (nth path (+ (- i path-to-cnt) levels-up))]
                                                                    (if (= :this pde) (:id component) pde))))
                            ))
                      (persistent! key)))
                root-container (:root-container component)
                component-to-take-from (get-in root-container k)]
          (do

;            (println "GP " property " from " path
;              " component: " (:id component)
;              " path-to-component = " path-to-component
;              " component-to-take-from: " (:id component-to-take-from))

              (property component-to-take-from)
        ))))
      (catch Exception e (log-error "get-property exception" (.getMessage e)
                           "comp:" (:path-to-target component) (:id component)
                           "path" path
                           "property" property)))

    ))


;; @todo 1. get-property macro should be written in a way user does not need to specify component argument
;;          get-property-private should be called with component agrument from macro generated code
;;
;;       2. when get-property-private has to evolve property because is not evolved yet - it has to update
;;          it in container somehow
;;
;(defn- get-property-private [component path property]
;  "Returns property from component or any of its children
;   looking up for it by path. Path is a vector which is
;   used to navigate component hierachy. Special path element
;   :_ means go 1 level up; any other element is treated as
;   child component id to look in."
;  (let [ parents (:parents component)]
;    (if (not (empty? parents))
;      (let [ parent-index-to-start (count (take-while (fn [e] (= :_ e)) path))
;             parent-to-start (nth parents parent-index-to-start)
;             parents-of-paernt-to-start (take-last (dec (- (count parents) parent-index-to-start)) parents)
;             path-down (replace {:this (:id component)} (take-last (- (count path) parent-index-to-start) path))
;             path-down-components (get-path-components parent-to-start path-down)
;             target-with-parents (concat (reverse path-down-components) [parent-to-start] parents-of-paernt-to-start)
;             target-component (first target-with-parents)
;             parents-of-target (vec (next target-with-parents))]
;        (do
;
;;          ;(if (= :screen-row property)
;;            (println "GP " property " from " (:id component)
;;            " path = " path
;;            " path-down = " path-down
;;            " path-down-components = " (map #(:id %1) path-down-components)
;;            " parent-to-start = " (:id parent-to-start)
;;            " its parents = " (map #(:id %1) parents-of-paernt-to-start)
;;            " target = " (:id target-component)
;;            " parents-of-target = " (map #(:id %1) parents-of-target)
;;            " actual value = " (property target-component)
;;            " evolved value = " (let [ evolver (property (:evolvers target-component))]
;;                                 (if evolver (evolver (assoc target-component
;;                                                        :parents parents-of-target
;;                                                        :evolve-reason-provider (:evolve-reason-provider component)))
;;                                   (property target-component)))
;;            " evolved properties = "  (:evolved-properties target-component)
;;            )
;;          ;)
;
;          (if target-component
;            (if (some #(= property %1) (:evolved-properties target-component))
;              ;(get-in parent-to-start (concat (mapcat (fn [v] [:children v]) path-down) [property]))
;              (property target-component)
;              (let [ evolver (property (:evolvers target-component))]
;                (if evolver (evolver (assoc target-component
;                                       :parents parents-of-target
;                                       :evolve-reason-provider (fn [id] nil);(:evolve-reason-provider component)
;                                       ))
;                  (property target-component)))
;              ))
;
;;          (let [ evolver (property (:evolvers target-component))]
;;            (if evolver (evolver (assoc target-component
;;                                   :parents parents-of-target
;;                                   :evolve-reason-provider (:evolve-reason-provider component)))
;;              (property target-component)))
;
;
;          ;(property target-component)
;
;          )))))


; TODO order is not needed
;
;
(defn merge-ordered [a b]
  (let [ key-order (distinct (concat (for [[k v] a] k) (for [[k v] b] k)))
         result (apply array-map (mapcat (fn [k] (if (and (map? (a k)) (map? (b k)))
                                             [k (merge-ordered (a k) (b k))]
                                             (if (contains? b k) ; b may intentionally override smth with nil
                                               [k (b k)]
                                               [k (a k)]))) key-order))]
    (do
      ;(log-debug " Merged result of class: " (.getClass result))
      result)))

(defn merge-properties [& property-maps]
  "Merges property maps. Latter map has precedence"
  {:added "1.0"}
  (let [dependencies (mapcat (fn [p] (:relative-dependencies p)) property-maps)
        input-channel-dependencies (mapcat (fn [p] (:input-channel-dependencies p)) property-maps)]
    (do

;      (println " merge-properties ------------------------------ ")
;      (println "  args: " property-maps)
;      (println "  result: " (reduce merge-ordered property-maps))

      (assoc
        (reduce merge-ordered property-maps)
        :relative-dependencies dependencies
        :input-channel-dependencies input-channel-dependencies))))


;;
;; TODO   For component inheritance, merge-properties should throw exception in case same property found in more than one parent,
;; TODO   and inheriting component does not declare its own value
;;
(defn defcomponent [type id properties & children]
  "Defines component of speficied type, with
   specified id and optionally with child components"
  (let [c (merge-properties
             ;; Do not inherit :skin-key from parent type if :look is defined explicitly
             (if (:look properties) (dissoc type :skin-key) type)
             properties)
        evolver-dependencies (into {} (for [[k v] (:evolvers c)] [k (get-relative-dependencies v)]))]
    (merge-properties c
      { :children (into (array-map) (for [c children] [(:id c) c]))
        :evolver-dependencies evolver-dependencies
        :id id})))

(defmacro defwidget [widget-type dflt-properties & base-widget-types]
  "Creates widget property map and associates it with a symbol. See defwidgettype for mode imformation"
  (let []

    `(def
       ~(symbol widget-type)
       (flatgui.base/merge-properties
         ~@base-widget-types
         ~dflt-properties
         {:widget-type ~widget-type}))))

(defmacro defevolverfn
  "Convenient macro to define evolver functions for components.
  Introduces let binding for old property name."
  ([property-name body]
    (let [body-dependencies (conj (flatgui.dependency/get-all-dependencies body) 'list)
          input-channel-dependencies (conj (flatgui.dependency/get-input-dependencies body) 'list)
          fnname (symbol (str (name property-name) "-evolver"))
          get-fn flatgui.base/get-property-private
          let-binding [(symbol (str "old-" (name property-name))) (list property-name 'component)
                        'get-property `(fn ([~'c ~'path ~'prop] (~get-fn ~'c ~'path ~'prop))
                                           ([~'path ~'prop] (~get-fn ~'component ~'path ~'prop)))]]
      (do (log-debug " defining evolver " fnname
                     " body-dependencies = " body-dependencies
                     " input-channel-dependencies = " input-channel-dependencies)
          ;(log-debug "  |_ body " body)
        `(do
           ; with-meta produces evolver fn meta which is analyzed by flatgui.dependency/get-relative-dependencies [evolver]
           ; alter-meta! alters Var meta that is analyzed by flatgui.dependency/get-all-dependencies
          (def ~fnname (with-meta (fn [~'component] (let ~let-binding ~body)) {:relative-dependencies ~body-dependencies
                                                                               :input-channel-dependencies ~input-channel-dependencies}))
          (alter-meta! (var ~fnname) (fn [~'m] (assoc ~'m
                                                 :relative-dependencies ~body-dependencies
                                                 :input-channel-dependencies ~input-channel-dependencies)))
          ))))
  ([fnname property-name body]
    (let [body-dependencies (conj (flatgui.dependency/get-all-dependencies body) 'list)
          input-channel-dependencies (conj (flatgui.dependency/get-input-dependencies body) 'list)
          get-fn flatgui.base/get-property-private
          let-binding [(symbol (str "old-" (name property-name))) (list property-name 'component)
                        'get-property `(fn ([~'c ~'path ~'prop] (~get-fn ~'c ~'path ~'prop))
                                           ([~'path ~'prop] (~get-fn ~'component ~'path ~'prop)))]]
      (do (log-debug " defining evolver " fnname
                     " body-dependencies = " body-dependencies
                     " input-channel-dependencies = " input-channel-dependencies)
        `(do
           ; with-meta produces evolver fn meta which is analyzed by flatgui.dependency/get-relative-dependencies [evolver]
           ; alter-meta! alters Var meta that is analyzed by flatgui.dependency/get-all-dependencies
           (def ~fnname (with-meta (fn [~'component] (let ~let-binding ~body)) {:relative-dependencies ~body-dependencies
                                                                                :input-channel-dependencies ~input-channel-dependencies}))
           (alter-meta! (var ~fnname) (fn [~'m] (assoc ~'m
                                                  :relative-dependencies ~body-dependencies
                                                  :input-channel-dependencies ~input-channel-dependencies)))
           )))))

(defmacro accessorfn [body]
  (let [body-dependencies (conj (flatgui.dependency/get-all-dependencies body) 'list)
        input-channel-dependencies (conj (flatgui.dependency/get-input-dependencies body) 'list)
        get-fn flatgui.base/get-property-private
        let-binding ['get-property get-fn]]
    `(with-meta (fn [~'component] (let ~let-binding ~body)) {:relative-dependencies ~body-dependencies
                                                             :input-channel-dependencies ~input-channel-dependencies})))

(defmacro defaccessorfn [fnname params body]
  (let [body-dependencies (conj (flatgui.dependency/get-all-dependencies body) 'list)
        input-channel-dependencies (conj (flatgui.dependency/get-input-dependencies body) 'list)
        get-fn flatgui.base/get-property-private
        let-binding ['get-property get-fn]]
    (do

      (log-debug " defining accessor " fnname
                 " body-dependencies = " body-dependencies
                 " input-channel-dependencies = " input-channel-dependencies)

      `(do ;(log-debug " defining accessor " ~fnname)
         ; with-meta produces evolver fn meta which is analyzed by flatgui.dependency/get-relative-dependencies [evolver]
         ; alter-meta! alters Var meta that is analyzed by flatgui.dependency/get-all-dependencies
        (def ~fnname (with-meta (fn ~params (let ~let-binding ~body)) {:relative-dependencies ~body-dependencies
                                                                       :input-channel-dependencies ~input-channel-dependencies}))
        (alter-meta! (var ~fnname) (fn [~'m] (assoc ~'m
                                               :relative-dependencies ~body-dependencies
                                               :input-channel-dependencies ~input-channel-dependencies)))))))



;;;
;;; Utils for working with components
;;;

(defmacro get-reason [] '((:evolve-reason-provider component) (:id component)))

(defn get-children-list [comp-property-map]
  (for [[id c] (:children comp-property-map)] c))

(defn get-children-id-list [comp-property-map]
  (for [[id c] (:children comp-property-map)] id))

(defn get-child-count [comp-property-map] (count (:children comp-property-map)))