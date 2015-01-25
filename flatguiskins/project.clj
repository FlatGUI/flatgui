; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

;breaks IDE (load-string (slurp "../projectcommon.clj"))
(def flatgui-version "0.1.0-SNAPSHOT")

(defproject org.flatgui/flatguiskins flatgui-version
  :description "Default FlatGUI skins"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.flatgui/flatguicore ~flatgui-version]]
  :omit-source true
  :aot :all)
