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
  (:require [flatgui.base :as fg]
            [flatgui.util.matrix :as m]
            [flatgui.widgets.component]
            [flatgui.widgets.textfield]
            [flatgui.widgets.abstractbutton]
            [flatgui.widgets.button])
  (:import [java.text DecimalFormat])
  (:use flatgui.comlogic))


(defn- btn-w [spinner-h] (* spinner-h 0.75))

(fg/defevolverfn spinner-num-clip-size-evolver :clip-size
  (let [ spinner-size (get-property component [] :clip-size)
         spinner-w (x spinner-size)
         spinner-h (y spinner-size)]
    (defpoint (- spinner-w (btn-w spinner-h)) spinner-h)))

(fg/defevolverfn spinner-button-clip-size-evolver :clip-size
  (let [ spinner-h (y (get-property component [] :clip-size))]
    (defpoint (btn-w spinner-h) (/ spinner-h 2) 0)))

(fg/defevolverfn spinner-up-pm-evolver :position-matrix
  (let [ spinner-size (get-property component [] :clip-size)
         spinner-w (x spinner-size)
         spinner-h (y spinner-size)]
    (m/transtation-matrix (- spinner-w (btn-w spinner-h)) 0)))

(fg/defevolverfn spinner-down-pm-evolver :position-matrix
  (let [ spinner-size (get-property component [] :clip-size)
         spinner-w (x spinner-size)
         spinner-h (y spinner-size)]
    (m/transtation-matrix (- spinner-w (btn-w spinner-h)) (/ spinner-h 2))))

(fg/defaccessorfn adjust-spinner-value [component old-model adj-fn]
  (let [ old-text (:text old-model)
         str->num (:str->num component)
         num->str (:num->str component)
         num (if (.isEmpty old-text) 0.0 (adj-fn (str->num component old-text) (get-property component [] :step)))
         strnum (num->str component num)
         len (.length strnum)]
    {:text strnum :caret-pos len :selection-mark len}))

(fg/defevolverfn spinner-model-evovler :model
  (condp = (fg/get-reason)
    [:up] (if (flatgui.widgets.abstractbutton/button-pressed? (get-property component [:up] :pressed-trigger))
            (adjust-spinner-value component old-model +)
            old-model)
    [:down] (if (flatgui.widgets.abstractbutton/button-pressed? (get-property component [:down] :pressed-trigger))
              (adjust-spinner-value component old-model -)
              old-model)
    (flatgui.widgets.textfield/text-model-evolver component)))

(fg/defwidget "spinnereditor"
  {:num-format (DecimalFormat. "###############.#######")
   :str->num (fn [_ s] (Double/valueOf s))
   :num->str (fn [component n] (let [ f (:num-format component)] (.format f n)))
   :text-supplier flatgui.widgets.textfield/textfield-num-only-text-suplier
   :skin-key [:spinner :editor]
   :evolvers {:clip-size spinner-num-clip-size-evolver
              :model spinner-model-evovler}}
  flatgui.widgets.textfield/textfield)

(fg/defwidget "spinner"
  { :step 1.0
    :children {:editor (fg/defcomponent spinnereditor :editor {})
               :up (fg/defcomponent flatgui.widgets.button/button :up {:skin-key [:spinner :up]
                                                                    :evolvers {:position-matrix spinner-up-pm-evolver
                                                                    :clip-size spinner-button-clip-size-evolver}})
               :down (fg/defcomponent flatgui.widgets.button/button :down {:skin-key [:spinner :down]
                                                                        :evolvers {:position-matrix spinner-down-pm-evolver
                                                                        :clip-size spinner-button-clip-size-evolver}})}}
  flatgui.widgets.component/component)