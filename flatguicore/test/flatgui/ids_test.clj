; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.ids-test
  (:require [clojure.test :as test]
            [flatgui.ids :as ids]))


(test/deftest create-compound-id-test
  (test/is (= :c (ids/create-compound-id nil :c)))
  (test/is (= :p (ids/create-compound-id :p nil)))
  (test/is (= :a_b_c (ids/create-compound-id :a_b :c)))
  (test/is (= "a_b_c" (name (ids/create-compound-id :a_b :c))))
  (test/is (= "a_b_c_d" (name (ids/create-compound-id :a_b :c_d)))))

(test/deftest check-simple-id-test
  (test/is (true? (ids/check-simple-id :abc)))
  (test/is (true? (ids/check-simple-id :a)))
  (test/is (true? (ids/check-simple-id :a1)))
  (test/is (false? (ids/check-simple-id :a_bc)))
  (test/is (false? (ids/check-simple-id "abc")))
  (test/is (false? (ids/check-simple-id "ab_c"))))

(test/deftest get-id-depth-test
  (test/is (= 1 (ids/get-id-depth :abc)))
  (test/is (= 2 (ids/get-id-depth :a_bc)))
  (test/is (= 3 (ids/get-id-depth :a_b_c))))

(test/deftest get-id-path-test
  (test/is (= [:a] (ids/get-id-path :a)))
  (test/is (= [:a :a_b] (ids/get-id-path :a_b)))
  (test/is (= [:a :a_b :a_b_c] (ids/get-id-path :a_b_c)))
  (test/is (= [:a :a_b :a_b_c :a_b_c_d] (ids/get-id-path :a_b_c_d))))

(test/deftest get-parent-id-test
  (test/is (= :a_b_c (ids/get-parent-id :a_b_c_d)))
  (test/is (= :a (ids/get-parent-id :a_b)))
  (test/is (nil? (ids/get-parent-id :a))))

