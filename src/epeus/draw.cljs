(ns epeus.draw
  (:require [epeus.tree-utils :refer [map-nodes]]
            [epeus.utils :refer [darken]]))

(def FONT-SIZE 8)
(def NODE-TEXT-FONT "16px Lato")
(def IMAGE-MARGIN 20)

(defn text-width
  [context text]
  (.-width (.measureText context text)))

(defn- build-node-data
  [context {fx :x} {:keys [x y uid title children root? color]}]
  (let [side      (if (< x fx) :left :right)
        has-kids? (not (empty? children))
        width     (text-width context title)]
    (vector uid {:x  x
                 :y  y
                 :title title
                 :tx (cond
                      root?                 x
                      (and (not has-kids?)
                           (= side :left))  (- x width)
                      (and has-kids?
                           (= side :left))  (- x (* 0.2 width))
                      (and has-kids?
                           (= side :right)) (- x (* 0.8 width))
                      :else                 x)
                 :ty (cond
                      root?     (+ y (* 1.15 FONT-SIZE))
                      has-kids? (- y (* 1.2 FONT-SIZE))
                      :else     (+ y (* 0.65 FONT-SIZE)))
                 :r? root?
                 :w  width
                 :c  color
                 :h  FONT-SIZE
                 :k? has-kids?
                 :s  side})))

(defn- build-dimensions
  [context items]
  (aset context "font" NODE-TEXT-FONT)
  (into {} (map-nodes (partial build-node-data context) items)))

(defn- adjust-stats
  [[minx miny maxx maxy] [lx ly rx ry]]
  (vector (min minx lx) (min miny ly) (max maxx rx) (max maxy ry)))

(defn- node-stats
  [[_ {:keys [tx ty w h]}]]
  [tx ty (+ tx w) (+ ty h)])

(defn- calculate-canvas-stats
  [dimensions]
  (->> dimensions
       (map node-stats)
       (reduce adjust-stats)))

(defn- calculate-canvas-size
  [minx miny maxx maxy]
  (vector (+ (* 2 IMAGE-MARGIN) (- maxx minx))
          (+ IMAGE-MARGIN       (- maxy miny))))

(defn- draw-node
  [context {:keys [tx ty title c] :as n}]
  (doto context
    (aset "font" NODE-TEXT-FONT)
    (aset "fillStyle" (darken c 0.15))
    (.fillText title tx ty)))

(defn- connection-points
  [fx fy fw fh fr? tx ty side]
  (let [sx (if fr? (+ fx fw) fx)
        sy (if fr? (+ fy (/ fh 2)) fy)]
    (if (= side :left)
      [[fx sy] [tx ty]]
      [[sx sy] [tx ty]])))

(defn- draw-connection
  [context {fx :x fy :y fw :w fh :h fr? :r?} {tx :x ty :y side :s color :c}]
  (let [[[sx sy] [ex ey]] (connection-points fx fy fw fh fr? tx ty side)
        dx (Math/max (Math/abs (/ (- sx ex) 2)) 10)
        dy (Math/max (Math/abs (/ (- sy ey) 2)) 10)]
    (doto context
      (aset "strokeStyle" color)
      (.beginPath)
      (.moveTo sx sy))
    (if (= side :left)
      (.bezierCurveTo context (- sx dx) sy (+ ex dx) ey ex ey)
      (.bezierCurveTo context (+ sx dx) sy (- ex dx) ey ex ey))
    (.stroke context)))

(defn- draw-nodes
  [context dimensions items]
  (doall
   (map-nodes (fn [p n]
                (let [from (get dimensions (:uid p))
                      to   (get dimensions (:uid n))]
                  (when from
                    (draw-connection context from to))
                  (doto context
                    (aset "lineWidth" 3)
                    (draw-node to)))) items)))

(defn- adjust-position
  [off-x off-y]
  (fn [[k {:keys [x y tx ty] :as v}]]
    [k (assoc v
         :x  (- x off-x)
         :y  (- y off-y)
         :tx (- tx off-x)
         :ty (- ty off-y))]))

(defn draw-image
  [items]
  (let [buffer                (.createElement js/document "canvas")
        context               (.getContext buffer "2d")
        dimensions            (build-dimensions context items)
        [minx miny maxx maxy] (calculate-canvas-stats dimensions)
        [width height]        (calculate-canvas-size minx miny maxx maxy)
        off-x                 (- minx IMAGE-MARGIN)
        off-y                 (- miny IMAGE-MARGIN)
        dimensions            (into {} (map (adjust-position off-y off-y) dimensions))]
    ;; resize buffer
    (set! (.-width buffer) width)
    (set! (.-height buffer) height)
    ;; prepare background
    (doto context
      (aset "fillStyle" "#FFFFFF")
      (.fillRect 0 0 width height)
      (draw-nodes dimensions items))
    ;; draw node and their connections
    (.toDataURL buffer "image/png")))
