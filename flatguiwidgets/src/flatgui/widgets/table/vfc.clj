; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Table colum Vertical Feature"
      :author "Denys Lebediev"}
  flatgui.widgets.table.vfc (:use flatgui.awt
                                  flatgui.comlogic
                                  flatgui.base
                                  flatgui.theme
                                  flatgui.paint
                                  flatgui.access
                                  flatgui.widgets.abstractbutton
                                  flatgui.widgets.componentbase
                                  flatgui.widgets.component
                                  flatgui.widgets.table.commons
                                  flatgui.inputchannels.mouse
                                  flatgui.util.matrix
                                  clojure.test)
  (:require flatgui.skins.flat))


; Common functionality

(defaccessorfn get-from-table [contentpane property] (get-property contentpane [] property))

(defaccessorfn get-from-header-vfc [contentpane header-id vfc-id property] (get-property contentpane [:header header-id vfc-id] property))

(defn is-vfc-degree-active [degree]
  (>= (if degree degree -1) 0))

(defaccessorfn get-value [contentpane model-r model-c]
  (let [ value-provider (get-from-table contentpane :value-provider)]
    (value-provider model-r model-c)))

(defaccessorfn get-column-model-index [contentpane header-id]
  (let [ header-ids (get-from-table contentpane :header-ids)]
    (.indexOf header-ids header-id)))

(defaccessorfn get-value-from-col [contentpane model-r header-id]
  (get-value contentpane model-r (get-column-model-index contentpane header-id)))


; Useful functions for v-features

(defaccessorfn max-degree-value [contentpane] (count (get-from-table contentpane :header-ids)))

(defaccessorfn apply-vf-by-degree [contentpane vfc-id apply-fn prev-row-order modes]
  (let [ header-list (get-from-table contentpane :header-ids)
         get-degree (fn [header-id]
                      (let [ degree (get-from-header-vfc contentpane header-id vfc-id :degree)
                            ;_ (println "      ** header-id " header-id " degree=" degree)
                            ]
                        (if (is-vfc-degree-active degree)
                          degree
                          (max-degree-value contentpane))))
         header-list-sort-key-fn (fn [h-id] (get-degree h-id))
         header-list-sorted (sort-by header-list-sort-key-fn header-list)
        ;_ (println "  ---------- apply-vf-by-degree called. Reason: " ((:evolve-reason-provider contentpane) (:id contentpane)))
        ;_ (println "          ----  header-list = " header-list)
        ;_ (println "    ---  header-list-sorted = " header-list-sorted)
        ]
      (loop [ cnt 0
              result prev-row-order
              prev-header-ids nil]
        (if (= cnt (count header-list))
          (vec result)
          (recur
            (inc cnt)
            (apply-fn contentpane prev-header-ids (nth header-list-sorted cnt) result modes)
            (conj prev-header-ids (nth header-list-sorted cnt)))))))

(defn find-subranges [v]
  (let [v-size (count v)]
  (loop [begin 0
         v-rest v
         result []]
    (if (= begin v-size)
      result
      (let [ sub-range (take-while (fn [e] (= e (first v-rest))) v-rest)
             sub-range-size (count sub-range)]
        (recur
          (+ begin sub-range-size)
          (take-last (- (count v-rest) (count sub-range)) v-rest)
          (conj result [begin sub-range-size])))))))




;@todo for some unknown reason it cannot parse vfcsorting when this functions is declared there
;
;
(deflookfn sorting-look (:theme :mode :degree)
  ;(flatgui.awt/setColor foreground)
  (let [text (if (> degree 0) (str degree))
        tx (- w (flatgui.awt/strw text))
        hy (/ h 2)
        ty (+ hy (flatgui.awt/halfstrh))]
    [ (cond
        (= :asc mode)
        (let [lx1 (* w 0.375)
              ly1 (- (/ h 2) (* w 0.0625))
              lx2 (* w 0.5)
              ly2 (- (/ h 2) (* w 0.25))
              lx3 (+px (* w 0.625))
              ly3 (- (/ h 2) (* w 0.0625))]
          (flatgui.skins.flat/arrow-up lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-1 theme) (:extra-2 theme)))
        (= :desc mode)
        (let [lx1 (* w 0.375)
              ly1 (+ (/ h 2) (* w 0.0625))
              lx2 (* w 0.5)
              ly2 (+ (/ h 2) (* w 0.25))
              lx3 (+px (* w 0.625))
              ly3 (+ (/ h 2) (* w 0.0625))]
          (flatgui.skins.flat/arrow-down lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-1 theme) (:extra-2 theme))))
     ;(if text (flatgui.awt/drawString text tx ty))
     ]))

