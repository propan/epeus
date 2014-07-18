(ns epeus.draw
  (:require [epeus.tree-utils :refer [map-nodes]]))

(def FONT-SIZE 16)
(def IMAGE-MARGIN 20)

(defn text-width
  [context text]
  (.-width (.measureText context text)))

(defn- adjust-bounds
  [[minx miny maxx maxy] [x y]]
  (vector (min minx x) (min miny y) (max maxx x) (max maxy y)))

(defn- get-bounds
  [context parent {:keys [uid x y title]}]
  (let [width (text-width context title)]
    (vector (+ x width) (+ y FONT-SIZE))))

(defn calculate-canvas-size
  [context items]
  (let [properties            (map-nodes (partial get-bounds context) items)
        [minx miny maxx maxy] (reduce adjust-bounds [0 0 0 0] properties)
        margin                (* 2 IMAGE-MARGIN)]
    (vector (+ margin (- maxx minx)) (+ margin (- maxy miny)))))

(defn- create-context
  [buffer]
  (doto (.getContext buffer "2d")
    (aset "font" "16px Lato")))

(defn draw-node
  [context graph {:keys [x y title color uid]}]
  (let [width (text-width context title)]
    (doto context
      (aset "font" "16px Lato")
      (aset "fillStyle" color)
      (.fillText title x y))
    (assoc graph uid [width FONT-SIZE])))

(defn draw-nodes
  [context items]
  (reduce (partial draw-node context) {} (map-nodes (fn [p n] n) items)))

;; find dimensions
;; create a canvas
;; draw nodes and memorize node dimensions
;; draw links

(defn draw-image
  [items]
  (let [buffer         (.createElement js/document "canvas")
        context        (create-context buffer)
        [width height] (calculate-canvas-size context items)]
    ;; resize buffer
    (set! (.-width buffer) width)
    (set! (.-height buffer) height)
    (doto context
      (aset "fillStyle" "#FFFFFF")
      (.fillRect 0 0 width height)
      (draw-nodes items))
    
    (.toDataURL buffer "image/png")))
