; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.testsuite
  (:require [flatgui.widgets.componentbase])
  (:import (java.awt.event MouseEvent KeyEvent)
           (java.awt Container)))


(def dummy-source (Container.))

(defn simulate-mouse-left [container id target]
  (flatgui.widgets.componentbase/evolve-container
    container
    target
    (MouseEvent. dummy-source id 0 0 0 0 0 0 1 false MouseEvent/BUTTON1)))

(defn simulate-mouse-click [container target]
  (-> (simulate-mouse-left container MouseEvent/MOUSE_PRESSED target)
      (simulate-mouse-left MouseEvent/MOUSE_RELEASED target)
      (simulate-mouse-left MouseEvent/MOUSE_CLICKED target)))

(defn simulate-key-event [container id key-code char-code target]
  (flatgui.widgets.componentbase/evolve-container
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