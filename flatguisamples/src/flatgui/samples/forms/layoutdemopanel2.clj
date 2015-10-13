; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.layoutdemopanel2
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.theme]
            [flatgui.layout]
            [flatgui.widgets.label :as label]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.abstractbutton :as abtn]
            [flatgui.widgets.textfield :as textfield]
            [flatgui.widgets.panel :as panel]
            [flatgui.widgets.button :as button]
            [flatgui.widgets.scrollpanel :as scrollpanel]))

(def layout-cfg
  [[[:a :-] [:a :-] [:a :-] [:b :c :d :-]]
   [[:e]    [:f :<] [:g]    [:h :-|]]
   [[:i]    [:j :>] [:k]    [:l]]
   [[:m :|] [:n :|] [:o :|] [:p :|]]])

(fg/defevolverfn :layout
  (if (and
        (= (fg/get-reason) [:config :apply])
        (abtn/button-pressed? (get-property [:config :apply] :pressed-trigger)))
    (read-string (get-property [:config :scroll :content-pane :textfield] :text))
    old-layout))

(def demo-window
  (fg/defcomponent
    window/window
    :layoutdemo
    {:clip-size (m/defpoint 5 3.5)
     :position-matrix (m/translation 4.0 0.25)
     :layout layout-cfg
     :text "Layout"
     :evolvers {:layout layout-evolver}}

    (fg/defcomponent button/button :a {:text ":a"})
    (fg/defcomponent button/button :b {:text ":b"})
    (fg/defcomponent button/button :c {:text ":c"})
    (fg/defcomponent button/button :d {:text ":d"})

    (fg/defcomponent button/button :e {:text ":e"})
    (fg/defcomponent button/button :f {:text ":f"})
    (fg/defcomponent button/button :g {:text ":g"})
    (fg/defcomponent button/button :h {:text ":h"})

    (fg/defcomponent button/button :i {:text ":i"})
    (fg/defcomponent button/button :j {:text ":j"})
    (fg/defcomponent button/button :k {:text ":k"})
    (fg/defcomponent button/button :l {:text ":l"})

    (fg/defcomponent button/button :m {:text ":m"})
    (fg/defcomponent button/button :n {:text ":n"})
    (fg/defcomponent button/button :o {:text ":o"})
    (fg/defcomponent button/button :p {:text ":p"})))

(def text-model (textfield/create-multi-line-model
                  (mapcat identity [["["] (mapv str layout-cfg) ["]"]])))

(fg/defevolverfn :config-valid
  (let [cfg-str (get-property [:this :scroll :content-pane :textfield] :text)]
    (if cfg-str
      (try ;For now, there is no better way to check config validity
        (let [cfg (read-string cfg-str)
              layout-cfg (flatgui.layout/coord-map-evolver
                           (assoc (get-in (:root-container component) [:children :layoutdemo]) :layout cfg))]
          (if layout-cfg true false))
        (catch Exception e (do (println "Config invalid: " (.getMessage e)) false)))
      false)))

;;; Allow entering only what's releavant for this sample layout config
(defn sample-only-text-suplier [component]
  (let [key (textfield/textfield-dflt-text-suplier component)]
    (if (#{"a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "m" "n" "o" "p"
           " " "[" "]"
           ":" "-" "|" "<" ">" "'" "."} key) key "")))

(def layout-window
  (fg/defcomponent
    window/window
    :config
    {:clip-size (m/defpoint 3.5 3.5)
     :position-matrix (m/translation 0.25 0.25)
     :text "Config"
     :config-valid true
     :layout [[[:scroll :-|]]
              [[:validity-indicator :-]]
              [[:apply :-]]]
     :evolvers {:config-valid config-valid-evolver}}

    (fg/defcomponent
      scrollpanel/scrollpanel
      :scroll
      {:children {:content-pane (fg/defcomponent
                                  scrollpanel/scrollpanelcontent
                                  :content-pane
                                  {:evolvers {:content-size (fg/accessorfn (get-property component [:this :textfield] :clip-size))
                                              :viewport-matrix textfield/auto-scroll-evolver}
                                   :children {:textfield (fg/defcomponent
                                                           textfield/textfield
                                                           :textfield
                                                           {:multiline true
                                                            :auto-size true
                                                            :paint-border false
                                                            :model text-model
                                                            :text-supplier sample-only-text-suplier
                                                            :text (:text text-model)})}})}})

    (fg/defcomponent label/label :validity-indicator
      {:text "Valid"
       :foreground (awt/color 64 255 64)
       :evolvers {:text (fg/accessorfn (if (get-property component [] :config-valid)
                                         "Valid"
                                         "Not valid"))
                  :foreground (fg/accessorfn (if (get-property component [] :config-valid)
                                               (awt/color 64 255 64)
                                               (awt/color 255 64 64)))}})

    (fg/defcomponent button/button :apply {:text "Apply"})))

(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:theme flatgui.theme/dark
     :clip-size (m/defpoint 40 20)
     :background (awt/color 9 17 26)

     ;; TODO this should be a part defroot probably
     :closed-focus-root true
     :focus-state {:mode :has-focus

                   :focused-child nil}}
    layout-window
    demo-window))