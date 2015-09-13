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


;;; Test components ans utils

(def test-component-1
  (let [main {:id :main
              :path-to-target []
              :clip-size (m/defpoint 10 10)
              :children {:a {:id :a :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}
                         :b {:id :b :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}
                         :c {:id :c :path-to-target [:main] :preferred-size (m/defpoint 3 1) :minimum-size (m/defpoint 1 1)}
                         :d {:id :d :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}}}]
    (assoc main :root-container main)))

(defn- suppress-ratios [coll]
  (map (fn [e] (into {} (for [[k v] e] [k (cond
                                            (#{:min :pref} k) (m/defpoint (double (m/x v)) (double (m/y v)))
                                            (ratio? v) (double v)
                                            :else v)]))) coll))


;;; assoc-constraints

(test/deftest assoc-constraints-test1
  (let [cfg [[:a [:b :---] :c [:d :--]]]
        main test-component-1
        expected (list
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 0   :flags nil}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 0.6 :flags "---"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :stch-weight 0   :flags nil}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 0.4 :flags "--"}))
        actual (layout/assoc-constraints main cfg \-)]
    (test/is (= expected (map suppress-ratios actual)))))

(test/deftest assoc-constraints-test2
  (let [cfg [[[:a :-]   [:b :---]]
             [[:c :---] [:d :-]]]
        main test-component-1
        expected (list
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 0.25 :flags "-"}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 0.75 :flags "---"})
                   (list
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :stch-weight 0.75 :flags "---"}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 0.25 :flags "-"}))
        actual (layout/assoc-constraints main cfg \-)]
    (test/is (= expected (map suppress-ratios actual)))))

;;; compute-x-dir


;;; coord-map-evolver

(test/deftest coord-map-evolver-test1
  (let [cfg [[:a [:b :---] :c [:d :--]]]
        main (assoc test-component-1 :layout cfg)
        expected (layout/flagnestedvec->coordmap
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0   :w 0.2 :y 0 :h 0.1 :flags nil}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.2 :w 0.3 :y 0 :h 0.1 :flags "---"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.5 :w 0.3 :y 0 :h 0.1 :flags nil}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.8 :w 0.2 :y 0 :h 0.1 :flags "--"}))
        actual (layout/coord-map-evolver main)]
    (test/is (= (m/defpoint 2 1) (fg/get-property-private main [:this :a] :preferred-size)))
    (test/is (= expected (into {} (for [[k v] actual] [k (first (suppress-ratios [v]))]))))
    ))


;(test/deftest map-direction-test1
;  (let [cfg [:a [:b :---] :c [:d :--]]
;        main test-component-1
;        expected (list
;                   {:element :a :min (m/defpoint 0.1 1.0) :pref (m/defpoint 0.2 1.0) :stch-weight 0   :x 0   :w 0.2 :y 0.5 :h 0.5 :flags nil}
;                   {:element :b :min (m/defpoint 0.1 1.0) :pref (m/defpoint 0.2 1.0) :stch-weight 0.6 :x 0.2 :w 0.3 :y 0.5 :h 0.5 :flags "---"}
;                   {:element :c :min (m/defpoint 0.1 1.0) :pref (m/defpoint 0.3 1.0) :stch-weight 0   :x 0.5 :w 0.3 :y 0.5 :h 0.5 :flags nil}
;                   {:element :d :min (m/defpoint 0.1 1.0) :pref (m/defpoint 0.2 1.0) :stch-weight 0.4 :x 0.8 :w 0.2 :y 0.5 :h 0.5 :flags "--"})
;        actual (layout/map-direction main cfg \- \< m/x)]
;    (test/is (= (m/defpoint 2 1) (fg/get-property-private main [:this :a] :preferred-size)))
;    (test/is (= expected (suppress-ratios actual)))
;    ))


