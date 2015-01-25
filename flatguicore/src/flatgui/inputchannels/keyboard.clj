; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Keyboard input channel"
      :author "Denys Lebediev"}
  flatgui.inputchannels.keyboard
  (:import [java.awt.event KeyEvent])
  (:use flatgui.inputchannels.channelbase clojure.test))


(defn- key-id [key-event] (.getID key-event))

(definputparser key-event? java.awt.event.KeyEvent true)

(definputparser key-typed? java.awt.event.KeyEvent (= java.awt.event.KeyEvent/KEY_TYPED (key-id repaint-reason)))

(definputparser key-pressed? java.awt.event.KeyEvent (= java.awt.event.KeyEvent/KEY_PRESSED (key-id repaint-reason)))

(definputparser get-key-char java.awt.event.KeyEvent (int (.getKeyChar repaint-reason)))

(definputparser get-key-str java.awt.event.KeyEvent (str (.getKeyChar repaint-reason)))

(definputparser get-key-code java.awt.event.KeyEvent (.getKeyCode repaint-reason))

(defn get-key [comp-property-map]
  (if (key-typed? comp-property-map)
    (get-key-char comp-property-map)
    (if (key-pressed? comp-property-map)
      (get-key-code comp-property-map))))