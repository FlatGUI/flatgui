; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Namespace that contains functions needed for
            defining look functions and obtaining paint
            sequnces from components and containers."
      :author "Denys Lebediev"}
  flatgui.paint
  (:require [flatgui.comlogic :as fgc]
            ;[flatgui.access :as access]
            [flatgui.awt :as awt]
            [flatgui.util.matrix :as m]
            [flatgui.util.rectmath :as r]
            ))

(defn get-color-from-component [component color-property]
  "Extracts color property value for component. It may be specified
   directly by absolute value, or by a keyword - reference to
   component's theme which may change during component's lifetime"
  (let [color-property-value (color-property component)]
    (if (nil? color-property-value)
      nil
      (if (keyword? color-property)
        (color-property-value (:theme component))
        color-property-value))))

(defn- empty-paint-seq-element? [e]
  (or (nil? e) (and (coll? e) (empty? e))))

(defn leaf? [v]
  (and
    (not (empty-paint-seq-element? v))
    (not (some (fn [e] (vector? e)) v))))

(defn- filter-nils-out [v]
  (if (and (not (empty? v)) (nil? (first v)))
    []
    v))

(defn- conj-if-not-empty [coll x]
  (if (empty? x)
    coll
    (conj coll x)))

(defn flatten-vector
  "Suppose a vector that does not contain vectors among
   its elements, it the tree leaf. This function unrolls
   tree-like structure of vectors into single vector that
   contains only leafs. It is supposed that each vector
   may contain either vectors only, or non-vector elements
   only."
  ([preceding v]
    (if (empty-paint-seq-element? v)
      preceding
      (if (leaf? v)
        (conj-if-not-empty preceding (filter-nils-out v))
        (loop [ result preceding
                i 0]
          (if (< i (count v))
            (recur
              (let [ e (nth v i)]
                  (if (leaf? e) (conj-if-not-empty result (filter-nils-out e)) (into result (flatten-vector e))))
              (inc i))
            result)))))
  ([v] (flatten-vector [] v)))

(defn- gen-param-keyword [param]
  (if (keyword? param)
    param
    (first param)))

(defn- gen-param-name [param]
  (symbol (name (gen-param-keyword param))))

(defn- gen-param-binding-value [param]
  `(let [ ~'value (~param ~'comp-property-map)]
    (if (fgc/standard-color? ~'value) (get-color-from-component ~'comp-property-map ~param) ~'value)))

(defn gen-param-binding [param]
  (if (keyword? param)
    (gen-param-binding-value param)
    (if (= 1 (count param))
      (gen-param-binding-value (first param))
      (list (first param) (gen-param-binding (next param))))))


(def PREDEFINED-PARAMS '(:clip-size :content-size :background :foreground :interop))

;;; TODO param-list should be a vector rather than list
(defmacro deflookfn [fnname param-list & awt-calls]
  "Convenient macro to define look functions for components.
  Defines look function with two parameters: comp-property-map
  and dirty-rects. By convention, dirty-rects is optional; also
  it may be null - this means whole component has to be
  repainted; dirty-rects does not necessarily contain olny
  that have intersection with the component.
  Introduces let binding for all params listed in param-list
  so that formes passed as awt-calls can refer all params by
  symbols. Params listed in param-list should be keywords for
  getting values from component which is a map of properties.
  Param symbols are created from those keyword names."

  (let [let-bindings (vec (concat
                            (mapcat (fn [e] (list (first e) (last e)))
                              (for [param (distinct (concat PREDEFINED-PARAMS param-list))]
                                [(gen-param-name param) (gen-param-binding param)]))
                           '[w (flatgui.util.matrix/x content-size)
                             h (flatgui.util.matrix/y content-size)
                             w- (flatgui.awt/-px (flatgui.comlogic/masknil w))
                             h- (flatgui.awt/-px (flatgui.comlogic/masknil h))
                             w-2 (flatgui.awt/-px (flatgui.comlogic/masknil w) 2)
                             h-2 (flatgui.awt/-px (flatgui.comlogic/masknil h) 2)]
                            ))]
    `(defn ~fnname [~'comp-property-map ~'dirty-rects] (flatgui.paint/flatten-vector (let ~let-bindings [~@awt-calls])))))

