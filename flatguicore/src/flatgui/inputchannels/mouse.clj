; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Mouse input channel"
      :author "Denys Lebediev"}
  flatgui.inputchannels.mouse
  (:import [java.awt.event MouseEvent MouseWheelEvent]
           (flatgui.core.awt FGMouseEvent))
  (:require [flatgui.inputchannels.channelbase :as channelbase]
            [flatgui.inputchannels.awtbase :as awtbase]))



; TODO
(defn get-unit-size-px [] 64.0)



;; TODO remove this, use mouse-event?
(channelbase/definputparser is-mouse-event? MouseEvent true)

(channelbase/definputparser mouse-event? MouseEvent true)

(defn mouse-event-obj? [reason] (instance? MouseEvent reason))

(channelbase/definputparser get-mouse-button MouseEvent (.getButton repaint-reason))

(channelbase/definputparser left-mouse-button? MouseEvent (= MouseEvent/BUTTON1 (.getButton repaint-reason)))

(channelbase/definputparser mouse-left? MouseEvent (= MouseEvent/BUTTON1_DOWN_MASK (bit-and (awtbase/modifiers repaint-reason) MouseEvent/BUTTON1_DOWN_MASK)))

(channelbase/definputparser mouse-mid? MouseEvent (= MouseEvent/BUTTON2_DOWN_MASK (bit-and (awtbase/modifiers repaint-reason) MouseEvent/BUTTON2_DOWN_MASK)))
(channelbase/definputparser mouse-right? MouseEvent (= MouseEvent/BUTTON3_DOWN_MASK (bit-and (awtbase/modifiers repaint-reason) MouseEvent/BUTTON3_DOWN_MASK)))

(channelbase/definputparser mouse-pressed? MouseEvent (= MouseEvent/MOUSE_PRESSED (.getID repaint-reason)))
(channelbase/definputparser mouse-released? MouseEvent (= MouseEvent/MOUSE_RELEASED (.getID repaint-reason)))
(channelbase/definputparser mouse-clicked? MouseEvent (= MouseEvent/MOUSE_CLICKED (.getID repaint-reason)))
(channelbase/definputparser mouse-entered? MouseEvent (= MouseEvent/MOUSE_ENTERED (.getID repaint-reason)))
(channelbase/definputparser mouse-moved? MouseEvent (= MouseEvent/MOUSE_MOVED (.getID repaint-reason)))
(channelbase/definputparser mouse-exited? MouseEvent (= MouseEvent/MOUSE_EXITED (.getID repaint-reason)))
(channelbase/definputparser mouse-dragged? MouseEvent (= MouseEvent/MOUSE_DRAGGED (.getID repaint-reason)))

(channelbase/definputparser get-mouse-x MouseEvent
  (let [unit-size-px (get-unit-size-px)]
    (/ (.getX repaint-reason) unit-size-px)))

(channelbase/definputparser get-mouse-y MouseEvent
  (let [unit-size-px (get-unit-size-px)]
    (/ (.getY repaint-reason) unit-size-px)))

(channelbase/definputparser get-mouse-rel-x FGMouseEvent
  (try
    (nth (.getXRelativeVec repaint-reason) (:target-id-path-index comp-property-map))
    (catch IndexOutOfBoundsException e
      (do
        (println "Caught IOB. Event " (.getID repaint-reason) " path" (.getTargetIdPath repaint-reason)
          " comp " (:id comp-property-map)
          " :target-id-path-index = " (:target-id-path-index comp-property-map)
          " vec from event: " (.getXRelativeVec repaint-reason))
        0.0))))

(channelbase/definputparser get-mouse-rel-y FGMouseEvent
  (nth (.getYRelativeVec repaint-reason) (:target-id-path-index comp-property-map)))


(channelbase/definputparser mouse-wheel? MouseWheelEvent true)

(channelbase/definputparser get-wheel-rotation MouseWheelEvent (.getWheelRotation repaint-reason))

;;; Dependency check

(defn find-mouse-dependency [s-expr]
  (if (seq? s-expr)
    (some
      (fn [n] (if
                (and
                  (symbol? n)
                  (let [n-var (resolve n)]
                    (if (var? n-var)
                      (= 'flatgui.inputchannels.mouse (.. n-var ns name)))))
                :mouse))
      s-expr)))
