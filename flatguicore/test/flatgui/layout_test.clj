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
        expected-flags {:element [{:element :a, :flags "-|"}
                                  {:element :b, :flags "-|"}
                                  {:element :c, :flags "-|"}]
                        :flags "-|"}
        flags (layout/cfg->flags cfg)]
    (test/is (= expected-flags flags))))

(test/deftest cfg->flags-test2
  (let [cfg [[:a :-|] [:b :-|] [:c :-|]]
        expected-flags [{:element :a, :flags "-|"}
                        {:element :b, :flags "-|"}
                        {:element :c, :flags "-|"}]
        flags (layout/cfg->flags cfg)]
    (test/is (= expected-flags flags))))

(test/deftest cfg->flags-test3
  (let [cfg [[:a :-|] [:b :-|] [:c :-|] :-||]
        expected-flags {:element [{:element :a, :flags "-|"}
                                  {:element :b, :flags "-|"}
                                  {:element :c, :flags "-|"}]
                        :flags "-||"}
        flags (layout/cfg->flags cfg)]
    (test/is (= expected-flags flags))))

(test/deftest cfg->flags-test4
  (let [cfg [:w-label [:w :---] :h-label [:h :--]]
        expected-flags [{:element :w-label, :flags nil}
                        {:element :w, :flags "---"}
                        {:element :h-label, :flags nil}
                        {:element :h, :flags "--"}]
        flags (layout/cfg->flags cfg)]
    (test/is (= expected-flags flags))))


(test/deftest cfg->flags-test5
  (let [cfg [:dimensions-label [:w-label [:w :---] :h-label [:h :--]] :edit-dim-btn]
        expected-flags [{:element :dimensions-label, :flags nil}
                        {:element [{:element :w-label, :flags nil} {:element :w, :flags "---"} {:element :h-label, :flags nil} {:element :h, :flags "--"}] :flags nil}
                        {:element :edit-dim-btn, :flags nil}]
        flags (layout/cfg->flags cfg)]
    (test/is (= expected-flags flags))))

(test/deftest cfg->flags-test6
  (let [cfg [:dimensions-label [:w-label [:w :---] :h-label [:h :--] :-] :edit-dim-btn]
        expected-flags [{:element :dimensions-label, :flags nil}
                        {:element [{:element :w-label, :flags "-"} {:element :w, :flags "---"} {:element :h-label, :flags "-"} {:element :h, :flags "--"}] :flags "-"}
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
                         :d {:id :d :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}
                         :e {:id :e :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}
                         :f {:id :f :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}
                         :g {:id :g :path-to-target [:main] :preferred-size (m/defpoint 3 1) :minimum-size (m/defpoint 1 1)}
                         :h {:id :h :path-to-target [:main] :preferred-size (m/defpoint 2 1) :minimum-size (m/defpoint 1 1)}}}]
    (assoc main :root-container main)))

(defn- num->double [coll]
  (map (fn [e] (into {} (for [[k v] e] [k (cond
                                            (#{:min :pref} k) (m/defpoint (double (m/x v)) (double (m/y v)))
                                            (number? v) (double v)
                                            :else v)]))) coll))


;;; assoc-constraints

(test/deftest assoc-constraints-test1
  (let [cfg [[:a [:b :---] :c [:d :--]]]
        main test-component-1
        expected (list
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 0.0   :flags nil}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 3.0 :flags "---"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :stch-weight 0.0   :flags nil}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 2.0 :flags "--"}))
        actual (layout/assoc-constraints main cfg \- + max)]
    (test/is (= expected (map num->double actual)))))

(test/deftest assoc-constraints-test2
  (let [cfg [[[:a :-]   [:b :---]]
             [[:c :---] [:d :-]]]
        main test-component-1
        expected (list
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 1.0 :flags "-"}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 3.0 :flags "---"})
                   (list
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :stch-weight 3.0 :flags "---"}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 1.0 :flags "-"}))
        actual (layout/assoc-constraints main cfg \- + max)]
    (test/is (= expected (map num->double actual)))))

(test/deftest assoc-constraints-test3
  (let [cfg [[[:a :- :|]      [:b :---- :|]]
             [[:c :---- :|||] [:d :- :|||]]]
        main test-component-1
        expected (list
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 1.0 :flags "-|"}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 1.0 :flags "----|"})
                   (list
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :stch-weight 3.0 :flags "----|||"}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 3.0 :flags "-|||"}))
        actual (layout/assoc-constraints main cfg \| + max)]
    (test/is (= expected (map num->double actual)))))

