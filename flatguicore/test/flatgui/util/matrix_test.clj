; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.util.matrix-test
  (:require
    [clojure.test :as test]
    [flatgui.util.matrix :as m]))


(test/deftest defmxcol-test
  (test/is (= [[3] [4] [1] [9]] (m/defmxcol 3 4 1 9))))

(test/deftest mx*-test
  (let [ tm [[1 0 0 2]
             [0 1 0 3]
             [0 0 1 1]
             [0 0 0 1]]
         v (m/defmxcol 5 6 2 1)
         expected (m/defmxcol 7 9 3 1)]
    (test/is (= expected (m/mx* tm v)))))

(test/deftest mx+-test
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
  (test/is (= r (m/mx+ a b)))
  (test/is (= r (m/mx+ b a)))))

(test/deftest mx-set-test
  (let [ a [[1 2 3 4]
            [5 6 7 8]
            [9 0 6 5]
            [4 3 2 1]]
         b [[1 2 3 5]
            [5 6 7 8]
            [9 1 6 5]
            [4 3 2 1]]]
    (test/is (= b (m/mx-set (m/mx-set a 2 1 1) 0 3 5)))))