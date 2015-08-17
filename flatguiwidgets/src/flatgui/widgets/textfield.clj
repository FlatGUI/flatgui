; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Text Field widget"
      :author "Denys Lebediev"}
  flatgui.widgets.textfield
  (:require [flatgui.awt :as awt]
            [flatgui.base :as fg]
            [flatgui.widgets.component]
            [flatgui.widgets.scrollpanel]
            [flatgui.inputchannels.keyboard :as keyboard]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.inputchannels.clipboard :as clipboard]
            [flatgui.inputchannels.awtbase :as inputbase]
            [flatgui.util.matrix :as m]
            [flatgui.comlogic :as fgc])
  (:import [java.awt.event KeyEvent]))


(defn create-single-line-model [text caret-pos selection-mark]
  {:text text
   :lines [text]
   :caret-pos caret-pos
   :selection-mark selection-mark
   :caret-line 0
   :caret-line-pos caret-pos
   :selection-mark-line 0
   :selection-mark-line-pos selection-mark})

(defn get-hgap [] (awt/halfstrh))

(defn- get-caret-x [text caret-pos]
  (awt/strw (subs text 0 caret-pos)))

(defn deccaretpos [c]
  (if (> c 0) (- c 1) 0))

(defn inccaretpos [c t]
  (let [len (awt/strlen t)]
    (if (< c len) (+ c 1) len)))

(defn evovle-caret-pos [component old-caret-pos old-caret-line-pos old-caret-line old-selection-mark old-text old-lines supplied-text]
  (let [t old-text
        key (keyboard/get-key component)
        typed (keyboard/key-typed? component)
        pressed (keyboard/key-pressed? component)
        fwd-selection-len (if (> old-caret-pos old-selection-mark)
                            (- old-caret-pos old-selection-mark)
                            0)
        with-ctrl (inputbase/with-ctrl? component)]
    (if typed
      (+ old-caret-pos (awt/strlen supplied-text))
      (if pressed
        (condp = key
          KeyEvent/VK_BACK_SPACE (if (> fwd-selection-len 0) (- old-caret-pos fwd-selection-len) (deccaretpos old-caret-pos))
          KeyEvent/VK_DELETE (if (> fwd-selection-len 0) (- old-caret-pos fwd-selection-len) old-caret-pos)
          KeyEvent/VK_LEFT (deccaretpos old-caret-pos)
          KeyEvent/VK_RIGHT (inccaretpos old-caret-pos t)
          KeyEvent/VK_HOME (if (or (not (:multiline component)) with-ctrl)
                             0
                             (- old-caret-pos old-caret-line-pos))
          KeyEvent/VK_END (if (or (not (:multiline component)) with-ctrl)
                            (awt/strlen t)
                            (+ old-caret-pos (- (.length (nth old-lines old-caret-line)) old-caret-line-pos)))
          KeyEvent/VK_UP (if (> old-caret-line 0)
                           (- old-caret-pos (+
                                              old-caret-line-pos
                                              (max 0 (- (.length (nth old-lines (dec old-caret-line))) old-caret-line-pos))
                                              1))  ;1 for linebreak
                           old-caret-pos)
          KeyEvent/VK_DOWN (if (< old-caret-line (dec (count old-lines)))
                             (+ old-caret-pos (+
                                                (min old-caret-line-pos (.length (nth old-lines (inc old-caret-line))))
                                                (- (.length (nth old-lines old-caret-line)) old-caret-line-pos)
                                                1))  ;1 for linebreak
                             old-caret-pos)
          old-caret-pos)
        old-caret-pos))))

(defn- insert-text [old-text prevcaretpos has-selection sstart send text]
  (if has-selection
    (str
      (subs old-text 0 sstart)
      text
      (subs old-text send))
    (str
      (subs old-text 0 prevcaretpos)
      text
      (subs old-text prevcaretpos))))

