; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Namespace that contains functions for
            working with component ids."
      :author "Denys Lebediev"}
  flatgui.ids
  (:require clojure.string))


(def delimiter \_)

(def delimiter-pattern (java.util.regex.Pattern/compile (str delimiter)))

(defn check-simple-id [id]
  "Checks whether provided simple id complies with the convention."
  (and
    (keyword? id)
    (nil? (some (fn [c] (= delimiter c)) (name id)))))

(defn throw-if-wrong-id [id]
  "Throws IllegalArgumentException in case provided simple id
   is incorrect. See check-simple-id."
  (if (not (check-simple-id id))
    (throw (IllegalArgumentException.
             (str "Component id is incorrect. It must be a keyword and must not contain underscore symbol: " id)))))

(defn create-compound-id [parent-id child-id]
  "Creates compound id from parent id and child id."
  (if (nil? parent-id)
    child-id
    (if (nil? child-id)
      parent-id
      (keyword (str (name parent-id) delimiter (name child-id))))))

(defn get-id-depth [id]
  "Returns id depth, which is 1 for simple ids,
   and equals to number of underlying simple ids
   for compound ids"
  (inc (count
         (filter
           (fn [e] (not (nil? e)))
           (map #{delimiter} (name id))))))

(defn get-id-names [id]
  "Splits id into a vector of names of its parts."
  (clojure.string/split (name id) delimiter-pattern))

(defn get-id-path [id]
  "Splits id into a vector of subsequent ids needed to
   access the component in the tree."
  (let [ id-parts (get-id-names id)]
    (loop [ id-accum (first id-parts)
            id-vector [(keyword id-accum)]
            i 1]
      (if (< i (count id-parts))
        (let [ id-accum-new (str id-accum delimiter (nth id-parts i))]
          (recur
            id-accum-new
            (conj id-vector (keyword id-accum-new))
            (inc i)))
        id-vector))))

(defn get-parent-id [id]
  "Returns parent component id for component
   with given compount id"
  (let [ last-delim-index (.lastIndexOf (name id) (str delimiter))]
    (if (>= last-delim-index 0)
      (keyword (.substring (name id) 0 last-delim-index)))))

;;; :a :b :c :d :e :f
;;; :a :b :c :x :y
;;;
(defn get-relative-path [from to]
  (if (= from to)
    [:this]
    (let [ from-count (count from)
           to-count (count to)
           min-count (min from-count to-count)
           same-count (loop [ i 0]
                        (if (and (< i min-count) (= (nth from i) (nth to i)))
                              (recur (inc i))
                              i))
           level-up-count (dec (- to-count same-count))
           another-branch (>= level-up-count 0)
           from-down-count (- from-count same-count)
           path-count (if another-branch (+ level-up-count from-down-count) (inc from-down-count))
           ;_ (println "from-count" from-count "to-count" to-count "same-count" same-count "level-up-count" level-up-count "from-down-count" from-down-count "path-count" path-count)
           ]
      (let [ path-vec (transient (vec (make-array clojure.lang.Keyword path-count)))]
        (loop [ i (if another-branch 0 1)
                path (if another-branch path-vec (assoc! path-vec 0 :this))]
          (cond
            (< i level-up-count) (recur
                                   (inc i)
                                   (assoc! path i :_))
            (< i path-count) (recur
                               (inc i)
                               (assoc! path i (nth from (+ same-count (- (if another-branch i (dec i)) (if another-branch level-up-count 0))))))
            :esle (persistent! path)))))))