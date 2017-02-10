; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.photoapp.folderlist
  (:require [flatgui.base :as fg]
            [flatgui.widgets.panel :as panel]
            [flatgui.awt :as awt]
            [flatgui.samples.forms.photoapp.listcommons :as listcommons]
            [flatgui.samples.forms.photoapp.folderwidget :as folderwidget])
  (:import (flatgui.samples.photoapp FGPhotoList)))

(fg/defevolverfn photo-list-creator :photo-list
  (if (nil? old-photo-list)
    (FGPhotoList.)
    old-photo-list))

(fg/defevolverfn :children
  (let [photo-list (get-property [:this] :photo-list)]
    (if (and
          (= [:this] (get-reason)) ; TODO it cannot add children while initializing
          photo-list
          (or (nil? old-children) (empty? old-children)))
      (let [folders (.getFolders photo-list)
            indices (range (count folders))]
        (into {} (map
                   (fn [i] (let [id (listcommons/gen-item-id i)
                                 folder (nth folders i)]
                             [id
                              (fg/defcomponent
                                folderwidget/folderwidget
                                id
                                {:model-index i
                                 :folder folder
                                 :look folderwidget/folder-look
                                 :text (.getTitle photo-list folder)})]))
                   indices)))
      old-children)))

(fg/defwidget "folderlist"
  {:photo-list nil
   :item-widget-dim 0.75
   :item-widget-gap 0.25
   :selected-item 0
   :background (awt/color 16 16 16)
   :evolvers   {:children children-evolver
                :photo-list photo-list-creator
                :selected-item listcommons/selected-item-evolver}}
  panel/panel)