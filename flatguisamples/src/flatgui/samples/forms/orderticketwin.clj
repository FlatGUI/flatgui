; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.orderticketwin
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.comlogic :as fgc]
            [flatgui.widgets.panel :as panel]
            [flatgui.widgets.checkbox :as checkbox]
            [flatgui.widgets.radiobutton :as radiobutton]
            [flatgui.widgets.abstractbutton :as abtn]
            [flatgui.widgets.window :as window]
            [flatgui.widgets.toolbar :as toolbar]
            [flatgui.widgets.label :as label]
            [flatgui.widgets.button :as button]
            [flatgui.widgets.textfield :as textfield]
            [flatgui.widgets.spinner :as spinner]
            [flatgui.widgets.slider :as slider]
            [flatgui.widgets.combobox :as combobox]
            [flatgui.widgets.table :as table]
            [flatgui.widgets.table.commons :as tcom] ;TODO common table API instead of requiring several namespaces
            [flatgui.widgets.table.header :as th]
            [flatgui.widgets.table.contentpane :as tcpane]
            [flatgui.widgets.table.columnheader :as tcolh]
            [flatgui.widgets.table.cell :as tcell])
  (:import (java.text DecimalFormat)
           (flatgui.samples FGDataSimulator)))

(def simulator (FGDataSimulator. 500))

(def bid-columns [:bidsource :bidsize :bidpx])

(def ask-columns [:askpx :asksize :asksource])

(def label-format (DecimalFormat. "###.###"))

(radiobutton/defradiogroupevolver ordtype-group-evolver [:market :limit :stop :stoplmt])

;;; Provides color component for quotes (coloring depending on price)
(fg/defaccessorfn quote-color-ref [component]
  (let [value-provider (get-property component [:_] :value-provider)
        header-ids (get-property component [:_] :header-ids)
        ask-index (.indexOf header-ids :askpx)
        index (if (>= ask-index 0) ask-index (.indexOf header-ids :bidpx))
        row-order (get-property component [] :row-order)
        max-val (value-provider (tcom/screen-row-to-model row-order 0) index)
        min-val (value-provider (tcom/screen-row-to-model row-order (dec (count row-order))) index)]
    (fgc/inrange (/ (- (value-provider (tcom/get-model-row component) index) min-val) (- max-val min-val)) 0.0 1.0)))

(fg/defevolverfn quote-background-evolver :background
  (let [selection (get-property [:this] :selection)
        theme (get-property [:this] :theme)
        dark-theme (get-property [:_ :_ :_ :_ :preferences :dark] :pressed)]
    (if (nth selection 1)
      (if dark-theme :prime-1 :prime-2)
      (let [ref-val (if (< (get-property [:this] :screen-row) 0) 0 (quote-color-ref component))]
        (if dark-theme
          (awt/mix-colors-coeff (:prime-4 theme) (:prime-3 theme) (* 0.5 ref-val))
          (awt/mix-colors-coeff (:prime-3 theme) (:prime-2 theme) ref-val))))))

;;; Value provider for filling ticket fields (regular text fields) with the values of clicked quote
(fg/defaccessorfn quote-val-provider [component reason bid-col-id ask-col-id]
  (case reason
    [:bids :content-pane]
    (let [value-provider (get-property component [:bids] :value-provider)
          header-ids (get-property component [:bids] :header-ids)
          selection-model (get-property component [:bids :content-pane] :selection-model)]
      (if selection-model (value-provider (tcom/get-anchor-model-row selection-model) (.indexOf header-ids bid-col-id))))
    [:asks :content-pane]
    (let [value-provider (get-property component [:asks] :value-provider)
          header-ids (get-property component [:asks] :header-ids)
          selection-model (get-property component [:asks :content-pane] :selection-model)]
      (if selection-model (value-provider (tcom/get-anchor-model-row selection-model) (.indexOf header-ids ask-col-id))))
    nil))

;;; Value provider for filling ticket field (spinner) with the values of clicked quote
(fg/defaccessorfn spinner-quote-val-provider [component reason bid-col-id ask-col-id]
  (case reason
    [:_ :bids :content-pane]
    (let [value-provider (get-property component [:_ :bids] :value-provider)
          header-ids (get-property component [:_ :bids] :header-ids)
          selection-model (get-property component [:_ :bids :content-pane] :selection-model)]
      (if selection-model (value-provider (tcom/get-anchor-model-row selection-model) (.indexOf header-ids bid-col-id))))
    [:_ :asks :content-pane]
    (let [value-provider (get-property component [:_ :asks] :value-provider)
          header-ids (get-property component [:_ :asks] :header-ids)
          selection-model (get-property component [:_ :asks :content-pane] :selection-model)]
      (if selection-model (value-provider (tcom/get-anchor-model-row selection-model) (.indexOf header-ids ask-col-id))))
    nil))

