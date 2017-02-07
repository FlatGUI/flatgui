; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc    "Base type for all FlatGUI widgets"
      :author "Denys Lebediev"}
flatgui.widgets.component
  (:use flatgui.comlogic
    ;flatgui.widgets.componentbase
        )
  (:require [flatgui.awt :as awt]
            [flatgui.base :as fg]
            [flatgui.paint :as fgp]
            [flatgui.focus :as focus]
            [flatgui.layout :as layout]
            [flatgui.util.matrix :as m]
            [flatgui.theme]
            [flatgui.skins.skinbase]
            [flatgui.skins.flat]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.dependency :as dep])
  (:import (flatgui.core.awt FGDummyInteropUtil)))


(fg/defevolverfn :z-position
  (if (get-property component [:this] :popup)
    (Integer/MAX_VALUE)
    (let [parent-z (if-let [p (get-property component [] :z-position)] p 0)]
      (if (#{:has-focus :parent-of-focused :throws-focus} (:mode (get-property component [:this] :focus-state)))
        (+ parent-z 1024)
        (+ parent-z 1)))))

(fg/defevolverfn :children-z-order
  (let [child-ids (map (fn [[k _]] k) (get-property [:this] :children))
        child-order (map (fn [e]  [e (get-property [:this e] :z-position)]) child-ids)
        sorted-list (mapv first (sort-by second child-order))]
    sorted-list))

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

(fg/defevolverfn :interop
  (let [parent (get-property component [] :interop)]
    (if parent parent old-interop)))

(fg/defevolverfn :font
  (let [parent (get-property component [] :font)]
    (if parent parent old-font)))

;(fg/defevolverfn :abs-position-matrix
;  (let [ parent-pm (get-property component [] :abs-position-matrix)
;         this-pm (get-property component [:this] :position-matrix)]
;    (if parent-pm
;      (m/mx* parent-pm this-pm)
;      this-pm)))

(fg/defevolverfn :mouse-down (mouse/mouse-left? component))

(fg/defevolverfn :has-mouse
  (cond
    (mouse/mouse-entered? component) true
    (mouse/mouse-exited? component) false
    :else old-has-mouse))

(fg/defevolverfn :_visible-popup
  (and
    (get-property [:this] :visible)
    (get-property [:this] :popup)))

(defn get-channel-to-propery-map-list [p evolvers]
  (map
    (fn [ch] {ch (list p)})
    (dep/get-input-channel-dependencies (p evolvers))))

;; TODO worked wrond when removing componentes feature has been added; probably this is not needed at all
;(fg/defevolverfn :input-channel-subscribers
;  (let [id (:id component)
;        channel-to-properties (let [evolvers (:evolvers component)
;                                    all-properties (for [[k _v] evolvers] k)]
;                                (apply
;                                  merge-with
;                                  concat
;                                  (mapcat (fn [p] (get-channel-to-propery-map-list p evolvers)) all-properties)))]
;    (concat
;      (list [[id] channel-to-properties])
;      (let [child-ids (for [[k _] (get-property [:this] :children)] k)]
;        (map
;          (fn [[child-id-path child-ch-to-props]] [(vec (concat [id] child-id-path)) child-ch-to-props])
;          (mapcat
;            (fn [k] (get-property [:this k] :input-channel-subscribers))
;            child-ids))))))
(fg/defevolverfn :input-channel-subscribers old-input-channel-subscribers)

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
    :interop (FGDummyInteropUtil.) ; For now, :interop has to be available when initializing the template. This may be changed
    :skin "flatgui.skins.flat"
    :theme flatgui.theme/light
    :clip-size (m/defpoint 1 1 0)
    :content-size (m/defpoint 1 1 0)

    ; When popup is false, a component is rendered inside the clip area of its parent only.
    ; This is the default mode for all regular components. Some components have to visually
    ; occupy areas beyond their parents clip rect - these are for example menus, dialogs.
    ; Use true value for popup property for such components.
    :popup false

    :_visible-popup false

    :focusable false
    :closed-focus-root false

    :z-position 0
    :children-z-order nil

    :input-channel-subscribers nil

    :position-matrix m/IDENTITY-MATRIX
    :viewport-matrix m/IDENTITY-MATRIX
    :abs-position-matrix m/IDENTITY-MATRIX

    :background :prime-3
    :foreground :prime-6

    :children nil
    :skin-key [:component]
    :default-properties-to-evolve-provider default-properties-to-evolve-provider
    :consumes? (fn [_] true)
    :evolvers {:interop interop-evolver
               :theme theme-evolver
               :skin skin-evolver
               :font font-evolver
               :look flatgui.skins.skinbase/skin-look-evolver
               ;:abs-position-matrix abs-position-matrix-evolver

               :_visible-popup _visible-popup-evolver

               :input-channel-subscribers input-channel-subscribers-evolver
               :z-position z-position-evolver
               :children-z-order children-z-order-evolver}))

