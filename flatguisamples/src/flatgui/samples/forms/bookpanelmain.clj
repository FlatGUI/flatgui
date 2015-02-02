; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns bookpanelmain
  (:import [flatgui.samples.bookpanel FGDataSimulator])
  (:require [flatgui.widgets.api :as w]
            [flatgui.skins.flat]
            [flatgui.appcontainer]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.util.matrix :as m]
            [flatgui.comlogic :as fgc]

            [flatgui.widgets.table.commons :as tcom] ;TODO common table API instead of requiring several namespaces
            [flatgui.widgets.table.header :as th]
            [flatgui.widgets.table.contentpane :as tcpane]
            [flatgui.widgets.table.columnheader :as tcolh]
            [flatgui.samples.forms.preferenceswin :as preferences]
            [flatgui.samples.forms.orderticketwin :as ticket]))

(def ROW_COUNT 500)

(def SIMULATOR (FGDataSimulator. ROW_COUNT))

(def HEADER_IDS [:symbol :side :id :qty :price :time-in-force :last-px :account :client :broker :exchange])

(def HEADER_ALIASES {:symbol "Symbol"
                     :side "Side"
                     :id "ID"
                     :qty "Qty"
                     :price "Px"
                     :time-in-force "TimeInForce"
                     :last-px "LastPx"
                     :account "Account"
                     :client "Client"
                     :broker "Broker"
                     :exchange "Exch"})



;;;
;;; Preferences window
;;;



;
; Order Ticket Window
;



;
; Trade Blotter Window
;

(fg/defevolverfn blotter-table-cs-evolver :clip-size
  (let [ win-size (get-property [] :clip-size)]
    (m/defpoint (- (m/x win-size) 0.25) (- (m/y win-size) 0.5))))

(fg/defevolverfn blotter-background-evolver :background
  (let [dark-theme (get-property [:_ :_ :_ :preferences :dark] :pressed)
        selection (get-property [:this] :selection)]
   (if (nth selection 1)
     (if (nth selection 0)
       (if dark-theme :prime-1 :prime-2)    ;(get-property [:this] :anchor-background)
       (if dark-theme :prime-1 :prime-2)    ;(get-property [:this] :selected-background)
       )
     (let [ref-val (let [ model-row (tcom/get-model-row component)]
                        (if (>= model-row 0)
                          (let [ value-provider (get-property [:_] :value-provider)
                                header-ids (get-property [:_] :header-ids)]
                               (value-provider
                                 model-row
                                 (.indexOf header-ids :side)))))
           ;ref-val nil
           ]
          (if (= :symbol (get-property [:this] :header-id))
            (if dark-theme (awt/color 51 167 167) (awt/color 225 255 255))
            (condp = ref-val
                   "Buy" (if dark-theme (awt/color 167 113 51) (awt/color 255 241 225))
                   "Cover" (if dark-theme (awt/color 167 113 51) (awt/color 255 241 225))
                   "Sell" (if dark-theme (awt/color 51 113 167) (awt/color 225 241 255))
                   "Short" (if dark-theme (awt/color 51 113 167) (awt/color 225 241 255))
                   (get-property [:this] :nonselected-background)))))))


(def blotter-table
  (fg/defcomponent w/table :table { :clip-size (m/defpoint 7.75 5.5 0)
                               :position-matrix (m/transtation-matrix 0.125 0.375)
                               :header-ids HEADER_IDS       ;[:symbol]
                               :header-aliases HEADER_ALIASES
                               :value-provider (fn [model-row model-col]
                                                 ;@todo need acceess to table component here?
                                                 (let [ col-id (name (nth HEADER_IDS model-col))] (.getValue SIMULATOR model-row col-id)))
                               :evolvers { :clip-size blotter-table-cs-evolver
                                           }}
    (th/deftableheader
      (tcolh/defcolumn :symbol [:sorting :filtering :grouping] {:clip-size (m/defpoint 2 tcom/default-row-height)})
      (tcolh/defcolumn :side [:sorting :filtering :grouping] {:clip-size (m/defpoint 2 tcom/default-row-height)})
      (tcolh/defcolumn :id [:sorting])
      (tcolh/defcolumn :qty [:sorting])
      (tcolh/defcolumn :price [:sorting])
      (tcolh/defcolumn :time-in-force [:sorting])
      (tcolh/defcolumn :last-px [:sorting])
      (tcolh/defcolumn :account [:sorting])
      (tcolh/defcolumn :client [:sorting])
      (tcolh/defcolumn :broker [:sorting])
      (tcolh/defcolumn :exchange [:sorting]))
    (tcpane/deftablecontent
      ROW_COUNT
      {:default-cell-component (flatgui.base/merge-properties w/tablecell
                                 {                          ;:foreground :prime-1
                                  :evolvers {:background blotter-background-evolver
                                             }})})))

(def tradeblotter-window (fg/defcomponent w/window :blotter { :clip-size (m/defpoint 8 6 0)
                                :position-matrix (m/transtation-matrix 7.5 0)
                                :text "Trade Blotter"}
                                       blotter-table

                           ))

;
; Container
;

(fg/defevolverfn main-theme-evolver :theme
  (if (get-property [:this :preferences :dark] :pressed)
    flatgui.theme/dark
    flatgui.theme/light))

(fg/defevolverfn main-skin-evolver :skin
  (if (get-property [:this :preferences :oldschool] :pressed)
    "flatgui.skins.oldschool"
    "flatgui.skins.smooth"))

;;;; TODO defapplication marco
(def bookpanel
  (flatgui.widgets.componentbase/initialize
    (fg/defcomponent w/component :main {:clip-size (m/defpoint 25 19 0)
                                   :background (awt/color (float (/ 0 255)) (float (/ 38 255)) (float (/ 70 255)))
                                   :evolvers {:theme main-theme-evolver
                                              ;:skin main-skin-evolver
                                              }}
                  ;debug-window
                  ticket/orderticket-window
                  tradeblotter-window
                  preferences/preferences-window)))
