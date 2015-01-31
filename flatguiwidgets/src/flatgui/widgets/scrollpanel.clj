; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Scrollable panel widget. Contains pre-installed
            vertical and horizontal scroll bars."
      :author "Denys Lebediev"}
  flatgui.widgets.scrollpanel (:use flatgui.comlogic)
  (:require [flatgui.base :as fg]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.util.matrix :as m]
            [flatgui.widgets.panel]
            [flatgui.widgets.scrollbar]))


(def default-scroll-bar-thickness 0.125)


(fg/defevolverfn scrollpanelcontent-clip-size-evolver :clip-size
  (let [  v-scrollbar-w (x (get-property component [:v-scrollbar] :clip-size))
          h-scrollbar-y (y (get-property component [:h-scrollbar] :clip-size))
          scrollpanel-size (get-property component [] :clip-size)]
     (point-op - scrollpanel-size (defpoint v-scrollbar-w h-scrollbar-y 0))))

(fg/defevolverfn scrollpanelcontent-viewport-matrix-evolver :viewport-matrix
  (let [reason ((:evolve-reason-provider component) (:id component))]
    ; In case of other reasons re-evolving is not needed actually. Since it may
    ; introcude precision impediment, let's avoid extra evolving
    (cond
      (or (= [:v-scrollbar :scroller] reason) (= [:h-scrollbar :scroller] reason))
      (let [v-scrollbar-h (y (get-property component [:v-scrollbar] :clip-size))
            v-scroller-y (m/mx-get (get-property component [:v-scrollbar :scroller] :position-matrix) 1 3)
            v-scroller-h (y (get-property component [:v-scrollbar :scroller] :clip-size))
            h-scrollbar-w (x (get-property component [:h-scrollbar] :clip-size))
            h-scroller-x (m/mx-get (get-property component [:h-scrollbar :scroller] :position-matrix) 0 3)
            h-scroller-w (x (get-property component [:h-scrollbar :scroller] :clip-size))
            v-scroll-pos (if (== v-scrollbar-h v-scroller-h) 0 (/ v-scroller-y (- v-scrollbar-h v-scroller-h)))
            h-scroll-pos (if (== h-scrollbar-w h-scroller-w) 0 (/ h-scroller-x (- h-scrollbar-w h-scroller-w)))
            extra-size (point-op - (get-property component [:this] :content-size) (get-property component [:this] :clip-size))
            mxy (round-to (- (* v-scroll-pos (y extra-size))) (flatgui.awt/px))]
        ;@todo use transtation-matrix function here instead of mx-set
        (m/mx-set
          (m/mx-set m/IDENTITY-MATRIX 0 3 (- (* h-scroll-pos (x extra-size))))
          1 3
          mxy))
      (mouse/mouse-wheel? component)
      (let [ old-y (m/mx-y old-viewport-matrix)]
        ;
        ;@todo  keep range
        ;
        (m/mx-set old-viewport-matrix 1 3 (- old-y (* (:wheel-rotation-step-y component) (mouse/get-wheel-rotation component)))))
      :else old-viewport-matrix)))


(fg/defwidget "scrollpanelcontent"
  {:position-matrix (m/transtation-matrix (flatgui.awt/px) (flatgui.awt/px) 0)
   :content-size (defpoint 14 12 0)
   :wheel-rotation-step-y 1
   :evolvers {:clip-size scrollpanelcontent-clip-size-evolver
              :viewport-matrix scrollpanelcontent-viewport-matrix-evolver}}
  flatgui.widgets.panel/panel)

(fg/defevolverfn verticalscroller-position-matrix-evolver :position-matrix
  (cond
;    (mouse/is-mouse-event? component)
;      (flatgui.widgets.scrollbar/scroller-position-matrix-evolver component)
    (= [:_ :content-pane] ((:evolve-reason-provider component) (:id component)))
      (let [ v-scroller-h (y (get-property component [:this] :clip-size))
             v-scrollbar-h (y (get-property component [] :clip-size))
             content-viewport-matrix (get-property component [:_ :content-pane] :viewport-matrix)
             content-extra-size (point-op - (get-property component [:_ :content-pane] :content-size) (get-property component [:_ :content-pane] :clip-size))

             new-v-scroller-y (if (> (y content-extra-size) 0) (- (* (/ (m/mx-y content-viewport-matrix) (y content-extra-size)) (- v-scrollbar-h v-scroller-h))) 0)
             ;new-v-scroller-y (- (* (/ (m/mx-y content-viewport-matrix) (y content-extra-size)) (- v-scrollbar-h v-scroller-h)))

             ;repaint-reason ((:evolve-reason-provider component) (:id component))
             ;_ (println "--- non-mouse-event --> new-v-scroller-y = " new-v-scroller-y " content viewport y: " (m/mx-y content-viewport-matrix)
             ;    " repaint-reason " repaint-reason " old y " (m/mx-y old-position-matrix) " new y " new-v-scroller-y)
             ]
        (m/mx-set old-position-matrix 1 3 new-v-scroller-y))
    :else
      ;old-position-matrix
    (flatgui.widgets.scrollbar/scroller-position-matrix-evolver component)))

