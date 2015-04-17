; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Dependency resolving"
      :author "Denys Lebediev"}
  flatgui.dependency
  (:use flatgui.comlogic
        clojure.test))


(defn get-dependency [s]
  (do

    ;(println " ------- get-dependency2 for " s)
    ;(if (= '(:apply-feature vfc) s) (println " ------- get-dependency2 for " s " of class " (.getClass s)  " eval " (eval s) ))
    ;(if (= 'apply-feature s) (println " ------- get-dependency2 for " s " of class " (.getClass s)  ))

    (if (seq? s)
      (let [ n (first s)]
        (if (and (symbol? n) (= "get-property" (name n)))
          (let [ full-path (condp = (count s)
                             3 (if (vector? (nth s 1)) (conjv (nth s 1) (nth s 2)) (throw (IllegalArgumentException. (str "get-property argument 0 should be a vector: " s))))
                             4 (if (vector? (nth s 2)) (conjv (nth s 2) (nth s 3)) (throw (IllegalArgumentException. (str "get-property argument 1 should be a vector: " s))))
                             (throw (IllegalArgumentException. (str "There should be 3 or 4 arguments to get-property: " s))))]
            (mapv (fn [e] (if (keyword? e) e :*)) full-path)))))))


(defn get-relative-dependencies [evolver]
    (:relative-dependencies (meta evolver)))

(defn get-all-dependencies [c]
  (do

    ;(println "get-all-dependencies is called for " c)
    ;(if (and (symbol? c) (= "get-model-row" (name c)) ) (println "--->>" (meta (resolve c)) " -->>> " (.getClass (resolve c)) ))

    (distinct
      (if (and (symbol? c) (:relative-dependencies (meta (resolve c))))
        (:relative-dependencies (meta (resolve c)))
        (let [ this-dep (get-dependency c)]
          (if this-dep
            (do
              ;(println " --- this-dep:" this-dep)
              (list this-dep))
            (if (coll? c)
              (mapcat (fn [e] (get-all-dependencies e)) c))))))))


(defn- resolve-wildcards-single [root-container abs-path]
  (let [ path-before-widcard (take-while (fn [e] (not (= :* e))) abs-path)]
    (if (= (count abs-path) (count path-before-widcard))
      [abs-path]
      (let [ path-after-wildcard (take-last (- (count abs-path) (inc (count path-before-widcard))) abs-path)
             ids-to-replace (for [[id child] (:children (get-in root-container (get-access-key path-before-widcard)))] id) ]
        (do ;(println "RESOLVED WILDCARDS: " abs-path " --> " (mapv #(vec (concat path-before-widcard [%1] path-after-wildcard)) ids-to-replace))
          (mapv #(vec (concat path-before-widcard [%1] path-after-wildcard)) ids-to-replace)))
      )))

(defn- resolve-wildcards [root-container abs-path]
  (do ;(println " FILTERED WILCDARDS " (filter #(= :* %1) abs-path) " - " (pos? (count (filter #(= :* %1) abs-path))))
    (loop [ n (count (filter #(= :* %1) abs-path))
          ret [abs-path]]
    (if (pos? n)
      (recur
        (dec n)
        (mapcat #(resolve-wildcards-single root-container %1) ret))
      ret))))

(defn compute-abs-dependencies [root-container path component relative-dependencies]
  (mapcat
    (fn [d] (let [ d-path-down (get-path-down (replace {:this (:id component)} d))
                   steps-up (- (count d) (count d-path-down))
                   ; last path element is this component id so additional -1 here
                   path-from-root (take (- (count path) (inc steps-up)) path)]
              (do ;(println d " this id is " (:id component) ">> d = " d ">> d-path-down = " d-path-down " path = " path " path-from-root = " path-from-root " result = " (vec (concat path-from-root d-path-down)))
                (resolve-wildcards root-container (vec (concat path-from-root d-path-down))))
               ))
    relative-dependencies))

(defn compute-evolver-dependencies [root-container path component]
  (let [ evolver-abs-dependencies (into {} (for [[k v] (:evolver-dependencies component)] [k (compute-abs-dependencies root-container path component v)]))]
    (assoc component :evolver-abs-dependencies evolver-abs-dependencies)))


(defn compute-dependencies
  ([root-container container path]
    (let [ computed-children (if (:children container)
                               (into (array-map) (for [[id c] (:children container)] [id (compute-dependencies root-container c (concat path (list (:id c))))])))]
      (assoc (compute-evolver-dependencies root-container path container)
        :children computed-children)))
  ([container] (compute-dependencies container container (list (:id container)))))

(defn- conj-distinct [v e]
  (if (not (some #(= %1 e) v))
    (conj v e)
    v))

;;;
;;; this produces :abs-dependents: a map of property name to list of vectors, where each vector is
;;; dependent id path plus last element: dependent property. Example: {:content-size ([:main :tiket :position-bound])}
;;;
(defn- extract-dependents-from [root-container id-path-to-component component]
  (let [ evolver-abs-dependencies (vec (:evolver-abs-dependencies component))
         c (count evolver-abs-dependencies)]
    (loop [ ret root-container
            i 0]
      (if (< i c)
        (recur
          (let [ abs-dependency (nth evolver-abs-dependencies i)
                 d-property (nth abs-dependency 0)
                 depends-on-vec (nth abs-dependency 1)
                 d-on-cnt (count depends-on-vec)]
            (loop [ p-ret ret
                    j 0]
              (if (< j d-on-cnt)
                (recur
                  (let [ d-vec (nth depends-on-vec j)
                         depends-on-property (nth d-vec (dec (count d-vec)))
                         depends-on-component (drop-last d-vec)
                         depends-on-component-key (get-access-key depends-on-component)]

                    (if (get-in p-ret depends-on-component-key)
                      ;todo fn to conj 2 elements
                      (update-in p-ret (conjv
                                       (conjv depends-on-component-key :abs-dependents)
                                       depends-on-property) conj-distinct (conjv id-path-to-component d-property))
                      p-ret))
                  (inc j))
                p-ret)))
          (inc i))
        ret))))

(defn- compute-dependents-raw
  ([root-container id-path-to-component component]
    (loop [ ret (extract-dependents-from root-container id-path-to-component component)
            children (:children component)]
      (if children
        (recur
          (let [ child-info (first children)
                 child-id (nth child-info 0)
                 child (nth child-info 1)]
            (compute-dependents-raw ret (conjv id-path-to-component child-id) child))
          (next children))
        ret)))
  ([root-container] (compute-dependents-raw root-container [(:id root-container)] root-container)))

(defrecord Dependents [properties children])

(defn- group-split-by [fk fv coll]
  (reduce
    (fn [ret x]
      (let [k (fk x)
            v (fv x)]
        ;(assoc ret k (update-in (get ret k (Dependents. [] [])) [(if (= 1 (count v)) :properties :children)] conj v))
        (if (= 1 (count v))
          (assoc ret k (update-in (get ret k (Dependents. [] [])) [:properties] conj-distinct (first v)))
          (assoc ret k (update-in (get ret k (Dependents. [] [])) [:children] conj-distinct v)))
        ))
    {}
    coll))

;(defn- group-split-by [fk fv coll]
;  (let [ k (fk x)
;         v (fv x)]
;    (if (= 1 (count v))
;      (assoc coll k (update-in (get coll k (Dependents. [] [])) [:properties] conj-distinct (first v)))
;      (assoc coll k (update-in (get coll k (Dependents. [] [])) [:children] conj-distinct v)))))

(defn- intree [coll]
  (let [ grouped (group-split-by first next coll)
        ;_ (println "intree coll " coll)
        ;_ (println "intree grouped " grouped)
        ]
    (into {} (for [[k v] grouped] [k (update-in v [:children] intree)]))))


(defn- abs->out [abs-dependents]
  (into {} (for [[k v] abs-dependents] [k (intree v)])))

;;;
;;; TODO no need for root-container
;;;
(defn compute-dependents
  ([root-container current-component id-path-to-current]
    "Traverses whole container tree and for each component, computes and assigns information
     about all its dependents (other components properties depending on it)"
    (let [ id-path-of-current (conj id-path-to-current (:id current-component))
           abs-dependents (:abs-dependents current-component)
           ;out-dependents (compute-dependents-of-component root-container id-path-of-current current-component)
           ]
      (do

        ;(println " associating dependent list for " (:id current-component) ": " out-dependents )

        (assoc
          current-component
          :out-dependents (abs->out abs-dependents)
          :children (if (:children current-component)
                      (into (array-map) (for [[k v] (:children current-component)] [k (compute-dependents root-container v id-path-of-current)])))
          ))))
  ([container] (compute-dependents container container [])))


(defn setup-dependencies [container]
  (let [ with-abs-dependencies (compute-dependencies container)]
    (compute-dependents (compute-dependents-raw with-abs-dependencies))))
;(defn setup-dependencies [container]
;  (let [ with-abs-dependencies (if (:evolver-abs-dependencies container) container (compute-dependencies container))]
;    (compute-dependents (compute-dependents-raw with-abs-dependencies))))


(defn- substitute-in-map-to-list-of-vecs [m src-id id]
  (into {}
    (for [[p list-of-vecs] m]
      [p (map (fn [v] (mapv #(if (= src-id %1) id %1) v)) list-of-vecs)])))

(defn clone-component [c new-id]
  (let [ c-id (:id c)]
    (compute-dependents
      nil
      (assoc c
        :id new-id
        :abs-dependents (substitute-in-map-to-list-of-vecs (:abs-dependents c) c-id new-id)
        :evolver-abs-dependencies (substitute-in-map-to-list-of-vecs (:evolver-abs-dependencies c) c-id new-id))
      (:path-to-target c))))

(defn- get-abs-dependents-key [c-path]
  (conjv (conjv (get-access-key (drop-lastv c-path)) :abs-dependents) (last c-path)))

;(defn apply-flex-changes [container flex-structure-changes]
;  (reduce-kv
;    (fn [i k v] (do                                         (println "UIIKV " k "->" (get-in container k) " concating " v)
;                    (update-in i k concat v)))
;    container
;    (into {} (map (fn [[k v]] [(get-abs-dependents-key k) v]) flex-structure-changes))))

(defn apply-flex-changes [container flex-structure-changes]
  (reduce-kv
    (fn [i k v] (do                                         ;(println "UIIKV " k "->" (get-in container k) " KEY " (get-access-key (drop-lastv k)) " concating " v)
                     (if (get-in container (get-access-key (drop-lastv k)))
                       (update-in i (get-abs-dependents-key k) concat v)
                       i)))
    container
    flex-structure-changes))


(defn- combine-dep [p n]
  (assoc
    p
    :properties (vec (concat (:properties p) (:properties n)))
    :children (merge-with combine-dep (:children p) (:children n))))


(defn recompute-out-dependents [container flex-structure-changes]
  (reduce-kv
    (fn [i k v] (let [ p (get-access-key (drop-lastv k))
                      ;a (conjv p :abs-dependents)
                       o (conjv p :out-dependents)]
                  (do
                    ;(println "RIKV " k "->" v)
                    ;(println " ----was- RIKV " (get-in i (conj o (last k))))

                    ;(println " -adding- RIKV " (intree v))
                    ;(println " -result- RIKV " (merge-with combine-dep (get-in i (conj o (last k))) (intree v)))

                    ;(println " -adding- RIKV " {(last k) (intree v)})
                    ;(println " -result- RIKV " {(last k) (intree (get-in i (conj o (last k))) v)})

                    ;(println "UIIKV " k "->" (get-in container k) " KEY " p)

                    (if (get-in container p)
                      (assoc-in i (conj o (last k)) (merge-with combine-dep (get-in i (conj o (last k))) (intree v)))
                      i)
                    ;(assoc-in i o (get-in i o))
                      )))
    container
    flex-structure-changes))