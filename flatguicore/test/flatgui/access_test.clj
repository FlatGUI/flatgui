; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.access-test
  ;;; TODO Get rid of :use
  (:use flatgui.comlogic flatgui.base flatgui.util.matrix)
  (:require [clojure.test :as test]
            [flatgui.access :as a]))


;;; FIXME
;(test/deftest get-components-test
;  (let [ container {:a 1 :b 2 :evolvers {} :children {:c1 {:a 2 :b 3 :evolvers {} :children {:c2 {:a 3 :b 2 :evolvers {}}}}}}
;        expected '({:a 1 :b 2 :evolvers {} :children {:c1 {:a 2 :b 3 :evolvers {} :children {:c2 {:a 3 :b 2 :evolvers {}}}}}}
;                    {:a 3 :b 2 :evolvers {}})]
;    (test/is (= expected (a/get-components container (fn [c] (= 2 (:b c))))))))

(test/deftest get-any-component-test
  (let [ container {:a 1 :b 2 :evolvers {} :children {:c1 {:a 2 :b 3 :evolvers {}
                                                           :children (array-map :c2 {:a 3 :b 2 :children {} :evolvers {}}
                                                                                :c3 {:a 4 :b 2 :children {} :evolvers {}})}}}]
    (test/is (= 1 (:a (a/get-any-component container (fn [c] (= 2 (:b c)))))))
    (test/is (= 2 (:a (a/get-any-component container (fn [c] (= 3 (:b c)))))))
    (nil? (a/get-any-component container (fn [c] (= 4 (:b c)))))))
