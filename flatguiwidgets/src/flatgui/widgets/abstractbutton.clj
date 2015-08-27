; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc    "Abstract button widget"
      :author "Denys Lebediev"}
flatgui.widgets.abstractbutton
  (:require [flatgui.base :as fg]
            [flatgui.paint :as fgp]
            [flatgui.awt :as awt]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.inputchannels.keyboard :as keyboard]
            [flatgui.widgets.label])
  (:import (java.awt.event KeyEvent)))


;(fgp/deflookfn deprecated-regular-button-look (:theme :has-mouse :pressed)
;  (if pressed (awt/setColor (:mid-dark theme)) (awt/setColor (:light theme)))
;  (awt/drawLine 0 0 w- 0)
;  (awt/drawLine 0 0 0 h-)
;  (if pressed (awt/setColor (:light theme)) (awt/setColor (:mid-dark theme)))
;  (awt/drawLine 0 h- w- h-)
;  (awt/drawLine w- 0 w- h-)
;  (fgp/call-look flatgui.widgets.label/label-look))


(fg/defevolverfn regular-pressed-evolver :pressed
  (cond
    (and (mouse/mouse-pressed? component) (mouse/left-mouse-button? component)) true
    (mouse/mouse-released? component) false
    (mouse/mouse-exited? component) false
    :else old-pressed))

(fg/defaccessorfn selection-trigger-key? [component]
  (and (keyboard/key-pressed? component) (= (keyboard/get-key component) KeyEvent/VK_SPACE)))

(fg/defevolverfn :input-pressed-trigger
  (if (or
        (and (mouse/mouse-released? component) (mouse/left-mouse-button? component))
        (selection-trigger-key? component))
    (not old-input-pressed-trigger)
    old-input-pressed-trigger))

(fg/defevolverfn check-pressed-evolver :pressed
  (cond

    (keyboard/key-event? component)
    (get-property component [:this] :input-pressed-trigger)

    (mouse/is-mouse-event? component)
    (or (and (mouse/mouse-left? component) (mouse/mouse-pressed? component))
        (get-property component [:this] :input-pressed-trigger))

    :else old-pressed))

(fg/defevolverfn :pressed-trigger
  (let [ pressed (get-property component [:this] :pressed)]
    [(nth old-pressed-trigger 1) pressed]))


(defn button-pressed? [trigger-value]
  (= trigger-value [false true]))


(fg/defwidget "abstractbutton"
  { :pressed false
    :evolvers {:input-pressed-trigger input-pressed-trigger-evolver
               :pressed-trigger pressed-trigger-evolver}}
  flatgui.widgets.label/label)