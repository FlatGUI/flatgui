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

(defn register-container-internal [container-name container]
  (intern container-ns-symbol (symbol container-name) (atom container)))

(defn- get-container-atom-var [container-name] (find-var (symbol "flatgui.appcontainer" container-name)))

(defn- get-container-atom [container-name] (var-get (get-container-atom-var container-name)))

(defn get-container [container-name] (deref (get-container-atom container-name)))

(defn app-evolve-container [container-name target-cell-ids reason]
  (swap!
    (get-container-atom container-name)
    (fn [c] (flatgui.widgets.componentbase/evolve-container c target-cell-ids reason))))

(defn register-container [container-name container]
  (if (and
        (get-container-atom-var container-name)
        (get-container-atom container-name)
        (get-container container-name))
    (flatgui.base/log-debug "Restored existing container name = " container-name " id = " (:id container))
    (do
      (register-container-internal
        container-name
        ;(flatgui.widgets.componentbase/initialize container)
        container)
      (flatgui.base/log-debug "Registered container name = " container-name " id = " (:id container) (System/identityHashCode container)))))
