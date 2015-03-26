; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.hellowindow
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.checkbox :as checkbox]
            [flatgui.widgets.label :as label]
            [flatgui.widgets.panel :as panel]
            [flatgui.inputchannels.host :as host]))

(def nogreeting-text "Not now")

(def greeting-text "Hello, world!")

(fg/defevolverfn greeting-evolver :text
  (if (get-property [:say-hello] :pressed)
    greeting-text
    nogreeting-text))

(def hello-window
  (fg/defcomponent
    window/window
    :hello
    {:clip-size (m/defpoint 3 1.5)
     :position-matrix (m/translation 1 1)
     :text "Hello World Example"}

    (fg/defcomponent
      checkbox/checkbox
      :say-hello
      {:clip-size (m/defpoint 1.75 0.25 0)
       :text "Greeting"
       :position-matrix (m/translation 0.125 0.75)})

    (fg/defcomponent
      label/label
      :greeting
      {:text nogreeting-text
       :clip-size (m/defpoint 2.25 0.25 0)
       :position-matrix (m/translation 1.0 0.75)
       :evolvers {:text greeting-evolver}})))

(fg/defevolverfn :clip-size
  (if (host/host-event? component)
    (let [host-size (host/get-host-size component)
          _ (println "Host-size = " host-size)]
      (m/defpoint (:w host-size) (:h host-size)))
    old-clip-size))


(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:clip-size (m/defpoint 4 2)
     :background (awt/color 9 17 26)
     :evolvers {:clip-size clip-size-evolver}}
    hello-window))