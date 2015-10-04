; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.util.decimal)

(defn round-granular [n g]
  (let [r (* g (Math/round (/ n g)))
        fg (/ 1 g); Without second stage of rounding 0.34999999999999987 -> 0.35000000000000003
        ]
       (/ (Math/round (* r fg)) fg)))