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
  (:import [java.awt.event MouseEvent MouseWheelEvent] [flatgui.core FGContainerBase])
  (:use flatgui.comlogic flatgui.inputchannels.channelbase flatgui.inputchannels.awtbase clojure.test))

(definputparser is-mouse-event? java.awt.event.MouseEvent true)

(definputparser get-mouse-button java.awt.event.MouseEvent (.getButton repaint-reason))

(definputparser left-mouse-button? java.awt.event.MouseEvent (= MouseEvent/BUTTON1 (.getButton repaint-reason)))

(definputparser mouse-left? java.awt.event.MouseEvent
  (do

   ; (println " comp-property-map = " comp-property-map)

    (= MouseEvent/BUTTON1_DOWN_MASK (bit-and (modifiers repaint-reason) MouseEvent/BUTTON1_DOWN_MASK))))

(definputparser mouse-mid? java.awt.event.MouseEvent (= MouseEvent/BUTTON2_DOWN_MASK (bit-and (modifiers repaint-reason) MouseEvent/BUTTON2_DOWN_MASK)))
(definputparser mouse-right? java.awt.event.MouseEvent (= MouseEvent/BUTTON3_DOWN_MASK (bit-and (modifiers repaint-reason) MouseEvent/BUTTON3_DOWN_MASK)))

(definputparser mouse-pressed? java.awt.event.MouseEvent (= MouseEvent/MOUSE_PRESSED (.getID repaint-reason)))
(definputparser mouse-released? java.awt.event.MouseEvent (= MouseEvent/MOUSE_RELEASED (.getID repaint-reason)))
(definputparser mouse-entered? java.awt.event.MouseEvent (= MouseEvent/MOUSE_ENTERED (.getID repaint-reason)))
(definputparser mouse-moved? java.awt.event.MouseEvent (= MouseEvent/MOUSE_MOVED (.getID repaint-reason)))
(definputparser mouse-exited? java.awt.event.MouseEvent (= MouseEvent/MOUSE_EXITED (.getID repaint-reason)))

;(definputparser mouse-with-shift? java.awt.event.MouseEvent (> (bit-and (modifiers repaint-reason) InputEvent/SHIFT_DOWN_MASK) 0))
;(definputparser mouse-with-ctrl? java.awt.event.MouseEvent (> (bit-and (modifiers repaint-reason) InputEvent/CTRL_DOWN_MASK) 0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; @todo Components should actually process their repaint reasons (like methods above)
; @todo and have way to convert pixels to units
;
(defn- container [] (FGContainerBase/getCurrentContainer))

(defn- get-unit-size-px [] (.getGeneralProperty (container) "UnitSizePx"))

;;
;(definputparser has-mouse? java.awt.event.MouseEvent
;  (let [ unit-size-px (get-unit-size-px)
;         c comp-property-map
;         x (/ (.getX repaint-reason) unit-size-px)
;         y (/ (.getY repaint-reason) unit-size-px)
;         comp-rect {:x (translate-x (if (nil? (:screen-x c)) (:x c) (:screen-x c)) c)
;                    :y (translate-y (if (nil? (:screen-y c)) (:y c) (:screen-y c)) c)
;                    ;:x (if (nil? (:screen-x c)) (:x c) (:screen-x c))
;                    ;:y (if (nil? (:screen-y c)) (:y c) (:screen-y c))
;                    :w (:w c)
;                    :h (:h c)}]
;    (and
;      (>= x (:x comp-rect))
;      (< x (+ (:x comp-rect) (:w comp-rect)))
;      (>= y (:y comp-rect))
;      (< y (+ (:y comp-rect) (:h comp-rect))))))
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(definputparser get-mouse-x java.awt.event.MouseEvent
  (let [ unit-size-px (get-unit-size-px)]
    (/ (.getX repaint-reason) unit-size-px)))

(definputparser get-mouse-y java.awt.event.MouseEvent
  (let [ unit-size-px (get-unit-size-px)]
    (/ (.getY repaint-reason) unit-size-px)))

; @todo implement these through repaint reason parser as well
;(defn get-mouse-rel-x [comp-property-map] (.getGeneralProperty (container) "MouseX"))
;(defn get-mouse-rel-y [comp-property-map] (.getGeneralProperty (container) "MouseY"))

;(defn get-mouse-rel-x [comp-property-map] (nth (.getGeneralProperty (container) "MouseXVec") (:target-id-path-index comp-property-map)))
;(defn get-mouse-rel-y [comp-property-map] (nth (.getGeneralProperty (container) "MouseYVec") (:target-id-path-index comp-property-map)))

(definputparser get-mouse-rel-x flatgui.core.awt.FGMouseEvent
  (try
    (nth (.getXRelativeVec repaint-reason) (:target-id-path-index comp-property-map))
    (catch IndexOutOfBoundsException e
      (do
        (println "Caught IOB. Event " (.getID repaint-reason) " path" (.getTargetIdPath repaint-reason)
          " comp " (:id comp-property-map)
          " :target-id-path-index = " (:target-id-path-index comp-property-map)
          " vec from event: " (.getXRelativeVec repaint-reason))
        0.0))))

(definputparser get-mouse-rel-y flatgui.core.awt.FGMouseEvent
  (nth (.getYRelativeVec repaint-reason) (:target-id-path-index comp-property-map)))


(definputparser mouse-wheel? java.awt.event.MouseWheelEvent true)

(definputparser get-wheel-rotation java.awt.event.MouseWheelEvent (.getWheelRotation repaint-reason))
