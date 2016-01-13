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
  (:require [flatgui.inputchannels.channelbase :as channelbase]))


(defn- key-id [key-event] (.getID key-event))

(channelbase/definputparser key-event? KeyEvent true)

(channelbase/definputparser key-typed? KeyEvent (= KeyEvent/KEY_TYPED (key-id repaint-reason)))

(channelbase/definputparser key-pressed? KeyEvent (= KeyEvent/KEY_PRESSED (key-id repaint-reason)))

(channelbase/definputparser key-released? KeyEvent (= KeyEvent/KEY_RELEASED (key-id repaint-reason)))

(channelbase/definputparser get-key-char KeyEvent (int (.getKeyChar repaint-reason)))

(channelbase/definputparser get-key-str KeyEvent (str (.getKeyChar repaint-reason)))

(channelbase/definputparser get-key-code KeyEvent (.getKeyCode repaint-reason))

(defn get-key [comp-property-map]
  (if (key-typed? comp-property-map)
    (get-key-char comp-property-map)
    (if (key-pressed? comp-property-map)
      (get-key-code comp-property-map))))