;(deflookfn vfc-rollover-look (:pressed :theme :has-mouse)
;  (if has-mouse
;    [(if pressed (setColor (:light theme)) (setColor (:mid-light theme)))
;     (fillRect 0 (* 2 (px)) w (-px h 4))
;     (setColor (:light theme))
;     (drawRect 0 (* 2 (px)) w- (-px h 5))])
;  ;(call-look label-look)
;  )

(defn- set-vfc-color [mode has-mouse theme]
  (setColor (cond
              (not= :none mode) (:prime-1 theme)
              has-mouse (mix-colors (:extra-1 theme) (:prime-1 theme))
              :else (mix-colors31 (:extra-1 theme) (:prime-1 theme)))))

(deflookfn filtering-look (:theme :mode :has-mouse)
  (let [fw (/ w 2)
        btm (+ (/ h 2) (/ fw 2))
        mid (- (/ h 2) (/ fw 4))
        top (- (/ h 2) (/ fw 2))
        ]
    [(set-vfc-color mode has-mouse theme)
     (drawLine (* w 0.25) btm (* w 0.75) btm)
     (drawLine (* w 0.25) btm (* w 0.25) mid)
     (drawLine (* w 0.75) btm (* w 0.75) mid)
     (drawLine (* w 0.25) mid (* w 0.3125) top)
     (drawLine (* w 0.3125) top (* w 0.375) top)
     (drawLine (* w 0.375) top (* w 0.5) mid)
     (drawLine (* w 0.5) mid (* w 0.5625) top)
     (drawLine (* w 0.5625) top (* w 0.625) top)
     (drawLine (* w 0.625) top (* w 0.75) mid)
     (fillRect (* w 0.5625) (+px top) (+px (* w 0.125)) (- mid top))
     ]))

(deflookfn grouping-look (:theme :mode :degree :has-mouse)
  (let [e (+px 0 3) ; TODO (* 0.25 w)
        t (/ e 2)
        he (+px 0 2) ; TODO (/ e 2)
        ]
    [(set-vfc-color mode has-mouse theme)
     (fillRect 0 t e e)
     (fillRect (+ e he) t e e)
     (fillRect (+ e he e he) t e e)]))


(defn get-new-mode [component]
  (let [ old-mode (:mode component)
         mode-vec (:mode-vec component)]
    (if old-mode
      (let [ index (.indexOf mode-vec old-mode)]
        (if (< index (dec (count mode-vec)))
          (nth mode-vec (inc index))
          (first mode-vec)))
      (first mode-vec))))

(defevolverfn :mode
  (if (should-evolve-header component)
    (get-new-mode component)
    old-mode))

(defevolverfn :degree
  (if (= :none (get-property component [:this] :mode))
    -1
    0))

(defn- get-vfc-width [colheader-h] (* colheader-h 0.75))

(defevolverfn vfc-clip-size-evolver :clip-size
  (let [ colheader-size (get-property component [] :clip-size)
         colheader-h (y colheader-size )
         vfcw (get-vfc-width colheader-h)]
    (defpoint vfcw colheader-h)))

(defevolverfn vfc-position-matrix-evolver :position-matrix
  (let [ colheader-size (get-property component [] :clip-size)
         vf-visual-order (get-property component [] :vf-visual-order)
         this-index (.indexOf vf-visual-order (:id component))
         vfcw (get-vfc-width (y colheader-size))]
    (condp = (:id component)
      :filtering (transtation-matrix (- (x colheader-size) vfcw) 0)
      :grouping (transtation-matrix (- (/ (x colheader-size) 2) (/ vfcw 2)) 0)
      (if (>= this-index 0)
        (transtation-matrix (* vfcw this-index) 0)
        old-position-matrix))))

;@todo for some reason it does not evolve mode if I comment out :has-mouse and :mouse-down and extend abstractbutton
;
;
(defwidget vfc
  { :degree 0
    :mode nil
    :mouse-down false
   ;:has-mouse false
    :mode-vec []

    ;@todo this will not be needed when vfc will extend abstractbutton
    :text ""

    :evolvers { :mode mode-evolver
                :degree degree-evolver
                :clicked-no-shift clicked-no-shift-evolver
                :clicked-with-shift clicked-with-shift-evolver
               ;:has-mouse has-mouse-evolver
                :mouse-down mouse-down-evolver
                :clip-size vfc-clip-size-evolver
                :position-matrix vfc-position-matrix-evolver
                }}
   component)