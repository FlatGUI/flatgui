; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Utils for accesing container components from REPL"}
  flatgui.util.repl
  (:require [flatgui.comlogic :as fgc]
            [flatgui.appcontainer :as appc]))


(defn get-property [container-name path property]
  (get-in
    (appc/get-container container-name)
    (fgc/conjv (fgc/get-access-key path) property)))

(defn get-all-properties [container-name path]
  (map
    (fn [[k _]] k)
    (get-in
      (appc/get-container container-name)
      (fgc/get-access-key path))))

(defn get-all-children [container-name path]
  (map
    (fn [[k _]] k)
    (get-in
      (appc/get-container container-name)
      (fgc/conjv (fgc/get-access-key path) :children))))

(defn set-property [container-name path property new-value]
  (do
    (appc/app-modify-container
      container-name
      (fn [c] (assoc-in
                c
                (fgc/conjv (fgc/get-access-key path) property)
                new-value)))
    (println "Done.")))

(defn update-property [container-name path property f]
  (do
    (appc/app-modify-container
      container-name
      (fn [c] (update-in
                c
                (fgc/conjv (fgc/get-access-key path) property)
                f)))
    (println "Done.")))