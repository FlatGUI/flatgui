; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.photoapp.imageitemwidget
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.paint :as fgp]
            [flatgui.widgets.component :as component]
            [flatgui.samples.forms.photoapp.listcommons :as listcommons]))

(def fore-color (awt/color 196 196 196))

(fgp/deflookfn img-look (:image-url :selected)
  [(awt/fitImage image-url (awt/+px 0) (awt/+px 0) (awt/-px w 2) (awt/-px h 2))
   (if selected
     [(awt/setColor fore-color)
      (awt/drawRect 0 0 w- h-)])])

(fg/defevolverfn :position-matrix
  (let [x (get-property [] :item-widget-dim)
        gap (get-property [] :item-widget-gap)
        pos (get-property [:this] :model-index)]
    (m/translation (* pos (+ x gap)) 0)))

(fg/defevolverfn :clip-size
  (let [w (get-property [] :item-widget-dim)
        h (m/y (get-property [] :clip-size))]
    (m/defpoint w h)))

;TODO cannot specify look fn in defwidget
(fg/defwidget "imageitemwidget"
  {:model-index 0
   :selected false
   :got-click false
   :evolvers {:position-matrix position-matrix-evolver
              :clip-size clip-size-evolver
              :selected listcommons/selected-evolver
              :got-click listcommons/got-click-evolver}}
  component/component)