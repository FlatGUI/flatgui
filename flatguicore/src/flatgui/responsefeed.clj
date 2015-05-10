; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.responsefeed
  (:require [flatgui.awt :as awt]
            [flatgui.util.matrix :as m]
            ))

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
  (extract-single :position-matrix awt/affinetransform container))

(defn extract-viewport-matrix [container]
  (extract-single :viewport-matrix awt/affinetransform container))

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