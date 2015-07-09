; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc    "Default 'flat' skin"
      :author "Denys Lebediev"}
flatgui.skins.flat
  ; TODO get rid of use
  (:use flatgui.awt
        flatgui.skins.skinbase
        flatgui.comlogic
        flatgui.paint)
  (:require [flatgui.awt :as awt]))

;;;; TODO
(defn label-look-impl [foreground text h-alignment v-alignment left top w h]
  [(flatgui.awt/setColor foreground)
   (let [ dx (condp = h-alignment
               :left (flatgui.awt/halfstrh)
               :right (- w (flatgui.awt/strw text) (flatgui.awt/halfstrh))
               (/ (- w (flatgui.awt/strw text)) 2))
         dy (condp = v-alignment
              :top (+ (flatgui.awt/halfstrh) (flatgui.awt/strh))
              :bottom (- h (flatgui.awt/halfstrh))
              (+ (/ h 2) (flatgui.awt/halfstrh)))]
     (flatgui.awt/drawString text (+ left dx) (+ top dy)))])

(deflookfn label-look (:text :h-alignment :v-alignment)
           (label-look-impl foreground text h-alignment v-alignment 0 0 w h))


;;;; TODO !!!! HGAP and get-caret-x is duplicated: widget and skin. Find place for it single
(defn get-hgap [] (flatgui.awt/halfstrh))
(defn- get-caret-x [text caret-pos]
  (flatgui.awt/strw (subs text 0 caret-pos)))

(deflookfn caret-look ( :model :foreground :first-visible-symbol)
           (let [ trunk-text (subs (:text model) first-visible-symbol)
                 trunk-caret-pos (- (:caret-pos model) first-visible-symbol)
                 hgap (get-hgap)
                 xc (+ hgap (get-caret-x trunk-text trunk-caret-pos))]
             [ (setColor foreground)
              (drawLine xc hgap xc (- h hgap))]))


(deflookfn textfield-look-impl (:foreground :text :h-alignment :v-alignment :caret-visible :theme :model :first-visible-symbol)
           (let [ trunk-text (subs text first-visible-symbol)]
             [ (let [ caret-pos (:caret-pos model)
                     selection-mark (:selection-mark model)
                     trunk-caret-pos (- caret-pos first-visible-symbol)
                     trunk-selection-mark (if (< selection-mark first-visible-symbol) 0 (- selection-mark first-visible-symbol))]
                 (if (not= caret-pos selection-mark)
                   [ (setColor (:prime-5 theme))
                    (let [ hgap (get-hgap)
                          sstart (min trunk-caret-pos trunk-selection-mark)
                          send (max trunk-caret-pos trunk-selection-mark)
                          x1 (get-caret-x trunk-text sstart)
                          x2 (get-caret-x trunk-text send)]
                      (fillRect (+ hgap x1) hgap (- x2 x1) (- h (* 2 hgap))))]))
              (if caret-visible (call-look caret-look))
              (label-look-impl foreground trunk-text h-alignment v-alignment 0 0 w h)]))


;;;;

