; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.util.rectmath-test
  (:require
    [clojure.test :as test]
    [flatgui.util.rectmath :as r]))


(test/deftest line&-test
  (test/is (= [1 2] (r/line& 0 2 1 3)))
  (test/is (= [2 4] (r/line& 2 5 0 4)))
  (test/is (= [1 2] (r/line& 0 4 1 2)))
  (test/is (= [2 3] (r/line& 2 3 1 4)))
  (test/is (nil? (r/line& 0 1 2 3)))
  (test/is (nil? (r/line& 4 5 0 2))))

(test/deftest valid-rect?-test
  (test/is (true? (r/valid-rect? {:x 1 :y 2 :w 4 :h 5})))
  (test/is (false? (r/valid-rect? {:x 1 :y 2 :w 0 :h 5})))
  (test/is (false? (r/valid-rect? {:x 1 :y 2 :w -2 :h 5})))
  (test/is (false? (r/valid-rect? {:x 1 :y 2 :w 5 :h 0})))
  (test/is (false? (r/valid-rect? {:x 1 :y 2 :w 5 :h -3})))
  (test/is (false? (r/valid-rect? {:x 1 :y 2 :w 0 :h -4}))))

(test/deftest rect&-test
  (test/is (= {:x 2 :y 3 :w 3 :h 2} (r/rect&
                                 {:x 0 :y 0 :w 5 :h 5}
                                 {:x 2 :y 3 :w 5 :h 5})))
  (test/is (= {:x 2 :y 3 :w 3 :h 2} (r/rect&
                                 {:x 2 :y 3 :w 5 :h 5}
                                 {:x 0 :y 0 :w 5 :h 5})))
  (test/is (= {:x 3 :y 2 :w 2 :h 3} (r/rect&
                                 {:x 0 :y 2 :w 5 :h 5}
                                 {:x 3 :y 0 :w 5 :h 5})))
  (test/is (= {:x 3 :y 2 :w 2 :h 3} (r/rect&
                                 {:x 3 :y 0 :w 5 :h 5}
                                 {:x 0 :y 2 :w 5 :h 5})))
  (test/is (= {:x 2 :y 2 :w 2 :h 2} (r/rect&
                                 {:x 1 :y 0 :w 5 :h 7}
                                 {:x 2 :y 2 :w 2 :h 2})))
  (test/is (= {:x 2 :y 2 :w 2 :h 2} (r/rect&
                                 {:x 2 :y 2 :w 2 :h 2}
                                 {:x 0 :y 1 :w 8 :h 7})))
  (test/is (= {:x 2 :y 2 :w 3 :h 4} (r/rect&
                                 {:x 2 :y 2 :w 3 :h 4}
                                 {:x 2 :y 2 :w 3 :h 4})))
  (test/is (nil? (r/rect&
              {:x 2 :y 2 :w 3 :h 4}
              {:x 10 :y 2 :w 6 :h 4}))))

(test/deftest rect--test
  (test/is (= (list {:x 1 :y 2 :w 6 :h 1}
               {:x 1 :y 3 :w 3 :h 2}
               {:x 5 :y 3 :w 2 :h 2}
               {:x 1 :y 5 :w 6 :h 1})
        (r/rect-
          {:x 1 :y 2 :w 6 :h 4}
          {:x 4 :y 3 :w 1 :h 2})))
  (test/is (= (list {:x 0 :y 0 :w 5 :h 3}
               {:x 0 :y 3 :w 2 :h 2})
        (r/rect-
          {:x 0 :y 0 :w 5 :h 5}
          {:x 2 :y 3 :w 5 :h 5})))
  (test/is (= (list {:x 22 :y 2 :w 3 :h 4})
        (r/rect-
          {:x 22 :y 2 :w 3 :h 4}
          {:x 10 :y 2 :w 6 :h 4})))
  (test/is (empty?
        (r/rect-
          {:x 1 :y 2 :w 3 :h 4}
          {:x 1 :y 2 :w 3 :h 4})))
  (test/is (empty?
        (r/rect-
          {:x 10 :y 5 :w 1 :h 1}
          {:x  4 :y 3 :w 9 :h 3}))))

