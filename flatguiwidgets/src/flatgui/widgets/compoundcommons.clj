; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.widgets.compoundcommons
  (:require [flatgui.base :as fg]))

(fg/defevolverfn :belongs-to-focused-editor
  (let [editor-focus-state (get-property [:editor] :focus-state)]
    (= :has-focus (:mode editor-focus-state))))

(fg/defevolverfn child-of-focused-evolver :belongs-to-focused-editor
  (let [editor-focus-state (get-property [] :focus-state)]
    (= :has-focus (:mode editor-focus-state))))

(fg/defevolverfn grandchild-of-focused-evolver :belongs-to-focused-editor
  (let [editor-focus-state (get-property [:_] :focus-state)]
    (= :has-focus (:mode editor-focus-state))))