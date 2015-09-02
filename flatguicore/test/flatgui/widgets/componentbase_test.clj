; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.widgets.componentbase-test
  ;;; TODO get rid of :use
  (:use flatgui.dependency flatgui.base flatgui.ids flatgui.comlogic flatgui.access flatgui.util.matrix clojure.test clojure.stacktrace))


(defn- remove-service-info [component]
  (let [ children (into (array-map) (for [[k v] (:children component)] [k (remove-service-info v)]))]
    (assoc
      (dissoc component :evolvers :evolve-reason-provider :properties-to-evolve-provider)
      :children children)))

;;; FIXME
;(deftest defcomponent-test
;  (let [ widget {:a 1 :b 2}
;        properties {:u 1 :v 2 :b 3}
;        expected {:id :x :a 1 :u 1 :v 2 :b 3 :children (array-map :e {:id :e :a 3} :g {:id :g :a 4} :f {:id :f :a 5})}]
;    (is (= expected (defcomponent widget :x properties {:id :e :a 3} {:id :g :a 4} {:id :f :a 5})))))
;
;(deftest defcomponent-test2
;  (let [ widget {:a 1 :b 2}
;        properties {}
;        expected {:id :x :a 1 :b 2 :children {}}]
;    (is (= expected (defcomponent widget :x properties)))))

(deftest remove-service-info-test
  (let [ component {:a 1 :b 2 :evolvers {} :children {:c1 {:c 3 :evolvers {} :children {:c2 {:x 5 :evolvers {}}}}}}
        expected {:a 1 :b 2 :children {:c1 {:c 3 :children {:c2 {:x 5 :children {}}}}}}]
    (is (= expected (remove-service-info component)))))

;;; FIXME
;(deftest evolve-test
;  (let [ component (array-map
;                       :a 2
;                       :b 3
;                       :c 4
;                       :evolvers {:a (fn [c] (inc (:a c)))
;                                  :b (fn [c] (+ (:b c) (:a c)))
;                                  :c (fn [c] (+ (:a c) (:b c) (:c c)))}
;                       :children (array-map
;                                    :c1 {:x 1
;                                       :evolvers {:x (fn [c] (* 2 (:x c)))}}
;                                    :c2 (array-map
;                                            :a 5
;                                            :y 1
;                                            :evolvers {:a (fn [c] (+ (:y c) (* 2 (:a c))))
;                                                       :y (fn [c] (+ (:a c) (:y c)))}
;                                            :children {:c1 {:z 1
;                                                            :evolvers {:z (fn [c] (+ 2 (:z c)))}}
;                                                       }
;                                            )
;                                    :c3 {:w 5}
;                                    )
;                       )
;         expected  {:a 3
;                    :b 6
;                    :c 13
;                    :children {:c1 {:x 2 :children {}}
;                               :c2 {:a 11
;                                    :y 12
;                                    :children {:c1 {:z 3 :children {}}}}
;                               :c3 {:w 5 :children {}}
;                               }
;                    }]
;      (is (= expected (remove-service-info (evolve component '()))))))
;
;
;(deftest evolve-test-2
;  (let [ component {:id :win :children (array-map
;                                         :panel { :id :panel
;                                                      :children (array-map
;                                                                  :b1 {:a 1
;                                                                       :evolvers {:a (fn [c] (inc (:a c)))}}
;                                                                  :b2 {:x 2
;                                                                       :evolvers {:x (fn [c] (+ (:x c) (get-property c [:b1] :a)))}}
;                                                                  )
;                                                  }
;                                         :table { :id :table
;                                                  :y 10
;                                                  :evolvers {:y (fn [c] (+ (:y c) (get-property c [:panel :b2] :x)))}
;                                                  :children {:header {:w 5
;                                                                      :evolvers {:w (fn [c] (+ (:w c) (get-property c [:_ :panel :b2] :x)))}}}}
;                                         ) }
;         expected {:id :win :children {:panel {:id :panel
;                                               :children {:b1 {:a 2 :children {}}
;                                                          :b2 {:x 4 :children {}}}
;                                               }
;                                       :table {:id :table
;                                               :y 14
;                                               :children {:header {:w 9
;                                                                   :children {}}}}
;                                       }}]
;    (is (= expected (remove-service-info (evolve component '()))))))

