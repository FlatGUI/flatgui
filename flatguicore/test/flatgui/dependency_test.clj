; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.dependency-test
  ;;; TODO Get rid of :use
  (:use flatgui.dependency
        flatgui.comlogic
        clojure.test))



(defmacro compute-relative-dependencies [form]
  (let [ dependencies (conj (flatgui.dependency/get-all-dependencies form) 'list)]
    `(assoc ~form :relative-dependencies ~dependencies)))

;;; FIXME
;(deftest get-all-dependencies-test
;  (let [ form '(defcomponent a {:b c
;                                :evolvers {:x (fn [c] (let [ d (get-property c [:u] :c)] (+ d 3)))
;                                           :y (fn [c] (+ 1 (get-property c [:a :b] :c)))}})
;        expected #{[:u] [:a :b]}]
;    (is (= expected (set (get-all-dependencies form))))))

;;; FIXME
;(deftest compute-all-dependencies-test
;  (let [ container {:id :win :children (array-map
;                                         :panel { :id :panel
;                                                 :children (array-map
;                                                             :b1 {:a 1 :id :b1
;                                                                  :evolvers {:a (fn [c] (inc (:a c)))}}
;                                                             :b2 (compute-relative-dependencies
;                                                                   {:x 2 :id :b2
;                                                                    :evolvers {:x (fn [c] (+ (:x c) (let [ get-property (fn [c f p])]
;                                                                                                      (get-property c [:b1] :a))))}})
;                                                             )
;                                                 }
;                                         :table { :id :table
;                                                 :y 10
;                                                 :evolvers {:y (fn [c] (+ (:y c) (let [ get-property (fn [c f p])]
;                                                                                   (get-property c [:panel :b2] :x))))}
;                                                 :children { :pane {:id :pane :a 4 :children {:cell (compute-relative-dependencies
;                                                                                                      {:w 5 :id :cell
;                                                                                                       :evolvers {:w (fn [c] (let [ get-property (fn [c f p])]
;                                                                                                                               (+
;                                                                                                                                 (:w c)
;                                                                                                                                 (get-property c [:_ :_ :panel :b2] :x)
;                                                                                                                                 (get-property c [:_ :_ :panel :b1] :x))))}})
;                                                                                              }}
;                                                            :header (compute-relative-dependencies
;                                                                      {:w 5 :id :header
;                                                                       :evolvers {:w (fn [c] (let [ get-property (fn [c f p])]
;                                                                                               (+
;                                                                                                 (:w c)
;                                                                                                 (get-property c [:_ :panel :b2] :x))))}})
;                                                            }}
;                                         ) }
;        expected-header '((:win :panel :b2))
;        expected-cell '((:win :panel :b2) (:win :panel :b1))
;        expected-b2 '((:win :panel :b1))
;        resolved (compute-dependencies container)
;        ]
;
;    (is (= expected-header (get-in resolved [:children :table :children :header :abs-dependencies ])))
;    (is (= expected-cell (get-in resolved [:children :table :children :pane :children :cell :abs-dependencies ])))
;    (is (= expected-b2 (get-in resolved [:children :panel :children :b2 :abs-dependencies ])))))