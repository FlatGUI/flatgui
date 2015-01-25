; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.comlogic-test
  (:require [clojure.test :as test]
            [flatgui.comlogic :as c]
            [flatgui.util.matrix :as m]))


(test/deftest defpoint-test
  (test/is (= (m/defmxcol 2 3 1 1) (c/defpoint 2 3 1))))

(test/deftest defpoint-test
  (test/is (= {:top-left (m/defmxcol 1 2 3 1) :bottom-right (m/defmxcol 4 5 6 1)} (c/defrect (c/defpoint 1 2 3) (c/defpoint 4 5 6))))
  (test/is (= {:top-left (m/defmxcol 1 2 3 1) :bottom-right (m/defmxcol 4 5 6 1)} (c/defrect 1 2 3 4 5 6))))




