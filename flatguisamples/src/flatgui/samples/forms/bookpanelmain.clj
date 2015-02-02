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
  ;(:use
  ;
  ;;flatgui.dependency
  ;;flatgui.widgets.componentbase
  ;
  ;;flatgui.inputchannels.mouse
  ;;flatgui.util.circularbuffer
  ;)
  (:require                                                 ;[flatgui.access] ; does not parse mouse events without having access loaded
            [flatgui.widgets.api :as w]
            [flatgui.skins.flat]
            [flatgui.appcontainer]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.util.matrix :as m]
            [flatgui.comlogic :as fgc]
            [flatgui.widgets.radiobutton :as radiobutton]
            [flatgui.widgets.table.commons :as tcom]
            [flatgui.widgets.abstractbutton :as abtn]
            [flatgui.widgets.table.header :as th]           ;TODO common table API instead of requiring several namespaces
            [flatgui.widgets.table.contentpane :as tcpane]
            [flatgui.widgets.table.columnheader :as tcolh]
            ))

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

(fg/defevolverfn s1text :text
  (let [ pos (get-property [:slider1] :position)]
    (.format label-format pos)))

(fg/defevolverfn s2text :text
  (let [ pos (get-property [:slider2] :position)]
    (.format label-format pos)))

; @todo
; super-evovler marco that would take evolver from parent type.
; Will have to keep the list of parent types in component.
; Throw exception if more than one found.
; One more way to call this macro - specifying exactly which type
; to take evovler from