(defmacro call-look [fnname]
  `(~fnname ~'comp-property-map ~'dirty-rects))

(deflookfn trouble-look (:x :y :w :h)
  (awt/setColor (awt/color 255 0 0))
  (awt/drawRect x y (awt/-px w) (awt/-px h))
  (vec (for [s (range 0 (+ h w) 0.1)]
         (let [dw (- s w)
               dh (- s h)
               x1 (if (> dh 0) (+ x dh) x)
               y1 (if (> dh 0) (+ y h) (+ y s))
               x2 (if (> dw 0) (+ x w) (+ x s))
               y2 (if (> dw 0) (+ y dw) y)]
           (awt/drawLine x1 y1 x2 y2)))))

(defn font-look [cmpnt]
  (if-let [font (:font cmpnt)] ["setFont" font]))

(defn paint-component-only [cmpnt dirty-rects]
  "Paints component, does not pait children"
  (if (:visible cmpnt)
    (:look-vec cmpnt)))

(defn paint-component-with-children [component dirty-rects]
  "Paints component and its children"
  (try
    (let [clip-w (m/x (:clip-size component))
          clip-h (m/y (:clip-size component))
          position-matrix (:position-matrix component)
          viewport-matrix (:viewport-matrix component)
          awt-position-matrix (awt/affinetransform position-matrix)
          awt-viewport-matrix (awt/affinetransform viewport-matrix)
          awt-viewport-matrix-inverse (awt/invert awt-viewport-matrix)
          awt-position-matrix-inverse (awt/invert awt-position-matrix)]
        (flatten-vector
          ; TODO WARNING! This is a temporary hack. Otherwise content-pane children do not respect table clip rect (not= (:widget-type component) "tablecell")
          [(if (not= (:widget-type component) "tablecell")  (awt/pushCurrentClip))
           (awt/transform awt-position-matrix)
           (if (not= (:widget-type component) "tablecell") (if (:popup component) (awt/setClip 0 0 clip-w clip-h) (awt/clipRect 0 0 clip-w clip-h)))
           (awt/transform awt-viewport-matrix)
           (paint-component-only component dirty-rects)
           (mapv #(paint-component-with-children % dirty-rects) (sort-by :z-position (for [[_ v] (:children component)] v)))
           (awt/transform awt-viewport-matrix-inverse)
           (awt/transform awt-position-matrix-inverse)
           (if (not= (:widget-type component) "tablecell") (awt/popCurrentClip))]))
    (catch Exception e (do
                         (println "Exception " (.getMessage e) ;TODO Log instead of  println
                           " when painting component id = " (:id component)
                           ;" component: " (container-to-str component)
                           )
                         (.printStackTrace e)))))

(defn paint-all [container clipx clipy clipw cliph]
  (paint-component-with-children container nil))

;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;; New painting approach optimized for web
;;;;;;;;;;;;;;;;;;;;;;;;

;;;;; TODO find better place for this, make configurable
;;;(def string-pool-properties [:text :image-url])

;;; TODO This works and may be used instead of string-pool-properties, but seems to be slow
;;;
;;; TODO Idea: gather all needed info in :aux-container while evolving
;;;
;(defn extract-strings [look-vec]
;  (mapcat
;    #(cond
;      (string? %) [%]
;      (vector? %) (extract-strings %)
;      :else [])
;    look-vec))
;
(defmulti extract-value (fn [property _ _] (class property)))
;
;(defmethod extract-value Keyword [property f container]
;  (f (property container)))
;
;(defmethod extract-value Collection [property f container]
;  ;; This reduce accumulates bit flags
;  (reduce #(+ (* 2 %1) %2) 0 (for [p property] (f (p container)))))
;
(defmethod extract-value nil [_ f container]
  (f container))

(defn- get-component-id-path-to-property-value [id-path container property f]
  (let [this-id-path (fgc/conjv id-path (:id container))]
    (apply
      merge
      {this-id-path (extract-value property f container)}
      (for [[_ v] (:children container)] (get-component-id-path-to-property-value this-id-path v property f)))))

;(defn get-component-id-path-to-position-matrix [container]
;  (get-component-id-path-to-property-value [] container :position-matrix awt/affinetransform))
;
;(defn get-component-id-path-to-viewport-matrix [container]
;  (get-component-id-path-to-property-value [] container :viewport-matrix awt/affinetransform))
;
;(defn get-component-id-path-to-clip-size [container]
;  (get-component-id-path-to-property-value [] container :clip-size (fn [c] [(m/x c) (m/y c)])))
;
;(defn get-component-id-path-to-look-vector [container]
;  (get-component-id-path-to-property-value [] container :look-vec identity))
;
;(defn get-component-id-path-to-child-count [container]
;  (get-component-id-path-to-property-value [] container :children count))
;
;(defn get-component-id-path-to-flags [container]
;  (get-component-id-path-to-property-value [] container [:rollover-notify-disabled :popup :visible] (fn [v] (if v 1 0))))
;
;;(defn get-component-id-path-to-strings [container]
;;  (get-component-id-path-to-property-value [] container nil (fn [container] (mapv #(% container) string-pool-properties))))
;
;;(defn get-component-id-path-to-strings [container]
;;  (get-component-id-path-to-property-value [] container :look-vec extract-strings))
;
;(defn get-component-id-path-to-strings
;  ;([container paths]
;  ; (into {} (map (fn [p] [p (extract-strings (get-in container (fgc/conjv (fgc/get-access-key p) :look-vec)))]) paths)))
;  ;([container id-path-to-component]
;  ; (into {} (for [[k v] id-path-to-component] [k (extract-strings (:look-vec v))])))
;
;  ([container]
;   (get-component-id-path-to-property-value [] container :look-vec extract-strings)))


(defn get-component-id-path-to-component [container paths]
  (if paths
    (into {} (map (fn [p] [p (get-in container (fgc/get-access-key p))]) paths))
    (get-component-id-path-to-property-value [] container nil identity)))

(defn- ready-for-paint? [container]
  (and
    (:position-matrix container)
    (:viewport-matrix container)
    (:clip-size container)
    (:look-vec container)))

;;; TODO investigate sorting impact on performance
(defn get-paint-all-sequence
  ([id-path container]
    (if (ready-for-paint? container)
      (apply concat
             [(conj id-path (:id container))]
             (map
               #(get-paint-all-sequence (conj id-path (:id container)) %)
               (sort-by :z-position (for [[_ v] (:children container)] v))))
     []))
  ([container] (get-paint-all-sequence [] container)))



;;;;;;;;;;;;;;
;;;;;;;;;;;;;; Old experimental approaches
;;;;;;;;;;;;;;

;(defn paint-component-only2 [cmpnt dirty-rects]
;  "Paints component, does not pait children"
;  (if (and (:visible cmpnt) (:look cmpnt))
;    (flatten-vector ((:look cmpnt) cmpnt dirty-rects))
;    ))

;(defn get-dirty-rect-from-container [container]
;  (let [ r (:dirty-rect container)]
;    (if r
;      [(double (:x r)) (double (:y r)) (double (:w r)) (double (:h r))])))

;;@todo with such approach clip rect should be passed to evolve
;;
;(defn paint-all [container clipx clipy clipw cliph]
;  (:paint-container-vec container))


;(def has-changes-predicate {:has-changes (fn [v] (true? v))})

;(defn- setup-clip [clip]
;  (if (nil? clip)
;    (awt/setClip nil)
;    (awt/setClip (:x clip) (:y clip) (:w clip) (:h clip))))

;(defn- get-all-components-for-dirty-rects [container all-rects-to-paint]
;  "Determines all dirty rects: rects taken by components that have to be
;  repainted because of changes, plus all rects that have to be repainted
;  because of possible component location change (old location areas).
;  Then returns all components that have to be painted to cover all those
;  dirty rects. Finds components starting from topmost, and skips ones
;  fully covered within dirty areas."
;  (let [ components-within-rects (sort-components-by-id-depth
;                                   (flatgui.access/get-components-within-all-rects container all-rects-to-paint))]
;    (loop [ remaining-dirty-rects all-rects-to-paint
;            components-to-paint nil
;            i 0]
;        (let [ relative-c (if (< i (count components-within-rects)) (nth components-within-rects i))
;               c (if (not (nil? relative-c))
;                   (convert-to-screen-coords relative-c))
;               dirty-rects-minus-c (rects- remaining-dirty-rects c)]
;        (if (or (= i (count components-within-rects)) (empty? remaining-dirty-rects))
;          components-to-paint
;          (recur
;            dirty-rects-minus-c
;            (if (= remaining-dirty-rects dirty-rects-minus-c) components-to-paint (conj components-to-paint c))
;            (inc i)))))))
;
;(defn paint-dirty [container]
;  "Paints all changed componets, and all components that have
;  to be reapinted because of location changes of other components.
;  Calculates dirty rects and provides them to look functions, so
;  look functions have chance to repaint only certain rects, not
;  whole components."
;  (let [ all-rects-to-paint (find-rects-to-paint container)
;         involved-components (get-all-components-for-dirty-rects container all-rects-to-paint)]
;    (paint-components (sort-components-by-id involved-components) all-rects-to-paint)))

;; TODO
;(defn- clip? [component] (#{"tablecontentpane" "window" "toolbar" "component" "tableheader"} (:widget-type component)))

;(defn paint-dirty-impl [component dirty-rects force-paint]
;  "Paints component and its children"
;  (try
;    (let [ clip-w (m/x (:clip-size component))
;          clip-h (m/y (:clip-size component))
;          position-matrix (:position-matrix component)
;          viewport-matrix (:viewport-matrix component)
;
;          ; TODO this is optimization for web: to transmit less commands
;          transform-matrix (m/mx* position-matrix viewport-matrix)
;          awt-transform-matrix (awt/affinetransform transform-matrix)
;          awt-transform-matrix-inverse (awt/invert awt-transform-matrix)
;
;          awt-position-matrix (awt/affinetransform position-matrix)
;          awt-viewport-matrix (awt/affinetransform viewport-matrix)
;          ;awt-viewport-matrix-inverse (invert awt-viewport-matrix)
;          ;awt-position-matrix-inverse (invert awt-position-matrix)
;
;          paint-this (or force-paint
;                         (   ;or (nil? dirty-rects)
;                           and dirty-rects
;                               (r/intersect?
;                                 dirty-rects
;                                 {:x (m/mx-x (:abs-position-matrix component))
;                                  :y (m/mx-y (:abs-position-matrix component))
;                                  :w (m/x (:clip-size component))
;                                  :h (m/y (:clip-size component))})))
;          ;_ (if (= :main (:id component)) (println "Painting " (:id component) " paint=" paint-this " dirty-rects = " dirty-rects " abs pm = " (:abs-position-matrix component)))
;          ;_ (if (and dirty-rects paint-this) (println "Painting " (:id component) " dirty-rects = " dirty-rects " abs pm = " (:abs-position-matrix component)))
;          children (:children component)]
;      (if paint-this
;        (flatten-vector
;          ; TODO WARNING! This is a temporary hack. Otherwise content-pane children do not respect table clip rect (not= (:widget-type component) "tablecell")
;          [(if (clip? component)  (awt/pushCurrentClip))
;           (awt/transform awt-position-matrix)
;
;           (if (clip? component) (if (:popup component) (awt/setClip 0 0 clip-w clip-h) (awt/clipRect 0 0 clip-w clip-h)))
;           (if (not= awt-viewport-matrix m/IDENTITY-MATRIX) (awt/transform awt-viewport-matrix))
;
;           (paint-component-only2 component dirty-rects)
;           ;(println "Painting component " (:id component) " :needs-repaint = " (:needs-repaint component))
;
;           (mapv #(paint-dirty-impl % dirty-rects (and paint-this (not= :main (:id component)))) (sort-by :z-position (for [[_ v] (:children component)] v)))
;
;           ;(transform awt-viewport-matrix-inverse)
;           ;(transform awt-position-matrix-inverse)
;           (awt/transform awt-transform-matrix-inverse)
;
;           (if (clip? component) (awt/popCurrentClip))
;           ])
;        (if (seq children)
;          (let [ children-look (mapv #(paint-dirty-impl % dirty-rects false) (sort-by :z-position (for [[_ v] children] v)))]
;            (if (seq children-look)
;              (flatten-vector
;                [                                               ;(transform awt-transform-matrix)
;                 (if (clip? component)  (awt/pushCurrentClip))
;                 (awt/transform awt-position-matrix)
;                 (if (clip? component) (if (:popup component) (awt/setClip 0 0 clip-w clip-h) (awt/clipRect 0 0 clip-w clip-h)))
;                 (if (not= awt-viewport-matrix m/IDENTITY-MATRIX) (awt/transform awt-viewport-matrix))
;
;                 children-look
;
;                 (awt/transform awt-transform-matrix-inverse)
;                 (if (clip? component) (awt/popCurrentClip))
;                 ;(transform awt-transform-matrix-inverse)
;                 ])
;              (do
;                ;(println "Chidren look skipped")
;                nil)))
;          )))
;    (catch Exception e (do
;                         (fg/log-debug "Exception " (.getMessage e)
;                                    " when painting component id = " (:id component)
;                                    ;" component: " (container-to-str component)
;                                    )
;                         (.printStackTrace e)))))
;
;(defn paint-dirty [component _ _]
;  (if (:dirty-rect component)
;    (do
;      ;(println "Dirty rect is " (:dirty-rect component) " painting...")
;      (paint-dirty-impl component (:dirty-rect component) nil))
;    (do
;      ;(println " Dirty rect is null, skipped painting")
;      [])))


;;;
;; TODO 1. it looks like flatten-vector is needed neither in deflookfn nor here
;; TODO 2. from debug output (see lv below), it looks like this is called twice for one component per cycle
;;;
(defn rebuild-look [component]
  (let [look-fn (:look component)
        font (:font component)]
    (if look-fn
      (try

        (do
          (if font
            (.setReferenceFont (:interop component) font (flatgui.awt/str->font font)))
          (let [font-look (font-look component)
                trouble-look (if (:has-trouble component) (trouble-look component nil))
                lv (if (or (not (nil? font-look)) (not (nil? trouble-look)))
                     (flatten-vector
                       [font-look
                        (look-fn component nil)
                        trouble-look])
                     (look-fn component nil))
                ;_ (println "||| lv " lv)
                ]
            lv))

        (catch Exception ex
          (do
            ;(fg/log-error "Error painting " target-id-path ":" (.getMessage ex))
            (.printStackTrace ex))))
      [])))
