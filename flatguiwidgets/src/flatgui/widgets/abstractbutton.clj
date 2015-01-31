; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Abstract button widget"
      :author "Denys Lebediev"}
  flatgui.widgets.abstractbutton (:use flatgui.comlogic) ;Needed for x,y which are in macro ; will not be needed when x,y are moved to matrix
  (:require [flatgui.base :as fg]
            [flatgui.paint :as fgp]
            [flatgui.awt :as awt]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.widgets.label]))


(fgp/deflookfn deprecated-regular-button-look (:theme :has-mouse :pressed)
  (if pressed (awt/setColor (:mid-dark theme)) (awt/setColor (:light theme)))
  (awt/drawLine 0 0 w- 0)
  (awt/drawLine 0 0 0 h-)
  (if pressed (awt/setColor (:light theme)) (awt/setColor (:mid-dark theme)))
  (awt/drawLine 0 h- w- h-)
  (awt/drawLine w- 0 w- h-)
  (fgp/call-look flatgui.widgets.label/label-look))


(fg/defevolverfn regular-pressed-evolver :pressed
  (cond
    (and (mouse/mouse-pressed? component) (mouse/left-mouse-button? component)) true
    (mouse/mouse-released? component) false
    (mouse/mouse-exited? component) false
    :else old-pressed))

(fg/defevolverfn :mouse-pressed-trigger
  (if (and (mouse/mouse-released? component) (mouse/left-mouse-button? component))
    (not old-mouse-pressed-trigger)
    old-mouse-pressed-trigger))

(fg/defevolverfn check-pressed-evolver :pressed
  ; Do not return nil
  (if (or (mouse/mouse-left? component) (get-property component [:this] :mouse-pressed-trigger)) true false))

(fg/defevolverfn :pressed-trigger
  (let [ pressed (get-property component [:this] :pressed)]
    [(nth old-pressed-trigger 1) pressed]))


(defn button-pressed? [trigger-value]
  (= trigger-value [false true]))


(fg/defwidget "abstractbutton"
  { :pressed false
    :evolvers {:mouse-pressed-trigger mouse-pressed-trigger-evolver
               :pressed-trigger pressed-trigger-evolver}}
  flatgui.widgets.label/label)