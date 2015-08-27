; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Namespace that contains FlatGUI component logic assumptions in one place.
            To be included into other FlatGUI implemantaion namespaces"
      :author "Denys Lebediev"}
    ;;; TODO either remove this namespace or make client apps not use it
  flatgui.comlogic
  (:import [clojure.lang Keyword]
           (java.awt Color))
  (:require [flatgui.util.matrix :as m]))

(defn masknil [a] (if (nil? a) 0.0 a))

(defn inrange [v rmin rmax]
  (min (max v rmin) rmax))

;(defn defpoint
;  ([x y z] (m/defmxcol x y z 1))
;  ([x y] (m/defpoint x y 0)))
;
;(defn x [point] (m/mx-get point 0 0))
;(defn y [point] (m/mx-get point 1 0))
;(defn z [point] (m/mx-get point 2 0))
;
;(defn point-op [op a b] (m/mx-set (m/mx-op op a b) 3 0 1))
;
;(def zero-point (m/defpoint 0 0 0))

;;; TODO following are deprecated

(def defpoint m/defpoint)

(def x m/x)

(def y m/y)

(def z  m/z)

(def point-op m/point-op)

(def zero-point m/zero-point)



(defn defrect
  ([top-left bottom-right] {:top-left top-left :bottom-right bottom-right})
  ([x1 y1 z1 x2 y2 z2] {:top-left (defpoint x1 y1 z1) :bottom-right (defpoint x2 y2 z2)}))


(defn get-children [component] (for [[k v] (:children component)] v))

(defn get-path-down [path]
  (let [ parent-index-to-start (count (take-while (fn [e] (= :_ e)) path))
         path-down (take-last (- (count path) parent-index-to-start) path)]
    path-down))

(defn get-access-key-old [target-id-path] (vec (mapcat (fn [e] [:children e]) (next target-id-path))))

(defn get-access-key
  ([target-id-path c]
    (if (> c 0)
      (loop [ i 1
              key (transient (vec (make-array Keyword (* 2 (dec c)))))]
      (if (< i c)
        (recur
          (inc i)
          (let [ ni (* (dec i) 2)]
            (assoc! (assoc! key ni :children) (inc ni) (nth target-id-path i))))
        (persistent! key)))
      []))
  ([target-id-path] (get-access-key target-id-path (count target-id-path))))

(defn child-reason? [reason]
  (and (vector? reason) (= 2 (count reason)) (= :this (nth reason 0))))

(defn parent-reason? [reason]
  (= reason []))

;;;; temporary
(defn make-color [r g b] (new Color r g b))
(def std-colors
  {:prime-1 (make-color 0 78 145) ; Button etc. face
   :prime-2 (make-color 51 113 167) ; Shadowed component background
   :prime-3 (make-color 225 241 255) ; Panel surface
   :prime-4 (make-color 255 255 255) ; Regular component background
   :prime-5 (make-color 225 241 255) ; Selection indication
   :prime-6 (make-color 0 78 145) ; Regular component foreground
   :prime-gradient-start (make-color 0 87 152) ; Same usage as :prime-1 but for faces with gradient
   :prime-gradient-end (make-color 0 70 136) ; Same usage as :prime-1 but for faces with gradient
   :extra-1 (make-color 199 199 199) ; Foreground extra
   :extra-2 (make-color 234 237 236) ; Background extra
   :engaged (make-color 34 168 108)  ; Engaged checkable (radiobutton, checkbox, etc)
   })
(def standard-theme-colors (into #{} (for [[k _] std-colors] k)))
(defn standard-color? [color-property-name] (contains? standard-theme-colors color-property-name))


;;; TODO move below functions to some util

(defn set-conj [s e]
  (conj (if s s #{}) e))

(defn set-conj! [s e]
  (conj! (if s s (transient #{})) e))

(defn drop-lastv [v]
  (let [ c (dec (count v))]
    (loop [ i 0
            r (transient (vec (make-array Keyword c)))]
      (if (< i c)
        (recur
          (inc i)
          (assoc! r i (nth v i)))
        (persistent! r)))))

;;;
;;; TODO which one is faster?
;;;
(defn conjv [v e]
  (let [ c (count v)]
    (loop [ i 0
            r (transient (vec (make-array Keyword (inc c))))]
      (if (< i c)
        (recur
          (inc i)
          (assoc! r i (nth v i)))
        (persistent! (assoc! r c e))))))
;(defn conjv [v e] (conj v e))


(defn round-to [n p]
  (let [ r (rem n p)]
    (if (zero? r)
      n
      (let [ m (int (/ n p))]
        (if (> r (/ p 2))
          (* p (inc m))
          (* p m))))))

(defn get-evolved-properties [container targret-access-key]
  (get-in (:aux-container container) (conjv targret-access-key :evolved-properties)))

(defn get-evolved-properties-by-path [container targret-path]
  (let [targret-access-key (get-access-key targret-path)]
    (get-in (:aux-container container) (conjv targret-access-key :evolved-properties))))

(defn property-evolved? [container targret-path property]
  (let [evolved-properties (get-evolved-properties-by-path container targret-path)]
    (if evolved-properties
      (not (nil? (evolved-properties property)))
      false)))

(defn get-changed-properties [container targret-access-key]
  (get-in (:aux-container container) (conjv targret-access-key :changed-properties)))

(defn get-changed-properties-by-path [container targret-path]
  (let [targret-access-key (get-access-key targret-path)]
    (get-in (:aux-container container) (conjv targret-access-key :changed-properties))))

(defn property-changed? [container targret-path property]
  (let [changed-properties (get-changed-properties-by-path container targret-path)]
    (if changed-properties
      (not (nil? (changed-properties property)))
      false)))