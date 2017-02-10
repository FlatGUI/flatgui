; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.photoapp.listcommons
  (:require [flatgui.base :as fg]
            [flatgui.inputchannels.mouse :as mouse]))

(defn gen-item-id [i] (keyword (str "item" i)))

(fg/defevolverfn :selected-item
  (if (and
        (vector? (get-reason))
        (= 2 (count (get-reason))))
    (if (get-property [:this (second (get-reason))] :got-click)
      (get-property [:this (second (get-reason))] :model-index)
      old-selected-item)
    old-selected-item))

(fg/defevolverfn :selected
  (let [pos (get-property [:this] :model-index)
        sel (get-property [] :selected-item)]
    (= pos sel)))

(fg/defevolverfn :got-click
  (if (and
        (= [:this] (get-reason))
        (get-property [:this] :selected))
    false
    (if (mouse/mouse-pressed? component)
      true
      old-got-click)))
