; Copyright (c) 2016 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns
  ^{:doc "Utilities for debugging and troubleshooting through REPL"
    :author "Denys Lebediev"}
  flatgui._old_debug
  (:require [flatgui._old_appcontainer :as ac]))

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