(test/deftest assoc-constraints-test4
  (let [cfg [[[:a :-]   ]
             [[:b :c :-]]]
        main test-component-1
        expected (list
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 1.0 :flags "-"})
                   (list
                     {:element [{:element :b, :flags "-"} {:element :c, :flags "-"}] :min (m/defpoint 0.2 0.1) :pref (m/defpoint 0.5 0.1) :stch-weight 1.0 :flags "-"}))
        actual (layout/assoc-constraints main cfg \- + max)]
    (test/is (= expected (map num->double actual)))))

(test/deftest assoc-constraints-test5
  (let [cfg [[[:a :-]              ]
             [[[:b :-] [:c :--] :-]]]
        main test-component-1
        expected (list
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 1.0 :flags "-"})
                   (list
                     {:element [{:element :b, :flags "-"} {:element :c, :flags "--"}] :min (m/defpoint 0.2 0.1) :pref (m/defpoint 0.5 0.1) :stch-weight 1.0 :flags "-"}))
        actual (layout/assoc-constraints main cfg \- + max)]
    (test/is (= expected (map num->double actual)))))

(test/deftest assoc-constraints-test6
  (let [cfg [[[:a :---- :|]                       ]
             [[[:c :- :|||] [:d :---- :|||] :----]]]
        main test-component-1
        expected (list
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 4.0 :flags "----|"})
                   (list
                     {:element [{:element :c, :flags "-|||"} {:element :d, :flags "----|||"}] :min (m/defpoint 0.2 0.1) :pref (m/defpoint 0.5 0.1) :stch-weight 4.0 :flags "----"}))
        actual (layout/assoc-constraints main cfg \- + max)]
    (test/is (= expected (map num->double actual)))))

(test/deftest assoc-constraints-test7
  (let [cfg [[:a [:b :-]      ]
             [:c [:d :e :-] :f]]
        main test-component-1
        expected (list
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 0.0 :flags nil}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 1.0 :flags "-"})
                   (list
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :stch-weight 0.0 :flags nil}
                     {:element [{:element :d, :flags "-"} {:element :e, :flags "-"}] :min (m/defpoint 0.2 0.1) :pref (m/defpoint 0.4 0.1) :stch-weight 1.0 :flags "-"}
                     {:element :f :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :stch-weight 0.0 :flags nil}))
        actual (layout/assoc-constraints main cfg \- + max)]
    (test/is (= expected (map num->double actual)))))

;;; compute-x-dir

