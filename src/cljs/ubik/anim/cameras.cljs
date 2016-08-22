(ns ubik.anim.cameras)

(def THREE js/THREE)

(defn get-perspective-camera
  ([] (get-perspective-camera 75 1 10000 {:x 0 :y 0 :z 1000}))
  ([fov near far {:keys [x y z]}]
   (let [aspect (/ (.-innerWidth js/window) (.-innerHeight js/window))
         camera (THREE.PerspectiveCamera. fov aspect near far)]
     (.set (.-position camera) x y z)
     camera)))