(test/deftest rects--test
  (test/is (= #{{:x 0  :y 0 :w 10 :h 3}
           {:x 0  :y 3 :w  2 :h 2}
           {:x 12 :y 7 :w  6 :h 2}
           {:x 8  :y 9 :w 10 :h 3}}
        (set (r/rects-
          (list {:x 0 :y 0 :w 10 :h 5} {:x 8 :y 7 :w 10 :h 5})
          {:x 2 :y 3 :w 10 :h 6}))))
  (test/is (= #{{:x 1, :y 1, :w 9, :h 2}
           {:x 1, :y 3, :w 3, :h 1}}
        (set (r/rects-
               (list {:x 4, :y 3, :w 9, :h 3} {:x 1, :y 1, :w 9, :h 2} {:x 1, :y 3, :w 3, :h 1})
               {:has-changes true, :prev-w 9, :prev-x 1, :y 3, :x 4, :prev-y 1, :h 3, :w 9, :prev-h 3, :id :c_1_1_1})))))

(test/deftest rect+-test
  (let [r1 {:x 1 :y 0 :w 3 :h 1}
        r2 {:x 0 :y 1 :w 4 :h 1}
        r3 {:x 0 :y 0 :w 2 :h 2}
        r4 {:x 2 :y 0 :w 1 :h 3}
        r12 {:x 1 :y 0 :w 3 :h 2}
        r34 {:x 0 :y 0 :w 3 :h 2}
        r5 {:x 1 :y 0 :w 3 :h 1}
        r6 {:x 0 :y 2 :w 4 :h 1}
        r7 {:x 0 :y 0 :w 2 :h 2}
        r8 {:x 3 :y 0 :w 1 :h 3}
        r9 {:x 1 :y 0 :w 3 :h 1}
        ra {:x 0 :y 0.2 :w 4 :h 1}
        rb {:x 0 :y 0 :w 2 :h 2}
        rc {:x 2 :y 1.125 :w 1 :h 3}
        r9a {:x 1 :y 0 :w 3 :h 1.2}
        rbc {:x 0 :y 1.125 :w 3 :h 0.875}
        rd {:x 0 :y 0 :w 2 :h 2}
        re {:x 1 :y 1 :w 1 :h 3}
        rde {:x 1 :y 0 :w 1 :h 4}
        rf {:x 0 :y 0 :w 4 :h 4}
        rg {:x 1 :y 1 :w 2 :h 2}
        rfg {:x 0 :y 0 :w 4 :h 4}]
    (test/is (= r1 (rect+ r1 r1)))
    (test/is (= r12 (rect+ r1 r2)))
    (test/is (= r12 (rect+ r2 r1)))
    (test/is (= r34 (rect+ r3 r4)))
    (test/is (= r34 (rect+ r4 r3)))
    (test/is (nil? (rect+ r5 r6)))
    (test/is (nil? (rect+ r6 r5)))
    (test/is (nil? (rect+ r7 r8)))
    (test/is (nil? (rect+ r8 r7)))
    (test/is (= r9a (rect+ r9 ra)))
    (test/is (= r9a (rect+ ra r9)))
    (test/is (= rbc (rect+ rb rc)))
    (test/is (= rbc (rect+ rc rb)))
    (test/is (= rde (rect+ rd re)))
    (test/is (= rde (rect+ re rd)))
    (test/is (= rfg (rect+ rf rg)))
    (test/is (= rfg (rect+ rg rf)))))

(test/deftest get-largest-adjacent-test
  (let [r1 {:x 2 :y 0 :w 5 :h 2}
        r2 {:x 0 :y 2 :w 7 :h 2}
        r3 {:x 2 :y 4 :w 5 :h 1}
        r4 {:x 2 :y 5 :w 1 :h 1}
        r5 {:x 4 :y 5 :w 3 :h 1}
        res {:x 2 :y 0 :w 5 :h 5}]
    (test/is (= res (get-largest-adjacent [r1 r2 r3 r4 r5])))))


