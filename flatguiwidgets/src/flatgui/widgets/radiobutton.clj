; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Radio button widget"
      :author "Denys Lebediev"}
  flatgui.widgets.radiobutton
  (:require [flatgui.base :as fg]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.widgets.abstractbutton]))


(fg/defevolverfn radio-pressed-evolver :pressed
  (if (or
        (and (mouse/mouse-pressed? component) (mouse/left-mouse-button? component))
        (flatgui.widgets.abstractbutton/selection-trigger-key? component))
    true
    old-pressed))

(defmacro defradiogroupevolver [fnname all-radio-ids]
  "Creates evolver fn for mutually exclusive radiobuttons.
   The created evolver assumes all radio buttons are added
   to the same container, and it should be assigned to each
   radio button of the group"
  (let [let-binding (vec
                      (list
                        'all-values (conj (mapcat (fn [e] [[e] (list 'get-property 'component [e] :pressed)]) all-radio-ids) 'hash-map)
                        'reason (list 'get-reason)))]
    `(flatgui.base/defevolverfn ~fnname :pressed
                                (let ~let-binding
         (if (contains? ~'all-values ~'reason)
           (if (and (not= [(:id ~'component)] ~'reason) (~'all-values ~'reason)) false ~'old-pressed)
           (flatgui.widgets.radiobutton/radio-pressed-evolver ~'component))))))

(defmacro radiogroupaccessor [all-radio-ids]
  "Creates evolver fn for mutually exclusive radiobuttons.
   The created evolver assumes all radio buttons are added
   to the same container, and it should be assigned to each
   radio button of the group"
  (let [let-binding (vec
                      (list
                        'all-values (conj (mapcat (fn [e] [[e] (list 'get-property 'component [e] :pressed)]) all-radio-ids) 'hash-map)
                        'reason (list 'get-reason)))]
    `(flatgui.base/accessorfn
                                (let ~let-binding
                                  (if (contains? ~'all-values ~'reason)
                                    (if (~'all-values ~'reason) false (:pressed ~'component))
                                    (flatgui.widgets.radiobutton/radio-pressed-evolver ~'component))))))

(fg/defwidget "radiobutton"
  {:v-alignment :center
   :h-alignment :left
   :focusable true
   :skin-key [:radiobutton]
   :evolvers {:pressed radio-pressed-evolver}}
  flatgui.widgets.abstractbutton/abstractbutton)