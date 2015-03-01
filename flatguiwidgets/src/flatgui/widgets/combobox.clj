; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Combo box widget"
      :author "Denys Lebediev"}
  flatgui.widgets.combobox
  (:require [flatgui.awt :as awt]
            [flatgui.base :as fg]
            [flatgui.widgets.component]
            [flatgui.widgets.textfield]
            [flatgui.widgets.abstractbutton]
            [flatgui.widgets.button]
            [flatgui.widgets.combobox.dropdown]
            [flatgui.util.matrix :as m]
            [flatgui.comlogic :as fgc]))

;; TODO move to theme namespace
;;
(defn- btn-w [combo-h] combo-h)

(fg/defevolverfn combo-editor-clip-size-evolver :clip-size
  (let [ combo-size (get-property component [] :clip-size)
         combo-w (m/x combo-size)
         combo-h (m/y combo-size)]
    (m/defpoint (- combo-w (btn-w combo-h)) combo-h)))

(fg/defevolverfn combo-button-clip-size-evolver :clip-size
  (let [ combo-h (m/y (get-property component [] :clip-size))]
    (m/defpoint (btn-w combo-h) combo-h 0)))

(fg/defevolverfn combo-button-pm-evolver :position-matrix
  (let [ combo-size (get-property component [] :clip-size)
         combo-w (m/x combo-size)
         combo-h (m/y combo-size)]
    (m/translation-matrix (- combo-w (btn-w combo-h)) 0)))

(defn- dropdown-item? [reason]
  (and
    (vector? reason)
    (= 3 (count reason))
    (= (fgc/drop-lastv reason) [:dropdown :content-pane])))

(fg/defaccessorfn get-clicked-item [component]
  (let [reason (fg/get-reason)]
    (if (dropdown-item? reason)
      (if (flatgui.widgets.abstractbutton/button-pressed?
            (get-property component [:dropdown :content-pane (last reason)] :pressed-trigger))
        (get-property component [:dropdown :content-pane (last reason)] :text)))))

(fg/defevolverfn combo-text-model-evolver :model
  (let [text (get-clicked-item component)]
    (if text
      (let [len (awt/strlen text)]
        {:text text :caret-pos len :selection-mark 0})
      (flatgui.widgets.textfield/text-model-evolver component))))

(fg/defevolverfn combo-editor-shift-evolver :first-visible-symbol
  (let [reason (fg/get-reason)]
    (if (and
          (dropdown-item? reason)
          (flatgui.widgets.abstractbutton/button-pressed?
            (get-property component [:dropdown :content-pane (last reason)] :pressed-trigger)))
      0
      (flatgui.widgets.textfield/first-visible-symbol-evolver component))))

(fg/defwidget "combobox"
  { :model []
    ;; TODO this is temporary just to get combo's dropdown painted on top. Need generic solution.
    :z-position Integer/MAX_VALUE
    :children {:editor (fg/defcomponent flatgui.widgets.textfield/textfield :editor
                                       {:skin-key [:combobox :editor]
                                        :evolvers {:model combo-text-model-evolver
                                        :clip-size combo-editor-clip-size-evolver
                                        :first-visible-symbol combo-editor-shift-evolver}})
               :arrow-button (fg/defcomponent flatgui.widgets.button/button :arrow-button
                                             {:skin-key [:combobox :arrow-button]
                                              :evolvers {:position-matrix combo-button-pm-evolver
                                                         :clip-size combo-button-clip-size-evolver}})
               :dropdown (fg/defcomponent flatgui.widgets.combobox.dropdown/dropdown :dropdown {:visible false})}}
  flatgui.widgets.component/component)