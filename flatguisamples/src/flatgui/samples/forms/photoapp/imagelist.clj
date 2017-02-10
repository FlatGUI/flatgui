; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.photoapp.imagelist
  (:require [flatgui.base :as fg]
            [flatgui.widgets.panel :as panel]
            [flatgui.awt :as awt]
            [flatgui.samples.forms.photoapp.listcommons :as listcommons]
            [flatgui.samples.forms.photoapp.imageitemwidget :as imageitemwidget]))

(fg/defevolverfn image-url-list-creator :image-url-list
  (let [photo-list (get-property [:flist] :photo-list)
        folder-index (get-property [:flist] :selected-item)
        folder (get-property [:flist (listcommons/gen-item-id folder-index)] :folder)]
    (if (and photo-list folder)
      (.getPhotoURLs photo-list folder)
      old-image-url-list)))

(fg/defevolverfn :children
  (let [image-url-list (get-property [:this] :image-url-list)]
    (if (and
          (= [:this] (get-reason)) ; TODO it cannot add children while initializing
          image-url-list)
      (let [indices (range (count image-url-list))]
        (into {} (map
                   (fn [i] (let [id (listcommons/gen-item-id i)
                                 image-url (nth image-url-list i)]
                             [id
                              (fg/defcomponent
                                imageitemwidget/imageitemwidget
                                id
                                {:image-url image-url
                                 :model-index i
                                 :look imageitemwidget/img-look})]))
                   indices)))
      old-children)))

(fg/defwidget "imagelist"
  {:image-url-list []
   :selected-item 0
   :item-widget-dim 1.0
   :item-widget-gap 0.0
   :background (awt/color 16 16 16)
   :evolvers   {:children children-evolver
                :image-url-list image-url-list-creator
                :selected-item listcommons/selected-item-evolver}}
  panel/panel)