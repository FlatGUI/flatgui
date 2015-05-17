; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.fdemowindow
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.checkbox :as checkbox]
            [flatgui.widgets.textfield :as textfield]
            [flatgui.widgets.label :as label]
            [flatgui.widgets.panel :as panel]
            [flatgui.widgets.component :as component]
            [flatgui.paint :as fgp]))


(fgp/deflookfn container-look (:theme :focus-state)
  [(fgp/call-look component/component-look)
   (awt/setColor (:prime-2 theme))
   (awt/drawRect 0 0 w- h-)
   (awt/drawString (str (:mode focus-state) "/"
                        (:focused-child focus-state)
                        (if (= (:mode focus-state) :throws-focus) (str "/" (:throw-mode focus-state)))) (awt/px) 0.25)])



(fg/defevolverfn tf-model-evolver :model
  (if (= (fg/get-reason) [:this])
    {:text (let [focus-state (get-property [:this] :focus-state)]
             (str (:mode focus-state) "/"
                  (:focused-child focus-state)
                  (if (= (:mode focus-state) :throws-focus) (str "/" (:throw-mode focus-state)))))
     :caret-pos 0
     :selection-mark 0}
    (textfield/text-model-evolver component)))


;; Panel 1 for win 1

(def panel-1
  (fg/defcomponent
    panel/panel
    :hello
    {:clip-size (m/defpoint 4 4)
     :position-matrix (m/translation 1 2)}

    (fg/defcomponent textfield/textfield :entry1
      {:clip-size (m/defpoint 3.0 0.375)
       :position-matrix (m/translation 0.25 0.75)
       :evolvers {:model tf-model-evolver}})

    (fg/defcomponent textfield/textfield :entry2
      {:clip-size (m/defpoint 3.0 0.375)
       :position-matrix (m/translation 0.25 1.25)
       :evolvers {:model tf-model-evolver}})

    (fg/defcomponent textfield/textfield :entry3
      {:clip-size (m/defpoint 3.0 0.375)
       :position-matrix (m/translation 0.25 1.75)
       :evolvers {:model tf-model-evolver}})))


(def title-1 "Focus Example - 1 - ")

(fg/defevolverfn window-text-evolver :text
  (let [focus-state (get-property [:this] :focus-state)]
    (str title-1 (:mode focus-state) "/" (:focused-child focus-state))))

(def title-2 "Focus Example - 2 - ")

(fg/defevolverfn window-text-evolver-2 :text
  (let [focus-state (get-property [:this] :focus-state)]
    (str title-2 (:mode focus-state) "/" (:focused-child focus-state))))

(def title-3 "Focus Example - 3 - ")

(fg/defevolverfn window-text-evolver-3 :text
  (let [focus-state (get-property [:this] :focus-state)]
    (str title-3 (:mode focus-state) "/" (:focused-child focus-state))))

(def title-4 "Focus Example - 4 - ")

(fg/defevolverfn window-text-evolver-4 :text
  (let [focus-state (get-property [:this] :focus-state)]
    (str title-4 (:mode focus-state) "/" (:focused-child focus-state))))

(def title-5 "Focus Example - 5 - ")

(fg/defevolverfn window-text-evolver-5 :text
  (let [focus-state (get-property [:this] :focus-state)]
    (str title-5 (:mode focus-state) "/" (:focused-child focus-state))))

(def title-6 "Focus Example - 6 - ")

(fg/defevolverfn window-text-evolver-6 :text
  (let [focus-state (get-property [:this] :focus-state)]
    (str title-6 (:mode focus-state) "/" (:focused-child focus-state))))


;; Window 1