(fg/defevolverfn s1pos :position
  (if (and
        (#{[:slider2] [:lock-checkbox]} (fg/get-reason))
        (get-property [:lock-checkbox] :pressed))
    (get-property [:slider2] :position)
    (flatgui.widgets.slider/slider-position-evolver component)))

(fg/defevolverfn s2pos :position
  (if (and
        (#{[:slider1] [:lock-checkbox]} (fg/get-reason))
        (get-property [:lock-checkbox] :pressed))
    (get-property [:slider1] :position)
    (flatgui.widgets.slider/slider-position-evolver component)))

;
; Debug Window
;

(def debug-window (fg/defcomponent w/window :debug {:clip-size (m/defpoint 3.5 5.5)
                                               :position-matrix (m/transtation-matrix 10 7)
                                               :text "Debug Panel"}

  (fg/defcomponent w/label :s1-text
    { :text "Slider 1:"
      :clip-size (m/defpoint 1.0 0.5 0)
      :position-matrix (m/transtation-matrix 0.25 0.5)})

  (fg/defcomponent w/label :s2-text
    { :text "Slider 2:"
      :clip-size (m/defpoint 1.0 0.5 0)
      :position-matrix (m/transtation-matrix 0.25 1.0)})

  (fg/defcomponent w/label :s1-label
    { :clip-size (m/defpoint 3 0.5 0)
      :h-alignment :left
      :position-matrix (m/transtation-matrix 1.28125 0.5)
      :evolvers {:text s1text}})

  (fg/defcomponent w/label :s2-label
    { :clip-size (m/defpoint 3 0.5 0)
      :h-alignment :left
      :position-matrix (m/transtation-matrix 1.28125 1.0)
      :evolvers {:text s2text}})


  (fg/defcomponent w/checkbox :lock-checkbox { :clip-size (m/defpoint 1.5 0.25 0)
                                          :text "Lock sliders"
                                          :position-matrix (m/transtation-matrix 0.25 1.75)})

  (fg/defcomponent w/slider :slider1 { :clip-size (m/defpoint 0.5 3.0 0)
                                  :orientation :vertical
                                  :position-matrix (m/transtation-matrix 0.25 2.25)
                                  :evolvers {:position s1pos}})

  (fg/defcomponent w/slider :slider2 { :clip-size (m/defpoint 0.5 3.0 0)
                                  :orientation :vertical
                                  :position-matrix (m/transtation-matrix 1.0 2.25)
                                  :evolvers {:position s2pos}})

  (fg/defcomponent w/spinner :spn { :clip-size (m/defpoint 1.375 0.375 0)
                               :position-matrix (m/transtation-matrix 1.625 2.25)})

;  (fg/defcomponent w/menu :ctx { :clip-size (m/defpoint 4 6 0)
;                            :position-matrix (m/transtation-matrix 3.625 2.75)})
                    ))

;;
;; Preferences window
;;

(radiobutton/defradiogroupevolver skin-group [:some-other :flat])

(radiobutton/defradiogroupevolver theme-group [:dark :light])

(def preferences-window
  (fg/defcomponent w/window :preferences
    {:clip-size (m/defpoint 3.75 1.375)
     :position-matrix (m/transtation-matrix 10 7)
     :text "Preferences"}

    ;(fg/defcomponent w/label :skin-label
    ;  {:text "Skin:"
    ;   :h-alignment :right
    ;   :clip-size (m/defpoint 0.75 0.25)
    ;   :position-matrix (m/transtation-matrix 0.125 0.875)})
    ;
    ;(fg/defcomponent w/radio/radiobutton :oldschool { :text "Oldschool"
    ;                                      :pressed true
    ;                                   :clip-size (m/defpoint 1.25 0.25)
    ;                                   :position-matrix (m/transtation-matrix 1.0 0.875)
    ;                                   :evolvers {:pressed skin-group}})
    ;
    ;(fg/defcomponent w/radio/radiobutton :smooth { :text "Smooth"
    ;                                  :clip-size (m/defpoint 1.0 0.25)
    ;                                  :position-matrix (m/transtation-matrix 2.375 0.875)
    ;                                  :evolvers {:pressed skin-group}})

    (fg/defcomponent w/label :theme-label
      {:text "Theme:"
       :h-alignment :right
       :clip-size (m/defpoint 0.75 0.25)
       :position-matrix (m/transtation-matrix 0.125 0.5)})

    (fg/defcomponent w/radiobutton :dark { :text "Dark"
                                     :pressed true
                                          :clip-size (m/defpoint 1.25 0.25)
                                          :position-matrix (m/transtation-matrix 1.0 0.5)
                                          :evolvers {:pressed theme-group}})

    (fg/defcomponent w/radiobutton :light { :text "Light"
                                       :clip-size (m/defpoint 1.0 0.25)
                                       :position-matrix (m/transtation-matrix 1.875 0.5)
                                       :evolvers {:pressed theme-group}})))


;
; Order Ticket Window
;

(def bid-columns [:bidsource :bidsize :bidpx])
(def ask-columns [:askpx :asksize :asksource])

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
      (if dark-theme :prime-1 :prime-2);(get-property [:this] :selected-background)
      (let [ref-val (if (< (get-property [:this] :screen-row) 0) 0 (quote-color-ref component))
            ;color-val (int (+ 60 (* ref-val 108)))
            ]
           ;(awt/color color-val color-val 168)
           (if dark-theme
             (awt/mix-colors-coeff (:prime-4 theme) (:prime-3 theme) (* 0.5 ref-val))
             (awt/mix-colors-coeff (:prime-3 theme) (:prime-2 theme) ref-val))
           ))))

(radiobutton/defradiogroupevolver ordtype-group-evolver [:market :limit :stop :stoplmt])

;; TODO use macros for text from quote evolvers

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
      {:text (str quote-val) :caret-pos (awt/strlen (str quote-val)) :selection-mark 0}
      (case (fg/get-reason)
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
        (flatgui.widgets.textfield/text-model-evolver component)))))

(fg/defevolverfn px-evolver :model
  (let [quote-val (spinner-quote-val-provider component (fg/get-reason) :bidpx :askpx)]
    (if quote-val
      (let [num->str (get-property [:this] :num->str)
            quote-val-str (num->str component quote-val)]
        {:text quote-val-str :caret-pos (awt/strlen quote-val-str) :selection-mark 0})
      (flatgui.widgets.spinner/spinner-model-evovler component))))

