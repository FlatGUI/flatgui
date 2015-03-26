; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc    "Host container state change input channel"
      :author "Denys Lebediev"}
flatgui.inputchannels.host
  (:require [flatgui.inputchannels.channelbase :as channelbase])
  (:import (flatgui.core FGHostStateEvent)))


; TODO
(defn get-unit-size-px [] 64.0)


(channelbase/definputparser host-event? FGHostStateEvent true)

(channelbase/definputparser get-event-type FGHostStateEvent (.getType repaint-reason))

(channelbase/definputparser get-host-size FGHostStateEvent
  (let [host-size (.getHostSize repaint-reason)]
    {:w (/ (.getWidth host-size) (get-unit-size-px))
     :h (/ (.getHeight host-size) (get-unit-size-px))}))