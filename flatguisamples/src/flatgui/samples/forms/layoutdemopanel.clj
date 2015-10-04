; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.layoutdemopanel
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.theme]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.label :as label]
            [flatgui.widgets.checkbox :as checkbox]
            [flatgui.widgets.textfield :as textfield]
            [flatgui.widgets.panel :as panel]
            [flatgui.widgets.button :as button]
            [flatgui.widgets.scrollpanel :as scrollpanel]))

(def layout-cfg
  [[[:name-label :>]       [:name-editor :-]]
   [[:password-label :>]   [:password-textfield :-]                      :show-chars-btn]
   [:position-label        [:x :y :-]                                    :edit-pos-btn]
   [:dimensions-label      [[:w-label] [:w :---] [:h-label] [:h :--] :-] :edit-dim-btn]
   [[:one :two :']         [:notes-entry :-|]]
   [[:upper-ed :-']        [:notes-entry]]
   [[:three :four]         [:notes-entry]]
   [[:five :six :-.]       [:notes-entry]]
   [[:long-editor :-]      [:long-editor :-]]
   [[:bottom-labels :<]    [:bleft :bright :-]]])

(def demo-window
  (fg/defcomponent
    window/window
    :layoutdemo
    {:clip-size (m/defpoint 5.125 5.5)
     :position-matrix (m/translation 5 1)
     :layout layout-cfg
     :text "Layout"}

    (fg/defcomponent label/label :name-label {:text "Name:"})
    (fg/defcomponent textfield/textfield :name-editor {})

    (fg/defcomponent label/label :password-label {:text "Password:"})
    (fg/defcomponent textfield/textfield :password-textfield {})
    (fg/defcomponent button/button :show-chars-btn {:text "A"})

    (fg/defcomponent label/label :position-label {:text "Position:"})
    (fg/defcomponent textfield/textfield :x {})
    (fg/defcomponent textfield/textfield :y {})
    (fg/defcomponent button/button :edit-pos-btn {:text "..."})

    (fg/defcomponent checkbox/checkbox :dimensions-label {:text "Dimensions:"})
    (fg/defcomponent label/label :w-label {:text "W:"})
    (fg/defcomponent textfield/textfield :w {})
    (fg/defcomponent label/label :h-label {:text "H:"})
    (fg/defcomponent textfield/textfield :h {})
    (fg/defcomponent button/button :edit-dim-btn {:text "..."})

    (fg/defcomponent checkbox/checkbox :one {:text "One"})
    (fg/defcomponent checkbox/checkbox :two {:text "Two"})

    (fg/defcomponent textfield/textfield :upper-ed {})

    (fg/defcomponent checkbox/checkbox :three {:text "Three"})
    (fg/defcomponent checkbox/checkbox :four {:text "Four"})

    (fg/defcomponent textfield/textfield :five {})
    (fg/defcomponent textfield/textfield :six {})

    (fg/defcomponent textfield/textfield :long-editor {})

    (fg/defcomponent label/label :bottom-labels {:text "Bottom labels:"})
    (fg/defcomponent label/label :bleft {:text "Left"})
    (fg/defcomponent label/label :bright {:text "Right"})

    (fg/defcomponent textfield/textfield :notes-entry {:multiline true})))

(def layout-window
  (fg/defcomponent
    window/window
    :config
    {:clip-size (m/defpoint 5.125 5.5)
     :position-matrix (m/translation 1 1)
     :text "Config"}

    (fg/defcomponent
      scrollpanel/scrollpanel
      :scroll
      {:clip-size (m/defpoint 4.875 4.375)
       :position-matrix (m/translation 0.125 0.5)
       :children {:content-pane (fg/defcomponent
                                  scrollpanel/scrollpanelcontent
                                  :content-pane
                                  {:evolvers {:content-size (fg/accessorfn (get-property component [:this :textfield] :clip-size))
                                              :viewport-matrix textfield/auto-scroll-evolver}
                                   :children {:textfield (fg/defcomponent
                                                           textfield/textfield
                                                           :textfield
                                                           {:clip-size (m/defpoint 4.875 4.375)
                                                            :position-matrix (m/translation 0 0)
                                                            :multiline true
                                                            :auto-size true
                                                            :paint-border false})}})}})))

(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:theme flatgui.theme/dark
     :clip-size (m/defpoint 40 20)
     :background (awt/color 9 17 26)
     :font "bold 14px sans-serif"

     ;; TODO this should be a part defroot probably
     :closed-focus-root true
     :focus-state {:mode :has-focus

                   :focused-child nil}}
    layout-window
    demo-window))