(fg/defevolverfn exch-evolver :model
  (let [quote-val (quote-val-provider component (fg/get-reason) :bidsource :asksource)]
    (if quote-val
      {:text (str quote-val) :caret-pos (awt/strlen (str quote-val)) :selection-mark 0}
      (flatgui.widgets.textfield/text-model-evolver component))))

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

(def orderticket-window (fg/defcomponent w/window :tiket { :clip-size (m/defpoint 7.25 7 0) :position-matrix (m/transtation-matrix 0 0) :text "Order Ticket"}

  ;TODO remove this when borders are implemented
  (fg/defcomponent w/panel :ticket-panel { :clip-size (m/defpoint 7.25 6.625 0)
                                      :position-matrix (m/transtation-matrix 0 0.375)
                                      :look (fn [c r] [])}

    (fg/defcomponent w/toolbar :general-toolbar {:position-matrix (m/transtation-matrix 0 0)
                                            :clip-size (m/defpoint 4.5 0.375)}
      (fg/defcomponent w/label :symbol-label
        {:text "Symbol:"
         :h-alignment :right
         :clip-size (m/defpoint 0.75 0.375)
         :position-matrix (m/transtation-matrix 0.125 0)})
      (fg/defcomponent w/textfield :symbol-entry
        {:clip-size (m/defpoint 0.75 0.3125)
         :position-matrix (m/transtation-matrix 0.875 0.03125)
         :evolvers {:model symbol-evolver}})
      (fg/defcomponent w/checkbox :follow-checkbox
        {:clip-size (m/defpoint 1.5 0.25 0)
         :text "Follow blotter"
         :position-matrix (m/transtation-matrix 1.75 0.0625)})
      (fg/defcomponent w/checkbutton :link-button
        {:position-matrix (m/transtation-matrix 3.375 0.03125)
         :clip-size (m/defpoint 0.75 0.3125)
         :text "Link"}))


    (fg/defcomponent w/toolbar :qty-toolbar {:position-matrix (m/transtation-matrix 4.5 0)
                                        :clip-size (m/defpoint 2.75 0.375)}
      (fg/defcomponent w/rolloverbutton :+1
        {:position-matrix (m/transtation-matrix 0.125 0.03125)
         :clip-size (m/defpoint 0.5625 0.3125)
         :text "+1"})
      (fg/defcomponent w/rolloverbutton :+10
        {:position-matrix (m/transtation-matrix (+ 0.125 (* 1 0.5625)) 0.03125)
         :clip-size (m/defpoint 0.5625 0.3125)
         :text "+10"})
      (fg/defcomponent w/rolloverbutton :+20
        {:position-matrix (m/transtation-matrix (+ 0.125 (* 2 0.5625)) 0.03125)
         :clip-size (m/defpoint 0.5625 0.3125)
         :text "+20"})
      (fg/defcomponent w/rolloverbutton :+100
        {:position-matrix (m/transtation-matrix (+ 0.125 (* 3 0.5625)) 0.03125)
         :clip-size (m/defpoint 0.5625 0.3125)
         :text "+100"}))


    (fg/defcomponent w/table :bids { :clip-size (m/defpoint 3.5 2.5)
                               :position-matrix (m/transtation-matrix 0.125 0.375)
                               :header-ids bid-columns
                               :header-aliases {:bidsource "BidSrc" :bidsize "BidSize" :bidpx "BidPx"}
                               ;:value-provider (fn [model-row model-col] (str model-row model-col))
                               :value-provider (fn [model-row model-col]
                                                   (let [ col-id (name (nth bid-columns model-col))] (.getValue SIMULATOR model-row col-id)))
                               }
                  (th/deftablefit

                    ; TODO this does not work, it gets reset on initialization
                    ;(merge-properties (tcolh/defcolumn :bidpx [:sorting]) {:children {:sorting {:degree 0 :mode :desc}}})
                    (tcolh/defcolumn :bidpx [:sorting])

                    (tcolh/defcolumn :bidsource [:sorting])
                    (tcolh/defcolumn :bidsize [:sorting]))
                  (tcpane/deftablecontent 16 {:selection-mode :single
                                       :row-height 0.25
                                       :wheel-rotation-step-y 0.25
                                       :default-cell-component (flatgui.base/merge-properties w/tablecell {:foreground (awt/color 0 0 0)
                                                                                                         :evolvers {:background quote-background-evolver}})}))


    (fg/defcomponent w/table :asks {:clip-size (m/defpoint 3.5 2.5)
                               :position-matrix (m/transtation-matrix 3.625 0.375)
                               :header-ids ask-columns
                               :header-aliases {:asksource "AskSrc" :asksize "AskSize" :askpx "AskPx"}
                               ;:value-provider (fn [model-row model-col] (str model-row model-col))
                               :value-provider (fn [model-row model-col]
                                                 (let [ col-id (name (nth ask-columns model-col))] (.getValue SIMULATOR model-row col-id)))
                               }
      (th/deftablefit
        (tcolh/defcolumn :asksource [:sorting])
        (tcolh/defcolumn :asksize [:sorting])
        (tcolh/defcolumn :askpx [:sorting]))
      (tcpane/deftablecontent 16 {:selection-mode :single
                           :row-height 0.25
                           :wheel-rotation-step-y 0.25
                           :default-cell-component (flatgui.base/merge-properties w/tablecell {:foreground (awt/color 0 0 0)
                                                                                             :evolvers {:background quote-background-evolver}})}))


    (fg/defcomponent w/label :qty-label { :text "Qty:"
                                     :h-alignment :right
                                     :clip-size (m/defpoint 0.5 0.375 0)
                                     :position-matrix (m/transtation-matrix 0.125 3.0)})

    (fg/defcomponent w/textfield :qty-entry {:text-supplier flatgui.widgets.textfield/textfield-num-only-text-suplier
                                        :clip-size (m/defpoint 1.0 0.375 0)
                                        :position-matrix (m/transtation-matrix 0.625 3.0)
                                        :evolvers {:model qty-evolver}})

    (fg/defcomponent w/label :px-label { :text "Px:"
                                    :h-alignment :right
                                    :clip-size (m/defpoint 0.5 0.375 0)
                                    :position-matrix (m/transtation-matrix 1.625 3.0)})

    (fg/defcomponent w/spinner :px-entry { :clip-size (m/defpoint 1.375 0.375 0)
                                     :step 0.01
                                     :position-matrix (m/transtation-matrix 2.125 3.0)
                                     :children {:editor (fg/defcomponent w/spinnereditor :editor {:evolvers {:model px-evolver}})}
                                     })

    (fg/defcomponent w/label :stgy-label {:text "Strategy:"
                                     :h-alignment :right
                                     :clip-size (m/defpoint 1.375 0.375 0)
                                     :position-matrix (m/transtation-matrix 3.125 3.0)})

    (fg/defcomponent w/combobox :stgy-entry {:model ["WVAP" "TWAP" "With Volume" "Dynamic" "Custom"]
                                        :clip-size (m/defpoint 1.375 0.375 0)
                                        :position-matrix (m/transtation-matrix 4.5 3.0)})

    (fg/defcomponent w/textfield :exch-entry {:clip-size (m/defpoint 1.0 0.375 0)
                                         :position-matrix (m/transtation-matrix 6.0 3.0)
                                         :evolvers {:model exch-evolver}
                                         })

    (fg/defcomponent w/label :aggr-label { :text "Aggressiveness:"
                                      :clip-size (m/defpoint 1.5 0.375 0)
                                      :position-matrix (m/transtation-matrix 0.125 3.5)})

    (fg/defcomponent w/slider :aggr-slider { :clip-size (m/defpoint 3.0 0.5 0)
                                        :position-matrix (m/transtation-matrix 1.75 3.5)})

    (fg/defcomponent w/label :type-label { :text "OrdType:"
                                      :clip-size (m/defpoint 1.0 0.375 0)
                                      :position-matrix (m/transtation-matrix 0.125 4.25)})

    (fg/defcomponent w/radiobutton :market { :text "Market:"
                                        :clip-size (m/defpoint 1.25 0.25 0)
                                        :position-matrix (m/transtation-matrix 1.125 4.25)
                                        :evolvers {:pressed ordtype-group-evolver}})
    (fg/defcomponent w/radiobutton :limit { :text "Limit:"
                                       :clip-size (m/defpoint 1.25 0.25 0)
                                       :position-matrix (m/transtation-matrix 1.125 4.5625)
                                       :evolvers {:pressed ordtype-group-evolver}})
    (fg/defcomponent w/radiobutton :stop { :text "Stop:"
                                      :clip-size (m/defpoint 1.25 0.25 0)
                                      :position-matrix (m/transtation-matrix 1.125 4.875)
                                      :evolvers {:pressed ordtype-group-evolver}})
    (fg/defcomponent w/radiobutton :stoplmt { :text "Stop lmt:"
                                         :clip-size (m/defpoint 1.25 0.25 0)
                                         :position-matrix (m/transtation-matrix 1.125 5.1875)
                                         :evolvers {:pressed ordtype-group-evolver}})

    (fg/defcomponent w/label :tif-label { :text "TimeInForce:"
                                     :h-alignment :right
                                     :clip-size (m/defpoint 1.375 0.375 0)
                                     :position-matrix (m/transtation-matrix 2.375 4.25)})

    (fg/defcomponent w/combobox :tif-entry { :model ["Day", "GTC", "OPG", "IOC", "FOK", "GTX", "GTD"]
                                        :clip-size (m/defpoint 1.375 0.375 0)
                                        :position-matrix (m/transtation-matrix 3.75 4.25)})

    ;(fg/defcomponent w/label :text-label { :text "Text:"
    ;                                  :h-alignment :right
    ;                                  :clip-size (m/defpoint 1.375 0.375 0)
    ;                                  :position-matrix (m/transtation-matrix 2.375 4.75)})
    ;
    ;(fg/defcomponent w/textfield :text-entry { :clip-size (m/defpoint 1.375 0.375 0)
    ;                                     :position-matrix (m/transtation-matrix 3.75 4.75)})

    (fg/defcomponent w/label :text-label { :text "Text:"
                                     :h-alignment :right
                                     :clip-size (m/defpoint 0.75 0.375 0)
                                     :position-matrix (m/transtation-matrix 5.125 4.25)})

    (fg/defcomponent w/textfield :text-entry {:clip-size (m/defpoint 1.0 0.375 0)
                                         :position-matrix (m/transtation-matrix 5.875 4.25)})


    ;(fg/defcomponent w/label :exch-label { :text "Exch:"
    ;                                 :h-alignment :right
    ;                                 :clip-size (m/defpoint 0.75 0.375 0)
    ;                                 :position-matrix (m/transtation-matrix 5.125 4.25)})
    ;
    ;(fg/defcomponent w/textfield :exch-entry {:clip-size (m/defpoint 1.0 0.375 0)
    ;                                     :position-matrix (m/transtation-matrix 5.875 4.25)
    ;                                     :evolvers {:model exch-evolver}
    ;                                     })

    (fg/defcomponent w/button :buy { :position-matrix (m/transtation-matrix 0.125 6.125)
                                :clip-size (m/defpoint 1.0 0.375)
                                :text "Buy"})

    (fg/defcomponent w/button :sell { :position-matrix (m/transtation-matrix 6.125 6.125)
                                 :clip-size (m/defpoint 1.0 0.375)
                                 :text "Sell"})

;    (fg/defcomponent w/menu :ctx-menu { :value-provider (fn [model-row model-col] (str model-row "-" model-col))
;                                   :clip-size (m/defpoint 3 5 0)
;                                   :position-matrix (m/transtation-matrix 2.5 3.75)}
;      (fg/defcomponent w/menucontentpane :content-pane
;        {
;          :row-count 8
;          :row-order (vec (range 0 8))
;          }))


    )))


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
                  orderticket-window
                  tradeblotter-window
                  preferences-window)))
