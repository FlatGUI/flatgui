; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.usagestats
  (:import (java.awt.event MouseEvent KeyEvent)
           (flatgui.core FGHostStateEvent FGClipboardEvent)))

(defn- stats-inc [old-num]
  (if old-num (inc old-num) 1))

(defn default-usagestats-collector [container target-cell-ids reason]
  (if-let [key (cond

                 (instance? MouseEvent reason)
                 (condp = (.getID reason)
                   MouseEvent/MOUSE_PRESSED :mouse-pressed
                   MouseEvent/MOUSE_MOVED :mouse-moved
                   MouseEvent/MOUSE_DRAGGED :mouse-dragged
                   nil)
                 ;(str :mouse (.getID reason))

                 (instance? KeyEvent reason)
                 (condp = (.getID reason)
                   KeyEvent/KEY_PRESSED :key-pressed
                   KeyEvent/KEY_TYPED :key-typed
                   nil)

                 (instance? FGHostStateEvent reason)
                 (if (= FGHostStateEvent/HOST_RESIZE (.getType reason)) :host-resize)

                 (instance? FGClipboardEvent reason)
                 (condp = (.getType reason)
                   FGClipboardEvent/CLIPBOARD_COPY :clipboard-copy
                   FGClipboardEvent/CLIPBOARD_PASTE :clipboard-paste
                   nil))]
    (update-in container [:_usage-stats target-cell-ids key] stats-inc)
    container))

;;; TODO
;;; utility fn that creates a container - same as yours but each component:
;;;  - shows usage intencity with color
;;;  - is focusable so that when it is selected - another panel displays details