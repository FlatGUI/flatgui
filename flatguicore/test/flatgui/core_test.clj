; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.core-test
  (:require [clojure.test :as test]
            [flatgui.core :as core]
            [flatgui.dependency]
            [flatgui.paint :as fgp]
            [flatgui.awt :as awt])
  (:import (flatgui.core.engine IResultCollector Container ClojureContainerParser)
           (flatgui.core.engine.ui FGAppContainer)
           (flatgui.core.awt FGAWTInteropUtil)))

(test/deftest build-abs-path-test
  (test/is (= [:a :b :c] (core/build-abs-path [:a :b :c] [:this])))
  (test/is (= [:a :b :c :d] (core/build-abs-path [:a :b :c] [:this :d])))
  (test/is (= [:a :b :c :d :e] (core/build-abs-path [:a :b :c] [:this :d :e])))
  (test/is (= [:a :b] (core/build-abs-path [:a :b :c] [])))
  (test/is (= [:a :u :v] (core/build-abs-path [:a :b :c] [:_ :u :v])))
  (test/is (= [:a] (core/build-abs-path [:a :b :c] [:_])))
  (test/is (= [:c] (core/build-abs-path [] [:c]))))

(test/deftest get-property-call?-test
  (test/is (true? (core/get-property-call? (list 'get-property [:a :b] :c))))
  (test/is (true? (core/get-property-call? (list 'get-property 'component [:a :b] :c))))
  (test/is (false? (core/get-property-call? (list 'component [:a :b] :c)))))

(test/deftest replace-rel-path-test
  (test/is (=
             (core/replace-rel-path (list 'get-property [:this] :c) [:a :b])
             (list 'get-property [:a :b] :c))))

(test/deftest replace-rel-path-test2
  (test/is (=
             (core/replace-rel-path (list 'get-property 'component [:this] :c) [:a :b])
             (list 'get-property [:a :b] :c))))

(test/deftest replace-all-rel-paths-test
  (test/is (=
             (core/replace-all-rel-paths
               (list 'println 1 2 (list 'get-property [:this :d] :c))
               [:a :b])
             (list 'println 1 2 (list 'get-property [:a :b :d] :c)))))

(test/deftest replace-all-rel-paths-test2
  (test/is (=
             (core/replace-all-rel-paths
               (list 'let ['a (list 'get-property 'component [:this] :c)] (list '+ 'a 1))
               [:a :b])
             (list 'let ['a (list 'get-property [:a :b] :c)] (list '+ 'a 1)))))

(test/deftest replace-all-rel-paths-test3
  (test/is (=
             (core/replace-all-rel-paths
               (list 'let ['a {:x (list 'get-property 'component [:this] :c)}] (list '+ {:x 'a} 1))
               [:a :b])
             (list 'let ['a {:x (list 'get-property [:a :b] :c)}] (list '+ {:x 'a} 1)))))

(test/deftest defroot-test
  (let [_ (core/defevolverfn e1 :a (+ 1 (get-property [:this] :a)))
        _ (core/defevolverfn e2 :b (- 2 (get-property [:this :c1] :a)))
        _ (core/defevolverfn e11 :b (+ 2 (get-property [] :x)))
        _ (core/defevolverfn e21 :d (+ 3 (get-property [:c1] :b)))
        container {:id :main
                   :a 4
                   :b 5
                   :evolvers {:a e1
                              :b e2}
                   :children {:c1 {:id :c1
                                   :b 5
                                   :evolvers {:b e11}}
                              :c2 {:id :c2
                                   :d 6
                                   :evolvers {:d e21}}}}]
    (test/is
      (=
        (core/defroot container)
        {:id :main
         :a 4
         :b 5
         :evolvers {:a '(+ 1 (get-property [:main] :a))
                    :b '(- 2 (get-property [:main :c1] :a))}
         :children {:c1 {:id :c1
                         :b 5
                         :evolvers {:b '(+ 2 (get-property [:main] :x))}}
                    :c2 {:id :c2
                         :d 6
                         :evolvers {:d '(+ 3 (get-property [:main :c1] :b))}}}}))))

(test/deftest collect-evolver-dependencies-test
  (let [_ (core/defevolverfn :x (if (get-property [:main :x :y] :z)
                                  (get-property [:main :a :b] :c)
                                  (do
                                    (println "Hello")
                                    (get-property [:main] :v))))]
    (test/is
      (=
        #{[:main :x :y :z] [:main :a :b :c] [:main :v]}
        (set (core/collect-evolver-dependencies x-evolver))))))

(test/deftest init-&-evolve-test
  (let [_ (core/defevolverfn evolver-c1-a :a (inc (get-property [] :src)))
        _ (core/defevolverfn evolver-c2-b :b (+
                                            (:d component)
                                            (get-property [:this] :c)
                                            (get-property [] :src)))
        _ (core/defevolverfn evolver-res :res (*
                                           (get-property [:this :c1] :a)
                                           (get-property [:this :c2] :b)))
        _ (core/defevolverfn evolver-c2-d :d (if (not (nil? (get-reason)))
                                            (+ (:d component) (:x (get-reason)))
                                            (:d component)))
        container (core/defroot
                    {:id :main
                     :src 1
                     :res nil
                     :evolvers {:res evolver-res} ; (* 2 8)
                     :children {:c1 {:id :c1
                                     :a 0
                                     :evolvers {:a evolver-c1-a}} ;2
                                :c2 {:id :c2
                                     :b 0
                                     :c 2
                                     :d 5
                                     :evolvers {:b evolver-c2-b ; (+ 5 2 1)
                                                :d evolver-c2-d}}}})
        results (atom {})
        result-collector (proxy [IResultCollector] []
                           (appendResult [path, _componentUid, property, newValue]
                             (swap! results (fn [r]
                                              (if (not (or (= :children property) (= :evolvers property)))
                                                (assoc r [path property] newValue)
                                                r)))
                             )
                           (postProcessAfterEvolveCycle [_a _m]))
        container-engine (Container.
                           (ClojureContainerParser.)
                           result-collector
                           container)
        init-res (get @results [[:main] :res])
        init-a (get @results [[:main :c1] :a])
        init-b (get @results [[:main :c2] :b])
        _ (.evolve container-engine [:main :c2] {:x 10})]
    (test/is (= 16 init-res))
    (test/is (= 2 init-a))
    (test/is (= 8 init-b))
    (test/is (= 36 (get @results [[:main] :res])))
    (test/is (= 2 (get @results [[:main :c1] :a])))
    (test/is (= 18 (get @results [[:main :c2] :b])))
    (test/is (= 15 (get @results [[:main :c2] :d])))))

(test/deftest init-&-evolve-test2
  (let [_ (core/defevolverfn :z-position
                             (let [pz {:a (get-property component [] :z-position)}]
                               (+ (:a pz) 1)))
        container (core/defroot
                    {:id :main
                     :z-position 1
                     :children {:c1 {:id :c1
                                     :popup false
                                     :z-position nil
                                     :evolvers {:z-position z-position-evolver}}}})
        results (atom {})
        result-collector (proxy [IResultCollector] []
                           (appendResult [path, _componentUid, property, newValue]
                             (swap! results (fn [r]
                                              (if (not (or (= :children property) (= :evolvers property)))
                                                (assoc r [path property] newValue)
                                                r)))
                             )
                           (postProcessAfterEvolveCycle [_a _m]))
        _container-engine (Container.
                            (ClojureContainerParser.)
                            result-collector
                            container)
        z-res (get @results [[:main :c1] :z-position])]
    (test/is (= 2 z-res))))

(test/deftest reuild-look-test
  (let [_ (core/defevolverfn :a (* (get-property [:this] :b) 2))
        _ (fgp/deflookfn test-look (:a) (awt/fillRect 0 0 a a))
        container (core/defroot
                    {:id :main
                     :a nil
                     :b 2
                     :look test-look
                     :look-vec []
                     :position-matrix nil
                     :clip-size nil
                     :evolvers {:a a-evolver}})
        ui-app (FGAppContainer. container (FGAWTInteropUtil. 64))
        _ (.initialize ui-app)
        container-accessor (.getContainerAccessor ui-app)]
    (test/is (= [["fillRect" 0 0 4 4]] (.get (.getComponent container-accessor 0) :look-vec)))))