(defn evolve-text [component prevcaretpos caretpos old-selection-mark old-text supplied-text]
  (let [has-selection (not= prevcaretpos old-selection-mark)
        sstart (min prevcaretpos old-selection-mark)
        send (max prevcaretpos old-selection-mark)
        backspace (= (keyboard/get-key component) KeyEvent/VK_BACK_SPACE)
        delete (= (keyboard/get-key component) KeyEvent/VK_DELETE)
        enter (and (= (keyboard/get-key component) KeyEvent/VK_ENTER) (:multiline component))]
    (if (and (keyboard/key-typed? component) (not enter))
      ;; TODO entered text should replace selection - this does not work for some reason
      (insert-text old-text prevcaretpos has-selection sstart send supplied-text)
      (if (keyboard/key-pressed? component)
        (cond
          (and (or backspace delete) has-selection) (str (subs old-text 0 sstart) (subs old-text send))
          backspace (if (> prevcaretpos 0)
                      (str (subs old-text 0 caretpos) (subs old-text (+ caretpos 1)))
                      old-text)
          delete (if (< prevcaretpos (awt/strlen old-text))
                   (str (subs old-text 0 prevcaretpos) (subs old-text (+ caretpos 1)))
                   old-text)
          enter (insert-text old-text prevcaretpos has-selection sstart send "\n")
          :else old-text)
        old-text))))

(defn evovle-selection-mark [component caret-pos old-selection-mark]
  (cond
    (keyboard/key-typed? component) caret-pos
    (keyboard/key-pressed? component) (if (inputbase/with-shift? component) old-selection-mark caret-pos)
    :else old-selection-mark))

(defn pos->coord [component lines pos]
  (if (and (:multiline component) (> (count lines) 1))
    (loop [cl 0
           l 0]
      ;; (- caretpos (max 0 (dec l))) rather than just caretpos because need to skip linebreaks
      (if (and (< cl (- pos (max 0 (dec l)))) (< l (count lines)))
        (recur
          (+ cl (.length (nth lines l)))
          (inc l))
        (let [line (max 0 (dec l))
              linebreaks-to-skip line
              total-len-prev-lines (if (pos? line) (- cl (.length (nth lines line))) 0)]
          [line (max 0 (- pos total-len-prev-lines linebreaks-to-skip))])))
    [0 pos]))

;; TODO avoid duplication with skin
(defn- text-str-h [] (* (flatgui.awt/strh) 2.5))

(defn- split-to-lines [text]
  (let [raw-lines (clojure.string/split-lines text)]
    (if (.endsWith text "\n")
      (fgc/conjv raw-lines "")
      raw-lines)))