(test/deftest compute-x-dir-test1
  (let [cfg (list
              (list
                {:stch-weight 0.0}
                {:stch-weight 0.6}
                {:stch-weight 0.0}
                {:stch-weight 0.4}))
        expected [[{:stch-weight 0.0 :total-stch-weight 0.0}
                   {:stch-weight 0.6 :total-stch-weight 0.6}
                   {:stch-weight 0.0 :total-stch-weight 0.0}
                   {:stch-weight 0.4 :total-stch-weight 0.4}]]
        actual (mapv (fn [cfg-row] (mapv #(dissoc % :total-stable-pref) cfg-row)) (layout/compute-x-dir cfg))]
    (test/is (= expected actual))))

(test/deftest compute-x-dir-test2
  (let [cfg (list
              (list
                {:stch-weight 0.2}
                {:stch-weight 0.8})
              (list
                {:stch-weight 0.8}
                {:stch-weight 0.2}))
        expected [[{:stch-weight 0.125  :total-stch-weight 0.5}
                   {:stch-weight 0.5    :total-stch-weight 0.5}]
                  [{:stch-weight 0.5    :total-stch-weight 0.5}
                   {:stch-weight 0.125  :total-stch-weight 0.5}]]
        actual (mapv (fn [cfg-row] (mapv #(dissoc % :total-stable-pref) cfg-row)) (layout/compute-x-dir cfg))]
    (test/is (= expected actual))))

(test/deftest compute-x-dir-test3
  (let [cfg (list
              (list
                {:stch-weight 0 :pref (m/defpoint 0.2 0.1)} {:stch-weight 1})
              (list
                {:stch-weight 0 :pref (m/defpoint 0.3 0.1)} {:stch-weight 1} {:stch-weight 0 :pref (m/defpoint 0.2 0.1)}))
        expected [[{:stch-weight 0 :total-stch-weight 0 :total-stable-pref 0.3 :pref (m/defpoint 0.2 0.1)}
                   {:stch-weight 1 :total-stch-weight 1 :total-stable-pref 0}
                   {:stch-weight 0 :total-stch-weight 0 :total-stable-pref 0.2 :pref (m/defpoint 0 0) :min (m/defpoint 0 0)}]
                  [{:stch-weight 0 :total-stch-weight 0 :total-stable-pref 0.3 :pref (m/defpoint 0.3 0.1)}
                   {:stch-weight 1 :total-stch-weight 1 :total-stable-pref 0}
                   {:stch-weight 0 :total-stch-weight 0 :total-stable-pref 0.2 :pref (m/defpoint 0.2 0.1)}]]
        actual (layout/compute-x-dir cfg)]
    (test/is (= expected actual))))

(test/deftest compute-y-dir-test1
  (let [cfg (list
              (list {:stch-weight 0.0})
              (list {:stch-weight 0.6})
              (list {:stch-weight 0.0})
              (list {:stch-weight 0.4}))
        expected [[{:stch-weight 0.0 :total-stch-weight 0.0}]
                  [{:stch-weight 0.6 :total-stch-weight 0.6}]
                  [{:stch-weight 0.0 :total-stch-weight 0.0}]
                  [{:stch-weight 0.4 :total-stch-weight 0.4}]]
        actual (mapv (fn [cfg-row] (mapv #(dissoc % :total-stable-pref) cfg-row)) (layout/compute-y-dir cfg))]
    (test/is (= expected actual))))

(test/deftest compute-y-dir-test2
  (let [cfg (list
              (list
                {:stch-weight 0.2}
                {:stch-weight 0.8})
              (list
                {:stch-weight 0.8}
                {:stch-weight 0.2}))
        expected [[{:stch-weight 0.125  :total-stch-weight 0.5}
                   {:stch-weight 0.5    :total-stch-weight 0.5}]
                  [{:stch-weight 0.5    :total-stch-weight 0.5}
                   {:stch-weight 0.125  :total-stch-weight 0.5}]]
        actual (mapv (fn [cfg-row] (mapv #(dissoc % :total-stable-pref) cfg-row)) (layout/compute-y-dir cfg))]
    (test/is (= expected actual))))

(test/deftest compute-y-dir-test3
  (let [cfg (list
              (list
                {:stch-weight 0.25} {:stch-weight 0.25})
              (list
                {:stch-weight 0.75} {:stch-weight 0.75}))
        expected [[{:stch-weight 0.25 :total-stch-weight 0.25} {:stch-weight 0.25 :total-stch-weight 0.25}]
                  [{:stch-weight 0.75 :total-stch-weight 0.75} {:stch-weight 0.75 :total-stch-weight 0.75}]]
        actual (mapv (fn [cfg-row] (mapv #(dissoc % :total-stable-pref) cfg-row)) (layout/compute-y-dir cfg))]
    (test/is (= expected actual))))



;;; coord-map-evolver

(test/deftest coord-map-evolver-test1
  (let [cfg [[:a [:b :---] :c [:d :--]]]
        main (assoc test-component-1 :layout cfg)
        expected (layout/flagnestedvec->coordmap
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.0 :w 0.2 :y 0.0 :h 0.1 :flags nil}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.2 :w 0.3 :y 0.0 :h 0.1 :flags "---"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.5 :w 0.3 :y 0.0 :h 0.1 :flags nil}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.8 :w 0.2 :y 0.0 :h 0.1 :flags "--"}))
        actual (layout/coord-map-evolver main)]
    (test/is (= (m/defpoint 2 1) (fg/get-property-private main [:this :a] :preferred-size)))
    (test/is (= expected (into {} (for [[k v] actual] [k (first (num->double [v]))]))))))

(test/deftest coord-map-evolver-test2
  (let [cfg [[[:a :-]    [:b :----]]
             [[:c :----] [:d :-]]]
        main (assoc test-component-1 :layout cfg)
        expected (layout/flagnestedvec->coordmap
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.0 :w 0.125 :y 0.0   :h 0.1 :flags "-"}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.5 :w 0.5   :y 0.0   :h 0.1 :flags "----"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.0 :w 0.5   :y 0.1 :h 0.1 :flags "----"}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.5 :w 0.125 :y 0.1 :h 0.1 :flags "-"}))
        actual (layout/coord-map-evolver main)]
    (test/is (= (m/defpoint 2 1) (fg/get-property-private main [:this :a] :preferred-size)))
    (test/is (= expected (into {} (for [[k v] actual] [k (first (num->double [v]))]))))))

(test/deftest coord-map-evolver-test3
  (let [cfg [[[:a :- :|]      [:b :---- :|]]
             [[:c :---- :|||] [:d :- :|||]]]
        main (assoc test-component-1 :layout cfg)
        expected (layout/flagnestedvec->coordmap
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.0 :w 0.125 :y 0.0  :h 0.25 :flags "-|"}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.5 :w 0.5   :y 0.0  :h 0.25 :flags "----|"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.0 :w 0.5   :y 0.25 :h 0.75 :flags "----|||"}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.5 :w 0.125 :y 0.25 :h 0.75 :flags "-|||"}))
        actual (layout/coord-map-evolver main)]
    (test/is (= expected (into {} (for [[k v] actual] [k (first (num->double [v]))]))))))

(test/deftest coord-map-evolver-test4
  (let [cfg [[[:a :- :|                  ]]
             [[[:c :----] [:d :-] :- :|||]]]
        main (assoc test-component-1 :layout cfg)
        expected (layout/flagnestedvec->coordmap
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.0 :w 1.0 :y 0.0  :h 0.25 :flags "-|"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.0 :w 0.8 :y 0.25 :h 0.75 :flags "----"}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.8 :w 0.2 :y 0.25 :h 0.75 :flags "-"}))
        raw-result (layout/coord-map-evolver main)
        actual (dissoc raw-result nil)
        filtered (filter (fn [[k _]] (keyword? k)) actual)]
    (test/is (= expected (into {} (for [[k v] filtered] [k (first (num->double [v]))]))))))

(test/deftest coord-map-evolver-test5
  (let [cfg [[[ :a :---- :|                      ]]
             [[[:c :- :|||] [:d :---- :|||] :----]]]
        main (assoc test-component-1 :layout cfg)
        expected (layout/flagnestedvec->coordmap
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.0 :w 1.0 :y 0.0 :h 0.9 :flags "----|"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.0 :w 0.2 :y 0.9 :h 0.1 :flags "-|||"}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.2 :w 0.8 :y 0.9 :h 0.1 :flags "----|||"}))
        raw-result (layout/coord-map-evolver main)
        actual (dissoc raw-result nil)
        filtered (filter (fn [[k _]] (keyword? k)) actual)]
    (test/is (= expected (into {} (for [[k v] filtered] [k (first (num->double [v]))]))))))

(test/deftest coord-map-evolver-test6
  (let [cfg [[[ :a :---- :|                      ]]
             [[[:c :- :|||] [:d :---- :|||] :----]]
             [[[:e :-] [:f :-] [:g :--] :----    ]]]
        main (assoc test-component-1 :layout cfg)
        expected (layout/flagnestedvec->coordmap
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.0  :w 1.0  :y 0.0 :h 0.8 :flags "----|"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.0  :w 0.2  :y 0.8 :h 0.1 :flags "-|||"}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.2  :w 0.8  :y 0.8 :h 0.1 :flags "----|||"}
                     {:element :e :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.0  :w 0.25 :y 0.9 :h 0.1 :flags "-"}
                     {:element :f :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.25 :w 0.25 :y 0.9 :h 0.1 :flags "-"}
                     {:element :g :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.5  :w 0.5  :y 0.9 :h 0.1 :flags "--"}))
        raw-result (layout/coord-map-evolver main)
        actual (dissoc raw-result nil)
        filtered (filter (fn [[k _]] (keyword? k)) actual)]
    (test/is (= expected (into {} (for [[k v] filtered] [k (first (num->double [v]))]))))))

