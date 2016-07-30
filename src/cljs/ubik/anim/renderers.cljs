(ns ubik.anim.renderers)

(def THREE js/THREE)

(defn get-renderer []
  (let [renderer (THREE.WebGLRenderer.)]
    (.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))
    renderer))

(defn get-main-renderer []
  (let [renderer (get-renderer)]
    (.appendChild (.-body js/document) (.-domElement renderer))
    renderer))
