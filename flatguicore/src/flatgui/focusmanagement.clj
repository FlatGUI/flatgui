; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Namespace that contains functionality related to focus management"
      :author "Denys Lebediev"}
  flatgui.focusmanagement
  (:use flatgui.base
        flatgui.inputchannels.mouse
        flatgui.inputchannels.keyboard
        flatgui.comlogic
        clojure.test)
  (:require flatgui.access))

;(defn is-focus-event [e] (and (map? e) (= :focus (:event-type e))))
;
;(defn- create-focus-event [] (hash-map :event-type :focus))
;
;(defn- throw-if-multiple [result]
;  "Throws IllegalStateException if result collection
;   has more that one element"
;  (if (> (count result) 1)
;    (throw (IllegalStateException. (str "More than one component at once: "
;                                     (vec (for [c result] (:id c))))))
;    result))
;
;(defn- get-next [container component-id]
;  "Returns id of next component in focus cycle after component-id.
;   If component-id is the last one in cycle, return first one in
;   case :closed-focus-root is true and nil otherwise"
;  (let [ focus-cycle (:focus-cycle container)]
;    (loop [ i 0]
;      (if (and (< i (count focus-cycle)) (not (= component-id (nth focus-cycle i))))
;        (recur (inc i))
;        (if (< i (dec (count focus-cycle)))
;          (nth focus-cycle (inc i))
;          (if (:closed-focus-root container)
;            (first focus-cycle))
;          )))))
;
;(defn- get-and-throw-if-multiple [container property-predicates]
;  "Calls flatgui.access/get-components and returns its result,
;  but throws IllegalStateException if there are more than one
;  elements in result collection (see throw-if-multiple)"
;  (let [ result (flatgui.access/get-components container property-predicates)]
;    (throw-if-multiple result)))
;
;(defn get-focus-owner [container]
;  "Finds and returns from given container compomnent that owns focus,
;  or nil if there is no focus owner in this container. Returns container
;  if it is focus owner itself"
;  (first (get-and-throw-if-multiple container {:has-focus true?})))
;
;(defn get-focus-owner-id [container]
;  "Finds compomnent that owns focus in given container
;  and returns its id"
;  (:id (get-focus-owner container)))
;
;(def throws-pred (fn [[id c]] (not (nil? (:throws-focus c)))))
;
;(defn find-focusable-child [comp-property-map check-for-last-owner]
;  "Finds a child in given container that can be given focus to.
;   This method is used for containers which are naturally non-focusable,
;   but usually contain focusable children. First, it looks for id of last focus
;   owner (if check-for-last-owner is true). If there is no one or if component
;   with such id does not exist any more, it just takes first focusable child."
;  (let [ evolve-reason (:evolve-reason comp-property-map)
;         focuse-cycle (get-property-from-component comp-property-map :focus-cycle evolve-reason)
;         last-focus-owner-id (if check-for-last-owner (:last-focus-owner-id comp-property-map))
;         predicate-data nil
;         predicate-fn-provider (fn [predicate-data container]
;                                 {:focus-cycle (fn [focus-cycle] (some (fn [id] (= last-focus-owner-id id)) focus-cycle))})
;         predicate-data-modifier (fn [predicate-data container] nil)
;         still-has-last-owner-in-cycle (if (not (nil? last-focus-owner-id))
;                                         (throw-if-multiple
;                                           (flatgui.access/get-components
;                                             comp-property-map
;                                             predicate-data
;                                             predicate-fn-provider
;                                             predicate-data-modifier)))]
;      (if (and (not (nil? last-focus-owner-id)) (not (empty? still-has-last-owner-in-cycle)))
;        last-focus-owner-id
;        (let [ first-in-cycle (if (not (nil? focuse-cycle)) (first focuse-cycle))
;               first-in-cycle-component (if (not (nil? first-in-cycle)) (first-in-cycle (:children comp-property-map)))]
;          (if (not (nil? first-in-cycle))
;            (if (:focusable first-in-cycle-component)
;              first-in-cycle
;              (find-focusable-child first-in-cycle-component check-for-last-owner)))))))
;
;(defn- find-dest-component-and-create-event [container parent-of-throwing-focus]
;  "Finds a component to give focus to when some component inside parent-of-throwing-focus
;   wans to throw focus. Works this way:
;    - looks for the next component in cycle
;    - if there is next component, gives focus either to it (if it is focusable),
;      or finds apropriate focusable child in it
;    - if there is no next component, then goes one level up, and does the same
;      in the parent of parent-of-throwing-focus"
;  (let [ throwing-component-id (first (first (filter throws-pred (:children parent-of-throwing-focus))))
;         next-id (get-next parent-of-throwing-focus throwing-component-id)]
;    (if (nil? next-id)
;      (let [ parent-of-parent-id (flatgui.ids/get-parent-id (:id parent-of-throwing-focus))]
;        (if (not (nil? parent-of-parent-id))
;          (let [ parent-of-parent
;                      ;TODO get-component-by-id does not seem to work as expected
;                      ;(flatgui.access/get-component-by-id container parent-of-parent-id)
;                      (flatgui.access/get-component-by-path container (next (flatgui.ids/get-id-path parent-of-parent-id)))
;                 id-of-parent-of-throwing-focus (:id parent-of-throwing-focus)]
;            (find-dest-component-and-create-event
;              container
;              (assoc-in parent-of-parent [:children id-of-parent-of-throwing-focus :throws-focus] id-of-parent-of-throwing-focus)))))
;      (let [next-component (next-id (:children parent-of-throwing-focus))
;            destination-id (if (:focusable next-component)
;                                    (:id next-component)
;                                    (find-focusable-child next-component false))]
;            (assoc (create-focus-event) :from throwing-component-id :to destination-id)))))
;
;
;(defn issue-focus-event-if-needed [container] container
;  "Issues focus event in case any components have to throw/obtain focus.
;   First looks for a component that requests focus, and issues focus event
;   to transfer focus to it if found.
;   If there are no requesting components then looks for a components that
;   throws focus"
;  (let [ requesting-focus (get-and-throw-if-multiple container {:requests-focus (fn [v] (not (nil? v)))})
;         having-focus (get-and-throw-if-multiple container {:has-focus true?})]
;    (if (not (empty? requesting-focus))
;        (let [ from (if (empty? having-focus) nil (:id (first having-focus)))
;               to (:requests-focus (first requesting-focus))]
;            (assoc (create-focus-event) :from from :to to))
;      (let [ predicate-data nil
;             predicate-fn-provider (fn [predicate-data container]
;                                      {:children (fn [children] (some throws-pred children))})
;             predicate-data-modifier (fn [predicate-data container] nil)
;             parents-of-throwing-focus (throw-if-multiple (flatgui.access/get-components container predicate-data predicate-fn-provider predicate-data-modifier))]
;          (if (not (empty? parents-of-throwing-focus))
;            (let [ parent-of-throwing-focus (first parents-of-throwing-focus)]
;              (find-dest-component-and-create-event container parent-of-throwing-focus)))))))
;
;;
;; Component focus-related evolvers
;;
;
;(defevolverfn :requests-focus
;  (let [ evolve-reason (:evolve-reason comp-property-map)]
;    (if (is-focus-event evolve-reason)
;      nil
;      (if (and
;            (has-mouse? comp-property-map)
;            (mouse-pressed? comp-property-map)
;            (mouse-left? comp-property-map))
;        (if (:focusable comp-property-map)
;          (:id comp-property-map)
;          (find-focusable-child comp-property-map true))))))
;
;(defevolverfn :has-focus
;  (let [ evolve-reason (:evolve-reason comp-property-map)]
;    (if (is-focus-event evolve-reason)
;      (= (:id comp-property-map) (:to evolve-reason))
;      (if (nil? (:throws-focus comp-property-map)) old-has-focus false))))
;
;(defevolverfn :focus-owner-id
;  (let [ focus-owner (get-focus-owner comp-property-map)]
;    (if (not (nil? focus-owner)) (:id focus-owner))))
;
;(defevolverfn :children-z-order
;  (let [ evolve-reason (:evolve-reason comp-property-map)
;         all-ids (if (nil? old-children-z-order) (get-children-id-list comp-property-map) old-children-z-order)
;         focused-children (filter
;                            (fn [id] (not (nil? (get-property-from-component (id (:children comp-property-map)) :focus-owner-id evolve-reason))))
;                            all-ids)]
;      (if (empty? focused-children)
;        all-ids
;        (concat
;          (filter (fn [id] (not (= id (first focused-children)))) all-ids)
;          focused-children))))
;
;(defevolverfn :is-focus-cycle-root
;  (if (nil? old-is-focus-cycle-root)
;    (let [ all-children (get-children-list comp-property-map)]
;      (some (fn [c] (or (:focusable c) (is-focus-cycle-root-evolver c))) all-children))
;    old-is-focus-cycle-root))
;
;;
;;TODO left->right top->bottom order fn, unit test for it
;;
;(defevolverfn :focus-cycle
;  (if (nil? old-focus-cycle)
;    (let [ all-focusable-children (filter (fn [c] (or (:focusable c) (is-focus-cycle-root-evolver c))) (get-children-list comp-property-map))
;           all-focusable-children-ids (map (fn [c] (:id c)) all-focusable-children)]
;      all-focusable-children-ids)
;    old-focus-cycle))
;
;(defevolverfn :throws-focus
;  (let [ evolve-reason (:evolve-reason comp-property-map)]
;    (if (key-event? comp-property-map)
;      (if (= java.awt.event.KeyEvent/VK_TAB (get-key comp-property-map)) (:id comp-property-map))
;      nil)))
;
;(defevolverfn :last-focus-owner-id
;  (let [ focus-owners (flatgui.access/get-components comp-property-map {:has-focus true?})
;         focus-owner (first focus-owners)]
;    (if (not (nil? focus-owner)) (:id focus-owner) old-last-focus-owner-id)))
;