;[:main :tiket :ticket-panel :aggr-slider]

(fg/defevolverfn default-content-size-evolver :content-size
  (let [
         ;_ (if (= (:id component) :app-panel)
         ;   (println "Evolving content size for " (:id component) (get-property component [:this] :clip-size)  " reason: " (fg/get-reason)) )
        ]
    (get-property component [:this] :clip-size)))

(fg/defevolverfn :preferred-size
  (let [text (get-property [:this] :text)
        text-w (if text (awt/strw component text) 0)
        text-h (if text (awt/strh component) 0)
        icon (get-property [:this] :icon)
        icon-w 0 ;TODO
        icon-h 0 ;TODO
        icon-to-text-pos (get-property [:this] :icon-to-text-pos)
        h-margin (get-property [:this] :h-margin)
        v-margin (get-property [:this] :v-margin)]
    (if (or (pos? icon-w) (pos? text-w) (pos? icon-h) (pos? text-h)) ; Empty text may still have height (just defined by font)
      (if (or (= icon-to-text-pos :left) (= icon-to-text-pos :right))
        (m/defpoint
          (cond
            (and (pos? icon-w) (pos? text-w))
            (+ v-margin icon-w v-margin text-w v-margin)
            (pos? text-w)
            (+ v-margin text-w v-margin)
            :else
            (+ v-margin icon-w v-margin))
          (+ h-margin (max text-h icon-h) h-margin))
        (m/defpoint
          (+ v-margin (max text-w icon-w) v-margin)
          (cond
            (and (pos? icon-h) (pos? text-h))
            (+ h-margin icon-h h-margin text-h h-margin)
            (pos? text-h)
            (+ h-margin text-h h-margin)
            :else
            (+ h-margin icon-h h-margin)))))))

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

    :h-margin 0.0625
    :v-margin 0.0625

    :icon-to-text-pos :left
    ;; :exterior- parameters define outer area that is not a part of container where components are layed out
    ;; For example, window is a container but window header is an outer space
    :exterior-top 0
    :exterior-left 0
    :exterior-bottom 0
    :exterior-right 0

;    ;@todo move this one to componentbase
;     :default-properties-to-evolve-provider default-properties-to-evolve-provider
;     :consumes? (fn [cmpnt] true)
      :has-mouse false

    ;; - TODO -
    ;; I had to make this true by default for Focus sample where it does not seem to initialize properly
    ;; and stays false until the first opportunity to evolve. This should not be normally needed.
    :accepts-focus? true

    :focus-traversal-order nil
    :focus-state focus/clean-state

    :layout nil
    :coord-map nil

     :evolvers (array-map

                 :visible visible-evolver
                 :enabled enabled-evolver

                 :has-mouse has-mouse-evolver

                 :content-size default-content-size-evolver

                 :accepts-focus? focus/accepts-focus-evolver
                 :focus-state focus/focus-state-evolver
                 :focus-traversal-order focus/focus-traversal-order-evolver

                 :coord-map layout/coord-map-evolver
                 :clip-size layout/clip-size-evolver
                 :position-matrix layout/position-matrix-evolver

                 :preferred-size preferred-size-evolver

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
