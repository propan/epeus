(ns epeus.draw
  (:require [epeus.tree-utils :refer [map-nodes]]))

(def FONT-SIZE 8)
(def NODE-TEXT-FONT "16px Lato")
(def IMAGE-MARGIN 20)

(defn text-width
  [context text]
  (.-width (.measureText context text)))

(defn- adjust-bounds
  [[minx miny maxx maxy] [lx ly rx ry]]
  (vector (min minx lx) (min miny ly) (max maxx rx) (max maxy ry)))

(defn- node-bounds
  [context parent {:keys [uid x y title]}]
  (let [width (text-width context title)]
    (vector x y (+ x width) (+ y FONT-SIZE))))

(defn calculate-bounds
  [context items]
  (aset context "font" NODE-TEXT-FONT)
  (let [properties (map-nodes (partial node-bounds context) items)]
    (reduce adjust-bounds properties)))

(defn calculate-canvas-size
  [minx miny maxx maxy]
  (vector (+ (* 2 IMAGE-MARGIN) (- maxx minx))
          (+ IMAGE-MARGIN       (- maxy miny))))

(defn draw-node
  [context off-x off-y graph {:keys [x y title color uid]}]
  (doto context
    (aset "font" NODE-TEXT-FONT)
    (aset "fillStyle" color)
    (.fillText title (- x off-x) (- y off-y)))
  (let [width (text-width context title)]
    (assoc graph uid [width FONT-SIZE])))

(defn draw-nodes
  [context off-x off-y items]
  (reduce (partial draw-node context off-x off-y) {} (map-nodes (fn [p n] n) items)))

(defn connection-points
  "Returns a connection side and a list of connection points for two nodes
   at (fx, fy) and (tx, ty) with bounds (fw, fh) and (tw, th) accordingly."
  [fx fy fw fh tx ty tw th]
  (let [fw2 (/ fw 2)]
    (if (< (+ tx tw) (+ fx fw2))
      [:left  [fx        (- fy 5)] [(+ tx tw) (- ty 5)]]
      [:right [(+ fx fw) (- fy 5)] [tx        (- ty 5)]])))

(defn draw-node-connection
  [context [fx fy fw fh] [tx ty tw th]]
  (let [[side [sx sy] [ex ey]] (connection-points fx fy fw fh tx ty tw th)
        dx                    (Math/max (Math/abs (/ (- sx ex) 2)) 10)
        dy                    (Math/max (Math/abs (/ (- sy ey) 2)) 10)]
    (doto context
      (.beginPath)
      (.moveTo sx sy))
    (if (= side :left)
      (.bezierCurveTo context (- sx dx) sy (+ ex dx) ey ex ey)
      (.bezierCurveTo context (+ sx dx) sy (- ex dx) ey ex ey))
    (.stroke context)))

(defn draw-connections
  [context graph off-x off-y items]
  (doseq [[from to] (map-nodes (fn [p n] [p n]) items) :when (map? from)]
    (let [[fw fh] (get graph (:uid from))
          [tw th] (get graph (:uid to))]
      (aset context "strokeStyle" (:color to))
      (aset context "lineWidth" 3)
      (draw-node-connection context
                            [(- (:x from) off-x) (- (:y from) off-y) fw fh]
                            [(- (:x to) off-x)   (- (:y to) off-y)   tw th]))))

(defn draw-image
  [items]
  (let [buffer                (.createElement js/document "canvas")
        context               (.getContext buffer "2d")
        [minx miny maxx maxy] (calculate-bounds context items)
        [width height]        (calculate-canvas-size minx miny maxx maxy)
        off-x                 (- minx IMAGE-MARGIN)
        off-y                 (- miny IMAGE-MARGIN)]
    ;; resize buffer
    (set! (.-width buffer) width)
    (set! (.-height buffer) height)
    ;; prepare background
    (doto context
      (aset "fillStyle" "#FFFFFF")
      (.fillRect 0 0 width height))
    ;; draw node and their connections
    (let [graph (draw-nodes context off-x off-y items)]
      (draw-connections context graph off-x off-y items))
    (.toDataURL buffer "image/png")))
