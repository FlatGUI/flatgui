; Copyright (c) 2016 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.widgets.uiapp-test
  (:require [clojure.test :as test]
            [flatgui.core :as core]
            [flatgui.util.matrix :as m]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.checkbox :as checkbox]
            [flatgui.widgets.label :as label])
  (:import (java.awt.event MouseEvent)
           (flatgui.core.awt FGAWTInteropUtil)
           (flatgui.core.engine.ui FGAppContainer)
           (java.awt Container)))

(test/deftest uiapp-test
  (let [a-text "aa123"
        b-text "bb456"
        _ (core/defevolverfn text-evolver
                             (if (get-property [:sw-text] :pressed)
                               b-text
                               a-text))
        _ (println "E=" text-evolver)
        t-win (core/defcomponent
                window/window
                :hello
                {:clip-size (m/defpoint 3 1.5)
                 :position-matrix (m/translation 1 1)
                 :text "Hello World Example"}

                (core/defcomponent
                  checkbox/checkbox
                  :sw-text
                  {:clip-size (m/defpoint 1.75 0.25 0)
                   :text "test"
                   :position-matrix (m/translation 1 1)})

                (core/defcomponent
                  label/label
                  :txt
                  {:text a-text
                   :clip-size (m/defpoint 2.25 0.25 0)
                   :position-matrix (m/translation 2.5 0.75)
                   :evolvers {:text text-evolver}}))
        _ (println "E1=" (get-in t-win [:children :txt :evolvers :text]))
        container (core/defroot t-win)
        ui-app (FGAppContainer. container (FGAWTInteropUtil. 64))
        _ (.initialize ui-app)
        txt-uid (.getComponentUid container [:main :txt])
        dummy-source (Container.)
        container-accessor (.getContainerAccessor ui-app)
        look-before-click (.get (.getComponent container-accessor txt-uid) :look-vec)
        _ (.evolve container (MouseEvent. dummy-source MouseEvent/MOUSE_PRESSED 0 MouseEvent/BUTTON1_DOWN_MASK 129 129 129 129 1 false MouseEvent/BUTTON1))
        _ (.evolve container (MouseEvent. dummy-source MouseEvent/MOUSE_RELEASED 0 MouseEvent/BUTTON1_DOWN_MASK 129 129 129 129 1 false MouseEvent/BUTTON1))
        _ (.evolve container (MouseEvent. dummy-source MouseEvent/MOUSE_CLICKED 0 MouseEvent/BUTTON1_DOWN_MASK 129 129 129 129 1 false MouseEvent/BUTTON1))
        look-after-click (.get (.getComponent container-accessor txt-uid) :look-vec)
        ]
    (test/is (some #(= a-text %) (first look-before-click)))
    (test/is (some #(= b-text %) (first look-after-click)))
    ))