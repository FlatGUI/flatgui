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
  (:require [flatgui.awt :as awt]
            [flatgui.util.matrix :as m]))

;;;
;;; Component
;;;

;(fgp/deflookfn component-look (:background :abs-position-matrix :clip-size)
;  (if (= :main (:id comp-property-map))
;    [(do
;       ;(if (= :main (:id comp-property-map)) (println " component-look for "
;       ;                                               (:id comp-property-map) " dirty-rects = " dirty-rects
;       ;                                               " abs pm = " abs-position-matrix
;       ;                                               " clip size = " clip-size))
;       (awt/setColor background))
;     ;(awt/fillRect 0 0 (m/x content-size) (m/y content-size))
;     (if (and dirty-rects abs-position-matrix)
;       ;Note: here it a single {:x .. :y .. :w .. :h ..} object, not a collection like in previous version. TODO rename parameter dirty-rects->dirty-rect
;       (let [ inter (flatgui.util.rectmath/rect&
;                      dirty-rects
;                      {:x (mx-x abs-position-matrix)
;                       :y (mx-y abs-position-matrix)
;                       :w (m/x clip-size)
;                       :h (m/y clip-size)})]
;         (if inter
;           (awt/fillRect
;             (- (:x inter) (mx-x abs-position-matrix))
;             (- (:y inter) (mx-y abs-position-matrix))
;             (:w inter)
;             (:h inter))))
;       ;(awt/fillRect 0 0 (m/x content-size) (m/y content-size))
;       []
;       )]
;    [(awt/setColor background)
;     (awt/fillRect 0 0 (m/x content-size) (m/y content-size))]
;    ))
(deflookfn component-look (:background :abs-position-matrix :clip-size)
           (awt/setColor background)
           (awt/fillRect 0 0 (m/x content-size) (m/y content-size)))


;;;
;;; Common label-look-impl for various components containing text
;;;

(defn label-look-impl [interop foreground text h-alignment v-alignment left top w h]
  [(flatgui.awt/setColor foreground)
   (let [ dx (condp = h-alignment
               :left (flatgui.awt/hsh interop)
               :right (- w (flatgui.awt/sw interop text) (flatgui.awt/hsh interop))
               (/ (- w (flatgui.awt/sw interop text)) 2))
         dy (condp = v-alignment
              :top (+ (flatgui.awt/hsh interop) (flatgui.awt/sh interop))
              :bottom (- h (flatgui.awt/hsh interop))
              (+ (/ h 2) (flatgui.awt/hsh interop)))]
     (flatgui.awt/drawString text (+ left dx) (+ top dy)))])

(deflookfn label-look (:text :h-alignment :v-alignment)
  (label-look-impl interop foreground text h-alignment v-alignment 0 0 w h))


;;;; TODO !!!! HGAP and get-caret-x is duplicated: widget and skin. Find place for it single
(defn get-hgap [interop] (flatgui.awt/hsh interop))
(defn- get-caret-x [interop text caret-pos]
  (flatgui.awt/sw interop (if (< caret-pos (.length text)) (subs text 0 caret-pos) text)))

(defn- text-str-h [interop] (* (flatgui.awt/sh interop) 2.5))

(defn- get-caret-y [interop caret-line] (* caret-line (text-str-h interop)))

(defn- get-caret-h [interop] (- (* (flatgui.awt/sh interop) 2) (get-hgap interop)))

(deflookfn caret-look ( :model :foreground :first-visible-symbol)
  (let [line (:caret-line model)
        trunk-text (subs (nth (:lines model) line) first-visible-symbol)
        trunk-caret-pos (- (:caret-line-pos model) first-visible-symbol)
        caret-y (+ (get-caret-y interop line) (get-hgap interop))
        xc (+ (get-hgap interop) (get-caret-x interop trunk-text trunk-caret-pos))
        caret-h (get-caret-h interop)]
    [(setColor foreground)
     (drawLine xc caret-y xc (+ caret-y caret-h))]))

