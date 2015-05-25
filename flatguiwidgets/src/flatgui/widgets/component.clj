; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Base type for all FlatGUI widgets"
      :author "Denys Lebediev"}
  flatgui.widgets.component (:use flatgui.comlogic
                                  flatgui.widgets.componentbase)
  (:require [flatgui.awt :as awt]
            [flatgui.base :as fg]
            [flatgui.paint :as fgp]
            [flatgui.focus :as focus]
            [flatgui.util.matrix :as m]
            [flatgui.theme]
            [flatgui.skins.skinbase]
            [flatgui.skins.flat]
            [flatgui.inputchannels.mouse :as mouse]))

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
(fgp/deflookfn component-look (:background :abs-position-matrix :clip-size)
  (awt/setColor background)
  (awt/fillRect 0 0 (m/x content-size) (m/y content-size)))


;; TODO implement proper z-position evovler after focus manager is initialized
(fg/defevolverfn :z-position
  (if (or
        (get-property component [:this] :popup)
        (#{:has-focus :parent-of-focused} (:mode (get-property component [:this] :focus-state))))
    (Integer/MAX_VALUE)
    0))

;; True there is no parent (get-property returns nil) or parent is visible (true)
(fg/defevolverfn :visible
  (not (false? (get-property component [] :visible))))

;; True there is no parent (get-property returns nil) or parent is enabled (true)
(fg/defevolverfn :enabled
  (not (false? (get-property component [] :enabled))))

(fg/defevolverfn :theme
  (let [parent (get-property component [] :theme)]
    (if parent parent old-theme)))

(fg/defevolverfn :skin
  (let [parent (get-property component [] :skin)]
    (if parent parent old-skin)))

(fg/defevolverfn :abs-position-matrix
  (let [ parent-pm (get-property component [] :abs-position-matrix)
         this-pm (get-property component [:this] :position-matrix)]
    (if parent-pm
      (m/mx* parent-pm this-pm)
      this-pm)))

(fg/defevolverfn :mouse-down (mouse/mouse-left? component))

(fg/defevolverfn :has-mouse
  (cond
    (mouse/mouse-entered? component) true
    (mouse/mouse-exited? component) false
    :else old-has-mouse))

(defn- default-properties-to-evolve-provider [container target-cell-ids reason]
  (fn [component]
    (let [ exclusions #{:look :evolvers}
           key-order (filter (fn [k] (not (contains? exclusions k))) (for [[k v] (:evolvers component)] k))]
      key-order)
;    (:evolving-properties component)
    ))

(fg/defwidget "componentbase"
  (array-map
    :visible true
    :enabled true
    :skin "flatgui.skins.flat"
    :theme flatgui.theme/light
    :clip-size (m/defpoint 1 1 0)
    :content-size (m/defpoint 1 1 0)

    ; When popup is false, a component is rendered inside the clip area of its parent only.
    ; This is the default mode for all regular components. Some components have to visually
    ; occupy areas beyond their parents clip rect - these are for example menus, dialogs.
    ; Use true value for popup property for such components.
    :popup false

    :focusable false
    :closed-focus-root false

    :z-position 0

    :position-matrix m/IDENTITY-MATRIX
    :viewport-matrix m/IDENTITY-MATRIX
    :abs-position-matrix m/IDENTITY-MATRIX

    :background :prime-3
    :foreground :prime-6

    :children nil
    :look component-look
    :default-properties-to-evolve-provider default-properties-to-evolve-provider
    :consumes? (fn [_] true)
    :evolvers {:theme theme-evolver
               :skin skin-evolver
               :look flatgui.skins.skinbase/skin-look-evolver
               :abs-position-matrix abs-position-matrix-evolver}))

;[:main :tiket :ticket-panel :aggr-slider]

(fg/defevolverfn default-content-size-evolver :content-size
  (let [
;         _ (if (= (:path-to-target component) [:main :tiket :ticket-panel :aggr-slider])
;            (println " Evolving content size for " (:id component) (get-property component [:this] :clip-size)  " reason: " (get-reason)) )
        ]
    (get-property component [:this] :clip-size)))

(fg/defwidget "component"
  (array-map
;     :visible true
;     :enabled true
     ;:clip-size (m/defpoint 1 1 0)
     ;:content-size (m/defpoint 1 1 0)
;     :z-position 0
;     :position-matrix IDENTITY-MATRIX
;     :viewport-matrix IDENTITY-MATRIX
;     :theme DEFAULT-SKIN
;     :background :default
;     ;    :focusable false
;     ;    :requests-focus nil
;     ;    :throws-focus nil
;     ;    :has-focus false
;     ;    :focus-owner-id nil
;     ;    :is-focus-cycle-root nil
;     ;    :closed-focus-root false
;     ;    :last-focus-owner-id nil
;     ;    :focus-cycle nil
;     ;    :children-z-order nil
;     :children nil
;     :look component-look

;    ;@todo move this one to componentbase
;     :default-properties-to-evolve-provider default-properties-to-evolve-provider
;     :consumes? (fn [cmpnt] true)
      :has-mouse false

    :focus-traversal-order nil
    :focus-state focus/clean-state

     :evolvers (array-map

                 :visible visible-evolver
                 :enabled enabled-evolver

                 :has-mouse has-mouse-evolver

                 :content-size default-content-size-evolver

                 :focus-state focus/focus-state-evolver

                 :focus-traversal-order focus/focus-traversal-order-evolver

                 :z-position z-position-evolver

                 ; Nov 26 2014 moving this to componentbase since table cells will also need this as an optimization for web
                 ; It was previosly moved out of there for performance reasons. Though now it does not seem to hurt performance
                 ;:abs-position-matrix abs-position-matrix-evolver

                 ;                :requests-focus requests-focus-evolver
                 ;                :has-focus has-focus-evolver
                 ;                :throws-focus throws-focus-evolver
                 ;                :is-focus-cycle-root is-focus-cycle-root-evolver
                 ;                :focus-cycle focus-cycle-evolver
                 ;                :last-focus-owner-id last-focus-owner-id-evolver
                 ;                :children-z-order children-z-order-evolver
                 ;                :focus-owner-id focus-owner-id-evolver
                 )
;     :initializers {
;                     :z-position z-position-evolver
;                     }
     ) componentbase)
