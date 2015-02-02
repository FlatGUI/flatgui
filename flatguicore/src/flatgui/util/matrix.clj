; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for matrix operations"
      :author "Denys Lebediev"}
  flatgui.util.matrix
  (:use clojure.test))


(def IDENTITY-MATRIX [[1 0 0 0]
                      [0 1 0 0]
                      [0 0 1 0]
                      [0 0 0 1]])

(defn transtation-matrix
  "Constructs translation matrix"
  ([tx ty tz]
   [[1 0 0 tx]
    [0 1 0 ty]
    [0 0 1 tz]
    [0 0 0  1]])
  ([tx ty] (transtation-matrix tx ty 0)))

(defn defmxcol [& values]
  "Defines 1-column matrix"
  ;(vec (for [v values] [v]))
  (mapv (fn [v] [v]) values)
  )

(defn mx-get [m r c]
  "Returns element specified by row r and column c from matrix m"
  (nth (nth m r) c))

(defn mx-x [m]
  (mx-get m 0 3))

(defn mx-y [m]
  (mx-get m 1 3))

(defn mx-row-count [m]
  "Returns row count of matrix m"
  (count m))

(defn mx-col-count [m]
  "Returns column count of matrix m"
  (if (> (mx-row-count m) 0)
    (count (nth m 0))
    0))

(defn mx* [a b]
  "Returns multiplication of matrices a and b"
  (let [ row-count (mx-row-count a)
         col-count (mx-col-count b)
         comb-len (mx-col-count a)]
    (loop [ r 0
          matrix (transient [])]
    (if (< r row-count)
      (recur
        (inc r)
        (conj! matrix
          (loop [ c 0
                  row (transient [])]
            (if (< c col-count)
              (recur
                (inc c)
                (conj! row
                  (loop [ i 0
                          value 0]
                    (if (< i comb-len)
                      (recur
                        (inc i)
                        (+ value (* (mx-get a r i) (mx-get b i c))))
                      value))))
              (persistent! row)))))
      (persistent! matrix)))))

(defn mx-op [op a b]
  "Returns result of op applied to matrices a and b"
  (let [ row-count (mx-row-count a)
         col-count (mx-col-count a)]
    (loop [ r 0
            matrix (transient [])]
      (if (< r row-count)
        (recur
          (inc r)
          (conj! matrix
            (loop [ c 0
                    row (transient [])]
              (if (< c col-count)
                (recur
                  (inc c)
                  (conj! row (op (mx-get a r c) (mx-get b r c))))
                (persistent! row)))))
        (persistent! matrix)))))

(defn mx+ [a b] (mx-op + a b))

(defn mx- [a b] (mx-op - a b))

(defn mx-set [m r c v]
  "Returns same matrix with element at position r,c set to value v"
  (assoc m r (assoc (nth m r) c v)))

(defn mx-setx [m v]
  "Returns same translation matrix with x translation set to value v"
  (assoc m 0 (assoc (nth m 0) 3 v)))

(defn mx-sety [m v]
  "Returns same translation matrix with x translation set to value v"
  (assoc m 1 (assoc (nth m 1) 3 v)))

(defn mx-set! [m r c v]
  "Returns same matrix with element at position r,c set to value v"
  (persistent! (assoc! (transient m) r (persistent! (assoc! (transient (nth m r)) c v)))))

(defn defpoint
  ([x y z] (defmxcol x y z 1))
  ([x y] (defpoint x y 0)))

(defn x [point] (mx-get point 0 0))

(defn y [point] (mx-get point 1 0))

(defn z [point] (mx-get point 2 0))

(defn point-op [op a b] (mx-set (mx-op op a b) 3 0 1))

(def zero-point (defpoint 0 0 0))

; Tests

(deftest defmxcol-test
  (is (= [[3] [4] [1] [9]] (defmxcol 3 4 1 9))))

(deftest mx*-test
  (let [ tm [[1 0 0 2]
             [0 1 0 3]
             [0 0 1 1]
             [0 0 0 1]]
         v (defmxcol 5 6 2 1)
         expected (defmxcol 7 9 3 1)]
    (is (= expected (mx* tm v)))))

(deftest mx+-test
  (let [ a [[1 2 3 4]
            [5 6 7 8]
            [9 0 6 5]
            [4 3 2 1]]

         b [[1 4 7 2]
            [0 1 0 3]
            [8 0 9 1]
            [0 4 0 1]]

         r  [[2   6 10  6]
             [5   7  7 11]
             [17  0 15  6]
             [4   7  2  2]]]
  (is (= r (mx+ a b)))
  (is (= r (mx+ b a)))))

(deftest mx-set-test
  (let [ a [[1 2 3 4]
            [5 6 7 8]
            [9 0 6 5]
            [4 3 2 1]]
         b [[1 2 3 5]
            [5 6 7 8]
            [9 1 6 5]
            [4 3 2 1]]]
    (is (= b (mx-set (mx-set a 2 1 1) 0 3 5)))))