(deflookfn textfield-look-impl (:foreground :text :h-alignment :v-alignment :caret-visible :theme :model :first-visible-symbol :multiline)
  (let [selection-start-in-line (if (> (:selection-mark model) (:caret-pos model)) (:caret-line-pos model) (:selection-mark-line-pos model))
        selection-end-in-line (if (> (:selection-mark model) (:caret-pos model)) (:selection-mark-line-pos model) (:caret-line-pos model))
        sstart-line (min (:selection-mark-line model) (:caret-line model))
        send-line (max (:selection-mark-line model) (:caret-line model))
        lines (:lines model)
        line-infos (map
                     (fn [i]
                       (let [line-text (nth lines i)]
                         {:line line-text
                          :y (* i (text-str-h interop))
                          :line-sstart (if (= i sstart-line) selection-start-in-line 0)
                          :line-send (cond
                                       (and (>= i sstart-line) (< i send-line)) (.length line-text)
                                       (= i send-line) selection-end-in-line
                                       :else 0)}))
                     (range 0 (count lines)))

       ;_ (println (str "|" text "|" trunk-text "|"))
       ;_ (println "lines " lines " count " (count lines) (str "|" (nth lines 0) "|") (.getClass (nth lines 0)))
       ;_ (println "Range: " (range 0 (count lines)))
       ;_ (println "line-infos " line-infos)
       ]
    (mapv
      (fn [line-info]
        (let [trunk-text (subs (:line line-info) first-visible-symbol)
              caret-pos (:caret-pos model)
              selection-mark (:selection-mark model)]
          [(if (not= caret-pos selection-mark)
             [(setColor (:prime-5 theme))
              (let [hgap (get-hgap interop)
                    sstart (:line-sstart line-info)
                    send (:line-send line-info)
                    x1 (get-caret-x interop trunk-text sstart)
                    x2 (get-caret-x interop trunk-text send)]
                (fillRect (+ hgap x1) (+ hgap (:y line-info)) (- x2 x1) (get-caret-h interop)))])
           (if caret-visible (call-look caret-look))
           (if multiline
             (label-look-impl interop foreground trunk-text h-alignment v-alignment 0 (:y line-info) w (text-str-h interop))
             (label-look-impl interop foreground trunk-text h-alignment v-alignment 0 0 w h))]))
      line-infos)))


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

(deflookfn dropdown-content-look (:theme)
               (call-look component-look)
               (awt/setColor (:prime-6 theme))
               (awt/drawRect 0 0 w- h-))

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

(deflookfn textfield-look (:has-mouse :focus-state :theme :paint-border)
           ;(call-look panel-look)
           ;(set-component-color)
           ;(draw-component-rect)
           (setColor (:prime-4 theme))
           (fillRect 0 0 w h)
           (if paint-border
             (if (has-focus)
               [(draw-component-rect w h (:prime-3 theme) (:focused theme))
                (setColor (:focused theme))
                (drawRect (awt/px) (awt/px) (awt/-px w 3) (awt/-px h 3))]
               (draw-component-rect w h (:prime-3 theme) (:prime-2 theme))))
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
           (label-look-impl interop foreground text h-alignment v-alignment h 0 w h))

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
                (fillRect left (+ hy side-gap) side-gap (- h (* 2.25 side-gap) hy))])
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

(deflookfn tableheader-look (:theme :mouse-down)
  (awt/setColor (:prime-4 theme))
  (awt/fillRect 0 0 w h))

(deflookfn tablecell-look (:theme :anchor :text :h-alignment :v-alignment :foreground :screen-col)
               [(awt/setColor (:prime-4 theme))
                (awt/drawRect 0 0 w- h-)
                (awt/setColor background)
                (if (= screen-col 0) (awt/fillRect (awt/px) 0 (awt/-px w-) h-) (awt/fillRect 0 0 w- h-))
                (label-look-impl interop foreground text h-alignment v-alignment 0 0 w h)
                (if anchor (awt/setColor (:prime-2 theme)))
                (if anchor (awt/drawRect 0 0 (awt/-px w-) (awt/-px h-)))])

(deflookfn sorting-look (:theme :mode :degree)
               ;(flatgui.awt/setColor foreground)
               (let [text (if (> degree 0) (str degree))
                     tx (- w (awt/sw interop text))
                     hy (/ h 2)
                     ty (+ hy (awt/hsh interop))]
                 [ (cond
                     (= :asc mode)
                     (let [lx1 (* w 0.375)
                           ly1 (- (/ h 2) (* w 0.0625))
                           lx2 (* w 0.5)
                           ly2 (- (/ h 2) (* w 0.25))
                           lx3 (awt/+px (* w 0.625))
                           ly3 (- (/ h 2) (* w 0.0625))]
                       (flatgui.skins.flat/arrow-up lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-1 theme) (:extra-2 theme)))
                     (= :desc mode)
                     (let [lx1 (* w 0.375)
                           ly1 (+ (/ h 2) (* w 0.0625))
                           lx2 (* w 0.5)
                           ly2 (+ (/ h 2) (* w 0.25))
                           lx3 (awt/+px (* w 0.625))
                           ly3 (+ (/ h 2) (* w 0.0625))]
                       (flatgui.skins.flat/arrow-down lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-1 theme) (:extra-2 theme))))
                  ;(if text (flatgui.awt/drawString text tx ty))
                  ]))

