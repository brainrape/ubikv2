(ns ubik.commons)

(def anim-types #{:top :center :bottom :bg})

(def current-anims (atom (into {} (map (fn [type] [type 0]) anim-types))))
