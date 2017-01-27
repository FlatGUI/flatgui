; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Means for defining input channel data provider"
      :author "Denys Lebediev"}
  flatgui.inputchannels.channelbase)

(defmacro definputparser [fnname event-type body]
  "Convenient macro for defining functions that obtain
  specific data from input channel object such as mouse,
  keyboard events, etc."
  (let [let-bindings '[repaint-reason (.getEvolveReason comp-property-map)]]
      `(defn ~fnname [~'comp-property-map] (let ~let-bindings (if (and (instance? ~event-type ~'repaint-reason) (not (nil? ~'repaint-reason)))
                                                              ~body)))))

(defn find-channel-dependency [s-expr channel-ns channel-kw]
  (if (seq? s-expr)
    (some
      (fn [n] (if
                (and
                  (symbol? n)
                  (let [n-var (resolve n)]
                    (if (var? n-var)
                      (= channel-ns (.. n-var ns name)))))
                channel-kw))
      s-expr)))
