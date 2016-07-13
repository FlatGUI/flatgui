; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.core-test
  (:require [clojure.test :as test]
            [flatgui.core :as core]))

(test/deftest build-abs-path-test
  (test/is (= [:a :b :c] (core/build-abs-path [:a :b :c] [:this])))
  (test/is (= [:a :b :c :d] (core/build-abs-path [:a :b :c] [:this :d])))
  (test/is (= [:a :b :c :d :e] (core/build-abs-path [:a :b :c] [:this :d :e])))
  (test/is (= [:a :b] (core/build-abs-path [:a :b :c] [])))
  (test/is (= [:a :u :v] (core/build-abs-path [:a :b :c] [:_ :u :v])))
  (test/is (= [:a] (core/build-abs-path [:a :b :c] [:_])))
  (test/is (= [:c] (core/build-abs-path [] [:c]))))

(test/deftest get-property-call?-test
  (test/is (true? (core/get-property-call? (list 'get-property [:a :b] :c))))
  (test/is (true? (core/get-property-call? (list 'get-property 'component [:a :b] :c))))
  (test/is (false? (core/get-property-call? (list 'component [:a :b] :c)))))

(test/deftest replace-rel-path-test
  (test/is (=
             (core/replace-rel-path (list 'get-property [:this] :c) [:a :b])
             (list 'get-property [:a :b] :c))))

(test/deftest replace-all-rel-paths-test
  (test/is (=
             (core/replace-all-rel-paths
               (list 'println 1 2 (list 'get-property [:this :d] :c))
               [:a :b])
             (list 'println 1 2 (list 'get-property [:a :b :d] :c)))))


(test/deftest defroot-test
  (let [_ (core/defevolverfn e1 (+ 1 (get-property [:this] :a)))
        _ (core/defevolverfn e2 (- 2 (get-property [:this :c1] :a)))
        _ (core/defevolverfn e11 (+ 2 (get-property [] :x)))
        _ (core/defevolverfn e21 (+ 3 (get-property [:c1] :b)))
        container {:id :main
                   :a 4
                   :b 5
                   :evolvers {:a e1
                              :b e2}
                   :children {:c1 {:id :c1
                                   :b 5
                                   :evolvers {:b e11}}
                              :c2 {:id :c2
                                   :d 6
                                   :evolvers {:d e21}}}}]
    (test/is
      (=
        (core/defroot container)
        {:id :main
         :a 4
         :b 5
         :evolvers {:a '(+ 1 (get-property [] :a))
                    :b '(- 2 (get-property [:c1] :a))}
         :children {:c1 {:id :c1
                         :b 5
                         :evolvers {:b '(+ 2 (get-property [] :x))}}
                    :c2 {:id :c2
                         :d 6
                         :evolvers {:d '(+ 3 (get-property [:c1] :b))}}}}))))

(test/deftest collect-evolver-dependencies-test
  (let [_ (core/defevolverfn e1 (if (get-property [:main :x :y] :z)
                                  (get-property [:main :a :b] :c)
                                  (do
                                    (println "Hello")
                                    (get-property [:main] :v))))]
    (test/is
      (=
        #{[:main :x :y :z] [:main :a :b :c] [:main :v]}
        (set (core/collect-evolver-dependencies e1))))))