; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns image
  (:require [flatgui.base]
            [flatgui.skins.flat]
            [flatgui.samples.forms.imagewindow :as iw]))

(def imagepanel (flatgui.base/defroot iw/root-panel))

(def icon "http://flatgui.org/resources/icon.png")
(def smile "http://flatgui.org/resources/smile_32x32.png")

(def imagepanelweb
  (->
    (assoc-in imagepanel [:children :hello :children :img-holder :regular-image-url] icon)
    (assoc-in [:children :hello :children :img-holder :image-url] icon)
    (assoc-in [:children :hello :children :img-holder :pressed-image-url] smile)
    (assoc-in [:children :hello :children :img-holder :_prefetch-res-uri-list] (list icon smile))))