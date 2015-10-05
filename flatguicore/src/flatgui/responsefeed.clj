; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.responsefeed
  (:require [flatgui.util.matrix :as m])
  (:import (java.awt.geom AffineTransform)))

;;; TODO Find better place; do not duplicate with flatgui.awt

(defn unitsizepx [] 64.0)

(defn affinetransform [matrix]
  (let [m00 (double (m/mx-get matrix 0 0))
        m10 (double (m/mx-get matrix 1 0))
        m01 (double (m/mx-get matrix 0 1))
        m11 (double (m/mx-get matrix 1 1))
        m02 (* (unitsizepx) (double (m/mx-get matrix 0 3)))
        m12 (* (unitsizepx) (double (m/mx-get matrix 1 3)))]
    (new AffineTransform m00 m10 m01 m11 m02 m12)))

;;; ;;;

(defn extract-single [property f container]
  (f (property container)))

(defn extract-multiple [property f container]
  ;; This reduce accumulates bit flags
  (reduce #(+ (* 2 %1) %2) 0 (for [p property] (f (p container)))))

(defn extract-strings [look-vec]
  (mapcat
    #(cond
      (string? %) [%]
      (vector? %) (extract-strings (if (pos? (count %))
                                     ; Clean #0: it is a string (a paint command name) that should not be added to pool
                                     (assoc % 0 nil)
                                     %))
      :else [])
    look-vec))

(defn extract-position-matrix [container]
  (extract-single :position-matrix affinetransform container))

(defn extract-viewport-matrix [container]
  (extract-single :viewport-matrix affinetransform container))

(defn- clip-size-extractor [c] [(m/x c) (m/y c)])

(defn extract-clip-size [container]
  (extract-single :clip-size clip-size-extractor container))

(defn extract-look-vector [container]
  (extract-single :look-vec identity container))

(defn extract-child-count [container]
  (extract-single :children count container))

(defn- boolean-bit-flag-extractor [v] (if v 1 0))

(defn extract-bit-flags [container]
  (extract-multiple [:rollover-notify-disabled :popup :visible] boolean-bit-flag-extractor container))

(defn extract-string-pool [container]
  (extract-single :look-vec extract-strings container))

(defn extract-cursor [container]
  (extract-single :cursor identity container))