(defmacro has-focus [] `(= :has-focus (:mode ~'focus-state)))

(defmacro set-component-color []
  `(setColor (:prime-1 ~'theme)))

;(defmacro draw-component-rect []
;  `[(drawLine (px) 0 ~'w-2 0)
;    (drawLine 0 (px) 0 ~'h-2)
;    (drawLine (px) ~'h- ~'w-2 ~'h-)
;    (drawLine ~'w- (px) ~'w- ~'h-2)])

(defmacro draw-leftsmooth-rect []
  `[(drawLine (px) 0 ~'w- 0)
    (drawLine 0 (px) 0 ~'h-2)
    (drawLine (px) ~'h- ~'w- ~'h-)
    (drawLine ~'w- (px) ~'w- ~'h-2)])


(defn draw-component-rect [w h panel-color component-color]
  (let [mc1 (mix-colors31 component-color panel-color)
        mc2 (mix-colors31 panel-color component-color)]
    [(setColor component-color)
     (drawLine (+px 0 2) 0 (-px w 3) 0)
     (drawLine (+px 0 2) (-px h 1) (-px w 3) (-px h 1))
     (drawLine 0 (+px 0 2) 0 (-px h 3))
     (drawLine (-px w 1) (+px 0 2) (-px w 1) (-px h 3))
     (setColor mc1)
     (drawLine 0 (px) (px) 0)
     (drawLine 0 (-px h 2) (px) (-px h 1))
     (drawLine (-px w 2) 0 (-px w 1) (px))
     (drawLine (-px w 2) (-px h 1) (-px w 1) (-px h 2))
     (setColor mc2)
     (drawLine 0 0 (px) (px))
     (drawLine 0 (-px h 1) (px) (-px h 2))
     (drawLine (-px w 2) (px) (-px w 1) 0)
     (drawLine (-px w 2) (-px h 2) (-px w 1) (-px h 1))
     ]))


(defn fill-component-rect [w h panel-color component-color]
  (let [mc (mix-colors component-color panel-color)]
    [(setColor mc)
     (drawLine (px) 0 (-px w 2) 0)
     (drawLine 0 (px) (-px w 1) (px))
     (drawLine 0 (-px h 2) (-px w 1) (-px h 2))
     (drawLine (px) (-px h 1) (-px w 2) (-px h 1))
     (setColor component-color)
     (drawLine (+px 0 2) 0 (-px w 3) 0)
     (drawLine (px) (px) (-px w 2) (px))
     (drawLine (px) (-px h 2) (-px w 2) (-px h 2))
     (drawLine (+px 0 2) (-px h 1) (-px w 3) (-px h 1))
     (fillRect 0 (+px 0 2) w (-px h 4))]))

(defn draw-leftsmooth-component-rect [w h panel-color component-color]
  (let [mc1 (mix-colors31 component-color panel-color)
        mc2 (mix-colors31 panel-color component-color)]
    [(setColor component-color)
     (drawLine (+px 0 2) 0 (-px w) 0)
     (drawLine (+px 0 2) (-px h 1) (-px w) (-px h 1))
     (drawLine 0 (+px 0 2) 0 (-px h 3))
     ;(drawLine (-px w 1) (+px 0 2) (-px w 1) (-px h 3))
     (setColor mc1)
     (drawLine 0 (px) (px) 0)
     (drawLine 0 (-px h 2) (px) (-px h 1))
     ;(drawLine (-px w 2) 0 (-px w 1) (px))
     ;(drawLine (-px w 2) (-px h 1) (-px w 1) (-px h 2))
     (setColor mc2)
     (drawLine 0 0 (px) (px))
     (drawLine 0 (-px h 1) (px) (-px h 2))
     ;(drawLine (-px w 2) (px) (-px w 1) 0)
     ;(drawLine (-px w 2) (-px h 2) (-px w 1) (-px h 1))
     ]))

(defn fill-leftsmooth-component-rect [w h panel-color component-color]
  (let [mc (mix-colors component-color panel-color)]
    [(setColor mc)
     (drawLine 0 0 (-px w 2) 0)
     (drawLine 0 (px) (-px w 1) (px))
     (drawLine 0 (-px h 2) (-px w 1) (-px h 2))
     (drawLine 0 (-px h 1) (-px w 2) (-px h 1))
     (setColor component-color)
     (drawLine 0 0 (-px w 3) 0)
     (drawLine 0 (px) (-px w 2) (px))
     (drawLine 0 (-px h 2) (-px w 2) (-px h 2))
     (drawLine 0 (-px h 1) (-px w 3) (-px h 1))
     (fillRect 0 (+px 0 2) w (-px h 4))]))

(defn draw-leftsmoothbutton-component-rect [w h panel-color component-color]
  (let [mc (mix-colors component-color panel-color)]
    [(setColor mc)
     (drawLine 0 0 (-px w 2) 0)
     (drawLine 0 (px) (-px w 1) (px))
     (drawLine 0 (-px h 2) (-px w 1) (-px h 2))
     (drawLine 0 (-px h 1) (-px w 2) (-px h 1))
     (setColor component-color)
     (drawLine 0 0 (-px w 3) 0)
     (drawLine 0 (px) (-px w 2) (px))
     (drawLine 0 (-px h 2) (-px w 2) (-px h 2))
     (drawLine 0 (-px h 1) (-px w 3) (-px h 1))
     (drawLine 0 (px) 0 (-px h 2))
     (drawLine (-px w 1) (px) (-px w 1) (-px h 2))
     (drawLine w (+px 0 2) w (-px h 3))]))

(defn fill-leftsmooth-btm-component-rect [w h panel-color component-color]
  (let [mc (mix-colors component-color panel-color)]
    [(setColor mc)
     ;(drawLine 0 0 (-px w 2) 0)
     ;(drawLine 0 (px) (-px w 1) (px))
     (drawLine 0 (-px h 2) (-px w 1) (-px h 2))
     (drawLine 0 (-px h 1) (-px w 2) (-px h 1))
     (setColor component-color)
     ;(drawLine 0 0 (-px w 3) 0)
     ;(drawLine 0 (px) (-px w 2) (px))
     (drawLine 0 (-px h 2) (-px w 2) (-px h 2))
     (drawLine 0 (-px h 1) (-px w 3) (-px h 1))
     (fillRect 0 0 w (-px h 2))]))

(defn draw-leftsmoothbutton-btm-component-rect [w h panel-color component-color]
  (let [mc (mix-colors component-color panel-color)]
    [(setColor mc)
     ;(drawLine 0 0 (-px w 2) 0)
     ;(drawLine 0 (px) (-px w 1) (px))
     (drawLine 0 (-px h 2) (-px w 1) (-px h 2))
     (drawLine 0 (-px h 1) (-px w 2) (-px h 1))
     (setColor component-color)
     ;(drawLine 0 0 w 0)
     ;(drawLine 0 (px) (-px w 2) (px))
     (drawLine 0 (-px h 2) (-px w 2) (-px h 2))
     (drawLine 0 (-px h 1) (-px w 3) (-px h 1))

     (drawLine 0 0 0 (-px h 2))
     (drawLine (-px w 1) 0 (-px w 1) (-px h 2))
     (drawLine w 0 w (-px h 3))]))

(defn fill-leftsmooth-top-component-rect [w h panel-color component-color]
  (let [mc (mix-colors component-color panel-color)]
    [(setColor mc)
     (drawLine 0 0 (-px w 2) 0)
     (drawLine 0 (px) (-px w 1) (px))
     ;(drawLine 0 (-px h 2) (-px w 1) (-px h 2))
     ;(drawLine 0 (-px h 1) (-px w 2) (-px h 1))
     (setColor component-color)
     (drawLine 0 0 (-px w 3) 0)
     (drawLine 0 (px) (-px w 2) (px))
     ;(drawLine 0 (-px h 2) (-px w 2) (-px h 2))
     ;(drawLine 0 (-px h 1) (-px w 3) (-px h 1))
     (fillRect 0 (+px 0 2) w (-px h 2))]))

(defn draw-leftsmoothbutton-top-component-rect [w h panel-color component-color]
  (let [mc (mix-colors component-color panel-color)]
    [(setColor mc)
     (drawLine 0 0 (-px w 2) 0)
     (drawLine 0 (px) (-px w 1) (px))
     ;(drawLine 0 (-px h 2) (-px w 1) (-px h 2))
     ;(drawLine 0 (-px h 1) (-px w 2) (-px h 1))
     (setColor component-color)
     (drawLine 0 0 (-px w 3) 0)
     (drawLine 0 (px) (-px w 2) (px))
     ;(drawLine 0 (-px h 2) (-px w 2) (-px h 2))
     ;(drawLine 0 (-px h 1) w (-px h 1))

     (drawLine 0 (px) 0 (-px h 1))
     (drawLine (-px w 1) (px) (-px w 1) h)
     (drawLine w (+px 0 2) w h)]))


;;;
;;; Panel
;;;

(deflookfn panel-look (:background)
           (flatgui.awt/setColor background)
           (fillRect 0 0 w h))


;;;
;;; Buttons
;;;

(deflookfn rollover-button-look (:pressed :theme :has-mouse)
           (let [bc (if pressed
                      (:prime-2 theme)
                      (:prime-1 theme))]
             [(if has-mouse (fill-component-rect w h (:prime-3 theme) bc))
              (call-look label-look)]))

(deflookfn regular-button-look (:theme :has-mouse :pressed)
           (let [bc (if pressed
                      (:prime-2 theme)
                      (:prime-1 theme))]
             [(fill-component-rect w h (:prime-3 theme) bc)
              (call-look label-look)]))


;;;
;;; Spinner
;;;

(defn arrow-up [lx1 ly1 lx2 ly2 lx3 ly3 theme fg bg]
  [(setColor (mix-colors fg bg))

   (drawLine lx1 ly1 lx2 ly2)
   (drawLine lx2 ly2 lx3 ly3)

   (setColor fg)

   (drawLine (+px lx1) ly1 (-px lx3) ly3)

   (drawLine (+px lx1) ly1 lx2 (+px ly2))
   (drawLine lx2 (+px ly2) (-px lx3) ly3)

   (drawLine (+px lx1 2) ly1 lx2 (+px ly2 2))
   (drawLine lx2 (+px ly2 2) (-px lx3 2) ly3)

   (drawLine (+px lx1 3) ly1 lx2 (+px ly2 3))
   (drawLine lx2 (+px ly2 3) (-px lx3 3) ly3)])

(defn arrow-down [lx1 ly1 lx2 ly2 lx3 ly3 theme fg bg]
  [(setColor (mix-colors fg bg))

   (drawLine lx1 ly1 lx2 ly2)
   (drawLine lx2 ly2 lx3 ly3)

   (setColor fg)

   (drawLine (+px lx1) ly1 (-px lx3) ly3)

   (drawLine (+px lx1) ly1 lx2 (-px ly2))
   (drawLine lx2 (-px ly2) (-px lx3) ly3)

   (drawLine (+px lx1 2) ly1 lx2 (-px ly2 2))
   (drawLine lx2 (-px ly2 2) (-px lx3 2) ly3)

   (drawLine (+px lx1 3) ly1 lx2 (-px ly2 3))
   (drawLine lx2 (-px ly2 3) (-px lx3 3) ly3)])


(deflookfn spinner-up-look (:pressed :has-mouse :theme :belongs-to-focused-editor)
           (fill-leftsmooth-top-component-rect w h (:prime-3 theme) (if pressed (:prime-2 theme) (:prime-1 theme)))
           (if belongs-to-focused-editor
             (draw-leftsmoothbutton-top-component-rect (awt/-px w 1) h (:prime-3 theme) (:focused theme)))
           (setColor (:prime-4 theme))
           (let [lx1 (* w 0.375)
                 ly1 (- h (* w 0.0625))
                 lx2 (* w 0.5)
                 ly2 (- h (* w 0.25))
                 lx3 (+px (* w 0.625))
                 ly3 ly1]
             (arrow-up lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-4 theme) (:prime-1 theme))))

(deflookfn spinner-down-look (:pressed :has-mouse :theme :belongs-to-focused-editor)
           (fill-leftsmooth-btm-component-rect w h (:prime-3 theme) (if pressed (:prime-2 theme) (:prime-1 theme)))
           (if belongs-to-focused-editor
             (draw-leftsmoothbutton-btm-component-rect (awt/-px w 1) h (:prime-3 theme) (:focused theme)))
           (setColor (:prime-4 theme))
           (let [lx1 (* w 0.375)
                 ly1 (* w 0.0625)
                 lx2 (* w 0.5)
                 ly2 (* w 0.25)
                 lx3 (+px (* w 0.625))
                 ly3 ly1]
             (arrow-down lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-4 theme) (:prime-1 theme))))

(deflookfn leftsmooth-editor-look (:has-mouse :theme :focus-state)
           ;(call-look panel-look)
           (setColor (:prime-4 theme))
           (fillRect 0 0 w h)
           (if (has-focus)
             [(draw-leftsmooth-component-rect w h (:prime-3 theme) (:focused theme))
              (setColor (:focused theme))
              (drawRect (awt/px) (awt/px) (awt/-px w 2) (awt/-px h 3))]
             (draw-leftsmooth-component-rect w h (:prime-3 theme) (:prime-2 theme)))
           (call-look textfield-look-impl))

;;;
;;; Combo Box
;;;

(deflookfn combobox-arrow-button-look (:has-mouse :pressed :theme :belongs-to-focused-editor)
           (fill-leftsmooth-component-rect w h (:prime-3 theme) (if pressed (:prime-2 theme) (:prime-1 theme)))
           (if belongs-to-focused-editor
             (draw-leftsmoothbutton-component-rect (awt/-px w 1) h (:prime-3 theme) (:focused theme)))
           (setColor (:prime-4 theme))
           (let [lx1 (* w 0.375)
                 ly1 (* w 0.375)
                 lx2 (* w 0.5)
                 ly2 (* w 0.5)
                 lx3 (* w 0.625)
                 ly3 (* w 0.375)]
             [(setColor (:prime-4 theme))
              (drawLine lx1 (+px ly1) lx2 (+px ly2))
              (drawLine lx2 (+px ly2) lx3 (+px ly3))
              (setColor (mix-colors (:prime-4 theme) (:prime-1 theme)))
              (drawLine lx1 ly1 lx2 ly2)
              (drawLine lx2 ly2 lx3 ly3)
              (drawLine lx1 (+px ly1 2) (-px lx2) (+px ly2 1))
              (drawLine (+px lx2) (+px ly2 1) lx3 (+px ly3 2))]))

;;;
;;; Scroll Bar
;;;

(deflookfn scroller-look (:has-mouse :theme)
           (setColor (:extra-1 theme))
           (let [vertical (< w h)]
             (if vertical
               (let [i (* 0.5 w)
                     g (* 0.25 w)]
                 [(fillOval g g i i)
                  (fillRect g i i (-px (- h w)))
                  (fillOval g (- h w-) i i)])
               (let [i (* 0.5 h)
                     g (* 0.25 h)]
                 [(fillOval g g i i)
                  (fillRect i g (-px (- w h)) i)
                  (fillOval (- w h) g i i)]))))

(deflookfn scrollbar-look (:theme :orientation)
           (setColor (:extra-1 theme))
           (if (= :vertical orientation)
             (let [i (* 0.5 w)
                   g (* 0.25 w)]
               [(fillOval 0 0 w w)
                (fillRect 0 (/ w 2) w (- h w))
                (fillOval 0 (- h w) w w)
                (setColor (:extra-2 theme))
                (fillOval g g i i)
                (fillRect g i i (-px (- h w)))
                (fillOval g (- h w-) i i)])
             (let [i (* 0.5 h)
                   g (* 0.25 h)]
               [(fillOval 0 0 h h)
                (fillRect (/ h 2) 0 (- w h) h)
                (fillOval (- w h) 0 h h)
                (setColor (:extra-2 theme))
                (fillOval g g i i)
                (fillRect i g (-px (- w h)) i)
                (fillOval (- w h) g i i)
                ])))


;;;
;;; Text Field
;;;

(deflookfn textfield-look (:has-mouse :focus-state :theme)
           ;(call-look panel-look)
           ;(set-component-color)
           ;(draw-component-rect)
           (setColor (:prime-4 theme))
           (fillRect 0 0 w h)
           (if (has-focus)
             [(draw-component-rect w h (:prime-3 theme) (:focused theme))
              (setColor (:focused theme))
              (drawRect (awt/px) (awt/px) (awt/-px w 3) (awt/-px h 3))]
             (draw-component-rect w h (:prime-3 theme) (:prime-2 theme)))
           (call-look textfield-look-impl))

;;;
;;; Check Box
;;;

(deflookfn checkbox-look (:theme :has-mouse :pressed :focus-state :foreground :v-alignment :h-alignment :text)
           ;(call-look component-look)
           [(fill-component-rect h- h- (:prime-3 theme) (:prime-1 theme))
            (if (has-focus)
              [(draw-component-rect h h (:prime-3 theme) (:focused theme))
               (setColor (:focused theme))
               (drawRect (awt/px) (awt/px) (awt/-px h 3) (awt/-px h 3))])
            (if pressed
              (let [lx1 (* h 0.25)
                    ly1 (* h 0.375)
                    lx2 (* h 0.375)
                    ly2 (* h 0.5)
                    lx3 (* h 0.625)
                    ly3 (* h 0.25)]
                [(setColor (:prime-4 theme))
                 (drawLine lx1 ly1 lx2 ly2)
                 (drawLine lx2 ly2 lx3 ly3)
                 (setColor (mix-colors (:prime-4 theme) (:engaged theme)))
                 (drawLine lx1 (+px ly1) lx2 (+px ly2))
                 (drawLine lx2 (+px ly2) lx3 (+px ly3))]))]
           (label-look-impl foreground text h-alignment v-alignment h 0 w h))

;;;
;;; Slider
;;;

(deflookfn sliderhandlebase-look (:theme :side-gap :orientation :sliderhandle-position)
           (setColor (:prime-4 theme))
           (if (= :vertical orientation)
             (let [left (/ (- w side-gap) 2)
                   hy (flatgui.util.matrix/mx-y sliderhandle-position)]
               [(fillOval left side-gap side-gap side-gap)
                (fillRect left (+ (/ side-gap 2) side-gap) side-gap (- h (* 3 side-gap)))
                (setColor (:prime-2 theme))
                (fillOval left (- h side-gap side-gap) side-gap side-gap)
                (fillRect left (+ hy side-gap) side-gap (- h (* 2.5 side-gap) hy))])
             (let [top (/ (- h side-gap) 2)
                   hx (flatgui.util.matrix/mx-x sliderhandle-position)]
               [(fillRect (+ (/ side-gap 2) side-gap) top (- w (* 3 side-gap)) side-gap)
                (fillOval (- w side-gap side-gap) top side-gap side-gap)
                (setColor (:prime-2 theme))
                (fillOval side-gap top side-gap side-gap)
                (fillRect (+ (/ side-gap 2) side-gap) top hx side-gap)])))

(deflookfn sliderhandle-look (:has-mouse :theme :belongs-to-focused-editor)
           (if belongs-to-focused-editor
             [(setColor (:focused theme))
              (fillOval 0 0 w h)
              (setColor (:prime-1 theme))
              (fillOval (awt/+px 0 2) (awt/+px 0 2) (awt/-px w 4) (awt/-px h 4))]
             [(setColor (:prime-1 theme))
              (fillOval 0 0 w h)])
           (setColor (:prime-4 theme))
           (let [d (* w 0.46875)]
             (fillOval (+px (- (/ w 2) (/ d 2))) (+px (- (/ h 2) (/ d 2))) (-px d) (-px d))))

;;;
;;; Table
;;;

(deflookfn columnheader-look (:theme :has-mouse :mouse-down)
           (setColor (if mouse-down (:extra-1 theme) (:extra-2 theme)))
           (fillRect 0 (px) (-px w 1) (-px h 2))
           (call-look label-look))

;;;
;;; Toolbar
;;;

(deflookfn toolbar-look (:theme :background)
           [(setColor background)
            ;(fillRect (px) (px) w-2 h-2)
            (fillRect 0 0 w h)
            (setColor (:prime-2 theme))
            (drawLine (px) (* h 0.125) (px) (- h (* h 0.25)))
            ])

;;;
;;; Radio Button
;;;

(deflookfn radiobutton-look (:theme :pressed :focus-state :foreground :v-alignment :h-alignment :text)
           (let [cout (if (has-focus) (:focused theme) (:prime-6 theme))
                 cin (if pressed (:engaged theme) (:prime-6 theme))
                 r h]
             [(setColor cout)
              (fillOval 0 0 r r)
              (setColor (:prime-4 theme))
              (let [d (* r 0.75)]
                (fillOval (- (/ r 2) (/ d 2)) (- (/ r 2) (/ d 2)) d d))
              (if pressed
                [(setColor cin)
                 (let [d (* r 0.5)]
                   (fillOval (- (/ r 2) (/ d 2)) (- (/ r 2) (/ d 2)) d d))])
              (label-look-impl foreground text h-alignment v-alignment h 0 w h)]))

;;;
;;; skin-map
;;;

(def skin-map
  {:spinner {:up spinner-up-look
             :down spinner-down-look
             :editor leftsmooth-editor-look}
   :button {:rollover rollover-button-look
            :regular regular-button-look}
   :combobox {:arrow-button combobox-arrow-button-look
              :editor leftsmooth-editor-look}
   :scrollbar {:scroller scroller-look
               :scrollbar scrollbar-look}
   :textfield textfield-look
   :checkbox checkbox-look
   :radiobutton radiobutton-look
   :slider {:base sliderhandlebase-look
            :handle sliderhandle-look}
   :table {:columnheader columnheader-look}
   :toolbar toolbar-look
   })