(test/deftest coord-map-evolver-test7
  (let [cfg [[:a [:b :-]      ]
             [:c [:d :e :-] :f]]
        main (assoc test-component-1 :layout cfg)
        expected (layout/flagnestedvec->coordmap
                   (list
                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.0  :w 0.2  :y 0.0 :h 0.1 :flags nil}
                     {:element :b :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.3  :w 0.5  :y 0.0 :h 0.1 :flags "-"}
                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.0  :w 0.3  :y 0.1 :h 0.1 :flags nil}
                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.3  :w 0.25 :y 0.1 :h 0.1 :flags "-"}
                     {:element :e :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.55 :w 0.25 :y 0.1 :h 0.1 :flags "-"}
                     {:element :f :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.8  :w 0.2  :y 0.1 :h 0.1 :flags nil}))
        raw-result (layout/coord-map-evolver main)
        actual (dissoc raw-result nil)
        filtered (filter (fn [[k _]] (keyword? k)) actual)]
    (test/is (= expected (into {} (for [[k v] filtered] [k (first (num->double [v]))]))))))


;(test/deftest coord-map-evolver-testN
;  (let [cfg [[[:a :---- :|] [:a :---- :|] ]                 ;TODO combine two :a with + by x and max by y
;             [[:c :- :|||] [:d :---- :|||]]]
;        main (assoc test-component-1 :layout cfg)
;        expected (layout/flagnestedvec->coordmap
;                   (list
;                     {:element :a :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.0 :w 0.8 :y 0.0  :h 0.25 :flags "-|"}
;                     {:element :c :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.3 0.1) :x 0.0 :w 0.2 :y 0.25 :h 0.75 :flags "----|||"}
;                     {:element :d :min (m/defpoint 0.1 0.1) :pref (m/defpoint 0.2 0.1) :x 0.2 :w 0.8 :y 0.25 :h 0.75 :flags "-|||"}))
;        raw-result (layout/coord-map-evolver main)
;        _ (println "Empty: " (get raw-result nil))
;        actual (dissoc raw-result nil)]
;    (test/is (= expected (into {} (for [[k v] actual] [k (first (num->double [v]))]))))))
;


