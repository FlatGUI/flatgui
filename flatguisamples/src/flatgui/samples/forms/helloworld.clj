; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns helloworld
  ;; TODO Get rid of :use
  (:use
        flatgui.widgets.component
        flatgui.widgets.label
        flatgui.widgets.window
        flatgui.widgets.checkbox)
  (:require flatgui.skins.flat
            flatgui.appcontainer
            [flatgui.base :as fg]
            [flatgui.comlogic :as fgc]
            [flatgui.awt :as awt]))

(def nogreeting-text "Not now")

(def greeting-text "Hello, world!")

(fg/defevolverfn greeting-evolver :text
  (if (get-property component [:say-hello] :pressed)
    greeting-text
    nogreeting-text))

(def hello-window
  (fg/defcomponent
    window
    :hello
    {:clip-size (fgc/defpoint 3 1.5)
     :position-matrix (flatgui.util.matrix/transtation-matrix 1 1)
     :text "Hello World Example"}

    (fg/defcomponent
      checkbox
      :say-hello
      {:clip-size (fgc/defpoint 1.75 0.25 0)
       :text "Greeting"
       :position-matrix (flatgui.util.matrix/transtation-matrix 0.125 0.75)})

    (fg/defcomponent
      label
      :greeting
      {:text nogreeting-text
       :clip-size (fgc/defpoint 2.25 0.25 0)
       :position-matrix (flatgui.util.matrix/transtation-matrix 1.0 0.75)
       :evolvers {:text greeting-evolver}})))

;;;; TODO use defcontainer marco instead
(def hellopanel
  (flatgui.widgets.componentbase/initialize
    (fg/defcomponent
      component
      :main
      {:clip-size (fgc/defpoint 40 23 0)
       :background (awt/color (float (/ 9 255)) (float (/ 17 255)) (float (/ 26 255)))}

       hello-window)))

(fg/log-debug "Application GUI has been created")