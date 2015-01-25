; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for rect math operations"
      :author "Denys Lebediev"}
  flatgui.util.rectmath
  (:use clojure.test))

(defn line& [a1 a2 b1 b2]
  "Returns intersection of 1-dimentional lines defined
  by their begin and end coords."
  (if (and (>= b1 a1) (> a2 b1) (>= b2 a2))
    [b1 a2]
    (if (and (>= a1 b1) (> b2 a1) (>= a2 b2))
      [a1 b2]
      (if (<= b1 a1 a2 b2)
        [a1 a2]
        (if (<= a1 b1 b2 a2)
          [b1 b2])))))

(defn valid-rect? [r]
  (and (> (:w r) 0) (> (:h r) 0)))

(defn intersect? [a b]
  "Returns true if intersection of 2-dimentional rectangles
   (each defined by :x, :y, :w, :h) exists, false otherwise"
  (let [ x1a (:x a)
        y1a (:y a)
        x2a (+ x1a (:w a))
        y2a (+ y1a (:h a))
        x1b (:x b)
        y1b (:y b)
        x2b (+ x1b (:w b))
        y2b (+ y1b (:h b))
        x-inter (line& x1a x2a x1b x2b)
        y-inter (line& y1a y2a y1b y2b)]
    (and (not (nil? x-inter)) (not (nil? y-inter)))))

(defn rect& [a b]
  "Returns intersection of 2-dimentional rectangles
   each defined by :x, :y, :w, :h"
  (let [ x1a (:x a)
       y1a (:y a)
       x2a (+ x1a (:w a))
       y2a (+ y1a (:h a))
       x1b (:x b)
       y1b (:y b)
       x2b (+ x1b (:w b))
       y2b (+ y1b (:h b))
       x-inter (line& x1a x2a x1b x2b)
       y-inter (line& y1a y2a y1b y2b)]
  (if (and (not (nil? x-inter)) (not (nil? y-inter)))
    (let [ x1 (first x-inter)
           x2 (second x-inter)
           y1 (first y-inter)
           y2 (second y-inter)]
      {:x x1 :y y1 :w (- x2 x1) :h (- y2 y1)}))))

(defn rect- [a b]
  "Subtracts b rect from a rect producing the list of
   remaining rects. The result may be empty, or may
   contain from one to four rects depending on the
   intersection of a and b."
  (let [ inter (rect& a b)]
    (if (nil? inter)
      (list a)
      (let [top {:x (:x a) :y (:y a) :w (:w a) :h (- (:y inter) (:y a))}
            btm {:x (:x a) :y (+ (:y inter) (:h inter)) :w (:w a) :h (- (+ (:y a) (:h a)) (+ (:y inter) (:h inter)))}
            left  {:x (:x a) :y (:y inter) :w (- (:x inter) (:x a)) :h (:h inter)}
            right {:x (+ (:x inter) (:w inter)) :y (:y inter) :w (- (+ (:x a) (:w a)) (+ (:x inter) (:w inter))) :h (:h inter)}]
        (filter valid-rect? (list top left right btm))))))

(defn rects- [rects b]
  "Subtracts b from each rect of rects list. See rect-."
  (mapcat (fn [r] (rect- r b)) rects))

;
; Tests
;

(deftest line&-test
  (is (= [1 2] (line& 0 2 1 3)))
  (is (= [2 4] (line& 2 5 0 4)))
  (is (= [1 2] (line& 0 4 1 2)))
  (is (= [2 3] (line& 2 3 1 4)))
  (is (nil? (line& 0 1 2 3)))
  (is (nil? (line& 4 5 0 2))))

(deftest valid-rect?-test
  (is (true? (valid-rect? {:x 1 :y 2 :w 4 :h 5})))
  (is (false? (valid-rect? {:x 1 :y 2 :w 0 :h 5})))
  (is (false? (valid-rect? {:x 1 :y 2 :w -2 :h 5})))
  (is (false? (valid-rect? {:x 1 :y 2 :w 5 :h 0})))
  (is (false? (valid-rect? {:x 1 :y 2 :w 5 :h -3})))
  (is (false? (valid-rect? {:x 1 :y 2 :w 0 :h -4}))))

