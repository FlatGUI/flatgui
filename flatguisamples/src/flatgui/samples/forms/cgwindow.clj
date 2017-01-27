; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns flatgui.samples.forms.cgwindow
  (:require [flatgui.util.matrix :as m]
            [flatgui.base :as fg]
            [flatgui.awt :as awt]
            [flatgui.inputchannels.mouse :as mouse]
            [flatgui.widgets.panel :as panel]
            [flatgui.widgets.floatingbar :as floatingbar]
            [flatgui.paint :as fgp])
  (:import (java.util UUID)))

(fgp/deflookfn bar-look (:theme :focus-state :id)
 [(fgp/call-look flatgui.skins.flat/component-look)
  (awt/setColor (:prime-2 theme))
  (awt/drawRect 0 0 w- h-)
  ;(awt/drawString (str id) (awt/px) 0.25)
  ])

(declare create-init-child)

(fg/defevolverfn :children
                 (do
                   ;(println "--------------- evolving :children for " (get-reason))
                   (cond

                     ;(nil? (get-reason))
                     ;(let [_ (println "==================================== INIT CHILDREN ==================================")
                     ;      ]
                     ;  (assoc
                     ;    old-children
                     ;    ;:c1 (create-init-child :c1 1)
                     ;    ;:c3 (create-init-child :c3 3)
                     ;    ;:c5 (create-init-child :c5 5)
                     ;    ;:c7 (create-init-child :c7 7)
                     ;    ;:c9 (create-init-child :c9 9)
                     ;    ))

                     ;(and (mouse/mouse-clicked? component) (mouse/mouse-left? component))
                     (mouse/mouse-clicked? component)
                     (let [c-id (keyword (str (UUID/randomUUID)))
                           c (fg/defcomponent
                               floatingbar/floatingbar
                               c-id
                               {:position-matrix (m/translation (mouse/get-mouse-rel-x component) (mouse/get-mouse-rel-y component))
                                :clip-size (m/defpoint 0.25 0.25)
                                :look bar-look
                                :background (awt/color 9 86 17)})

                           c-map (into {} (map (fn [%]
                                                 (let [rcid (keyword (str c-id "-" %))]
                                                   [rcid (assoc c :id rcid
                                                                  :position-matrix (m/translation (+ (mouse/get-mouse-rel-x component) (* 0.015625 %)) (mouse/get-mouse-rel-y component)))]))
                                               (range 300)))

                           ]
                       ;(assoc
                       ;  old-children
                       ;  c-id
                       ;  c)
                       (merge old-children c-map)
                       )

                     :else
                     old-children)))


;(defn create-init-child [id d]
;  (let [size 2
;        md (* size d)]
;    (fg/defcomponent
;      floatingbar/floatingbar
;      id
;      {:position-matrix (m/translation md md)
;       :clip-size (m/defpoint size size)
;       :background (awt/color 86 26 17)
;       :look bar-look
;       ;:evolvers {:children children-mleft-evolver}
;       })))

(fg/defevolverfn :visible
  (> (count (get-property [:c2] :children)) 2))

(def root-panel
  (fg/defcomponent
    panel/panel
    :main
    {:clip-size (m/defpoint 40 20)
     :background (awt/color 9 17 26)
     :evolvers {:children children-evolver}
     }

    (fg/defcomponent
      floatingbar/floatingbar
      :c1
      {:position-matrix (m/translation 1 1)
       :clip-size (m/defpoint 2 2)
       :background (awt/color 86 26 17)
       :look bar-look
       :visible false
       :evolvers {:visible visible-evolver}
       })

    (fg/defcomponent
      floatingbar/floatingbar
      :c2
      {:position-matrix (m/translation 0 0)
       :clip-size (m/defpoint 2 2)
       :background (awt/color 86 26 17)
       :look bar-look
       :visible false
       ;:evolvers {:children children-evolver}
       })


    ;(fg/defcomponent
    ;  floatingbar/floatingbar
    ;  :c55
    ;  {:position-matrix (m/translation 5 1)
    ;   :clip-size (m/defpoint 2 2)
    ;   :background (awt/color 86 26 17)
    ;   :look bar-look
    ;   :children {:c551 (fg/defcomponent
    ;                      floatingbar/floatingbar
    ;                      :c551
    ;                      {:position-matrix (m/translation 0.5 0.5)
    ;                       :clip-size (m/defpoint 1 1)
    ;                       :background (awt/color 17 26 86)
    ;                       :look bar-look
    ;                       :evolvers {:children children-mleft-evolver}})}
    ;   ;:evolvers {:children children-mleft-evolver}
    ;   })
    ))