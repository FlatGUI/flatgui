; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.textwindow
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.theme]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.textfield :as textfield]
            [flatgui.widgets.panel :as panel]
            [flatgui.widgets.scrollpanel :as scrollpanel]))


(def text-window
  (fg/defcomponent
    window/window
    :hello
    {:clip-size (m/defpoint 5.125 5.5)
     :position-matrix (m/translation 1 1)
     :text "Text Example"}

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
                                                            :paint-border false})}})}})

    (fg/defcomponent
      textfield/textfield
      :textfield2
      {:clip-size (m/defpoint 4.875 0.375)
       :position-matrix (m/translation 0.125 5.0)})))

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
    text-window))