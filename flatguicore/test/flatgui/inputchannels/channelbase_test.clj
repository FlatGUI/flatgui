; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.inputchannels.channelbase-test
  (:require
    [flatgui.inputchannels.channelbase :as ic]
    [clojure.test :as test]))

;;;TODO
;(test/deftest definputparser-test
;  (do
;    (ic/definputparser test-parser clojure.lang.IPersistentMap [(:a repaint-reason) (:b repaint-reason)])
;    (is (= [3 4] (test-parser {:evolve-reason-provider (fn [_] {:a 3 :b 4})})))))
;
;(test/deftest definputparser-testnil
;  (do
;    (ic/definputparser test-parser clojure.lang.IPersistentMap [(:a repaint-reason) (:b repaint-reason)])
;    (is (nil? (test-parser {:evolve-reason-provider (fn [_] nil)})))))