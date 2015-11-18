; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.appcontainer
  (:require flatgui.widgets.componentbase flatgui.base))

(def container-ns-symbol (symbol "flatgui.appcontainer"))

(def fork-map-prefix "fork-map-")

(defn register-fork-map-internal [container-name]
  (intern container-ns-symbol (symbol (str fork-map-prefix container-name)) (atom {})))

(defn register-container-internal [container-name container]
  (intern container-ns-symbol (symbol container-name) (atom container)))

(defn- get-container-atom-var [container-name] (find-var (symbol "flatgui.appcontainer" container-name)))

(defn- get-fork-map-atom-var [container-name] (find-var (symbol "flatgui.appcontainer" (str fork-map-prefix container-name))))

(defn- get-container-atom [container-name] (var-get (get-container-atom-var container-name)))

(defn- get-fork-map-atom [container-name] (var-get (get-fork-map-atom-var container-name)))

(defn get-container [container-name] (deref (get-container-atom container-name)))

(defn get-fork [container-name reason] (get (deref (get-fork-map-atom container-name)) reason))

(defn app-evolve-container [container-name target-cell-ids reason]
  (swap!
    (get-container-atom container-name)
    (fn [c] (flatgui.widgets.componentbase/evolve-container c target-cell-ids reason))))

(defn app-fork-container [container-name target-cell-ids reason]
  (swap!
    (get-fork-map-atom container-name)
    (fn [m] (assoc m reason (flatgui.widgets.componentbase/evolve-container (get-container container-name) target-cell-ids reason)))))

(defn app-clear-forks [container-name]
  (reset!
    (get-fork-map-atom container-name)
    {}))

(defn app-use-fork [container-name reason]
  (reset!
    (get-container-atom container-name)
    (get-fork container-name reason)))

(defn app-modify-container [container-name f]
  (swap!
    (get-container-atom container-name)
    f))

(defn- update-interop [container-template interop-util]
  (let [updated (assoc container-template :interop interop-util)
        children (:children container-template)]
    (if children
      (assoc
        updated
        :children (into (array-map) (for [[k v] children] [k (update-interop v interop-util)])))
      updated)))

(defn register-container [container-name container-template interop-util]
  (let [container (update-interop container-template interop-util)]
    (if (and
          (get-container-atom-var container-name)
          (get-container-atom container-name)
          (get-container container-name))
      (flatgui.base/log-debug "Restored existing container name = " container-name " id = " (:id container))
      (do
        (register-container-internal
          container-name
          container)
        (register-fork-map-internal container-name)
        (flatgui.base/log-debug "Registered container name = " container-name " id = " (:id container) (System/identityHashCode container))))))