(def focus-window
  (fg/defcomponent
    window/window
    :hello
    {:clip-size (m/defpoint 7 7)
     :position-matrix (m/translation 1 0.25)
     :text title-1
     :evolvers {:text window-text-evolver}}

    (fg/defcomponent textfield/textfield :entry1
      {:clip-size (m/defpoint 3.0 0.375)
       :position-matrix (m/translation 0.25 0.75)
       :evolvers {:model tf-model-evolver}})

    (fg/defcomponent textfield/textfield :entry2
      {:clip-size (m/defpoint 3.0 0.375)
       :position-matrix (m/translation 0.25 1.25)
       :evolvers {:model tf-model-evolver}})

    (fg/defcomponent textfield/textfield :entry3
      {:clip-size (m/defpoint 3.0 0.375)
       :position-matrix (m/translation 0.25 1.75)
       :evolvers {:model tf-model-evolver}})

    (fg/defcomponent panel/panel :panel1
      {:clip-size (m/defpoint 3.0 3.0)
       :position-matrix (m/translation 0.25 2.5)
       :look container-look})

    (fg/defcomponent panel/panel :panel2
      {:clip-size (m/defpoint 3.0 3.0)
       :position-matrix (m/translation 3.5 2.5)
       :look container-look}

         (fg/defcomponent panel/panel :panel2-1
           {:clip-size (m/defpoint 2.75 1.0)
            :position-matrix (m/translation 0.125 0.5)
            :look container-look}

           (fg/defcomponent textfield/textfield :entry1
             {:clip-size (m/defpoint 1.125 0.375)
              :position-matrix (m/translation 0.125 0.5)
              :evolvers {:model tf-model-evolver}})

           (fg/defcomponent textfield/textfield :entry2
             {:clip-size (m/defpoint 1.125 0.375)
              :position-matrix (m/translation 1.5 0.5)
              :evolvers {:model tf-model-evolver}}))

         (fg/defcomponent panel/panel :panel2-2
           {:clip-size (m/defpoint 2.75 1.0)
            :position-matrix (m/translation 0.125 1.625)
            :look container-look}

           (fg/defcomponent textfield/textfield :entry1
             {:clip-size (m/defpoint 1.125 0.375)
              :position-matrix (m/translation 0.125 0.5)
              :evolvers {:model tf-model-evolver}})

           (fg/defcomponent textfield/textfield :entry2
             {:clip-size (m/defpoint 1.125 0.375)
              :position-matrix (m/translation 1.5 0.5)
              :evolvers {:model tf-model-evolver}})))))

(def focus-window-2
  (fg/defcomponent
    window/window
    :hello2
    {:clip-size (m/defpoint 7 7)
     :position-matrix (m/translation 9 0.25)
     :text title-2
     :evolvers {:text window-text-evolver-2}}

    (fg/defcomponent textfield/textfield :entry1
      {:clip-size (m/defpoint 3.0 0.375)
       :position-matrix (m/translation 0.25 0.75)
       :evolvers {:model tf-model-evolver}})

    (fg/defcomponent textfield/textfield :entry2
      {:clip-size (m/defpoint 3.0 0.375)
       :position-matrix (m/translation 0.25 1.25)
       :evolvers {:model tf-model-evolver}})

    (fg/defcomponent textfield/textfield :entry3
      {:clip-size (m/defpoint 3.0 0.375)
       :position-matrix (m/translation 0.25 1.75)
       :evolvers {:model tf-model-evolver}})

    (fg/defcomponent panel/panel :panel1
      {:clip-size (m/defpoint 6.5 4.0)
       :position-matrix (m/translation 0.25 2.5)
       :look container-look}

      (fg/defcomponent textfield/textfield :entry1-1
        {:clip-size (m/defpoint 1.5 0.375)
         :position-matrix (m/translation 0.125 0.5)
         :evolvers {:model tf-model-evolver}})

      (fg/defcomponent textfield/textfield :entry1-2
        {:clip-size (m/defpoint 1.5 0.375)
         :position-matrix (m/translation 1.75 0.5)
         :evolvers {:model tf-model-evolver}})

           (fg/defcomponent panel/panel :panel1-1
             {:clip-size (m/defpoint 6.25 2.75)
              :position-matrix (m/translation 0.125 1.125)
              :look container-look}

             (fg/defcomponent panel/panel :panel1-1-1
               {:clip-size (m/defpoint 3.0 2.25)
                :position-matrix (m/translation 0.125 0.375)
                :look container-look})

             (fg/defcomponent panel/panel :panel1-1-2
               {:clip-size (m/defpoint 2.875 2.25)
                :position-matrix (m/translation 3.25 0.375)
                :look container-look}

               (fg/defcomponent panel/panel :panel1-1-2-1
                 {:clip-size (m/defpoint 2.625 1.5)
                  :position-matrix (m/translation 0.125 0.5)
                  :look container-look}

               (fg/defcomponent textfield/textfield :entry1-1-2-1
                 {:clip-size (m/defpoint 1.5 0.375)
                  :position-matrix (m/translation 0.125 0.5)
                  :evolvers {:model tf-model-evolver}})))))))