(defn- add-val [s v]
  (.format label-format (+ (Double/valueOf s) v)))

(defn- recreare-text-model [old-model v]
  (let [old-text (:text old-model)
        new-text (add-val (if (.isEmpty old-text) "0" old-text) v)]
    {:text new-text :caret-pos (awt/strlen new-text) :selection-mark 0}))

(fg/defevolverfn qty-evolver :model
  (let [quote-val (quote-val-provider component (fg/get-reason) :bidsize :asksize)]
    (if quote-val
      ;; If some quote is clicked then Qty gets filled from the quote
      {:text (str quote-val) :caret-pos (awt/strlen (str quote-val)) :selection-mark 0}
      (case (fg/get-reason)
        ;; If one toolbar buttons is clicked then Qty is adjusted according to its value
        [:qty-toolbar :+1] (if (abtn/button-pressed? (get-property [:qty-toolbar :+1] :pressed-trigger))
                             (recreare-text-model old-model 1)
                             old-model)
        [:qty-toolbar :+10] (if (abtn/button-pressed? (get-property [:qty-toolbar :+10] :pressed-trigger))
                              (recreare-text-model old-model 10)
                              old-model)
        [:qty-toolbar :+20] (if (abtn/button-pressed? (get-property [:qty-toolbar :+20] :pressed-trigger))
                              (recreare-text-model old-model 20)
                              old-model)
        [:qty-toolbar :+100] (if (abtn/button-pressed? (get-property [:qty-toolbar :+100] :pressed-trigger))
                               (recreare-text-model old-model 100)
                               old-model)
        ;; Default text evolving for all rerular cases (neither quote, nor toolbar buttons)
        (flatgui.widgets.textfield/text-model-evolver component)))))

(fg/defevolverfn px-evolver :model
  (let [quote-val (spinner-quote-val-provider component (fg/get-reason) :bidpx :askpx)]
    (if quote-val
      ;; If quote is clicked, fill editor from quote
      (let [num->str (get-property [:this] :num->str)
            quote-val-str (num->str component quote-val)]
        {:text quote-val-str :caret-pos (awt/strlen quote-val-str) :selection-mark 0})
      (flatgui.widgets.spinner/spinner-model-evovler component))))

(fg/defevolverfn exch-evolver :model
  (let [quote-val (quote-val-provider component (fg/get-reason) :bidsource :asksource)]
    (if quote-val
      ;; If quote is clicked, fill editor from quote
      {:text (str quote-val) :caret-pos (awt/strlen (str quote-val)) :selection-mark 0}
      (flatgui.widgets.textfield/text-model-evolver component))))

;;; If "Follow blotter" mode, fills symbol editor from selected blotter row (blotter is in another window)
(fg/defevolverfn symbol-evolver :model
  (if (and
        (= (fg/get-reason) [:_ :_ :_ :blotter :table :content-pane])
        (get-property [:follow-checkbox] :pressed))
    (let [blotter-value-provider (get-property [:_ :_ :_ :blotter :table] :value-provider)
          blotter-header-ids (get-property [:_ :_ :_ :blotter :table] :header-ids)
          selection-model (get-property [:_ :_ :_ :blotter :table :content-pane] :selection-model)
          symbol (if selection-model
                   (blotter-value-provider (tcom/get-anchor-model-row selection-model) (.indexOf blotter-header-ids :symbol))
                   "")]
      {:text symbol :caret-pos (awt/strlen (str symbol)) :selection-mark 0})
    (flatgui.widgets.textfield/text-model-evolver component)))

(def general-toolbar
  (fg/defcomponent toolbar/toolbar :general-toolbar
    {:position-matrix (m/transtation 0 0)
    :clip-size (m/defpoint 4.5 0.375)}
    (fg/defcomponent label/label :symbol-label
      {:text "Symbol:"
       :h-alignment :right
       :clip-size (m/defpoint 0.75 0.375)
       :position-matrix (m/transtation 0.125 0)})
    (fg/defcomponent textfield/textfield :symbol-entry
      {:clip-size (m/defpoint 0.75 0.3125)
       :position-matrix (m/transtation 0.875 0.03125)
       :evolvers {:model symbol-evolver}})
    (fg/defcomponent checkbox/checkbox :follow-checkbox
      {:clip-size (m/defpoint 1.5 0.25 0)
       :text "Follow blotter"
       :position-matrix (m/transtation 1.75 0.0625)})
    (fg/defcomponent button/checkbutton :link-button
      {:position-matrix (m/transtation 3.375 0.03125)
       :clip-size (m/defpoint 0.75 0.3125)
       :text "Link"})))

