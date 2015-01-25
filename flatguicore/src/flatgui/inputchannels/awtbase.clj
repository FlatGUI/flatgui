; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Base AWT input channel functionality"
      :author "Denys Lebediev"}
  flatgui.inputchannels.awtbase
  (:import [java.awt.event InputEvent])
  (:use flatgui.inputchannels.channelbase))

(defn modifiers [event] (.getModifiersEx event))

(definputparser with-shift? java.awt.event.InputEvent (> (bit-and (modifiers repaint-reason) InputEvent/SHIFT_DOWN_MASK) 0))
(definputparser with-ctrl? java.awt.event.InputEvent (> (bit-and (modifiers repaint-reason) InputEvent/CTRL_DOWN_MASK) 0))