(defn- set-vfc-color [mode has-mouse theme]
  (awt/setColor (cond
                  (not= :none mode) (:prime-1 theme)
                  has-mouse (awt/mix-colors (:extra-1 theme) (:prime-1 theme))
                  :else (awt/mix-colors31 (:extra-1 theme) (:prime-1 theme)))))

(deflookfn filtering-look (:theme :mode :has-mouse)
               (let [fw (/ w 2)
                     btm (+ (/ h 2) (/ fw 2))
                     mid (- (/ h 2) (/ fw 4))
                     top (- (/ h 2) (/ fw 2))]
                 [(set-vfc-color mode has-mouse theme)
                  (awt/drawLine (* w 0.25) btm (* w 0.75) btm)
                  (awt/drawLine (* w 0.25) btm (* w 0.25) mid)
                  (awt/drawLine (* w 0.75) btm (* w 0.75) mid)
                  (awt/drawLine (* w 0.25) mid (* w 0.3125) top)
                  (awt/drawLine (* w 0.3125) top (* w 0.375) top)
                  (awt/drawLine (* w 0.375) top (* w 0.5) mid)
                  (awt/drawLine (* w 0.5) mid (* w 0.5625) top)
                  (awt/drawLine (* w 0.5625) top (* w 0.625) top)
                  (awt/drawLine (* w 0.625) top (* w 0.75) mid)
                  (awt/fillRect (* w 0.5625) (awt/+px top) (awt/+px (* w 0.125)) (- mid top))]))

(deflookfn grouping-look (:theme :mode :degree :has-mouse)
               (let [e (awt/+px 0 3) ; TODO (* 0.25 w)
                     t (/ e 2)
                     he (awt/+px 0 2) ; TODO (/ e 2)
                     ]
                 [(set-vfc-color mode has-mouse theme)
                  (awt/fillRect 0 t e e)
                  (awt/fillRect (+ e he) t e e)
                  (awt/fillRect (+ e he e he) t e e)]))


;;;
;;; Menu cell
;;;

(deflookfn menucell-look (:theme :anchor :id)
               [(awt/setColor background)
                ;; TODO 1 px is cut temporarily: until borders are introduced
                ;(flatgui.awt/fillRect 0 0 (m/x content-size) (m/y content-size))
                (awt/fillRect (awt/px) 0 (awt/-px (m/x content-size) 2) (awt/-px (m/y content-size)))
                (call-look label-look)])

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
              (label-look-impl interop foreground text h-alignment v-alignment (if (= h-alignment :left) h 0) 0 w h)]))

;;;
;;; Window
;;;

(defn draw-focused? [focus-state]
  (#{:parent-of-focused :has-focus} (:mode focus-state)))

(deflookfn window-look (:theme :header-h :text :focus-state)
               [(call-look component-look)
                (if (draw-focused? focus-state)
                  [(awt/setColor (:prime-2 theme))
                   (awt/drawRect 0 0 w- h-)])
                (awt/setColor (if (draw-focused? focus-state) (:prime-4 theme) (:prime-2 theme)))
                (awt/fillRect 0 0 w header-h)
                (label-look-impl interop (if (draw-focused? focus-state) (:prime-1 theme) (:prime-4 theme)) text :left :center 0 0 w header-h)])


;;;
;;; skin-map
;;;

(def skin-map
  {:label label-look
   :spinner {:up spinner-up-look
             :down spinner-down-look
             :editor leftsmooth-editor-look}
   :button {:rollover rollover-button-look
            :regular regular-button-look}
   :combobox {:arrow-button combobox-arrow-button-look
              :editor leftsmooth-editor-look
              :dropdown {:content-pane dropdown-content-look}}
   :scrollbar {:scroller scroller-look
               :scrollbar scrollbar-look}
   :textfield textfield-look
   :checkbox checkbox-look
   :radiobutton radiobutton-look
   :slider {:base sliderhandlebase-look
            :handle sliderhandle-look}
   :table {:tableheader tableheader-look
           :columnheader columnheader-look
           :sorting sorting-look
           :filtering filtering-look
           :grouping grouping-look
           :tablecell tablecell-look}
   :menu {:menucell menucell-look}
   :toolbar toolbar-look
   :window window-look
   :component component-look
   })