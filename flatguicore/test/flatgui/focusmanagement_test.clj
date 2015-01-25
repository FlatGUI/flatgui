; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.focusmanagement-test)

;
;(deftest is-focus-event-test
;  (let [ good-obj {:event-type :focus :somedata 123}
;         bad-obj-1 {:a :b}
;         bad-obj-2 "test"]
;    (is (true? (is-focus-event good-obj)))
;    (is (false? (is-focus-event bad-obj-1)))
;    (is (false? (is-focus-event bad-obj-2)))))
;
;(deftest issue-focus-event-if-needed-test
;  (let [ test-container {:x 0 :y 0 :w 10 :h 10 :id :1
;                         :children {:c1 {:x 2 :y 2 :w 5 :h 5 :id :c1
;                                         :children {:cc1 {:x 1 :y 0 :w 2 :h 2 :id :cc1 :has-focus true}}}
;                                    :c2 {:x 8 :y 2 :w 1 :h 5 :id :c2 :requests-focus :c2}
;                                    }
;                         }
;         expected-focus-event {:event-type :focus :from :cc1 :to :c2}
;         actual-focus-event (issue-focus-event-if-needed test-container)
;         ]
;    (is (= expected-focus-event actual-focus-event))))
;
;(deftest get-next-test1
;  (let [ test-container {:focus-cycle [:a :b :c :d]
;                         :closed-focus-root true
;                         }]
;    (is (= :b (get-next test-container :a)))
;    (is (= :c (get-next test-container :b)))
;    (is (= :d (get-next test-container :c)))
;    (is (= :a (get-next test-container :d)))))
;
;(deftest get-next-test2
;  (let [ test-container {:focus-cycle [:a :b :c :d]
;                         :closed-focus-root false
;                         }]
;    (is (= :b (get-next test-container :a)))
;    (is (= :c (get-next test-container :b)))
;    (is (= :d (get-next test-container :c)))
;    (is (= nil (get-next test-container :d)))))