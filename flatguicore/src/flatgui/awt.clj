; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.awt
  (:import [flatgui.core FGContainerBase])
  (:use flatgui.util.matrix))

(defn- container [] (FGContainerBase/getCurrentContainer))

(defn awtutil [] (.getAWTUtil (container)))
(defn strw [t] (.getStringWidth (awtutil) t))
(defn strh [] (.getFontAscent (awtutil)))
(defn halfstrh [] (/ (strh) 2))
(defn strlen [t] (if (nil? t) 0 (.length t)))

;(defn evalreason [] (.getInputForCell (container) :keyw "RepaintReason"))
;(defn evalreasonclass [] (.getClass (evalreason)))

;;;;
;;;;@todo without FGContainerBase/isInitialized check unit tests fail when w- and h- are calculated
;;;;      by code generated for look fn
;;;;      And all such dependencies fail when doint aot complilation. Need to refactor
;;;;
;;;;(defn unitsizepx [] (if (true? (FGContainerBase/isInitialized)) (double (.getGeneralProperty (container) "UnitSizePx")) 64.0))
(defn unitsizepx [] (if (container) (double (.getGeneralProperty (container) "UnitSizePx")) 64.0))
;(defn unitsizepx [] 64.0)

(defn px [] (/ 1.0 (unitsizepx)))
(defn tounit [a] (/ a (unitsizepx)))
(defn -px
  ([a] (- a (px)))
  ([a n] (- a (* n (px))))
  )
(defn +px
  ([a] (+ a (px)))
  ([a n] (+ a (* n (px))))
  )

(defn color [r g b] (new java.awt.Color r g b))

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
  (let [ m00 (double (mx-get matrix 0 0))
         m10 (double (mx-get matrix 1 0))
         m01 (double (mx-get matrix 0 1))
         m11 (double (mx-get matrix 1 1))
         m02 (* (unitsizepx) (double (mx-get matrix 0 3)))
         m12 (* (unitsizepx) (double (mx-get matrix 1 3)))]
    (new java.awt.geom.AffineTransform m00 m10 m01 m11 m02 m12)))

(defn invert [at]
  (.createInverse at))

; Extended command codes are 2-bytes where 1st byte is 00000000

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