(def focus-window-3
  (fg/defcomponent
    window/window
    :hello3
    {:clip-size (m/defpoint 7 3.25)
     :position-matrix (m/translation 1 7.5)
     :text title-3
     :evolvers {:text window-text-evolver-3}}

    (fg/defcomponent panel/panel :panel
      {:clip-size (m/defpoint 6.75 2.5)
       :position-matrix (m/translation 0.125 0.5)
       :look container-look}

      (fg/defcomponent panel/panel :panel1
        {:clip-size (m/defpoint 3.0 0.875)
         :position-matrix (m/translation 0.25 0.75)
         :look container-look}

        (fg/defcomponent textfield/textfield :entry1-1
                         {:clip-size (m/defpoint 1.0 0.375)
                          :position-matrix (m/translation 0.125 0.375)
                          :evolvers {:model tf-model-evolver}}))

      (fg/defcomponent textfield/textfield :entry1
        {:clip-size (m/defpoint 1.5 0.375)
         :position-matrix (m/translation 0.125 1.75)
         :evolvers {:model tf-model-evolver}})

      (fg/defcomponent textfield/textfield :entry2
        {:clip-size (m/defpoint 1.5 0.375)
         :position-matrix (m/translation 1.75 1.75)
         :evolvers {:model tf-model-evolver}})

      (fg/defcomponent textfield/textfield :entry3
        {:clip-size (m/defpoint 1.5 0.375)
         :position-matrix (m/translation 3.375 1.75)
         :evolvers {:model tf-model-evolver}})
     )
    ))

(def focus-window-4
  (fg/defcomponent
    window/window
    :hello4
    {:clip-size (m/defpoint 7.0 2.0)
     :position-matrix (m/translation 9 7.5)
     :text title-4
     :evolvers {:text window-text-evolver-4}}

    (fg/defcomponent panel/panel :panel
                     {:clip-size (m/defpoint 3.0 1.375)
                      :position-matrix (m/translation 0.125 0.5)
                      :look container-look}

                     (fg/defcomponent panel/panel :panel1
                                      {:clip-size (m/defpoint 2.75 0.875)
                                       :position-matrix (m/translation 0.125 0.375)
                                       :look container-look}

                                      (fg/defcomponent textfield/textfield :entry1-1
                                                       {:clip-size (m/defpoint 2.5 0.375)
                                                        :position-matrix (m/translation 0.125 0.375)
                                                        :evolvers {:model tf-model-evolver}}))
                     )
    ))

(def focus-window-5
  (fg/defcomponent
    window/window
    :hello5
    {:clip-size (m/defpoint 4.25 1.625)
     :position-matrix (m/translation 9 9.75)
     :text title-5
     :evolvers {:text window-text-evolver-5}}

    (fg/defcomponent panel/panel :panel1
                     {:clip-size (m/defpoint 2.75 0.875)
                      :position-matrix (m/translation 0.125 0.5)
                      :look container-look}

                     (fg/defcomponent textfield/textfield :entry1-1
                                      {:clip-size (m/defpoint 2.5 0.375)
                                       :position-matrix (m/translation 0.125 0.375)
                                       :evolvers {:model tf-model-evolver}}))
    ))

(def focus-window-6
  (fg/defcomponent
    window/window
    :hello6
    {:clip-size (m/defpoint 4.25 1.625)
     :position-matrix (m/translation 13.5 9.75)
     :text title-6
     :evolvers {:text window-text-evolver-6}}

    (fg/defcomponent textfield/textfield :entry1-1
                     {:clip-size (m/defpoint 2.5 0.375)
                      :position-matrix (m/translation 0.125 0.5)
                      :evolvers {:model tf-model-evolver}})    ))


(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:clip-size (m/defpoint 40 25)
     :background (awt/color 9 17 26)

     ;; TODO this should be a part defroot probably
     :closed-focus-root true
     :focus-state {:mode :has-focus
                   :focused-child nil}}

    focus-window
    focus-window-2
    focus-window-3
    focus-window-4
    focus-window-5
    focus-window-6))