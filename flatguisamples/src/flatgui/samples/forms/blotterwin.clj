; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.blotterwin
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.table :as table]
            [flatgui.widgets.table.commons :as tcom] ;TODO common table API instead of requiring several namespaces
            [flatgui.widgets.table.header :as th]
            [flatgui.widgets.table.contentpane :as tcpane]
            [flatgui.widgets.table.columnheader :as tcolh]
            [flatgui.widgets.table.cell :as tcell])
  (:import (flatgui.samples FGDataSimulator)))

(def row-count 500)

(def simulator (FGDataSimulator. row-count))

(def header-ids [:symbol :side :id :qty :price :time-in-force :last-px :account :client :broker :exchange])

(def header-aliases
  {:symbol "Symbol"
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

;;; This clip size evolver for blotter table
;;; resizes table together with window resize
(fg/defevolverfn blotter-table-cs-evolver :clip-size
                 (let [ win-size (get-property [] :clip-size)]
                   (m/defpoint (- (m/x win-size) 0.25) (- (m/y win-size) 0.5))))

;;; This evolver for blotter table provides coloring per Side and also
;;; keeps special selection color for selected rows
(fg/defevolverfn blotter-background-evolver :background
  (let [dark-theme (= (get-property component [:this] :theme) flatgui.theme/dark)
        selection (get-property [:this] :selection)]
    (if (nth selection 1) ;TODO more self-documenting approach for selection model
      (if (nth selection 0)
        ;; Anchor selection
        (if dark-theme :prime-1 :prime-2)
        ;; Regular selection
        (if dark-theme :prime-1 :prime-2))
      ;; If there is no selection then let ref-val be the Side value of table row
      (let [ref-val (let [model-row (tcom/get-model-row component)]
                      ;; There is no guarantee that table cell has a row
                      ;; assigned to it so need to check explicitly
                      (if (>= model-row 0)
                        ;; If it has then take table's value provider, and check the
                        ;; Side value in given table row
                        (let [value-provider (get-property [:_] :value-provider)
                              header-ids (get-property [:_] :header-ids)]
                          (value-provider
                            model-row
                            (.indexOf header-ids :side)))))]
        (if (= :symbol (get-property [:this] :header-id))
          ;; Have special color for Symbol column
          (if dark-theme (awt/color 51 167 167) (awt/color 225 255 255))
          ;; And have a color per Side value for all other columns
          (condp = ref-val
            "Buy" (if dark-theme (awt/color 167 113 51) (awt/color 255 241 225))
            "Cover" (if dark-theme (awt/color 167 113 51) (awt/color 255 241 225))
            "Sell" (if dark-theme (awt/color 51 113 167) (awt/color 225 241 255))
            "Short" (if dark-theme (awt/color 51 113 167) (awt/color 225 241 255))
            ;; Just in case there is some unknown Side value, have default color for it
            (get-property [:this] :nonselected-background)))))))

(def blotter-table
  (fg/defcomponent table/table :table
    {:clip-size (m/defpoint 7.75 5.5 0)
     :position-matrix (m/transtation 0.125 0.375)
     :header-ids header-ids
     :header-aliases header-aliases
     :value-provider (fn [model-row model-col]
                       ;;TODO need acceess to table component here?
                       (let [col-id (name (nth header-ids model-col))]
                         (.getValue simulator model-row col-id)))
     :evolvers {:clip-size blotter-table-cs-evolver}}
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
    row-count
    {:default-cell-component (flatgui.base/merge-properties tcell/tablecell
                               {:evolvers {:background blotter-background-evolver}})})))

(def tradeblotter-window
  (fg/defcomponent window/window :blotter
    {:clip-size (m/defpoint 8 6 0)
     :position-matrix (m/transtation 7.5 0)
     :text "Trade Blotter"}
    blotter-table))