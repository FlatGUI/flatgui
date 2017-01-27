; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.testsuite
  (:require [flatgui.widgets._old_componentbase]
            [flatgui.comlogic :as fgc]
            [flatgui.util.matrix :as m]
            [flatgui.awt :as awt])
  (:import (java.awt.event MouseEvent KeyEvent)
           (java.awt Container)
           (flatgui.core FGEvolveInputData)))


(def dummy-source (Container.))

(defn resolve-feed-fn [container]
  (if-let [fg-contanier (:_fg-container container)]
    (fn [_container target reason]
      (do
        (.feedTargetedEvent fg-contanier target (FGEvolveInputData. reason false))
        container))
    flatgui.widgets._old_componentbase/evolve-container))

(defn access-container [fg-container-accessor] {:_fg-container fg-container-accessor})

(defn get-container [fg-container-accessor] (.getContainer (.getFGModule fg-container-accessor)))

;;;
;;; Helpers for hi-level simulators
;;;

(defn create-mouse-event [container id modifiers click-count button target]
  (let [abs-pos-mx (get-in container (conj (fgc/get-access-key target) :abs-position-matrix))
        x (* (+ (m/mx-x abs-pos-mx) (awt/px)) (awt/unitsizepx))
        y (* (+ (m/mx-y abs-pos-mx) (awt/px)) (awt/unitsizepx))]
    (MouseEvent. dummy-source id 0 modifiers x y x y click-count false button)))

(defn move-mouse-to [container target]
  ((resolve-feed-fn container)
    container
    target
    (create-mouse-event container MouseEvent/MOUSE_MOVED 0 0 0 target)))

(defn simulate-mouse-left [container id target]
  ((resolve-feed-fn container)
    container
    target
    (create-mouse-event container id MouseEvent/BUTTON1_DOWN_MASK 1 MouseEvent/BUTTON1 target)))

;;;
;;; Hi-level event simulators
;;;

(defn simulate-mouse-click [container target]
  (-> (move-mouse-to container target)
      (simulate-mouse-left MouseEvent/MOUSE_PRESSED target)
      (simulate-mouse-left MouseEvent/MOUSE_RELEASED target)
      (simulate-mouse-left MouseEvent/MOUSE_CLICKED target)))

(defn simulate-key-event [container id key-code char-code target]
  ((resolve-feed-fn container)
    container
    target
    (KeyEvent. dummy-source id 0 0 (if (= id KeyEvent/KEY_TYPED) KeyEvent/VK_UNDEFINED key-code) char-code)))

(defn simulate-key-type [container key-code char-code target]
  (-> (simulate-key-event container KeyEvent/KEY_PRESSED key-code char-code target)
      (simulate-key-event KeyEvent/KEY_TYPED key-code char-code target)
      (simulate-key-event KeyEvent/KEY_RELEASED key-code char-code target)))

(defn simulate-string-type [container str target]
  (let [len (.length str)]
    (loop [c container
           i 0]
      (if (< i len)
        (recur
          (simulate-key-type
            c
            (int (.charAt (clojure.string/upper-case (String/valueOf (.charAt str i))) 0))
            (.charAt str i)
            target)
          (inc i))
        c))))

;;;
;;; Utilities to find components
;;;

;;; Table

(defn table-cell [container table-path pred]
  (let [cells-path (conj (fgc/get-access-key (conj table-path :content-pane)) :children)
        children (get-in container cells-path)
        matching (filter (fn [[_id c]] (pred c)) children)]
    (if-not (empty? matching)
      (vec (concat table-path [:content-pane (first (first matching))]))
      (throw (IllegalArgumentException. (str "No cell matching predicate " pred))))))

(defn click-cell-with-text [container table-path text]
  (simulate-mouse-click container (table-cell container table-path (fn [cell] (= text (:text cell))))))
