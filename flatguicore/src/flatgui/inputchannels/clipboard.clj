; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc    "Clipboard input channel"
      :author "Denys Lebediev"}
flatgui.inputchannels.clipboard
  (:require [flatgui.inputchannels.channelbase :as channelbase])
  (:import (flatgui.core FGClipboardEvent)))


(channelbase/definputparser clipboard-paste? FGClipboardEvent (= (.getType repaint-reason) FGClipboardEvent/CLIPBOARD_PASTE))

(channelbase/definputparser clipboard-copy? FGClipboardEvent (= (.getType repaint-reason) FGClipboardEvent/CLIPBOARD_COPY))

(channelbase/definputparser get-event-type FGClipboardEvent (.getType repaint-reason))

(channelbase/definputparser get-plain-text FGClipboardEvent
  (str (.getData repaint-reason)))