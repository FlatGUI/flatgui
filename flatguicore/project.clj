; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(def flatgui-version "0.1.0-SNAPSHOT")

(def jetty-version "9.2.7.v20150116")

(defproject org.flatgui/flatguicore flatgui-version
  :description "FlatGUI Core"
  :url "http://flatgui.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.eclipse.jetty/jetty-server ~jetty-version]
                 [org.eclipse.jetty/jetty-servlet ~jetty-version]
                 [org.eclipse.jetty/jetty-servlets ~jetty-version]
                 [org.eclipse.jetty.websocket/websocket-servlet ~jetty-version]
                 [org.eclipse.jetty/jetty-util ~jetty-version]
                 [junit/junit "4.12"]]
  :deploy-repositories {"releases" {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/" :creds :gpg}
                        "snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots/" :creds :gpg}}
  :java-source-paths ["src/java"]
  :omit-source true
  :aot :all

  :scm {:url "https://github.com/flatgui/flatgui/"}
  :pom-addition [:developers [:developer
                              [:name "Denys Lebediev"]
                              [:email "denis@flatgui.org"]
                              [:timezone "+2"]]])
