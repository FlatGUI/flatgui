; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns colorchooser
  (:require [flatgui.skins.flat]
            [flatgui.usagestats]
            [flatgui.samples.forms.colorchooserwin :as cc])
  (:import (java.util.function BiConsumer)))

(def colorpanel (flatgui.base/defroot
                  (assoc cc/root-panel :_usage-stats-collector flatgui.usagestats/default-usagestats-collector)))

(def usage-stats-reporter
  (reify BiConsumer
    (accept [_this session-info fg-container]
      (if-let [stats (:_usage-stats (.getContainer (.getFGModule fg-container)))]
        (let [stats-report-str (apply str (for [[k v] stats] (str k "\n"
                                                                  (apply str (for [[e c] v] (str "    " e ":" c " ")))
                                                                  "\n")))]
          (println "USAGE STATS for" session-info "\n" stats-report-str))))))