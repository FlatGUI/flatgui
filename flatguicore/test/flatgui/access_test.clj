; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.access-test
  (:require [clojure.test :as test]
            [flatgui.access :as a]
            [flatgui.util.matrix :as m]))


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

; get-component-path-by-id-path does not seem to be needed
;(test/deftest get-component-path-by-id-path-test
;  (let [container {:id :c :a 1 :b 2 :children {:c1 {:a 2 :b 3 :id :c1
;                                               :children {:c2 {:a 3 :b 2 :id :c2}
;                                                          :c3 {:a 4 :b 2 :id :c3}}}}}]
;    (test/is (=
;               [{:a 1 :b 2} {:a 2 :b 3} {:a 4 :b 2}]
;               (mapv #(dissoc % :children :id) (a/get-component-path-by-id-path container [:c :c1 :c3]))))))

; assign-mouse-rel-coords does not seem to be needed
;(test/deftest assign-mouse-rel-coords-test
;  (let [container {:id :c :a 1 :b 2 :position-matrix m/IDENTITY-MATRIX
;                   :children {:c1 {:a 2 :b 3 :id :c1 :position-matrix (m/translation 3 2)
;                              :children {:c2 {:a 3 :b 2 :id :c2 :position-matrix (m/translation 6 8)}
;                                         :c3 {:a 4 :b 2 :id :c3}}}}}
;        component-path (mapv
;                         #(dissoc % :children :id :a :b)
;                         (a/get-component-path-by-id-path container [:c :c1 :c2]))]
;    (test/is (=
;               [{:mouse-x-relative 11 :mouse-y-relative 13}
;                {:mouse-x-relative 8 :mouse-y-relative 11}
;                {:mouse-x-relative 2 :mouse-y-relative 3}]
;               (map
;                 #(dissoc % :position-matrix)
;                 (a/assign-mouse-rel-coords component-path 2 3))))))