(deftest rect&-test
  (is (= {:x 2 :y 3 :w 3 :h 2} (rect&
                                 {:x 0 :y 0 :w 5 :h 5}
                                 {:x 2 :y 3 :w 5 :h 5})))
  (is (= {:x 2 :y 3 :w 3 :h 2} (rect&
                                 {:x 2 :y 3 :w 5 :h 5}
                                 {:x 0 :y 0 :w 5 :h 5})))
  (is (= {:x 3 :y 2 :w 2 :h 3} (rect&
                                 {:x 0 :y 2 :w 5 :h 5}
                                 {:x 3 :y 0 :w 5 :h 5})))
  (is (= {:x 3 :y 2 :w 2 :h 3} (rect&
                                 {:x 3 :y 0 :w 5 :h 5}
                                 {:x 0 :y 2 :w 5 :h 5})))
  (is (= {:x 2 :y 2 :w 2 :h 2} (rect&
                                 {:x 1 :y 0 :w 5 :h 7}
                                 {:x 2 :y 2 :w 2 :h 2})))
  (is (= {:x 2 :y 2 :w 2 :h 2} (rect&
                                 {:x 2 :y 2 :w 2 :h 2}
                                 {:x 0 :y 1 :w 8 :h 7})))
  (is (= {:x 2 :y 2 :w 3 :h 4} (rect&
                                 {:x 2 :y 2 :w 3 :h 4}
                                 {:x 2 :y 2 :w 3 :h 4})))
  (is (nil? (rect&
              {:x 2 :y 2 :w 3 :h 4}
              {:x 10 :y 2 :w 6 :h 4}))))

(deftest rect--test
  (is (= (list {:x 1 :y 2 :w 6 :h 1}
               {:x 1 :y 3 :w 3 :h 2}
               {:x 5 :y 3 :w 2 :h 2}
               {:x 1 :y 5 :w 6 :h 1})
        (rect-
          {:x 1 :y 2 :w 6 :h 4}
          {:x 4 :y 3 :w 1 :h 2})))
  (is (= (list {:x 0 :y 0 :w 5 :h 3}
               {:x 0 :y 3 :w 2 :h 2})
        (rect-
          {:x 0 :y 0 :w 5 :h 5}
          {:x 2 :y 3 :w 5 :h 5})))
  (is (= (list {:x 22 :y 2 :w 3 :h 4})
        (rect-
          {:x 22 :y 2 :w 3 :h 4}
          {:x 10 :y 2 :w 6 :h 4})))
  (is (empty?
        (rect-
          {:x 1 :y 2 :w 3 :h 4}
          {:x 1 :y 2 :w 3 :h 4})))
  (is (empty?
        (rect-
          {:x 10 :y 5 :w 1 :h 1}
          {:x  4 :y 3 :w 9 :h 3}))))

(deftest rects--test
  (is (= #{{:x 0  :y 0 :w 10 :h 3}
           {:x 0  :y 3 :w  2 :h 2}
           {:x 12 :y 7 :w  6 :h 2}
           {:x 8  :y 9 :w 10 :h 3}}
        (set (rects-
          (list {:x 0 :y 0 :w 10 :h 5} {:x 8 :y 7 :w 10 :h 5})
          {:x 2 :y 3 :w 10 :h 6}))))
  (is (= #{{:x 1, :y 1, :w 9, :h 2}
           {:x 1, :y 3, :w 3, :h 1}}
        (set (rects-
               (list {:x 4, :y 3, :w 9, :h 3} {:x 1, :y 1, :w 9, :h 2} {:x 1, :y 3, :w 3, :h 1})
               {:has-changes true, :prev-w 9, :prev-x 1, :y 3, :x 4, :prev-y 1, :h 3, :w 9, :prev-h 3, :id :c_1_1_1}))))
  )

