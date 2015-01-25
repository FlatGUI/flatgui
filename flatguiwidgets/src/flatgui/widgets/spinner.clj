; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Spinner widget"
      :author "Denys Lebediev"}
  flatgui.widgets.spinner
  (:import [java.text DecimalFormat])
  (:use flatgui.awt
        flatgui.comlogic
        flatgui.base
        flatgui.paint
        flatgui.widgets.component
        flatgui.widgets.abstractbutton
        flatgui.widgets.textfield
        flatgui.widgets.button
        flatgui.util.matrix
        clojure.test))


(defn- btn-w [spinner-h] (* spinner-h 0.75))

(defevolverfn spinner-num-clip-size-evolver :clip-size
  (let [ spinner-size (get-property component [] :clip-size)
         spinner-w (x spinner-size)
         spinner-h (y spinner-size)]
    (defpoint (- spinner-w (btn-w spinner-h)) spinner-h)))

(defevolverfn spinner-button-clip-size-evolver :clip-size
  (let [ spinner-h (y (get-property component [] :clip-size))]
    (defpoint (btn-w spinner-h) (/ spinner-h 2) 0)))

(defevolverfn spinner-up-pm-evolver :position-matrix
  (let [ spinner-size (get-property component [] :clip-size)
         spinner-w (x spinner-size)
         spinner-h (y spinner-size)]
    (transtation-matrix (- spinner-w (btn-w spinner-h)) 0)))

(defevolverfn spinner-down-pm-evolver :position-matrix
  (let [ spinner-size (get-property component [] :clip-size)
         spinner-w (x spinner-size)
         spinner-h (y spinner-size)]
    (transtation-matrix (- spinner-w (btn-w spinner-h)) (/ spinner-h 2))))

;
; @todo defaccessorfn should inject component as a first parameter
;
(defaccessorfn adjust-spinner-value [component old-model adj-fn]
  (let [ old-text (:text old-model)
         str->num (:str->num component)
         num->str (:num->str component)
         num (if (.isEmpty old-text) 0.0 (adj-fn (str->num component old-text) (get-property component [] :step)))
         strnum (num->str component num)
         len (.length strnum)]
    {:text strnum :caret-pos len :selection-mark len}))

(defevolverfn spinner-model-evovler :model
  (condp = (get-reason)
    [:up] (if (button-pressed? (get-property component [:up] :pressed-trigger))
            (adjust-spinner-value component old-model +)
            old-model)
    [:down] (if (button-pressed? (get-property component [:down] :pressed-trigger))
              (adjust-spinner-value component old-model -)
              old-model)
    (text-model-evolver component)))

(defwidget "spinnereditor"
  {:num-format (DecimalFormat. "###############.#######")
   :str->num (fn [_ s] (java.lang.Double/valueOf s))
   :num->str (fn [component n] (let [ f (:num-format component)] (.format f n)))
   :text-supplier textfield-num-only-text-suplier
   :skin-key [:spinner :editor]
   :evolvers {:clip-size spinner-num-clip-size-evolver
              :model spinner-model-evovler}}
  textfield)

(defwidget "spinner"
  { :step 1.0
    :children {:editor (defcomponent spinnereditor :editor {})
               :up (defcomponent button :up { :skin-key [:spinner :up]
                                              :evolvers { :position-matrix spinner-up-pm-evolver
                                                         :clip-size spinner-button-clip-size-evolver}})
               :down (defcomponent button :down { :skin-key [:spinner :down]
                                                  :evolvers { :position-matrix spinner-down-pm-evolver
                                                             :clip-size spinner-button-clip-size-evolver}})}
    }
  component)

;
; Tests
;