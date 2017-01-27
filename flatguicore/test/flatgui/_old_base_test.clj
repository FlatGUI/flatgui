; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui._old_base-test
  (:require [clojure.test :as test]
            [flatgui._old_base :as fg]))

;;; FIXME continue the test blow
;(test/deftest get-property-private-test
;  (let [parents '( {:children {:x {:a 1}} :a 0}
;                    {:children {:y {:a 2 :children {:c1 {:a 3}}}}}
;                    {:children {:z {:a 4 :children {:c1 {:a 5 :children {:c1 {:a 6}}}}}}}
;                    )
;        component {:parents parents}]
;    (test/is (= 0 (b/get-property-private component  [] :a)))
;    (test/is (= 1 (b/get-property-private component  [:x] :a)))
;    (test/is (= 2 (b/get-property-private component  [:_ :y] :a)))
;    (test/is (= 3 (b/get-property-private component  [:_ :y :c1] :a)))
;    (test/is (= 4 (b/get-property-private component  [:_ :_ :z] :a)))
;    (test/is (= 5 (b/get-property-private component  [:_ :_ :z :c1] :a)))
;    (test/is (= 6 (b/get-property-private component  [:_ :_ :z :c1 :c1] :a)))))
(test/deftest get-property-private-test
  (let [main {:id :main :path-to-target [] :children {:a {:id :a :path-to-target [:main] :p :pa}
                                                      :b {:id :b :path-to-target [:main] :p :pb}
                                                      }}
        main (assoc main :root-container main)]
    (test/is (= :pa (fg/get-property-private main [:this :a] :p)))))


;;; FIXME
;(test/deftest defwidget-test
;  (let [base-1 {:a 1 :b 2 :widget-type "base-1"}
;        base-2 {:a 1 :b 2 :c {:x 4 :y 5} :widget-type "base-2"}
;        sub-type {:b 3 :c {:y 6}}
;        expected-type {:a 1 :b 3 :c {:x 4 :y 6} :widget-type "defwidget-test"}]
;    (do
;      (b/defwidget "defwidget-test" sub-type base-1 base-2)
;      (test/is (= expected-type defwidget-test)))))

;;; FIXME
;(test/deftest defwidget-test1
;  (let [sub-type {:b 3 :c {:y 6}}
;        expected-type {:b 3 :c {:y 6} :widget-type "defwidget-test1"}]
;    (do
;      (b/defwidget "defwidget-test1" sub-type)
;      (test/is (= expected-type defwidget-test1)))))

(test/deftest merge-ordered-test
  (let [m1 (array-map :a 1 :b (array-map :x 1 :y 2) :c 3)
        m2 (array-map :d 2 :b (array-map :y 3 :z 3) :c 4)]
    (test/is (= (array-map :a 1, :b {:x 1, :y 3, :z 3}, :c 4, :d 2) (fg/merge-ordered m1 m2)))))