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

(defn fully-contains? [a b]
  (if (and
        (< (:x a) (:x b)) (> (:w a) (:w b))
        (< (:y a) (:y b)) (> (:h a) (:h b)))
    a))

(defn rect+ [a b]
  (cond
    (fully-contains? a b)
    a
    (fully-contains? b a)
    b
    :else
    (let [h-inter (rectmath/line& (:x a) (+ (:x a) (:w a)) (:x b) (+ (:x b) (:w b)))
          v-inter (rectmath/line& (:y a) (+ (:y a) (:h a)) (:y b) (+ (:y b) (:h b)))
          v-edge (and (nil? v-inter) (or
                                       (= (:y b) (+ (:y a) (:h a)))
                                       (= (:y a) (+ (:y b) (:h b)))))]
      (if (and h-inter (or v-inter v-edge))
        {:x (first h-inter)
         :y (min (:y a) (:y b))
         :w (- (nth h-inter 1) (nth h-inter 0))
         :h (if v-edge
              (+ (:h a) (:h b))
              (- (+ (:h a) (:h b)) (- (nth v-inter 1) (nth v-inter 0))))}
        (let [h-edge (and (nil? h-inter) (or
                                           (= (:x b) (+ (:x a) (:w a)))
                                           (= (:x a) (+ (:x b) (:w b)))))]
          (if (and v-inter (or h-inter h-edge))
            {:x (min (:x a) (:x b))
             :y (nth v-inter 0)
             :w (if h-edge
                  (+ (:w a) (:w b))
                  (- (+ (:w a) (:w b)) (- (nth h-inter 1) (nth h-inter 0))))
             :h (- (nth v-inter 1) (nth v-inter 0))}))))))

(defn square [a]
  (if a
    (* (:w a) (:h a))
    0))

(defn get-largest-adjacent [rects]
  (let [c (count rects)]
    (case c
      0 nil
      1 (first rects)
      2 (let [adj (rect+ (nth rects 0) (nth rects 1))]
          (if adj
            adj
            (max-key square (nth rects 0) (nth rects 1))))
      (loop [i 0
             fr nil]
        (if (< i c)
          (recur
            (inc i)
            (max-key square
                     fr
                     (loop [j 0
                            r nil]
                       (if (< j i)
                         (recur
                           (inc j)
                           (let [adj (rect+ (nth rects i) (nth rects j))]
                             (if adj
                               (let [with-indexes (map #(vector %1 %2) rects (range))
                                     filtered (filter #(not (#{i j} (second %))) with-indexes)
                                     other-rects (map first filtered)]
                                 (max-key square r (get-largest-adjacent (conj other-rects adj))))
                               r)))
                         r))))
          fr)))))