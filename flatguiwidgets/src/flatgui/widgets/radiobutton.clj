; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Radio button widget"
      :author "Denys Lebediev"}
  flatgui.widgets.radiobutton (:use
                               flatgui.comlogic
                               flatgui.base
                               flatgui.ids
                               flatgui.paint
                               flatgui.theme
                               flatgui.widgets.component
                               flatgui.widgets.abstractbutton
                               flatgui.widgets.label
                               flatgui.inputchannels.mouse
                               clojure.test))


(defevolverfn radio-pressed-evolver :pressed
  (if (and (mouse-pressed? component) (left-mouse-button? component))
    true
    old-pressed))

(defmacro defradiogroupevolver [fnname all-radio-ids]
  "Creates evolver fn for mutually exclusive radiobuttons.
   The created evolver assumes all radio buttons are added
   to the same container, and it should be assigned to each
   radio button of the group"
  (let [ let-binding (vec
                       (list
                         'all-values (conj (mapcat (fn [e] [[e] (list 'get-property 'component [e] :pressed)]) all-radio-ids) 'hash-map)
                         'reason (list 'get-reason)))]
    `(defevolverfn ~fnname :pressed
       (let ~let-binding
         (if (contains? ~'all-values ~'reason)
           (if (~'all-values ~'reason) false ~'old-pressed)
           (radio-pressed-evolver ~'component))))))

(defwidget "radiobutton"
  { :v-alignment :center
    :h-alignment :left
   ;:look radiobutton-look
    :skin-key [:radiobutton]
    :evolvers {:pressed radio-pressed-evolver}}
  abstractbutton)

;
; Tests
;