(fg/defevolverfn horizontalscroller-position-matrix-evolver :position-matrix
  (cond
;    (is-mouse-event? component)
;    (flatgui.widgets.scrollbar/scroller-position-matrix-evolver component)
    (= [:_ :content-pane] ((:evolve-reason-provider component) (:id component)))
    (let [ h-scroller-w (x (get-property component [:this] :clip-size))
           h-scrollbar-w (x (get-property component [] :clip-size))
           content-viewport-matrix (get-property component [:_ :content-pane] :viewport-matrix)
           content-extra-size (point-op - (get-property component [:_ :content-pane] :content-size) (get-property component [:_ :content-pane] :clip-size))

           new-h-scroller-x (if (> (x content-extra-size) 0) (- (* (/ (m/mx-x content-viewport-matrix) (x content-extra-size)) (- h-scrollbar-w h-scroller-w))) 0)
           ;new-h-scroller-x (- (* (/ (m/mx-x content-viewport-matrix) (x content-extra-size)) (- h-scrollbar-w h-scroller-w)))

           ;repaint-reason ((:evolve-reason-provider component) (:id component))
           ;_ (println "--- non-mouse-event --> new-v-scroller-y = " new-v-scroller-y " content viewport y: " (m/mx-y content-viewport-matrix)
           ;    " repaint-reason " repaint-reason " old y " (m/mx-y old-position-matrix) " new y " new-v-scroller-y)
           ]
      (m/mx-set old-position-matrix 0 3 new-h-scroller-x))
    :else
    ;old-position-matrix
    (flatgui.widgets.scrollbar/scroller-position-matrix-evolver component)))


;(fg/defwidget "scrollpanelverticalscroller"
;  {:evolvers {:position-matrix verticalscroller-position-matrix-evolver}}
;  scroller)

(fg/defwidget "scrollpanelverticalbar"
  {:scroll-bar-thickness default-scroll-bar-thickness
   :orientation :vertical
   :evolvers { :clip-size (fg/accessorfn (defpoint
                                           (if (get-property component [:this] :visible) (:scroll-bar-thickness component) 0)
                                           (-
                                             (y (get-property component [] :clip-size))
                                             (get-property component [:h-scrollbar] :scroll-bar-thickness))
                                           0))
              :content-size (fg/accessorfn (:clip-size component))
              :position-matrix (fg/accessorfn (m/mx-set
                                                m/IDENTITY-MATRIX
                                                0 3
                                                (- (x (get-property component [] :clip-size)) (:scroll-bar-thickness component))))}

             ; @todo   with this notation it does not take into account dependencies of verticalscroller-position-matrix-evolver,
             ; @todo   such as (get-property component [:_ :content-pane] :viewport-matrix)
             ;    :children (merge
             ;                (:children flatgui.widgets.scrollbar/scrollbar)
             ;                {:scroller scrollpanelverticalscroller})
   :children {:scroller (fg/defcomponent flatgui.widgets.scrollbar/scroller :scroller
                                         {:evolvers {:position-matrix verticalscroller-position-matrix-evolver}})}}
  flatgui.widgets.scrollbar/scrollbar)

(fg/defwidget "scrollpanelhorizontalbar"
  {:scroll-bar-thickness default-scroll-bar-thickness
   :orientation :horizontal
   :evolvers { :clip-size (fg/accessorfn (defpoint
                                           (-
                                             (x (get-property component [] :clip-size))
                                             (get-property component [:v-scrollbar] :scroll-bar-thickness))


                                           ; @todo !!!!!!!!!!!!!
                                           ; This thickness auto-adjusting adds extra cycle to container initialization
                                           ;
                                           (if (get-property component [:this] :visible) (:scroll-bar-thickness component) 0)
                                           ;(:scroll-bar-thickness component)
                                           ;
                                           ;
                                           ;
                                           0))
              :content-size (fg/accessorfn (get-property component [:this] :clip-size))
              :position-matrix (fg/accessorfn (m/mx-set
                                                m/IDENTITY-MATRIX
                                                1 3
                                                (- (y (get-property component [] :clip-size)) (:scroll-bar-thickness component))))}
   ; @todo   with this notation it does not take into account dependencies of verticalscroller-position-matrix-evolver,
   ; @todo   such as (get-property component [:_ :content-pane] :viewport-matrix)
   ;    :children (merge
   ;                (:children flatgui.widgets.scrollbar/scrollbar)
   ;                {:scroller scrollpanelhorizontalscroller})
   :children {:scroller (fg/defcomponent flatgui.widgets.scrollbar/scroller :scroller
                                         {:evolvers {:position-matrix horizontalscroller-position-matrix-evolver}})}}
  flatgui.widgets.scrollbar/scrollbar)

(fg/defwidget "scrollpanel"
  {:children {:v-scrollbar (fg/defcomponent scrollpanelverticalbar :v-scrollbar {})
              :h-scrollbar (fg/defcomponent scrollpanelhorizontalbar :h-scrollbar {})
              :content-pane (fg/defcomponent scrollpanelcontent :content-pane {})}}
  flatgui.widgets.panel/panel)


