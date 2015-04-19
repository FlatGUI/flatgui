; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Skin utilities"
      :author "Denys Lebediev"}
  flatgui.skins.skinbase
  (:use flatgui.base))


(defn widgettype->key [widgettype]
  (keyword widgettype))

(defn get-look-from-skin [skin-name skin-key]
  (let [skin (find-ns (symbol skin-name))]
    (if skin
      (let [skin-map (var-get (ns-resolve skin 'skin-map))]
        (get-in skin-map skin-key))
      (throw (IllegalArgumentException. (str "Skin not found:" skin-name))))))

(defevolverfn skin-look-evolver :look
  (cond

    (:skin-key component)
    (get-look-from-skin (get-property component [:this] :skin) (:skin-key component))

    old-look
    old-look

    :else
    (throw (IllegalStateException.
             (str "Component " (:path-to-target component) " " (:id component)
                  " has neither " :skin-key " nor " :look " defined.")))))