(fg/defevolverfn text-model-evolver :model
  (cond
    (clipboard/clipboard-event? component)
    (let [text (clipboard/get-plain-text component)]
      (merge old-model {:text text
                        :lines (if (get-property [:this] :multiline) (split-to-lines text) [text])
                        :caret-pos 0
                        :selection-mark 0
                        :caret-line 0
                        :caret-line-pos 0
                        :selection-mark-line 0
                        :selection-mark-line-pos 0}))

    (mouse/is-mouse-event? component)
    (if (and
          (pos? (count (:lines old-model)))
          (mouse/mouse-left? component))
      (let [click-line (if (get-property [:this] :multiline)
                         (min (int (/ (mouse/get-mouse-rel-y component) (text-str-h))) (dec (count (:lines old-model))))
                         0)
            click-line-text (nth (:lines old-model) click-line)
            click-x (mouse/get-mouse-rel-x component)
            click-line-pos (if (pos? (.length click-line-text))
                             (max
                               0
                               (loop [i 0]
                                 (if (or (= i (.length click-line-text)) (>= (awt/strw (subs click-line-text 0 i)) click-x))
                                   (if (> click-x (awt/strw click-line-text)) i (dec i))
                                   (recur
                                     (inc i)))))
                             0)
            click-pos (if (> click-line 0)
                        (+
                          (apply + (map #(.length %) (take (dec click-line) (:lines old-model)))) ; All previous lines
                          (dec click-line) ; A linebreak symbol after each previous line
                          click-line-pos)
                        click-line-pos)]
        (merge old-model {:caret-pos click-pos
                          :selection-mark (if (mouse/mouse-dragged? component) (:selection-mark old-model) click-pos)
                          :caret-line click-line
                          :caret-line-pos click-line-pos
                          :selection-mark-line (if (mouse/mouse-dragged? component) (:selection-mark-line old-model) click-line)
                          :selection-mark-line-pos (if (mouse/mouse-dragged? component) (:selection-mark-line-pos old-model) click-line-pos)}))
      old-model)

    :else
    (let [text-supplier (:text-supplier component)
          supplied-text (text-supplier component)
          prevcaretpos (:caret-pos old-model)
          old-caret-line (:caret-line old-model)
          old-caret-line-pos (:caret-line-pos old-model)
          old-selection-mark (:selection-mark old-model)
          old-text (:text old-model)
          old-lines (:lines old-model)
          caretpos (evovle-caret-pos component prevcaretpos old-caret-line-pos old-caret-line old-selection-mark old-text old-lines supplied-text)
          text (evolve-text component prevcaretpos caretpos old-selection-mark old-text supplied-text)
          lines (split-to-lines text)
          selection-mark (evovle-selection-mark component caretpos old-selection-mark)
          caret-coord (pos->coord component lines caretpos)
          selection-mark-coord (pos->coord component lines selection-mark)
          ;debug-text (.replace text "\n" "*")
          ;_ (println "new lines" lines
          ;           "count" (count lines)
          ;           "lengths" (map count lines)
          ;           "caretpos" caretpos
          ;           "caret-coord" caret-coord
          ;           (str (subs debug-text 0 caretpos) "|" (subs debug-text caretpos)))
          ]
      {:text text
       :caret-pos caretpos
       :selection-mark selection-mark
       :lines lines
       :caret-line (nth caret-coord 0)
       :caret-line-pos (nth caret-coord 1)
       :selection-mark-line (nth selection-mark-coord 0)
       :selection-mark-line-pos (nth selection-mark-coord 1)})))

(fg/defevolverfn :text (:text (get-property component [:this] :model)))

(fg/defevolverfn :first-visible-symbol
  (if (and (not (:multiline component)) (not (:auto-size component)))
    (let [model (get-property component [:this] :model)]
      (if (>= old-first-visible-symbol (:caret-pos model))
        (:caret-pos model)
        (let [caret-pos (- (:caret-pos model) old-first-visible-symbol)
              text (:text model)
              caret-x (get-caret-x (subs text old-first-visible-symbol) caret-pos)
              width (- (m/x (get-property component [:this] :clip-size)) (* 1 (get-hgap)) (awt/px))]
          (if (> caret-x width)
            (let [diff (- caret-x width)]
              (+
                old-first-visible-symbol
                (loop [i 1]
                  (if (>= (awt/strw (subs text old-first-visible-symbol (+ old-first-visible-symbol i))) diff)
                    i
                    (recur
                      (inc i))))))
            old-first-visible-symbol))))
    0))

(defn- keep-in-range [mx cs content-size]
  (let [x (m/mx-x mx)
        y (m/mx-y mx)]
    (m/translation
      (if (neg? x) (max x (- (m/x cs) (m/x content-size)))  x)
      (if (neg? y) (max y (- (m/y cs) (m/y content-size))) y))))

(fg/defevolverfn auto-scroll-evolver :viewport-matrix
  (let [text-field-id (first (first (:children component)))
        model (get-property [:this text-field-id] :model)
        lines (:lines model)
        reason (fg/get-reason)]
    (if (and (pos? (count lines)) (or (= reason [:this text-field-id]) (= reason [:this])))
      (let [caret-line-pos (:caret-line-pos model)
            caret-line (:caret-line model)
            line-text (nth lines caret-line)
            caret-x-left (awt/strw (if (< caret-line-pos (.length line-text)) (subs line-text 0 caret-line-pos) line-text))
            caret-x-right (+ (* 2 (get-hgap)) (awt/strw (if (< caret-line-pos (.length line-text)) (subs line-text 0 caret-line-pos) line-text)))
            caret-y-top (* caret-line (text-str-h))
            caret-y-btm (* (inc caret-line) (text-str-h))
            vmx (- (m/mx-x old-viewport-matrix))
            vmy (- (m/mx-y old-viewport-matrix))
            cs (get-property [:this] :clip-size)]
        (keep-in-range
          (if (and
                (>= caret-x-left vmx)
                (< caret-x-right (+ vmx (m/x cs)))
                (>= caret-y-top vmy)
                (< caret-y-btm (+ vmy (m/y cs))))
            (flatgui.widgets.scrollpanel/scrollpanelcontent-viewport-matrix-evolver component)
            (m/translation
              (cond
                (< caret-x-left vmx) (- caret-x-left)
                (>= caret-x-right (+ vmx (m/x cs))) (- (- caret-x-right (m/x cs)))
                :else (- vmx))
              (cond
                (< caret-y-top vmy) (- caret-y-top)
                (>= caret-y-btm (+ vmy (m/y cs))) (- (- caret-y-btm (m/y cs)))
                :else (- vmy))))
          cs
          (get-property [:this] :content-size)))
      (if (and
            (vector? reason)
            (= 2 (count reason))
            (= :scroller (nth reason 1))
            (get-property [(nth reason 0) :scroller] :mouse-capture))
        (flatgui.widgets.scrollpanel/scrollpanelcontent-viewport-matrix-evolver component)
        old-viewport-matrix))))

(fg/defevolverfn auto-size-evolver :clip-size
  (if (:auto-size component)
    (let [parent-size (get-property [] :clip-size)
          parent-w (m/x parent-size)
          parent-h (m/y parent-size)
          lines (:lines (get-property [:this] :model))]
      (if (pos? (count lines))
        (let [preferred-w (+ (apply max (map awt/strw lines)) (* 2 (get-hgap)))
              preferred-h (* (count lines) (text-str-h))]
          (m/defpoint (max preferred-w parent-w) (max preferred-h parent-h)))
        (m/defpoint parent-w parent-h)))
    old-clip-size))

;;;
;;;TODO listen to timer
;;;
(fg/defevolverfn :caret-visible
  (= :has-focus (:mode (get-property [:this] :focus-state))))

(defn textfield-dflt-text-suplier [component]
  (if (not
        (#{KeyEvent/VK_BACK_SPACE KeyEvent/VK_DELETE KeyEvent/VK_LEFT KeyEvent/VK_RIGHT KeyEvent/VK_HOME KeyEvent/VK_END}
          (keyboard/get-key component)))
    (keyboard/get-key-str component)
    ""))

(defn textfield-num-only-text-suplier [component]
  (let [key (textfield-dflt-text-suplier component)]
    (if (some #(= key %) '("0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "0" ".")) key "")))

(fg/defwidget "textfield"
  {:v-alignment :center
   :h-alignment :left
   :multiline false
   :auto-size false
   :paint-border true
   :text-supplier textfield-dflt-text-suplier
   :caret-visible false
   :model {:text "" :caret-pos 0 :selection-mark 0 :caret-line 0}
   :text ""
   :first-visible-symbol 0
   :focusable true
   :skin-key [:textfield]
   ;; TODO move out
   :foreground :prime-1
   :no-mouse-press-capturing true
   :evolvers {:model text-model-evolver
              :text text-evolver
              :first-visible-symbol first-visible-symbol-evolver
              :caret-visible caret-visible-evolver
              :clip-size auto-size-evolver}}
  flatgui.widgets.component/component)