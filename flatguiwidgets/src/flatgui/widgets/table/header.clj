; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Table widget"
      :author "Denys Lebediev"}
  flatgui.widgets.table.header
  (:require [flatgui.base :as fg]
            [flatgui.util.matrix :as m]
            [flatgui.paint :as fgp]
            [flatgui.awt :as awt]
            [flatgui.comlogic :as fgc]
            [flatgui.widgets.panel]
            [flatgui.widgets.table.commons :as tcom]))

;; TODO move to theme namespace
;;
(fgp/deflookfn header-look (:theme :mouse-down)
  (awt/setColor (:prime-4 theme))
  (awt/fillRect 0 0 w h))

(fg/defevolverfn header-clip-size-evolver :clip-size
  (let [ content-clip-size (get-property component [:content-pane] :clip-size)
         header-h (:default-height component)]
    (m/defpoint (m/x content-clip-size) header-h)))

(fg/defevolverfn header-content-size-evolver :content-size
  (let [ content-content-size (get-property component [:content-pane] :content-size)
         header-h (:default-height component)]
    (m/defpoint (m/x content-content-size) header-h)))

(fg/defaccessorfn find-clicked-no-shift-header-id [component header-ids]
  (first (filter (fn [h-id]
                   (or
                     (true? (get-property component [:this h-id] :clicked-no-shift))
                     ; :sorting is a special case: click to it is equal to click to column header
                     (true? (get-property component [:this h-id :sorting] :clicked-no-shift))))
           header-ids)))
(fg/defaccessorfn find-clicked-with-shift-header-id [component header-ids]
  (first (filter (fn [h-id]
                   (or
                     (true? (get-property component [:this h-id] :clicked-with-shift))
                     ; :sorting is a special case: click to it is equal to click to column header
                     (true? (get-property component [:this h-id :sorting] :clicked-with-shift))))
           header-ids)))


(fg/defevolverfn :active-headers
  (let [ header-ids (get-property component [] :header-ids)
         no-shift (find-clicked-no-shift-header-id component header-ids)
         with-shift (find-clicked-with-shift-header-id component header-ids)
         clicked-header-id (if no-shift no-shift with-shift)
         new-sorting-mode (get-property component [:this clicked-header-id :sorting] :mode)]
    (cond
      (= :none new-sorting-mode) (vec (filter (fn [h-id] (not (= clicked-header-id h-id))) old-active-headers))
      no-shift (vector no-shift)
      (nil? with-shift) old-active-headers
      (some #(= clicked-header-id %1) old-active-headers) old-active-headers
      :else (fgc/conjv old-active-headers clicked-header-id))))

(fg/defevolverfn :column-x-locations
  (let [ header-ids (get-property component [] :header-ids)]
    (into (hash-map) (for [h-id header-ids] [h-id (m/mx-get (get-property component [:header h-id] :position-matrix) 0 3)]))))

(fg/defevolverfn :column-widths
  (let [ header-ids (get-property component [] :header-ids)]
    (into (hash-map) (for [h-id header-ids] [h-id (m/x (get-property component [:header h-id] :clip-size))]))))


(fg/defwidget "tableheader"
  { :default-height tcom/default-row-height
    :position-matrix (m/translation-matrix 0 0 0)
    :active-headers nil
    :fit-width false

    :look header-look

    :evolvers {
                :content-size header-content-size-evolver
                :clip-size header-clip-size-evolver
                ;@todo do not duplicate this code with scrollpane.clj
                :viewport-matrix (fg/accessorfn
                                   (let [h-scrollbar-w (m/x (get-property component [:h-scrollbar] :clip-size))
                                         h-scroller-x (m/mx-get (get-property component [:h-scrollbar :scroller] :position-matrix) 0 3)
                                         h-scroller-w (m/x (get-property component [:h-scrollbar :scroller] :clip-size))
                                         h-scroll-pos (if (== h-scrollbar-w h-scroller-w) 0 (/ h-scroller-x (- h-scrollbar-w h-scroller-w)))
                                         extra-size (m/point-op - (:content-size component) (:clip-size component))]
                                     (m/mx-set m/IDENTITY-MATRIX 0 3 (- (* h-scroll-pos (m/x extra-size))))))
                :active-headers active-headers-evolver

                :column-x-locations column-x-locations-evolver
                :column-widths column-widths-evolver
                }}
  flatgui.widgets.panel/panel)

(defmacro deftableheader [& columns] `(flatgui.base/defcomponent tableheader :header {} ~@columns))

;;; TODO get rid of this
(defmacro deftablefit [& columns] `(flatgui.base/defcomponent tableheader :header {:fit-width true} ~@columns))
