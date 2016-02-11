(ns
  ^{:doc "Utilities for debugging and troubleshooting through REPL"
    :author "Denys Lebediev"}
  flatgui.debug
  (:require [flatgui.appcontainer :as ac]))

(defn get-simplified-container [container]
  (filter
    (fn [[k _]]
      (not
        (#{:children
           :evolvers
           :abs-dependents
           :out-dependents
           :evolver-dependencies
           :evolver-abs-dependencies
           :input-channel-subscribers
           :default-properties-to-evolve-provider
           :aux-container
           :theme
           :skin
           :look
           :interop}
          k)))
    container))

(def gs get-simplified-container)

(defn dump-container [container-id]
  (get-simplified-container (ac/get-container container-id)))

(def dc dump-container)