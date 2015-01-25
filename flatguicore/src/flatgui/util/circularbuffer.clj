; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for operations on circular buffer"
      :author "Denys Lebediev"}
  flatgui.util.circularbuffer
  (:use clojure.test))

(defn cbuf
  ([tail-index head-index order]
    { :tail-index tail-index
      :head-index head-index
      :order order
      })
  ([tail-index head-index]
    (cbuf tail-index head-index (range 0 (inc (- head-index tail-index))))))

(defn get-element-position [cb e]
  "Returns position to which given element of buffer
   correspond. e is always in range [0;<buffer len>)."
  (+ (:tail-index cb) (.indexOf (:order cb) e)))

(defn- supply-tail [cb number-of-elements]
  "Generates and adds more elements to tail"
  (if (pos? number-of-elements)
    (let [ current-count (count (:order cb))
           new-tail-index (- (:tail-index cb) number-of-elements)
           new-order (concat (range current-count (+ current-count number-of-elements)) (:order cb))]
      (cbuf new-tail-index (:head-index cb) new-order))
    cb))

(defn- supply-head [cb number-of-elements]
  "Generates and adds more elements to head"
  (if (pos? number-of-elements)
    (let [ current-count (count (:order cb))
           new-head-index (+ (:head-index cb) number-of-elements)
           new-order (concat (:order cb) (range current-count (+ current-count number-of-elements)))]
      (cbuf (:tail-index cb) new-head-index new-order))
    cb))

(defn- crop-tail [cb number-of-elements]
  "Crops tail by number-of-elements"
  (if (pos? number-of-elements)
    (let [ current-count (count (:order cb))
           new-tail-index (+ (:tail-index cb) number-of-elements)
           new-order (take-last (- current-count number-of-elements) (:order cb))]
      (cbuf new-tail-index (:head-index cb) new-order))
    cb))

(defn- crop-head [cb number-of-elements]
  "Crops head by number-of-elements"
  (if (pos? number-of-elements)
    (let [ current-count (count (:order cb))
           new-head-index (- (:head-index cb) number-of-elements)
           new-order (take (- current-count number-of-elements) (:order cb))]
      (cbuf (:tail-index cb) new-head-index new-order))
    cb))

(defn- from-head-to-tail [cb number-of-elements]
  "Scrolls the sequence by number-of-elements in the
   direction of tail"
  (if (pos? number-of-elements)
    (cbuf
      (- (:tail-index cb) number-of-elements)
      (- (:head-index cb) number-of-elements)
      (take (count (:order cb)) (concat (take-last number-of-elements (:order cb))  (:order cb))))
    cb))

(defn- from-tail-to-head [cb number-of-elements]
  "Scrolls the sequence by number-of-elements in the
   direction of head"
  (if (pos? number-of-elements)
    (cbuf
      (+ (:tail-index cb) number-of-elements)
      (+ (:head-index cb) number-of-elements)
      (take-last (count (:order cb)) (concat (:order cb) (take number-of-elements (:order cb)))))
    cb))

(defn move-cbuf [cb new-tail-index new-head-index]
  "Returns circular buffer created from cb and moved
   and/or resized according to new tail and head indices.
   new-tail-index must be greater or equal to zero.
   new-head-index must be greater of equal to new-tail-index"
  (let [ tail-growth (- (:tail-index cb) new-tail-index)
         head-growth (- new-head-index (:head-index cb))
         tail-to-head (max 0 (min head-growth (- tail-growth)))
         head-to-tail (max 0 (min (- head-growth) tail-growth))
         tail-gen (- (max 0 tail-growth) head-to-tail)
         head-gen (- (max 0 head-growth) tail-to-head)
         tail-crop (max 0 (- (- tail-growth) tail-to-head))
         head-crop (max 0 (- (- head-growth) head-to-tail))]
    (do

;      (println
;        "tail-growth=" tail-growth
;        "head-growth=" head-growth
;        "tail-to-head=" tail-to-head
;        "head-to-tail=" head-to-tail
;        "tail-gen=" tail-gen
;        "head-gen=" head-gen
;        "tail-crop=" tail-crop
;        "head-crop=" head-crop)
      (->
        (from-head-to-tail cb head-to-tail)
        (from-tail-to-head tail-to-head)
        ;(crop-tail tail-crop)
        (crop-head head-crop)
        (supply-tail tail-gen)
        (supply-head head-gen)
        )
      )))

(defn cbuf-size [cb] (count (:order cb)))

;
; Tests
;

(deftest move-cbuf-test1
  (is (= '(2 3 4 5 0 1) (:order (move-cbuf (cbuf 0 5) 2 7))))
  (is (= '(0 1 2 3 4 5) (:order (move-cbuf (cbuf 2 7 '(2 3 4 5 0 1)) 0 5))))
  (is (= '(0 1 2 3 4 5 6 7 8) (:order (move-cbuf (cbuf 0 2) 0 8))))
  (is (= '(6 7 8 0 1 2 3 4 5) (:order (move-cbuf (cbuf 3 8) 0 8))))
  (is (= '(2 3 4 5) (:order (move-cbuf (cbuf 0 5) 2 5))))
  (is (= '(0 1 2 3 4) (:order (move-cbuf (cbuf 0 5) 0 4))))
  (is (= '(2 3 4) (:order (move-cbuf (cbuf 0 5) 2 4))))
  (is (= '(2 3 4) (:order (move-cbuf (cbuf 0 5) 2 4))))
  (is (= '(5 0 1 2 3 4 6 7) (:order (move-cbuf (cbuf 1 5) 0 7))))
  (is (= '(1 2 3 4 5 0 6 7) (:order (move-cbuf (cbuf 0 5) 1 8))))
  (is (= '(5 3 4 0 1 2) (:order (move-cbuf (cbuf 3 7) 0 5)))))

(deftest move-cbuf-test2
  (let [ cb (cbuf 0 7)
         ->2 (move-cbuf cb 2 9)
         ->1 (move-cbuf ->2 3 10)
         <-3 (move-cbuf ->1 0 7)]
    (is (= cb <-3))))

(deftest get-element-position-test
  (let [ cb (cbuf 0 5)
         ->2 (move-cbuf cb 2 7)
         ->1 (move-cbuf ->2 3 8)
         <-2 (move-cbuf ->1 1 6)]
    (is (= 3 (get-element-position cb 3)))
    (is (= 3 (get-element-position ->2 3)))
    (is (= 0 (get-element-position cb 0)))
    (is (= 1 (get-element-position cb 1)))
    (is (= 6 (get-element-position ->2 0)))
    (is (= 7 (get-element-position ->2 1)))
    (is (= 6 (get-element-position ->1 0)))
    (is (= 7 (get-element-position ->1 1)))
    (is (= 3 (get-element-position ->1 3)))
    (is (= 8 (get-element-position ->1 2)))
    (is (= 1 (get-element-position <-2 1)))
    (is (= 2 (get-element-position <-2 2)))
    (is (= 6 (get-element-position <-2 0)))
    ))