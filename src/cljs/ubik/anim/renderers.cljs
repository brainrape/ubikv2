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
   (.-innerHeight js/window)))

(defn update-animation
  ([renderer animation rt-texture anim-state]
   (let [render-fn (fn [scene camera] (.render renderer scene camera rt-texture true))]
     (animation anim-state render-fn)))
  ([renderer animation anim-state]
   (let [render-fn (fn [scene camera] (.render renderer scene camera))]
     (animation anim-state render-fn))))

(defn get-plane-render-target
  ([] (get-plane-render-target (.-innerWidth js/window) (.-innerHeight js/window)))
  ([size-x size-y]
   (let [rt-texture (get-render-target)
         geometry (THREE.PlaneGeometry. size-x size-y)
         material (THREE.MeshBasicMaterial. #js {:map (.-texture rt-texture)})
         mesh (THREE.Mesh. geometry material)]
     {:rt-texture rt-texture :mesh mesh})))
