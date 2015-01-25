; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Default themes"
      :author "Denys Lebediev"}
    flatgui.theme
  (:use flatgui.awt))

(def dark-theme
  {:default (flatgui.awt/color 56 56 56)
   :very-light (flatgui.awt/color 160 160 160)
   :theme-border (flatgui.awt/color 96 96 96)
   :theme-border-light (flatgui.awt/color 120 120 120)
   :light (flatgui.awt/color 96 96 96)
   :theme-component-foreground (flatgui.awt/color 96 96 96)
   :mid-light (flatgui.awt/color 72 72 72)
   :mid-dark (flatgui.awt/color 32 32 32)
   :half-dark (flatgui.awt/color 24 24 24)
   :theme-text-background (flatgui.awt/color 16 16 16)
   :theme-light-background (flatgui.awt/color 16 16 16)
   :theme-component-background (flatgui.awt/color 16 16 16)
   :dark (flatgui.awt/color 16 16 16)
   :light-selection (flatgui.awt/color 96 128 223)
   :active-selection (flatgui.awt/color 50 80 160)
   :theme-text-selection (flatgui.awt/color 32 64 80)
   :mid-light-selection (flatgui.awt/color 32 64 80)
   :inactive-selection (flatgui.awt/color 12 32 64)
   :dark-selection (flatgui.awt/color 12 16 24)
   :light-text (flatgui.awt/color 255 255 255)
   :theme-caption (flatgui.awt/color 196 196 196)
   :default-text (flatgui.awt/color 196 196 196)

   :table-feature-icon (flatgui.awt/color 50 160 80)
   :light-table-feature-icon (flatgui.awt/color 96 224 128)
   :dark-table-feature-icon (flatgui.awt/color 48 112 64)
   })

(def light-theme
  {:default (flatgui.awt/color 196 196 196)
   :very-light (flatgui.awt/color 255 255 255)
   :theme-border (flatgui.awt/color 96 96 96)
   :theme-border-light (flatgui.awt/color 144 144 144)
   :light (flatgui.awt/color 238 238 238)
   :theme-component-foreground (flatgui.awt/color 64 64 112)
   :mid-light (flatgui.awt/color 144 144 144)
   :mid-dark (flatgui.awt/color 96 96 96)
   :half-dark (flatgui.awt/color 136 136 136)
   :theme-text-background (flatgui.awt/color 255 255 255)
   ;:theme-light-background (flatgui.awt/color 168 168 168)
   :theme-light-background (flatgui.awt/color 144 144 144)
   :theme-component-background (flatgui.awt/color 48 48 48)
   :dark (flatgui.awt/color 72 72 72)
   :light-selection (flatgui.awt/color 96 128 224)
   :active-selection (flatgui.awt/color 102 126 223)
   :theme-text-selection (flatgui.awt/color 129 153 255)
   :mid-light-selection (flatgui.awt/color 102 126 223)
   :inactive-selection (flatgui.awt/color 82 106 208)
   :dark-selection (flatgui.awt/color 12 16 24)
   :light-text (flatgui.awt/color 24 24 24)
   :theme-caption (flatgui.awt/color 255 255 255)
   :default-text (flatgui.awt/color 0 0 0)

   :table-feature-icon (flatgui.awt/color 50 160 80)
   :light-table-feature-icon (flatgui.awt/color 96 224 128)
   :dark-table-feature-icon (flatgui.awt/color 48 112 64)
   })



;;; TODO think of theme metadata (dark/light, tone ...)
;;;

(def light
 {:prime-1 (flatgui.awt/color 0 78 145) ; Button etc. face
  :prime-2 (flatgui.awt/color 51 113 167) ; Shadowed component background
  :prime-3 (flatgui.awt/color 225 241 255) ; Panel surface
  :prime-4 (flatgui.awt/color 255 255 255) ; Regular component background
  :prime-5 (flatgui.awt/color 225 241 255) ; Selection indication
  :prime-6 (flatgui.awt/color 0 78 145) ; Regular component foreground
  :prime-gradient-start (flatgui.awt/color 0 87 152) ; Same usage as :prime-1 but for faces with gradient
  :prime-gradient-end (flatgui.awt/color 0 70 136) ; Same usage as :prime-1 but for faces with gradient
  :extra-1 (flatgui.awt/color 199 199 199) ; Foreground extra
  :extra-2 (flatgui.awt/color 234 237 236) ; Background extra
  :engaged (flatgui.awt/color 34 168 108)  ; Engaged checkable (radiobutton, checkbox, etc)
  })

(def dark
 {:prime-1 (flatgui.awt/color 2 57 104) ; Button etc. face
  :prime-2 (flatgui.awt/color 51 113 167) ; Shadowed component background
  :prime-3 (flatgui.awt/color 0 78 145) ; Panel surface
  :prime-4 (flatgui.awt/color 255 255 255) ; Regular component background
  :prime-5 (flatgui.awt/color 225 241 255) ; Selection indication
  :prime-6 (flatgui.awt/color 255 255 255) ; Regular component foreground
  :prime-gradient-start (flatgui.awt/color 3 67 114) ; Same usage as :prime-1 but for faces with gradient
  :prime-gradient-end (flatgui.awt/color 2 51 95) ; Same usage as :prime-1 but for faces with gradient
  :extra-1 (flatgui.awt/color 145 145 145) ; Foreground extra
  :extra-2 (flatgui.awt/color 234 237 236) ; Background extra
  :engaged (flatgui.awt/color 34 168 108)  ; Engaged checkable (radiobutton, checkbox, etc)
  })