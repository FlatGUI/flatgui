; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.util.circularbuffer-test
  (:require
    [clojure.test :as test]
    [flatgui.util.circularbuffer :as cb]))


(test/deftest move-cbuf-test1
  (test/is (= '(2 3 4 5 0 1) (:order (cb/move-cbuf (cb/cbuf 0 5) 2 7))))
  (test/is (= '(0 1 2 3 4 5) (:order (cb/move-cbuf (cb/cbuf 2 7 '(2 3 4 5 0 1)) 0 5))))
  (test/is (= '(0 1 2 3 4 5 6 7 8) (:order (cb/move-cbuf (cb/cbuf 0 2) 0 8))))
  (test/is (= '(6 7 8 0 1 2 3 4 5) (:order (cb/move-cbuf (cb/cbuf 3 8) 0 8))))
  ;FIXME (test/is (= '(2 3 4 5) (:order (cb/move-cbuf (cb/cbuf 0 5) 2 5))))
  (test/is (= '(0 1 2 3 4) (:order (cb/move-cbuf (cb/cbuf 0 5) 0 4))))
  ;FIXME (test/is (= '(2 3 4) (:order (cb/move-cbuf (cb/cbuf 0 5) 2 4))))
  ;FIXME (test/is (= '(2 3 4) (:order (cb/move-cbuf (cb/cbuf 0 5) 2 4))))
  (test/is (= '(5 0 1 2 3 4 6 7) (:order (cb/move-cbuf (cb/cbuf 1 5) 0 7))))
  (test/is (= '(1 2 3 4 5 0 6 7) (:order (cb/move-cbuf (cb/cbuf 0 5) 1 8))))
  (test/is (= '(5 3 4 0 1 2) (:order (cb/move-cbuf (cb/cbuf 3 7) 0 5)))))

(test/deftest move-cbuf-test2
  (let [cb (cb/cbuf 0 7)
        ->2 (cb/move-cbuf cb 2 9)
        ->1 (cb/move-cbuf ->2 3 10)
        <-3 (cb/move-cbuf ->1 0 7)]
    (test/is (= cb <-3))))

(test/deftest get-element-position-test
  (let [cb (cb/cbuf 0 5)
        ->2 (cb/move-cbuf cb 2 7)
        ->1 (cb/move-cbuf ->2 3 8)
        <-2 (cb/move-cbuf ->1 1 6)]
    (test/is (= 3 (cb/get-element-position cb 3)))
    (test/is (= 3 (cb/get-element-position ->2 3)))
    (test/is (= 0 (cb/get-element-position cb 0)))
    (test/is (= 1 (cb/get-element-position cb 1)))
    (test/is (= 6 (cb/get-element-position ->2 0)))
    (test/is (= 7 (cb/get-element-position ->2 1)))
    (test/is (= 6 (cb/get-element-position ->1 0)))
    (test/is (= 7 (cb/get-element-position ->1 1)))
    (test/is (= 3 (cb/get-element-position ->1 3)))
    (test/is (= 8 (cb/get-element-position ->1 2)))
    (test/is (= 1 (cb/get-element-position <-2 1)))
    (test/is (= 2 (cb/get-element-position <-2 2)))
    (test/is (= 6 (cb/get-element-position <-2 0)))))