(def qty-toolbar
  (fg/defcomponent toolbar/toolbar :qty-toolbar
    {:position-matrix (m/transtation 4.5 0)
     :clip-size (m/defpoint 2.75 0.375)}
    (fg/defcomponent button/rolloverbutton :+1
      {:position-matrix (m/transtation 0.125 0.03125)
       :clip-size (m/defpoint 0.5625 0.3125)
       :text "+1"})
    (fg/defcomponent button/rolloverbutton :+10
      {:position-matrix (m/transtation (+ 0.125 (* 1 0.5625)) 0.03125)
       :clip-size (m/defpoint 0.5625 0.3125)
       :text "+10"})
    (fg/defcomponent button/rolloverbutton :+20
      {:position-matrix (m/transtation (+ 0.125 (* 2 0.5625)) 0.03125)
       :clip-size (m/defpoint 0.5625 0.3125)
       :text "+20"})
    (fg/defcomponent button/rolloverbutton :+100
      {:position-matrix (m/transtation (+ 0.125 (* 3 0.5625)) 0.03125)
       :clip-size (m/defpoint 0.5625 0.3125)
       :text "+100"})))

(def bids-table
  (fg/defcomponent table/table :bids
    {:clip-size (m/defpoint 3.5 2.5)
     :position-matrix (m/transtation 0.125 0.375)
     :header-ids bid-columns
     :header-aliases {:bidsource "BidSrc" :bidsize "BidSize" :bidpx "BidPx"}
     :value-provider (fn [model-row model-col] (let [ col-id (name (nth bid-columns model-col))] (.getValue simulator model-row col-id)))}
    (th/deftablefit
      (tcolh/defcolumn :bidpx [:sorting])
      (tcolh/defcolumn :bidsource [:sorting])
      (tcolh/defcolumn :bidsize [:sorting]))
    (tcpane/deftablecontent 16
      {:selection-mode :single
       :row-height 0.25
       :wheel-rotation-step-y 0.25
       :default-cell-component (flatgui.base/merge-properties tcell/tablecell
                                 {:foreground (awt/color 0 0 0)
                                  :evolvers {:background quote-background-evolver}})})))

(def asks-table
  (fg/defcomponent table/table :asks
    {:clip-size (m/defpoint 3.5 2.5)
     :position-matrix (m/transtation 3.625 0.375)
     :header-ids ask-columns
     :header-aliases {:asksource "AskSrc" :asksize "AskSize" :askpx "AskPx"}
     :value-provider (fn [model-row model-col] (let [ col-id (name (nth ask-columns model-col))] (.getValue simulator model-row col-id)))}
    (th/deftablefit
      (tcolh/defcolumn :asksource [:sorting])
      (tcolh/defcolumn :asksize [:sorting])
      (tcolh/defcolumn :askpx [:sorting]))
    (tcpane/deftablecontent 16
      {:selection-mode :single
       :row-height 0.25
       :wheel-rotation-step-y 0.25
       :default-cell-component (flatgui.base/merge-properties tcell/tablecell
                                 {:foreground (awt/color 0 0 0)
                                  :evolvers {:background quote-background-evolver}})})))

