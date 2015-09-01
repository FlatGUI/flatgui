; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.layout-test
  (:require [clojure.test :as test]
            [flatgui.layout :as layout]))

(test/deftest cfg->flags-test1
  (let [cfg [:a :b :c :-|]
        expected-flags [{:element :a, :flags "-|"}
                        {:element :b, :flags "-|"}
                        {:element :c, :flags "-|"}]
        flags (layout/cfg->flags cfg)]
    (test/is (= expected-flags flags))))

(test/deftest cfg->flags-test2
  (let [cfg [:dimensions-label [:w-label [:w :---] :h-label [:h :--]] :edit-dim-btn]
        expected-flags [{:element :dimensions-label, :flags nil}
                        [{:element :w-label, :flags nil} {:element :w, :flags "---"} {:element :h-label, :flags nil} {:element :h, :flags "--"}]
                        {:element :edit-dim-btn, :flags nil}]
        flags (layout/cfg->flags cfg)]
    (test/is (= expected-flags flags))))

;(test/deftest cfg->flags-test3
;  (let [cfg [:dimensions-label [:w-label [[[:w-feet]
;                                           [:w-inch]] :---] :h-label [[[:h-feet]
;                                                                       [:h-inch]] :--]] :edit-dim-btn]
;
;        expected-flags [{:element :dimensions-label, :flags nil}
;                        [{:element :w-label, :flags nil}
;                         [{:element :w-feet, :flags nil}  ; No way to distinguish that :w-inch has to be UNDER :w-feet
;                          {:element :w-inch, :flags nil}] ; So looks like this is not the way to go
;                         {:element :h-label, :flags nil}
;                         [{:element :h-feet, :flags nil}
;                          {:element :h-inch, :flags nil}]]
;                        {:element :edit-dim-btn, :flags nil}]
;        flags (layout/cfg->flags cfg)]
;    (test/is (= expected-flags flags))))


