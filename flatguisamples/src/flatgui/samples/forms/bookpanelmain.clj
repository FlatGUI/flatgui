; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns bookpanelmain
  (:import [flatgui.samples.bookpanel FGDataSimulator]
           (java.text DecimalFormat))
  (:require flatgui.skins.flat flatgui.appcontainer)
  (:use flatgui.comlogic
        flatgui.base
        flatgui.dependency
        flatgui.widgets.componentbase
        flatgui.widgets.component
        flatgui.widgets.label
        flatgui.widgets.panel
        flatgui.widgets.scrollpanel
        flatgui.widgets.table.commons
        flatgui.widgets.table
        flatgui.widgets.table.cell
        flatgui.widgets.table.contentpane
        flatgui.widgets.table.header
        flatgui.widgets.table.columnheader
        flatgui.widgets.table.vfc
        flatgui.widgets.table.vfcsorting
        flatgui.widgets.table.vfcfiltering
        flatgui.widgets.table.vfcgrouping
        flatgui.widgets.floatingbar
        flatgui.widgets.window
        flatgui.widgets.toolbar
        flatgui.widgets.abstractbutton
        flatgui.widgets.button
        flatgui.widgets.textfield
        flatgui.widgets.slider
        flatgui.widgets.checkbox
        flatgui.widgets.radiobutton
        flatgui.widgets.spinner
        flatgui.widgets.abstractmenu
        flatgui.widgets.menu
        flatgui.widgets.combobox
        flatgui.inputchannels.mouse
        flatgui.util.circularbuffer
        flatgui.util.matrix)
  (:require [flatgui.awt :as awt]))

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



;
; Evolvers for Debug Window
;

(def label-format (DecimalFormat. "###.###"))

(defevolverfn s1text :text
  (let [ pos (get-property component [:slider1] :position)]
    (.format label-format pos)))

(defevolverfn s2text :text
  (let [ pos (get-property component [:slider2] :position)]
    (.format label-format pos)))

; @todo
; super-evovler marco that would take evolver from parent type.
; Will have to keep the list of parent types in component.
; Throw exception if more than one found.
; One more way to call this macro - specifying exactly which type
; to take evovler from

