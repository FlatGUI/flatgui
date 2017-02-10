; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.photoapp.folderwidget
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.widgets.label :as label]
            [flatgui.paint :as fgp]
            [flatgui.samples.forms.photoapp.listcommons :as listcommons]))

(def non-selected-color (awt/color 16 16 16))

(def selected-color (awt/color 24 24 24))

(def fore-color (awt/color 196 196 196))

(fgp/deflookfn folder-look (:image-url :selected)
  [(fgp/call-look flatgui.skins.flat/label-look)
   (if selected
     [(awt/setColor fore-color)
      (awt/drawRect 0 0 w- h-)])
   ;(awt/fitImage image-url 0 0 w h)
   ])

(fg/defevolverfn :position-matrix
  (let [h (get-property [] :item-widget-dim)
        gap (get-property [] :item-widget-gap)
        pos (get-property [:this] :model-index)]
    (m/translation 0 (* pos (+ h gap)))))

(fg/defevolverfn :clip-size
  (let [h (get-property [] :item-widget-dim)
        w (m/x (get-property [] :clip-size))]
    (m/defpoint w h)))

;TODO need this?
(fg/defevolverfn :background
  (if (get-property [:this] :selected)
    selected-color
    non-selected-color))

;TODO cannot specify look fn in defwidget
(fg/defwidget "folderwidget"
  {:font "12px sans-serif"
   :foreground fore-color
   :background non-selected-color
   :model-index 0
   :selected false
   :got-click false
   :evolvers {:position-matrix position-matrix-evolver
              :clip-size clip-size-evolver
              :selected listcommons/selected-evolver
              :background background-evolver
              :got-click listcommons/got-click-evolver}}
  label/label)