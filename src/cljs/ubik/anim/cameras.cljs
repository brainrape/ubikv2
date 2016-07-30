(ns ubik.anim.cameras)

(def THREE js/THREE)

(defn get-perspective-camera []
  (let [aspect (/ (.-innerWidth js/window) (.-innerHeight js/window))
        camera (THREE.PerspectiveCamera. 75 aspect 1 10000)]
    (set! (.-z (.-position camera)) 1000)
    camera))
