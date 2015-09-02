; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.layout-test
  (:require [clojure.test :as test]
            [flatgui.base :as fg]
            [flatgui.layout :as layout]
            [flatgui.util.matrix :as m]))


;;; cfg->flags


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


;;; map-direction

(defn- suppress-ratios [coll]
  (map (fn [e] (into {} (for [[k v] e] [k (cond
                                            (#{:min :pref} k) (m/defpoint (double (m/x v)) (double (m/y v)))
                                            (ratio? v) (double v)
                                            :else v)]))) coll))

(test/deftest map-direction-test1
  (let [cfg [:a [:b :---] :c [:d :--]]
        main {:id :main
              :path-to-target []
              :clip-size (m/defpoint 10 1)
              :children {:a {:id :a :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}
                         :b {:id :b :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}
                         :c {:id :c :path-to-target [:main] :preferred-size (m/defpoint 3 1) :minimum-size (m/defpoint 1 1)}
                         :d {:id :d :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}}}
        main (assoc main :root-container main)
        expected (list
                   {:element :a :min (m/defpoint 0.1 1.0) :pref (m/defpoint 0.2 1.0) :stch-weight 0   :x 0   :w 0.2 :y 0.5 :h 0.5 :flags nil}
                   {:element :b :min (m/defpoint 0.1 1.0) :pref (m/defpoint 0.2 1.0) :stch-weight 0.6 :x 0.2 :w 0.3 :y 0.5 :h 0.5 :flags "---"}
                   {:element :c :min (m/defpoint 0.1 1.0) :pref (m/defpoint 0.3 1.0) :stch-weight 0   :x 0.5 :w 0.3 :y 0.5 :h 0.5 :flags nil}
                   {:element :d :min (m/defpoint 0.1 1.0) :pref (m/defpoint 0.2 1.0) :stch-weight 0.4 :x 0.8 :w 0.2 :y 0.5 :h 0.5 :flags "--"})
        actual (layout/map-direction main cfg \- \< m/x)]
    (test/is (= (m/defpoint 2 1) (fg/get-property-private main [:this :a] :preferred-size)))
    (test/is (= expected (suppress-ratios actual)))
    ))


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


