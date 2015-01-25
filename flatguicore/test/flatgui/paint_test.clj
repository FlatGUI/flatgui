; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.paint-test
  (:require [clojure.test :as test]
            [flatgui.paint :as p]))


(test/deftest leaf?-test
  (test/is (true? (p/leaf? [1 2])))
  (test/is (true? (p/leaf? [1])))
  (test/is (true? (p/leaf? [1 nil])))
  (test/is (false? (p/leaf? [1 [1 2]])))
  (test/is (false? (p/leaf? nil)))
  (test/is (false? (p/leaf? []))))

(test/deftest flatten-vector-test1
  (let [expected [[1 2 3 4]]
        actual (p/flatten-vector [1 2 3 4])]
    (test/is (= expected actual))))

(test/deftest flatten-vector-test2
  (let [sample [[1 2 3 4] []]
        expected [[1 2 3 4]]
        actual (p/flatten-vector sample)]
    (test/is (= expected actual))))

(test/deftest flatten-vector-test3
  (let [sample [[1 2 3 4] nil []]
        expected [[1 2 3 4]]
        actual (p/flatten-vector sample)]
    (test/is (= expected actual))))

(test/deftest flatten-vector-test4
  (let [sample [[1 2 3 4] [5 6 7 8]]
        expected [[1 2 3 4] [5 6 7 8]]
        actual (p/flatten-vector sample)]
    (test/is (= expected actual))))

(test/deftest flatten-vector-test5
  (let [sample [ [1 2 3 4] [ [5 6 7 8] [9 10 11 12] ] ]
        expected [[1 2 3 4] [5 6 7 8] [9 10 11 12]]
        actual (p/flatten-vector sample)]
    (test/is (= expected actual))))

(test/deftest flatten-vector-test6
  (let [sample [ [1 2 3 4]
                [
                 [5 6 7 8]
                 nil
                 [9 10 11 12]
                 [
                  [
                   [13 14]
                   [15 16]
                   ]
                  [17 18]
                  [
                   [
                    [19 20]
                    []
                    [21 22]
                    ]
                   nil
                   ]
                  ]
                 ]
                ]
        expected [[1 2 3 4] [5 6 7 8] [9 10 11 12] [13 14] [15 16] [17 18] [19 20] [21 22]]
        actual (p/flatten-vector sample)]
    (test/is (= expected actual))))

(test/deftest flatten-vector-test8
  (let [sample [nil]
        expected []
        actual (p/flatten-vector sample)]
    (test/is (= expected actual))))

(test/deftest flatten-vector-test9
  (let [sample [nil nil]
        expected []
        actual (p/flatten-vector sample)]
    (test/is (= expected actual))))

(test/deftest flatten-vector-test10
  (let [sample [nil [1 nil]]
        expected [[1 nil]]
        actual (p/flatten-vector sample)]
    (test/is (= expected actual))))

(test/deftest flatten-vector-test11
  (let [sample [1 nil]
        expected [[1 nil]]
        actual (p/flatten-vector sample)]
    (test/is (= expected actual))))

;;; FIXME these do not compile after refactoring: Unable to resolve symbol: x in this context
;(test/deftest deflookfn-test
;  (do
;    (p/deflookfn test-look (:xx :yy :z) (str xx) (str yy) (str "here is " z))
;    (test/is (= [["22" "33" "here is 44"]] (test-look {:xx 22 :yy 33 :z 44 :w 55 :children {:a 1 :b 2}} nil)))))
;
;(test/deftest deflookfn-test1
;  (do
;    (p/deflookfn test-look ([:xx :position] :c :background) (str "xx=" xx) (str "c=" c) (str "b=" background))
;    (test/is (= [["xx=1" "c=2" "b=test"]] (test-look {:c 2 :position {:xx 1} :background :default :theme {:default "test"}} nil)))))
;

;;; FIXME
;(test/deftest paint-component-with-children-test
;  (do
;    (p/deflookfn test-look (:content-size) (str ("content: " content-size)))
;    (let [component (defcomponent {} :c {:clip-size (defpoint 1 1 0)
;                                         :content-size (defpoint 1 1 0)}
;                        (defcomponent {} :cc1 {}
;                            (defcomponent {} :cc1c {}))
;                        (defcomponent {} :cc2 {}))]
;      (test/is (= )))))