(def orderticket-window
  (fg/defcomponent window/window :tiket
    {:clip-size (m/defpoint 7.25 7 0)
     :position-matrix (m/transtation 0 0)
     :text "Order Ticket"}

    (fg/defcomponent panel/panel :ticket-panel
      {:clip-size (m/defpoint 7.25 6.625 0)
       :position-matrix (m/transtation 0 0.375)
       :look (fn [c r] [])}

    general-toolbar

    qty-toolbar

    bids-table

    asks-table

    (fg/defcomponent label/label :qty-label
      {:text "Qty:"
       :h-alignment :right
       :clip-size (m/defpoint 0.5 0.375 0)
       :position-matrix (m/transtation 0.125 3.0)})

    (fg/defcomponent textfield/textfield :qty-entry
      {:text-supplier textfield/textfield-num-only-text-suplier
       :clip-size (m/defpoint 1.0 0.375 0)
       :position-matrix (m/transtation 0.625 3.0)
       :evolvers {:model qty-evolver}})

    (fg/defcomponent label/label :px-label
      {:text "Px:"
       :h-alignment :right
       :clip-size (m/defpoint 0.5 0.375 0)
       :position-matrix (m/transtation 1.625 3.0)})

    (fg/defcomponent spinner/spinner :px-entry
      {:clip-size (m/defpoint 1.375 0.375 0)
       :step 0.01
       :position-matrix (m/transtation 2.125 3.0)}
      (fg/defcomponent spinner/spinnereditor :editor {:evolvers {:model px-evolver}}))

    (fg/defcomponent label/label :stgy-label
      {:text "Strategy:"
       :h-alignment :right
       :clip-size (m/defpoint 1.375 0.375 0)
       :position-matrix (m/transtation 3.125 3.0)})

    (fg/defcomponent combobox/combobox :stgy-entry
      {:model ["WVAP" "TWAP" "With Volume" "Dynamic" "Custom"]
       :clip-size (m/defpoint 1.375 0.375 0)
       :position-matrix (m/transtation 4.5 3.0)})

    (fg/defcomponent textfield/textfield :exch-entry
      {:clip-size (m/defpoint 1.0 0.375 0)
       :position-matrix (m/transtation 6.0 3.0)
       :evolvers {:model exch-evolver}})

    (fg/defcomponent label/label :aggr-label
      {:text "Aggressiveness:"
       :clip-size (m/defpoint 1.5 0.375 0)
       :position-matrix (m/transtation 0.125 3.5)})

    (fg/defcomponent slider/slider :aggr-slider
      {:clip-size (m/defpoint 3.0 0.5 0)
       :position-matrix (m/transtation 1.75 3.5)})

    (fg/defcomponent label/label :type-label
      {:text "OrdType:"
       :clip-size (m/defpoint 1.0 0.375 0)
       :position-matrix (m/transtation 0.125 4.25)})

    (fg/defcomponent radiobutton/radiobutton :market
      {:text "Market:"
       :clip-size (m/defpoint 1.25 0.25 0)
       :position-matrix (m/transtation 1.125 4.25)
       :evolvers {:pressed ordtype-group-evolver}})

    (fg/defcomponent radiobutton/radiobutton :limit
      {:text "Limit:"
       :clip-size (m/defpoint 1.25 0.25 0)
       :position-matrix (m/transtation 1.125 4.5625)
       :evolvers {:pressed ordtype-group-evolver}})

    (fg/defcomponent radiobutton/radiobutton :stop
      {:text "Stop:"
       :clip-size (m/defpoint 1.25 0.25 0)
       :position-matrix (m/transtation 1.125 4.875)
       :evolvers {:pressed ordtype-group-evolver}})

    (fg/defcomponent radiobutton/radiobutton :stoplmt
      {:text "Stop lmt:"
       :clip-size (m/defpoint 1.25 0.25 0)
       :position-matrix (m/transtation 1.125 5.1875)
       :evolvers {:pressed ordtype-group-evolver}})

    (fg/defcomponent label/label :tif-label
      {:text "TimeInForce:"
       :h-alignment :right
       :clip-size (m/defpoint 1.375 0.375 0)
       :position-matrix (m/transtation 2.375 4.25)})

    (fg/defcomponent combobox/combobox :tif-entry
      {:model ["Day", "GTC", "OPG", "IOC", "FOK", "GTX", "GTD"]
       :clip-size (m/defpoint 1.375 0.375 0)
       :position-matrix (m/transtation 3.75 4.25)})

    (fg/defcomponent label/label :text-label
      {:text "Text:"
       :h-alignment :right
       :clip-size (m/defpoint 0.75 0.375 0)
       :position-matrix (m/transtation 5.125 4.25)})

    (fg/defcomponent textfield/textfield :text-entry
      {:clip-size (m/defpoint 1.0 0.375 0)
       :position-matrix (m/transtation 5.875 4.25)})

    (fg/defcomponent button/button :buy
      {:position-matrix (m/transtation 0.125 6.125)
       :clip-size (m/defpoint 1.0 0.375)
       :text "Buy"})

    (fg/defcomponent button/button :sell
      {:position-matrix (m/transtation 6.125 6.125)
       :clip-size (m/defpoint 1.0 0.375)
       :text "Sell"}))))
