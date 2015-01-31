; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Text Field widget"
      :author "Denys Lebediev"}
    flatgui.widgets.textfield (:use flatgui.comlogic)
  (:require [flatgui.awt :as awt]
            [flatgui.base :as fg]
            [flatgui.inputchannels.keyboard :as keyboard]
            [flatgui.inputchannels.awtbase :as inputbase])
  (:import [java.awt.event KeyEvent]))


(defn get-hgap [] (awt/halfstrh))

(defn- get-caret-x [text caret-pos]
  (awt/strw (subs text 0 caret-pos)))

(defn deccaretpos [c]
  (if (> c 0) (- c 1) 0))

(defn inccaretpos [c t]
  (let [len (awt/strlen t)]
    (if (< c len) (+ c 1) len)))

(defn evovle-caret-pos [component old-caret-pos old-selection-mark old-text supplied-text]
  (let [t old-text
        key (keyboard/get-key component)
        typed (keyboard/key-typed? component)
        pressed (keyboard/key-pressed? component)
        fwd-selection-len (if (> old-caret-pos old-selection-mark)
                            (- old-caret-pos old-selection-mark)
                            0)]
    (if typed
      (+ old-caret-pos (awt/strlen supplied-text))
      (if pressed
        (condp = key
          KeyEvent/VK_BACK_SPACE (if (> fwd-selection-len 0) (- old-caret-pos fwd-selection-len) (deccaretpos old-caret-pos))
          KeyEvent/VK_DELETE (if (> fwd-selection-len 0) (- old-caret-pos fwd-selection-len) old-caret-pos)
          KeyEvent/VK_LEFT (deccaretpos old-caret-pos)
          KeyEvent/VK_RIGHT (inccaretpos old-caret-pos t)
          KeyEvent/VK_HOME 0
          KeyEvent/VK_END (awt/strlen t)
          old-caret-pos)
        old-caret-pos))))

(defn evolve-text [component prevcaretpos caretpos old-selection-mark old-text supplied-text]
  (let [ has-selection (not= prevcaretpos old-selection-mark)
        sstart (min prevcaretpos old-selection-mark)
        send (max prevcaretpos old-selection-mark)
        backspace (= (keyboard/get-key component) KeyEvent/VK_BACK_SPACE)
        delete (= (keyboard/get-key component) KeyEvent/VK_DELETE)]
    (if (keyboard/key-typed? component)
      (str
        (subs old-text 0 prevcaretpos)
        supplied-text
        (subs old-text prevcaretpos))
      (if (keyboard/key-pressed? component)
        (cond
          (and (or backspace delete) has-selection) (str (subs old-text 0 sstart) (subs old-text send))
          backspace (if (> prevcaretpos 0)
                      (str (subs old-text 0 caretpos) (subs old-text (+ caretpos 1)))
                      old-text)
          delete (if (< prevcaretpos (awt/strlen old-text))
                   (str (subs old-text 0 prevcaretpos) (subs old-text (+ caretpos 1)))
                   old-text)
          :else old-text)
        old-text))))

(defn evovle-selection-mark [component caret-pos old-selection-mark]
  (cond
    (keyboard/key-typed? component) caret-pos
    (keyboard/key-pressed? component) (if (inputbase/with-shift? component) old-selection-mark caret-pos)
    :else old-selection-mark))

(fg/defevolverfn text-model-evolver :model
              (let [text-supplier (:text-supplier component)
                    supplied-text (text-supplier component)
                    prevcaretpos (:caret-pos old-model)
                    old-selection-mark (:selection-mark old-model)
                    old-text (:text old-model)
                    caretpos (evovle-caret-pos component prevcaretpos old-selection-mark old-text supplied-text)
                    text (evolve-text component prevcaretpos caretpos old-selection-mark old-text supplied-text)
                    selection-mark (evovle-selection-mark component caretpos old-selection-mark)]
                {:text text :caret-pos caretpos :selection-mark selection-mark}))

(fg/defevolverfn :text (:text (get-property component [:this] :model)))

(fg/defevolverfn :first-visible-symbol
              (let [ model (get-property component [:this] :model)]
                (if (>= old-first-visible-symbol (:caret-pos model))
                  (:caret-pos model)
                  (let [ caret-pos (- (:caret-pos model) old-first-visible-symbol)
                        text (:text model)
                        caret-x (get-caret-x (subs text old-first-visible-symbol) caret-pos)
                        width (- (x (get-property component [:this] :clip-size)) (* 1 (get-hgap)) (awt/px))]
                    (if (> caret-x width)
                      (let [ diff (- caret-x width)]
                        (+
                          old-first-visible-symbol
                          (loop [ i 1]
                            (if (>= (awt/strw (subs text old-first-visible-symbol (+ old-first-visible-symbol i))) diff)
                              i
                              (recur
                                (inc i))))))
                      old-first-visible-symbol)))))

;;;
;;;TODO listen to timer
;;;
(fg/defevolverfn :caret-visible
              (and
                (:has-focus component)
                (> (rem (System/currentTimeMillis) 1000) 500)))

(defn textfield-dflt-text-suplier [component]
  (if (not
        (#{KeyEvent/VK_BACK_SPACE KeyEvent/VK_DELETE KeyEvent/VK_LEFT KeyEvent/VK_RIGHT KeyEvent/VK_HOME KeyEvent/VK_END}
          (keyboard/get-key component)))
    (keyboard/get-key-str component)
    ""))

(defn textfield-num-only-text-suplier [component]
  (let [key (textfield-dflt-text-suplier component)]
    (if (some #(= key %) '("0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "0" ".")) key "")))

(fg/defwidget "textfield"
  {:v-alignment :center
   :h-alignment :left
   :text-supplier textfield-dflt-text-suplier
   :caret-visible true
   :model {:text "" :caret-pos 0 :selection-mark 0}
   :text ""
   :first-visible-symbol 0
   :skin-key [:textfield]
   ;; TODO move out
   :foreground :prime-1
   :evolvers {:model text-model-evolver
              :text text-evolver
              :first-visible-symbol first-visible-symbol-evolver}}
  flatgui.widgets.component/component)