(defevolverfn s1pos :position
  (if (and
        (#{[:slider2] [:lock-checkbox]} (get-reason))
        (get-property component [:lock-checkbox] :pressed))
    (get-property component [:slider2] :position)
    (flatgui.widgets.slider/slider-position-evolver component)))

(defevolverfn s2pos :position
  (if (and
        (#{[:slider1] [:lock-checkbox]} (get-reason))
        (get-property component [:lock-checkbox] :pressed))
    (get-property component [:slider1] :position)
    (flatgui.widgets.slider/slider-position-evolver component)))

;
; Debug Window
;

(def debug-window (defcomponent window :debug {:clip-size (defpoint 3.5 5.5)
                                               :position-matrix (transtation-matrix 10 7)
                                               :text "Debug Panel"}

  (defcomponent label :s1-text
    { :text "Slider 1:"
      :clip-size (defpoint 1.0 0.5 0)
      :position-matrix (transtation-matrix 0.25 0.5)})

  (defcomponent label :s2-text
    { :text "Slider 2:"
      :clip-size (defpoint 1.0 0.5 0)
      :position-matrix (transtation-matrix 0.25 1.0)})

  (defcomponent label :s1-label
    { :clip-size (defpoint 3 0.5 0)
      :h-alignment :left
      :position-matrix (transtation-matrix 1.28125 0.5)
      :evolvers {:text s1text}})

  (defcomponent label :s2-label
    { :clip-size (defpoint 3 0.5 0)
      :h-alignment :left
      :position-matrix (transtation-matrix 1.28125 1.0)
      :evolvers {:text s2text}})


  (defcomponent checkbox :lock-checkbox { :clip-size (defpoint 1.5 0.25 0)
                                          :text "Lock sliders"
                                          :position-matrix (transtation-matrix 0.25 1.75)})

  (defcomponent slider :slider1 { :clip-size (defpoint 0.5 3.0 0)
                                  :orientation :vertical
                                  :position-matrix (transtation-matrix 0.25 2.25)
                                  :evolvers {:position s1pos}})

  (defcomponent slider :slider2 { :clip-size (defpoint 0.5 3.0 0)
                                  :orientation :vertical
                                  :position-matrix (transtation-matrix 1.0 2.25)
                                  :evolvers {:position s2pos}})

  (defcomponent spinner :spn { :clip-size (defpoint 1.375 0.375 0)
                               :position-matrix (transtation-matrix 1.625 2.25)})

;  (defcomponent menu :ctx { :clip-size (defpoint 4 6 0)
;                            :position-matrix (transtation-matrix 3.625 2.75)})
                    ))

;;
;; Preferences window
;;

(defradiogroupevolver skin-group [:some-other :flat])

(defradiogroupevolver theme-group [:dark :light])

(def preferences-window
  (defcomponent window :preferences
    {:clip-size (defpoint 3.75 1.375)
     :position-matrix (transtation-matrix 10 7)
     :text "Preferences"}

    ;(defcomponent label :skin-label
    ;  {:text "Skin:"
    ;   :h-alignment :right
    ;   :clip-size (defpoint 0.75 0.25)
    ;   :position-matrix (transtation-matrix 0.125 0.875)})
    ;
    ;(defcomponent radiobutton :oldschool { :text "Oldschool"
    ;                                      :pressed true
    ;                                   :clip-size (defpoint 1.25 0.25)
    ;                                   :position-matrix (transtation-matrix 1.0 0.875)
    ;                                   :evolvers {:pressed skin-group}})
    ;
    ;(defcomponent radiobutton :smooth { :text "Smooth"
    ;                                  :clip-size (defpoint 1.0 0.25)
    ;                                  :position-matrix (transtation-matrix 2.375 0.875)
    ;                                  :evolvers {:pressed skin-group}})

    (defcomponent label :theme-label
      {:text "Theme:"
       :h-alignment :right
       :clip-size (defpoint 0.75 0.25)
       :position-matrix (transtation-matrix 0.125 0.5)})

    (defcomponent radiobutton :dark { :text "Dark"
                                     :pressed true
                                          :clip-size (defpoint 1.25 0.25)
                                          :position-matrix (transtation-matrix 1.0 0.5)
                                          :evolvers {:pressed theme-group}})

    (defcomponent radiobutton :light { :text "Light"
                                       :clip-size (defpoint 1.0 0.25)
                                       :position-matrix (transtation-matrix 1.875 0.5)
                                       :evolvers {:pressed theme-group}})))


;
; Order Ticket Window
;

(def bid-columns [:bidsource :bidsize :bidpx])
(def ask-columns [:askpx :asksize :asksource])

(defaccessorfn quote-color-ref [component]
 (let [value-provider (get-property component [:_] :value-provider)
       header-ids (get-property component [:_] :header-ids)
       ask-index (.indexOf header-ids :askpx)
       index (if (>= ask-index 0) ask-index (.indexOf header-ids :bidpx))
       row-order (get-property component [] :row-order)
       max-val (value-provider (screen-row-to-model row-order 0) index)
       min-val (value-provider (screen-row-to-model row-order (dec (count row-order))) index)]
   (inrange (/ (- (value-provider (get-model-row component) index) min-val) (- max-val min-val)) 0.0 1.0)))

(defevolverfn quote-background-evolver :background
  (let [selection (get-property component [:this] :selection)
        theme (get-property component [:this] :theme)
        dark-theme (get-property component [:_ :_ :_ :_ :preferences :dark] :pressed)]
    (if (nth selection 1)
      (if dark-theme :prime-1 :prime-2);(get-property component [:this] :selected-background)
      (let [ref-val (if (< (get-property component [:this] :screen-row) 0) 0 (quote-color-ref component))
            ;color-val (int (+ 60 (* ref-val 108)))
            ]
           ;(awt/color color-val color-val 168)
           (if dark-theme
             (awt/mix-colors-coeff (:prime-4 theme) (:prime-3 theme) (* 0.5 ref-val))
             (awt/mix-colors-coeff (:prime-3 theme) (:prime-2 theme) ref-val))
           ))))

(defradiogroupevolver ordtype-group-evolver [:market :limit :stop :stoplmt])

;; TODO use macros for text from quote evolvers

(defaccessorfn quote-val-provider [component reason bid-col-id ask-col-id]
  (case reason
     [:bids :content-pane]
     (let [value-provider (get-property component [:bids] :value-provider)
           header-ids (get-property component [:bids] :header-ids)
           selection-model (get-property component [:bids :content-pane] :selection-model)]
       (if selection-model (value-provider (get-anchor-model-row selection-model) (.indexOf header-ids bid-col-id))))
     [:asks :content-pane]
     (let [value-provider (get-property component [:asks] :value-provider)
           header-ids (get-property component [:asks] :header-ids)
           selection-model (get-property component [:asks :content-pane] :selection-model)]
       (if selection-model (value-provider (get-anchor-model-row selection-model) (.indexOf header-ids ask-col-id))))
     nil))

(defaccessorfn spinner-quote-val-provider [component reason bid-col-id ask-col-id]
  (case reason
    [:_ :bids :content-pane]
    (let [value-provider (get-property component [:_ :bids] :value-provider)
          header-ids (get-property component [:_ :bids] :header-ids)
          selection-model (get-property component [:_ :bids :content-pane] :selection-model)]
         (if selection-model (value-provider (get-anchor-model-row selection-model) (.indexOf header-ids bid-col-id))))
    [:_ :asks :content-pane]
    (let [value-provider (get-property component [:_ :asks] :value-provider)
          header-ids (get-property component [:_ :asks] :header-ids)
          selection-model (get-property component [:_ :asks :content-pane] :selection-model)]
         (if selection-model (value-provider (get-anchor-model-row selection-model) (.indexOf header-ids ask-col-id))))
    nil))

(defn- add-val [s v]
  (.format label-format (+ (Double/valueOf s) v)))

(defn- recreare-text-model [old-model v]
  (let [old-text (:text old-model)
        new-text (add-val (if (.isEmpty old-text) "0" old-text) v)]
    {:text new-text :caret-pos (awt/strlen new-text) :selection-mark 0}))

(defevolverfn qty-evolver :model
  (let [quote-val (quote-val-provider component (get-reason) :bidsize :asksize)]
    (if quote-val
      {:text (str quote-val) :caret-pos (awt/strlen (str quote-val)) :selection-mark 0}
      (case (get-reason)
        [:qty-toolbar :+1] (if (button-pressed? (get-property component [:qty-toolbar :+1] :pressed-trigger))
                             (recreare-text-model old-model 1)
                             old-model)
        [:qty-toolbar :+10] (if (button-pressed? (get-property component [:qty-toolbar :+10] :pressed-trigger))
                              (recreare-text-model old-model 10)
                              old-model)
        [:qty-toolbar :+20] (if (button-pressed? (get-property component [:qty-toolbar :+20] :pressed-trigger))
                              (recreare-text-model old-model 20)
                              old-model)
        [:qty-toolbar :+100] (if (button-pressed? (get-property component [:qty-toolbar :+100] :pressed-trigger))
                               (recreare-text-model old-model 100)
                               old-model)
        (flatgui.widgets.textfield/text-model-evolver component)))))

(defevolverfn px-evolver :model
  (let [quote-val (spinner-quote-val-provider component (get-reason) :bidpx :askpx)]
    (if quote-val
      (let [num->str (get-property component [:this] :num->str)
            quote-val-str (num->str component quote-val)]
        {:text quote-val-str :caret-pos (awt/strlen quote-val-str) :selection-mark 0})
      (flatgui.widgets.spinner/spinner-model-evovler component))))

(defevolverfn exch-evolver :model
  (let [quote-val (quote-val-provider component (get-reason) :bidsource :asksource)]
    (if quote-val
      {:text (str quote-val) :caret-pos (awt/strlen (str quote-val)) :selection-mark 0}
      (flatgui.widgets.textfield/text-model-evolver component))))

(defevolverfn symbol-evolver :model
  (if (and
        (= (get-reason) [:_ :_ :_ :blotter :table :content-pane])
        (get-property component [:follow-checkbox] :pressed))
    (let [blotter-value-provider (get-property component [:_ :_ :_ :blotter :table] :value-provider)
          blotter-header-ids (get-property component [:_ :_ :_ :blotter :table] :header-ids)
          selection-model (get-property component [:_ :_ :_ :blotter :table :content-pane] :selection-model)
          symbol (if selection-model
                   (blotter-value-provider (get-anchor-model-row selection-model) (.indexOf blotter-header-ids :symbol))
                   "")]
      {:text symbol :caret-pos (awt/strlen (str symbol)) :selection-mark 0})
    (flatgui.widgets.textfield/text-model-evolver component)))

(def orderticket-window (defcomponent window :tiket { :clip-size (defpoint 7.25 7 0) :position-matrix (transtation-matrix 0 0) :text "Order Ticket"}

  ;TODO remove this when borders are implemented
  (defcomponent panel :ticket-panel { :clip-size (defpoint 7.25 6.625 0)
                                      :position-matrix (transtation-matrix 0 0.375)
                                      :look (fn [c r] [])}

    (defcomponent toolbar :general-toolbar {:position-matrix (transtation-matrix 0 0)
                                            :clip-size (defpoint 4.5 0.375)}
      (defcomponent label :symbol-label
        {:text "Symbol:"
         :h-alignment :right
         :clip-size (defpoint 0.75 0.375)
         :position-matrix (transtation-matrix 0.125 0)})
      (defcomponent textfield :symbol-entry
        {:clip-size (defpoint 0.75 0.3125)
         :position-matrix (transtation-matrix 0.875 0.03125)
         :evolvers {:model symbol-evolver}})
      (defcomponent checkbox :follow-checkbox
        {:clip-size (defpoint 1.5 0.25 0)
         :text "Follow blotter"
         :position-matrix (transtation-matrix 1.75 0.0625)})
      (defcomponent checkbutton :link-button
        {:position-matrix (transtation-matrix 3.375 0.03125)
         :clip-size (defpoint 0.75 0.3125)
         :text "Link"}))


    (defcomponent toolbar :qty-toolbar {:position-matrix (transtation-matrix 4.5 0)
                                        :clip-size (defpoint 2.75 0.375)}
      (defcomponent rolloverbutton :+1
        {:position-matrix (transtation-matrix 0.125 0.03125)
         :clip-size (defpoint 0.5625 0.3125)
         :text "+1"})
      (defcomponent rolloverbutton :+10
        {:position-matrix (transtation-matrix (+ 0.125 (* 1 0.5625)) 0.03125)
         :clip-size (defpoint 0.5625 0.3125)
         :text "+10"})
      (defcomponent rolloverbutton :+20
        {:position-matrix (transtation-matrix (+ 0.125 (* 2 0.5625)) 0.03125)
         :clip-size (defpoint 0.5625 0.3125)
         :text "+20"})
      (defcomponent rolloverbutton :+100
        {:position-matrix (transtation-matrix (+ 0.125 (* 3 0.5625)) 0.03125)
         :clip-size (defpoint 0.5625 0.3125)
         :text "+100"}))


    (defcomponent table :bids { :clip-size (defpoint 3.5 2.5)
                               :position-matrix (transtation-matrix 0.125 0.375)
                               :header-ids bid-columns
                               :header-aliases {:bidsource "BidSrc" :bidsize "BidSize" :bidpx "BidPx"}
                               ;:value-provider (fn [model-row model-col] (str model-row model-col))
                               :value-provider (fn [model-row model-col]
                                                   (let [ col-id (name (nth bid-columns model-col))] (.getValue SIMULATOR model-row col-id)))
                               }
                  (deftablefit

                    ; TODO this does not work, it gets reset on initialization
                    ;(merge-properties (defcolumn :bidpx [:sorting]) {:children {:sorting {:degree 0 :mode :desc}}})
                    (defcolumn :bidpx [:sorting])

                    (defcolumn :bidsource [:sorting])
                    (defcolumn :bidsize [:sorting]))
                  (deftablecontent 16 {:selection-mode :single
                                       :row-height 0.25
                                       :wheel-rotation-step-y 0.25
                                       :default-cell-component (flatgui.base/merge-properties tablecell {:foreground (awt/color 0 0 0)
                                                                                                         :evolvers {:background quote-background-evolver}})}))


    (defcomponent table :asks {:clip-size (defpoint 3.5 2.5)
                               :position-matrix (transtation-matrix 3.625 0.375)
                               :header-ids ask-columns
                               :header-aliases {:asksource "AskSrc" :asksize "AskSize" :askpx "AskPx"}
                               ;:value-provider (fn [model-row model-col] (str model-row model-col))
                               :value-provider (fn [model-row model-col]
                                                 (let [ col-id (name (nth ask-columns model-col))] (.getValue SIMULATOR model-row col-id)))
                               }
      (deftablefit
        (defcolumn :asksource [:sorting])
        (defcolumn :asksize [:sorting])
        (defcolumn :askpx [:sorting]))
      (deftablecontent 16 {:selection-mode :single
                           :row-height 0.25
                           :wheel-rotation-step-y 0.25
                           :default-cell-component (flatgui.base/merge-properties tablecell {:foreground (awt/color 0 0 0)
                                                                                             :evolvers {:background quote-background-evolver}})}))


    (defcomponent label :qty-label { :text "Qty:"
                                     :h-alignment :right
                                     :clip-size (defpoint 0.5 0.375 0)
                                     :position-matrix (transtation-matrix 0.125 3.0)})

    (defcomponent textfield :qty-entry {:text-supplier textfield-num-only-text-suplier
                                        :clip-size (defpoint 1.0 0.375 0)
                                        :position-matrix (transtation-matrix 0.625 3.0)
                                        :evolvers {:model qty-evolver}})

    (defcomponent label :px-label { :text "Px:"
                                    :h-alignment :right
                                    :clip-size (defpoint 0.5 0.375 0)
                                    :position-matrix (transtation-matrix 1.625 3.0)})

    (defcomponent spinner :px-entry { :clip-size (defpoint 1.375 0.375 0)
                                     :step 0.01
                                     :position-matrix (transtation-matrix 2.125 3.0)
                                     :children {:editor (defcomponent spinnereditor :editor {:evolvers {:model px-evolver}})}
                                     })

    (defcomponent label :stgy-label {:text "Strategy:"
                                     :h-alignment :right
                                     :clip-size (defpoint 1.375 0.375 0)
                                     :position-matrix (transtation-matrix 3.125 3.0)})

    (defcomponent combobox :stgy-entry {:model ["WVAP" "TWAP" "With Volume" "Dynamic" "Custom"]
                                        :clip-size (defpoint 1.375 0.375 0)
                                        :position-matrix (transtation-matrix 4.5 3.0)})

    (defcomponent textfield :exch-entry {:clip-size (defpoint 1.0 0.375 0)
                                         :position-matrix (transtation-matrix 6.0 3.0)
                                         :evolvers {:model exch-evolver}
                                         })

    (defcomponent label :aggr-label { :text "Aggressiveness:"
                                      :clip-size (defpoint 1.5 0.375 0)
                                      :position-matrix (transtation-matrix 0.125 3.5)})

    (defcomponent slider :aggr-slider { :clip-size (defpoint 3.0 0.5 0)
                                        :position-matrix (transtation-matrix 1.75 3.5)})

    (defcomponent label :type-label { :text "OrdType:"
                                      :clip-size (defpoint 1.0 0.375 0)
                                      :position-matrix (transtation-matrix 0.125 4.25)})

    (defcomponent radiobutton :market { :text "Market:"
                                        :clip-size (defpoint 1.25 0.25 0)
                                        :position-matrix (transtation-matrix 1.125 4.25)
                                        :evolvers {:pressed ordtype-group-evolver}})
    (defcomponent radiobutton :limit { :text "Limit:"
                                       :clip-size (defpoint 1.25 0.25 0)
                                       :position-matrix (transtation-matrix 1.125 4.5625)
                                       :evolvers {:pressed ordtype-group-evolver}})
    (defcomponent radiobutton :stop { :text "Stop:"
                                      :clip-size (defpoint 1.25 0.25 0)
                                      :position-matrix (transtation-matrix 1.125 4.875)
                                      :evolvers {:pressed ordtype-group-evolver}})
    (defcomponent radiobutton :stoplmt { :text "Stop lmt:"
                                         :clip-size (defpoint 1.25 0.25 0)
                                         :position-matrix (transtation-matrix 1.125 5.1875)
                                         :evolvers {:pressed ordtype-group-evolver}})

    (defcomponent label :tif-label { :text "TimeInForce:"
                                     :h-alignment :right
                                     :clip-size (defpoint 1.375 0.375 0)
                                     :position-matrix (transtation-matrix 2.375 4.25)})

    (defcomponent combobox :tif-entry { :model ["Day", "GTC", "OPG", "IOC", "FOK", "GTX", "GTD"]
                                        :clip-size (defpoint 1.375 0.375 0)
                                        :position-matrix (transtation-matrix 3.75 4.25)})

    ;(defcomponent label :text-label { :text "Text:"
    ;                                  :h-alignment :right
    ;                                  :clip-size (defpoint 1.375 0.375 0)
    ;                                  :position-matrix (transtation-matrix 2.375 4.75)})
    ;
    ;(defcomponent textfield :text-entry { :clip-size (defpoint 1.375 0.375 0)
    ;                                     :position-matrix (transtation-matrix 3.75 4.75)})

    (defcomponent label :text-label { :text "Text:"
                                     :h-alignment :right
                                     :clip-size (defpoint 0.75 0.375 0)
                                     :position-matrix (transtation-matrix 5.125 4.25)})

    (defcomponent textfield :text-entry {:clip-size (defpoint 1.0 0.375 0)
                                         :position-matrix (transtation-matrix 5.875 4.25)})


    ;(defcomponent label :exch-label { :text "Exch:"
    ;                                 :h-alignment :right
    ;                                 :clip-size (defpoint 0.75 0.375 0)
    ;                                 :position-matrix (transtation-matrix 5.125 4.25)})
    ;
    ;(defcomponent textfield :exch-entry {:clip-size (defpoint 1.0 0.375 0)
    ;                                     :position-matrix (transtation-matrix 5.875 4.25)
    ;                                     :evolvers {:model exch-evolver}
    ;                                     })

    (defcomponent button :buy { :position-matrix (transtation-matrix 0.125 6.125)
                                :clip-size (defpoint 1.0 0.375)
                                :text "Buy"})

    (defcomponent button :sell { :position-matrix (transtation-matrix 6.125 6.125)
                                 :clip-size (defpoint 1.0 0.375)
                                 :text "Sell"})

;    (defcomponent menu :ctx-menu { :value-provider (fn [model-row model-col] (str model-row "-" model-col))
;                                   :clip-size (defpoint 3 5 0)
;                                   :position-matrix (transtation-matrix 2.5 3.75)}
;      (defcomponent menucontentpane :content-pane
;        {
;          :row-count 8
;          :row-order (vec (range 0 8))
;          }))


    )))


;
; Trade Blotter Window
;

(defevolverfn blotter-table-cs-evolver :clip-size
  (let [ win-size (get-property component [] :clip-size)]
    (defpoint (- (x win-size) 0.25) (- (y win-size) 0.5))))

(defevolverfn blotter-background-evolver :background
  (let [dark-theme (get-property component [:_ :_ :_ :preferences :dark] :pressed)
        selection (get-property component [:this] :selection)]
   (if (nth selection 1)
     (if (nth selection 0)
       (if dark-theme :prime-1 :prime-2)    ;(get-property component [:this] :anchor-background)
       (if dark-theme :prime-1 :prime-2)    ;(get-property component [:this] :selected-background)
       )
     (let [ref-val (let [ model-row (get-model-row component)]
                        (if (>= model-row 0)
                          (let [ value-provider (get-property component [:_] :value-provider)
                                header-ids (get-property component [:_] :header-ids)]
                               (value-provider
                                 model-row
                                 (.indexOf header-ids :side)))))
           ;ref-val nil
           ]
          (if (= :symbol (get-property component [:this] :header-id))
            (if dark-theme (awt/color 51 167 167) (awt/color 225 255 255))
            (condp = ref-val
                   "Buy" (if dark-theme (awt/color 167 113 51) (awt/color 255 241 225))
                   "Cover" (if dark-theme (awt/color 167 113 51) (awt/color 255 241 225))
                   "Sell" (if dark-theme (awt/color 51 113 167) (awt/color 225 241 255))
                   "Short" (if dark-theme (awt/color 51 113 167) (awt/color 225 241 255))
                   (get-property component [:this] :nonselected-background)))))))


(def blotter-table
  (defcomponent table :table { :clip-size (defpoint 7.75 5.5 0)
                               :position-matrix (transtation-matrix 0.125 0.375)
                               :header-ids HEADER_IDS       ;[:symbol]
                               :header-aliases HEADER_ALIASES
                               :value-provider (fn [model-row model-col]
                                                 ;@todo need acceess to table component here?
                                                 (let [ col-id (name (nth HEADER_IDS model-col))] (.getValue SIMULATOR model-row col-id)))
                               :evolvers { :clip-size blotter-table-cs-evolver
                                           }}
    (deftableheader
      (defcolumn :symbol [:sorting :filtering :grouping] {:clip-size (defpoint 2 DFLT_ROW_HEIGHT)})
      (defcolumn :side [:sorting :filtering :grouping] {:clip-size (defpoint 2 DFLT_ROW_HEIGHT)})
      (defcolumn :id [:sorting])
      (defcolumn :qty [:sorting])
      (defcolumn :price [:sorting])
      (defcolumn :time-in-force [:sorting])
      (defcolumn :last-px [:sorting])
      (defcolumn :account [:sorting])
      (defcolumn :client [:sorting])
      (defcolumn :broker [:sorting])
      (defcolumn :exchange [:sorting]))
    (deftablecontent
      ROW_COUNT
      {:default-cell-component (flatgui.base/merge-properties tablecell
                                 {                          ;:foreground :prime-1
                                  :evolvers {:background blotter-background-evolver
                                             }})})))

(def tradeblotter-window (defcomponent window :blotter { :clip-size (defpoint 8 6 0)
                                :position-matrix (transtation-matrix 7.5 0)
                                :text "Trade Blotter"}
                                       blotter-table

                           ))

;
; Container
;

(defevolverfn main-theme-evolver :theme
  (if (get-property component [:this :preferences :dark] :pressed)
    flatgui.theme/dark
    flatgui.theme/light))

(defevolverfn main-skin-evolver :skin
  (if (get-property component [:this :preferences :oldschool] :pressed)
    "flatgui.skins.oldschool"
    "flatgui.skins.smooth"))

;;;; TODO defapplication marco
(def bookpanel
  (flatgui.widgets.componentbase/initialize
    (defcomponent component :main {:clip-size (defpoint 25 19 0)
                                   :background (awt/color (float (/ 0 255)) (float (/ 38 255)) (float (/ 70 255)))
                                   :evolvers {:theme main-theme-evolver
                                              ;:skin main-skin-evolver
                                              }}
                  ;debug-window
                  orderticket-window
                  tradeblotter-window
                  preferences-window)))
