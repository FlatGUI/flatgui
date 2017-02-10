; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.pawindow
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.widgets.component :as component]
            [flatgui.widgets.label :as label]
            [flatgui.widgets.panel :as panel]
            [flatgui.paint :as fgp]
            [flatgui.inputchannels.host :as host]
            [flatgui.samples.forms.photoapp.listcommons :as listcommons]
            [flatgui.samples.forms.photoapp.folderlist :as folderlist]
            [flatgui.samples.forms.photoapp.imagelist :as imagelist])
  (:import (flatgui.core.awt FGImageLoader)))

(def outer-gap 0.25)

(def avatar-x (* outer-gap 2))

(def inner-gap 0.375)


(fgp/deflookfn img-look (:image-url :w :h)
  (if image-url
    [(awt/fitImage image-url 0 0 w h)]))

(fgp/deflookfn display-img-look (:image-ratio :image-url :w :h)
  [(fgp/call-look flatgui.skins.flat/component-look)
   (if image-url
     (let [this-ratio (/ w h)]
       (if (> image-ratio this-ratio)
         (let [cross-r (/ this-ratio image-ratio)
               sh (* h cross-r)
               sy (/ (- h sh) 2)]
           [(awt/fitImage image-url 0 sy w sh)])
         (let [cross-r (/ image-ratio this-ratio)
               sw (* w cross-r)
               sx (/ (- w sw) 2)]
           [(awt/fitImage image-url sx 0 sw h)]))))])

(fg/defevolverfn image-display-evolver :image-url
  (let [im-index (get-property [:imlist] :selected-item)]
    (get-property [:imlist (listcommons/gen-item-id im-index)] :image-url)))

(fg/defevolverfn :image-ratio
 (let [image-url (get-property [:this] :image-url)
       image-size (FGImageLoader/getImageSize image-url)]
   (if image-size
     (let [image-w (.getWidth image-size)
           image-h (.getHeight image-size)]
       (/ image-w image-h))
     old-image-ratio)))

(fg/defevolverfn main-clip-size-evolver :clip-size
  (if (host/host-event? component)
    (let [host-size (host/get-host-size component)]
      (m/defpoint (:w host-size) (:h host-size)))
    old-clip-size))

(fg/defevolverfn flist-clip-size-evolver :clip-size
  (let [this-pos (get-property [:this] :position-matrix)
        parent-size (get-property [] :clip-size)]
    (m/defpoint (m/x old-clip-size) (- (m/y parent-size) (m/mx-y this-pos) outer-gap))))

(fg/defaccessorfn get-im-w-space [component]
  (-
    (m/x (get-property [] :clip-size))
    outer-gap
    inner-gap
    (m/x (get-property [:flist] :clip-size))
    outer-gap))

(fg/defevolverfn imlist-clip-size-evolver :clip-size
  (let [im-w-space (get-im-w-space component)
        imlist-h (m/y old-clip-size)]
    (m/defpoint (- im-w-space (* 2 imlist-h)) imlist-h)))

(fg/defaccessorfn get-im-x [component]
  (let [flist-w (m/x (get-property [:flist] :clip-size))]
    (+ outer-gap flist-w inner-gap)))

(fg/defevolverfn imlist-position-matrix-evolver :position-matrix
  (let [this-clip-size (get-property [:this] :clip-size)
        imlist-h (m/y this-clip-size)
        parent-h (m/y (get-property [] :clip-size))]
    (m/translation
      (+ (get-im-x component) imlist-h)
      (- parent-h imlist-h outer-gap))))

(fg/defevolverfn imdisplay-position-matrix-evolver :position-matrix
  (m/translation
    (get-im-x component)
    (m/mx-y old-position-matrix)))

(fg/defevolverfn imdisplay-clip-size-evolver :clip-size
  (let [this-pos (get-property [:this] :position-matrix)
        parent-size (get-property [] :clip-size)
        imlist-h (m/y (get-property [:imlist] :clip-size))]
    (m/defpoint
      (- (m/x parent-size) (m/mx-x this-pos) outer-gap)
      (- (m/y parent-size) (m/mx-y this-pos) inner-gap imlist-h outer-gap))))

(def avatar-panel
  (fg/defcomponent
    panel/panel
    :avatar
    {:clip-size (m/defpoint 1.5 2.25)
     :position-matrix (m/translation avatar-x 0.5)
     :image-url "file:///D:\\PhotosPub\\dl.png"
     :look img-look}))

(def top-text-x 2.5)

(def top-text-color (awt/color 196 196 196))

(def top-text-h-font "bold 16px sans-serif")

(def top-text-font "14px sans-serif")

(def f-im-y 3.0)

(fg/defwidget "toptext"
  {:font top-text-font
   :h-alignment :left
   :foreground top-text-color}
  label/label)

(def top0
  (fg/defcomponent
    toptext
    :top0
    {:text "Hi there!"
     :font top-text-h-font
     :clip-size (m/defpoint 10 0.5)
     :position-matrix (m/translation top-text-x 0.75)}))

(def top1
  (fg/defcomponent
    toptext
    :top1
    {:text "My name is Denis Lebedev. Welcome to my personal page."
     :clip-size (m/defpoint 10 0.5)
     :position-matrix (m/translation top-text-x 1.25)}))

(def top2
  (fg/defcomponent
    toptext
    :top2
    {:text "My main interests are coding and travelling."
     :clip-size (m/defpoint 10 0.5)
     :position-matrix (m/translation top-text-x 1.5)}))

(def top3
  (fg/defcomponent
    toptext
    :top3
    {:text "Below are my travel memories served by something that I've coded."
     :clip-size (m/defpoint 10 0.5)
     :position-matrix (m/translation top-text-x 1.75)}))

(def flist
  (fg/defcomponent
    folderlist/folderlist
    :flist
    {:position-matrix (m/translation outer-gap f-im-y)
     :clip-size (m/defpoint 4 6)
     :evolvers {:clip-size flist-clip-size-evolver}}))

(def im-x 6)

(def imlist
  (fg/defcomponent
    imagelist/imagelist
    :imlist
    {:position-matrix (m/translation im-x 8.0)
     :clip-size (m/defpoint 6 1)
     :evolvers {:position-matrix imlist-position-matrix-evolver
                :clip-size imlist-clip-size-evolver}}))

(def imdisplay
  (fg/defcomponent
    component/component
    :imdisplay
    {:look display-img-look
     :image-url nil
     :image-ratio {:orientation :landsdcape :ratio 1}
     :position-matrix (m/translation im-x f-im-y)
     :clip-size (m/defpoint 6 4)
     :background (awt/color 16 16 16)
     :evolvers {:image-url image-display-evolver
                :image-ratio image-ratio-evolver
                :position-matrix imdisplay-position-matrix-evolver
                :clip-size imdisplay-clip-size-evolver}}))

(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:clip-size (m/defpoint 40 20)
     :background (awt/color 24 24 24)
     :evolvers {:clip-size main-clip-size-evolver}}

    avatar-panel
    top0 top1 top2 top3
    flist
    imdisplay
    imlist))