; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.colorchooserwin)


;
;;
;; Evolvers for Debug Window
;;
;
;
;(fg/defevolverfn s1text :text
;                 (let [ pos (get-property [:slider1] :position)]
;                   (.format label-format pos)))
;
;(fg/defevolverfn s2text :text
;                 (let [ pos (get-property [:slider2] :position)]
;                   (.format label-format pos)))
;
;; @todo
;; super-evovler marco that would take evolver from parent type.
;; Will have to keep the list of parent types in component.
;; Throw exception if more than one found.
;; One more way to call this macro - specifying exactly which type
;; to take evovler from
;
;(fg/defevolverfn s1pos :position
;                 (if (and
;                       (#{[:slider2] [:lock-checkbox]} (fg/get-reason))
;                       (get-property [:lock-checkbox] :pressed))
;                   (get-property [:slider2] :position)
;                   (flatgui.widgets.slider/slider-position-evolver component)))
;
;(fg/defevolverfn s2pos :position
;                 (if (and
;                       (#{[:slider1] [:lock-checkbox]} (fg/get-reason))
;                       (get-property [:lock-checkbox] :pressed))
;                   (get-property [:slider1] :position)
;                   (flatgui.widgets.slider/slider-position-evolver component)))
;
;;
;; Debug Window
;;
;
;(def debug-window (fg/defcomponent w/window :debug {:clip-size (m/defpoint 3.5 5.5)
;                                                    :position-matrix (m/transtation-matrix 10 7)
;                                                    :text "Debug Panel"}
;
;                                   (fg/defcomponent w/label :s1-text
;                                                    { :text "Slider 1:"
;                                                     :clip-size (m/defpoint 1.0 0.5 0)
;                                                     :position-matrix (m/transtation-matrix 0.25 0.5)})
;
;                                   (fg/defcomponent w/label :s2-text
;                                                    { :text "Slider 2:"
;                                                     :clip-size (m/defpoint 1.0 0.5 0)
;                                                     :position-matrix (m/transtation-matrix 0.25 1.0)})
;
;                                   (fg/defcomponent w/label :s1-label
;                                                    { :clip-size (m/defpoint 3 0.5 0)
;                                                     :h-alignment :left
;                                                     :position-matrix (m/transtation-matrix 1.28125 0.5)
;                                                     :evolvers {:text s1text}})
;
;                                   (fg/defcomponent w/label :s2-label
;                                                    { :clip-size (m/defpoint 3 0.5 0)
;                                                     :h-alignment :left
;                                                     :position-matrix (m/transtation-matrix 1.28125 1.0)
;                                                     :evolvers {:text s2text}})
;
;
;                                   (fg/defcomponent w/checkbox :lock-checkbox { :clip-size (m/defpoint 1.5 0.25 0)
;                                                                               :text "Lock sliders"
;                                                                               :position-matrix (m/transtation-matrix 0.25 1.75)})
;
;                                   (fg/defcomponent w/slider :slider1 { :clip-size (m/defpoint 0.5 3.0 0)
;                                                                       :orientation :vertical
;                                                                       :position-matrix (m/transtation-matrix 0.25 2.25)
;                                                                       :evolvers {:position s1pos}})
;
;                                   (fg/defcomponent w/slider :slider2 { :clip-size (m/defpoint 0.5 3.0 0)
;                                                                       :orientation :vertical
;                                                                       :position-matrix (m/transtation-matrix 1.0 2.25)
;                                                                       :evolvers {:position s2pos}})
;
;                                   (fg/defcomponent w/spinner :spn { :clip-size (m/defpoint 1.375 0.375 0)
;                                                                    :position-matrix (m/transtation-matrix 1.625 2.25)})
;
;                                   ;  (fg/defcomponent w/menu :ctx { :clip-size (m/defpoint 4 6 0)
;                                   ;                            :position-matrix (m/transtation-matrix 3.625 2.75)})
;                                   ))
