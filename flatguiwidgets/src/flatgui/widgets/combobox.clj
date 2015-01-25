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
  (:use flatgui.awt
                              flatgui.comlogic
                              flatgui.base
                              flatgui.paint
                              flatgui.widgets.component
                              flatgui.widgets.abstractbutton
                              flatgui.widgets.textfield
                              flatgui.widgets.button
                              flatgui.widgets.combobox.dropdown
                              flatgui.util.matrix
                              clojure.test))


;(defn- arrow [la ra ba arr-fn]
;  (loop [ l la
;          r ra
;          ly ba
;          a []]
;    (if (<= l r)
;      (recur
;        (+px l 1)
;        (-px r 1)
;        (arr-fn ly)
;        (conj a (drawLine l ly r ly)))
;      (conj a (drawLine l ly l ly)))))
;
;
;(deflookfn arrow-button-look (:theme)
;  (call-look deprecated-regular-button-look)
;  (setColor (:theme-component-foreground theme))
;  (let [ q (/ w 4)
;         q3 (* q 3)]
;    (arrow q q3 (/ h 3) +px)))

;; TODO move to theme namespace
;;
(defn- btn-w [combo-h] combo-h)

(defevolverfn combo-editor-clip-size-evolver :clip-size
  (let [ combo-size (get-property component [] :clip-size)
         combo-w (x combo-size)
         combo-h (y combo-size)]
    (defpoint (- combo-w (btn-w combo-h)) combo-h)))

(defevolverfn combo-button-clip-size-evolver :clip-size
  (let [ combo-h (y (get-property component [] :clip-size))]
    (defpoint (btn-w combo-h) combo-h 0)))

(defevolverfn combo-button-pm-evolver :position-matrix
  (let [ combo-size (get-property component [] :clip-size)
         combo-w (x combo-size)
         combo-h (y combo-size)]
    (transtation-matrix (- combo-w (btn-w combo-h)) 0)))

(defn- dropdown-item? [reason]
  (and
    (vector? reason)
    (= 3 (count reason))
    (= (drop-lastv reason) [:dropdown :content-pane])))

(defaccessorfn get-clicked-item [component]
  (let [ reason (get-reason)]
    (if (dropdown-item? reason)
      (if (button-pressed? (get-property component [:dropdown :content-pane (last reason)] :pressed-trigger))
        (get-property component [:dropdown :content-pane (last reason)] :text)))))

(defevolverfn combo-text-model-evolver :model
  (let [ text (get-clicked-item component)]
    (if text
      (let [ len (strlen text)]
        {:text text :caret-pos len :selection-mark 0})
      (text-model-evolver component))))

(defevolverfn combo-editor-shift-evolver :first-visible-symbol
  (let [ reason (get-reason)]
    (if (and
          (dropdown-item? reason)
          (button-pressed? (get-property component [:dropdown :content-pane (last reason)] :pressed-trigger)))
      0
      (flatgui.widgets.textfield/first-visible-symbol-evolver component))))

(defwidget "combobox"
  { :model []

    ;; TODO this is temporary just to get combo's dropdown painted on top. Need generic solution.
    :z-position Integer/MAX_VALUE

    :children {:editor (defcomponent textfield :editor {:skin-key [:combobox :editor]
                                                        :evolvers {:model combo-text-model-evolver
                                                                   :clip-size combo-editor-clip-size-evolver
                                                                   :first-visible-symbol combo-editor-shift-evolver}
                                                         })
               :arrow-button (defcomponent button :arrow-button { ;:look arrow-button-look
                                                                  :skin-key [:combobox :arrow-button]
                                                                  :evolvers { :position-matrix combo-button-pm-evolver
                                                                              :clip-size combo-button-clip-size-evolver}})
               :dropdown (defcomponent dropdown :dropdown { :visible false})
               }
    }
  component)

;
; Tests
;