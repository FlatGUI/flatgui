; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Abstract button widget"
      :author "Denys Lebediev"}
  flatgui.widgets.abstractbutton (:use flatgui.awt
                                       flatgui.base
                                       flatgui.paint
                                       flatgui.comlogic
                                       flatgui.widgets.component
                                       flatgui.widgets.label
                                       flatgui.inputchannels.mouse
                                       clojure.test))


;(deflookfn rollover-button-look (:pressed :theme :has-mouse)
;  (if has-mouse
;    [(if pressed (setColor (:light theme)) (setColor (:mid-light theme)))
;     (fillRect (* 2 (px)) (* 2 (px)) (-px w 4) (-px h 4))
;     (setColor (:light theme))
;     (drawRect (* 2 (px)) (* 2 (px)) (-px w 5) (-px h 5))])
;  (call-look label-look))

(deflookfn deprecated-regular-button-look (:theme :has-mouse :pressed)
  (if pressed (setColor (:mid-dark theme)) (setColor (:light theme)))
  (drawLine 0 0 w- 0)
  (drawLine 0 0 0 h-)
  (if pressed (setColor (:light theme)) (setColor (:mid-dark theme)))
  (drawLine 0 h- w- h-)
  (drawLine w- 0 w- h-)
  (call-look label-look))



(defevolverfn regular-pressed-evolver :pressed
  (cond
    (and (mouse-pressed? component) (left-mouse-button? component)) true
    (mouse-released? component) false
    (mouse-exited? component) false
    :else old-pressed))

(defevolverfn :mouse-pressed-trigger
  (if (and (mouse-released? component) (left-mouse-button? component))
    (not old-mouse-pressed-trigger)
    old-mouse-pressed-trigger))

(defevolverfn check-pressed-evolver :pressed
  ; Do not return nil
  (if (or (mouse-left? component) (get-property component [:this] :mouse-pressed-trigger)) true false))

(defevolverfn :pressed-trigger
  (let [ pressed (get-property component [:this] :pressed)]
    [(nth old-pressed-trigger 1) pressed]))


(defn button-pressed? [trigger-value]
  (= trigger-value [false true]))


(defwidget "abstractbutton"
  { :pressed false
   ;:has-mouse false
    :evolvers {                                             ;:has-mouse has-mouse-evolver
               :mouse-pressed-trigger mouse-pressed-trigger-evolver
               :pressed-trigger pressed-trigger-evolver}}
  label)

;
; Tests
;