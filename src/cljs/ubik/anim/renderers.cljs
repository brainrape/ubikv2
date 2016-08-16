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

(defonce main-renderer (get-main-renderer))

(defn get-render-target []
  (THREE.WebGLRenderTarget.
   (.-innerWidth js/window)
   (.-innerHeight js/window)
   #js {:minFilter THREE.LinearFilter, :magFilter THREE.NearestFilter, :format THREE.RGBFormat}))

(defn update-animation
  ([renderer animation rt-texture anim-state]
   (let [render-fn (fn [scene camera] (.render renderer scene camera rt-texture true))]
     (animation anim-state render-fn)))
  ([renderer animation anim-state]
   (let [render-fn (fn [scene camera] (.render renderer scene camera))]
     (animation anim-state render-fn))))
