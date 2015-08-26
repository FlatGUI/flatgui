; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.awt
    (:require
      [flatgui.util.matrix :as m]
      [flatgui.base :as fg] [flatgui.base :as fg])
    (:import
      [java.awt Font Color]
      [java.awt.geom AffineTransform]))


(fg/defaccessorfn strw [component text]
  (.getStringWidth (get-property component [:this] :interop) text))

(fg/defaccessorfn strh [component]
  (.getFontAscent (get-property component [:this] :interop)))

(fg/defaccessorfn halfstrh [component]
  (/ (strh component) 2))

(defn unitsizepx [] 64.0)

(defn px [] (/ 1.0 (unitsizepx)))
(defn tounit [a] (/ a (unitsizepx)))
(defn -px
  ([a] (- a (px)))
  ([a n] (- a (* n (px)))))
(defn +px
  ([a] (+ a (px)))
  ([a n] (+ a (* n (px)))))

(defn color [r g b] (new Color r g b))

(defn mix-colors [c1 c2]
  (color
    (int (/ (+ (.getRed c1) (.getRed c2)) 2))
    (int (/ (+ (.getGreen c1) (.getGreen c2)) 2))
    (int (/ (+ (.getBlue c1) (.getBlue c2)) 2))))

(defn mix-colors31 [c1 c2]
  (color
    (int (/ (+ (* 3 (.getRed c1)) (.getRed c2)) 4))
    (int (/ (+ (* 3 (.getGreen c1)) (.getGreen c2)) 4))
    (int (/ (+ (* 3 (.getBlue c1)) (.getBlue c2)) 4))))

(defn mix-colors-coeff [c1 c2 d]
  (color
    (int (+ (* d (.getRed c1)) (* (- 1.0 d) (.getRed c2))))
    (int (+ (* d (.getGreen c1)) (* (- 1.0 d) (.getGreen c2))))
    (int (+ (* d (.getBlue c1)) (* (- 1.0 d) (.getBlue c2))))))

(defn affinetransform [matrix]
  (let [m00 (double (m/mx-get matrix 0 0))
        m10 (double (m/mx-get matrix 1 0))
        m01 (double (m/mx-get matrix 0 1))
        m11 (double (m/mx-get matrix 1 1))
        m02 (* (unitsizepx) (double (m/mx-get matrix 0 3)))
        m12 (* (unitsizepx) (double (m/mx-get matrix 1 3)))]
    (new AffineTransform m00 m10 m01 m11 m02 m12)))

(defn invert [at]
  (.createInverse at))

;; Converts CSS-syntax font string to AWT font
(defn str->font [font-str]
  (let [italic (.contains font-str "italic")
        bold (.contains font-str "bold")
        style (+ (if italic Font/ITALIC 0) (if bold Font/BOLD 0))
        before-px-str (subs font-str 0 (.indexOf font-str "px"))
        space-index-before-px (.lastIndexOf before-px-str " ")
        size (Integer/valueOf (str (if (>= space-index-before-px 0) (subs before-px-str (inc space-index-before-px)) before-px-str)))
        after-px-str (.replace (subs font-str (.lastIndexOf font-str "px")) "," "")
        font-family-divider-index (.indexOf after-px-str " ")
        name (if (> font-family-divider-index 0) (subs after-px-str 0 font-family-divider-index) after-px-str)]
    (Font. name (int style) size)))

; Extended command codes are 2-bytes where 1st byte is 00000000

; Basic 1-byte commands

(defn setColor                                              ;1  [xxx00|000]
  ([c] (if c
         ["setColor" c]
         (throw (IllegalStateException. "nil color"))))
  ([r g b] (setColor (color r g b))))

(defn drawRect [x y w h] ["drawRect" x y w h])              ;2  [xxxxx|001]
(defn fillRect [x y w h] ["fillRect" x y w h])              ;3  [xxxxx|010]
(defn drawRoundRect [x y w h arcW arcH] ["drawRoundRect" x y w h arcW arcH]) ;4
(defn drawOval [x y w h] ["drawOval" x y w h])              ;5  [xxxxx|011]
(defn fillOval [x y w h] ["fillOval" x y w h])              ;6  [xxxxx|100]
(defn drawString [t x y] ["drawString" (if t t "") x y])    ;7  [x1010|000] [x1110|000]
(defn drawLine [x1 y1 x2 y2] ["drawLine" x1 y1 x2 y2])      ;8  [xxxxx|101]
(defn transform [at] ["transform" at])                      ;9  [xxxxx|110]
(defn clipRect [x y w h] ["clipRect" x y w h])              ;10 [xxxxx|111]
(defn setClip [x y w h] ["setClip" x y w h])                ;11 [xxxx1|000]  Point-based all combinations except 127x31
(defn pushCurrentClip [] ["pushCurrentClip"])               ;12 [00010|000]
(defn popCurrentClip [] ["popCurrentClip"])                 ;13 [00110|000]

; Extended commands

(defn drawImage [imgUri x y] ["drawImage" imgUri x y])          ;1 (regular), 2 (cached)
(defn fitImage [imgUri x y w h] ["fitImage" imgUri x y w h])    ;3 (regular), 4 (cached)
(defn fillImage [imgUri x y w h] ["fillImage" imgUri x y w h])  ;5 (regular), 6 (cached)
(defn setFont [font-str] ["setFont" font-str])      ;7