;(test/deftest find-rects-to-paint-test
;  (let [ test-container {:x 0 :y 0 :w 20 :h 20 :id :c
;                         :children { :c1 {:id :c1 :x  3 :y 2 :w 7 :h 4 :prev-x  1 :prev-y 1 :prev-w 7 :prev-h 4 :has-changes true}
;                                     :c2 {:id :c2 :x  9 :y 5 :w 2 :h 2 :prev-x  9 :prev-y 5 :prev-w 2 :prev-h 2 :has-changes true}
;                                     :c3 {:id :c3 :x 13 :y 2 :w 2 :h 3 :prev-x 13 :prev-y 2 :prev-w 2 :prev-h 3 :has-changes true}
;                                     :c4 {:id :c4 :x 13 :y 7 :w 2 :h 2 :has-changes true}
;                                     :c5 {:id :c5 :x 16 :y 7 :w 1 :h 2}
;                                     }}
;         expected-rects #{ ; What remains from old position of c1
;                           {:x 1 :y 1 :w 7 :h 1}
;                           {:x 1 :y 2 :w 2 :h 3}
;                           ; New position of c1
;                           {:x 3 :y 2 :w 7 :h 4}
;                           ; c2
;                           {:x 9 :y 5 :w 2 :h 2}
;                           ; c3
;                           {:x 13 :y 2 :w 2 :h 3}
;                           ; c4
;                           {:x 13 :y 7 :w 2 :h 2}
;                          }]
;    (test/is (= expected-rects (set (find-rects-to-paint test-container))))))
;
;(test/deftest find-rects-to-paint-test-2
;  (let [ test-container {:x 0 :y 0 :w 15 :h 10 :id :c
;                         :children {:c_1 {:id :c_1 :x 0 :y 0 :w 3 :h 6 :prev-x 0 :prev-y 0 :prev-w 3 :prev-h 6
;                                          :children {:c_1_1 {:id :c_1_1 :x 1 :y 1 :w 2 :h 3 :prev-x 1 :prev-y 1 :prev-w 2 :prev-h 3
;                                                             :children {:c_1_1_1 {:id :c_1_1_1 :x 3 :y 2 :w 9 :h 3 :prev-x 1 :prev-y 0 :prev-w 9 :prev-h 3 :has-changes true}}}
;                                                     :c_1_2 {:id :c_1_2 :x 10 :y 5 :w 1 :h 1 :prev-x 10 :prev-y 5 :prev-w 1 :prev-h 1}
;                                                     :c_1_3 {:id :c_1_3 :x 12 :y 1 :w 2 :h 3 :prev-x 12 :prev-y 1 :prev-w 2 :prev-h 3}}}}}
;         expected-rects #{ ; What remains from old position of c1  :c_1_1_1
;                          {:x 2 :y 1 :w 9 :h 2}
;                          {:x 2 :y 3 :w 2 :h 1}
;                          ; New position of :c_1_1_1
;                          {:x 4 :y 3 :w 9 :h 3}
;                          }]
;    (test/is (= expected-rects (set (find-rects-to-paint test-container))))))

;(test/deftest get-all-components-for-dirty-rects-test
;  (let [ test-container {:x 0 :y 0 :w 15 :h 10 :id :c
;                         :children {:c_1 {:id :c_1 :x 0 :y 0 :w 3 :h 6 :prev-x 0 :prev-y 0 :prev-w 3 :prev-h 6
;                                         :children {:c_1_1 {:id :c_1_1 :x 1 :y 1 :w 2 :h 3 :prev-x 1 :prev-y 1 :prev-w 2 :prev-h 3
;                                                            :children {:c_1_1_1 {:id :c_1_1_1 :x 3 :y 2 :w 9 :h 3 :prev-x 1 :prev-y 0 :prev-w 9 :prev-h 3 :has-changes true}}}
;                                                    :c_1_2 {:id :c_1_2 :x 10 :y 5 :w 1 :h 1 :prev-x 10 :prev-y 5 :prev-w 1 :prev-h 1}
;                                                    :c_1_3 {:id :c_1_3 :x 12 :y 1 :w 2 :h 3 :prev-x 12 :prev-y 1 :prev-w 2 :prev-h 3}
;                                                    :c_1_4 {:id :c_1_4 :x 12 :y 5 :w 3 :h 3 :prev-x 12 :prev-y 5 :prev-w 3 :prev-h 3 :has-changes true}}}}}
;         expected-ids #{:c :c_1_1 :c_1_4 :c_1_1_1}]
;    (test/is (= expected-ids (set (map (fn [c] (:id c)) (get-all-components-for-dirty-rects test-container (find-rects-to-paint test-container))))))))
;
;(test/deftest get-all-components-for-dirty-rects-test2
;  (let [ test-container {:x 0 :y 0 :w 15 :h 10 :id :c
;                         :children {:c_1 {:id :c_1 :x 3 :y 2 :w 4 :h 4
;                                          :children {:c_1_1 {:id :c_1_1 :x 1 :y 1 :w 1 :h 1}
;                                                     :c_1_2 {:id :c_1_2 :x 3 :y 1 :w 1 :h 1}
;                                                     }}}}
;         expected-ids #{:c_1_1 :c_1_2 :c_1}]
;    (test/is (= expected-ids (set (map (fn [c] (:id c)) (get-all-components-for-dirty-rects test-container '({:x 4 :y 3 :w 3 :h 2}))))))))

