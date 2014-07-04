(ns epeus.components.web
  (:require-macros [cljs.core.async.macros :refer [alt! go]])
  (:require [cljs.core.async :as async :refer [chan tap]]
            [epeus.events :refer [mouse-down mouse-move mouse-up keyboard-alt]]
            [epeus.components.node :refer [node-component]]
            [epeus.history :refer [create-restore-point]]
            [epeus.utils :as u :refer [element-bounds hidden next-uid]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def INITIAL-COLORS ["#77E401" "#A345FF" "#FFDA45" "#FF8845" "#45FFD2" "#45D2FF" "#45ADFF" "#AD45FF" "#FFF045" "#FF45A6" "#FF5959"])

(defn map-nodes
  [f root]
  (let [walk (fn walk [parent node]
               (lazy-seq
                (cons (f parent node)
                      (mapcat (fn [[ k v]] (walk node v))
                              (:children node)))))]
    (walk nil root)))

(defn apply-tree
  [f root]
  (let [walk (fn walk [node]
               (-> (f node)
                   (update-in [:children]
                              #(reduce
                                (fn [r [k v]] (assoc r k (walk v))) {} %))))]
    (walk root)))

(defn apply-match
  [f root p]
  (let [walk (fn walk [node]
               (if (p node)
                 (f node)
                 (update-in node [:children]
                            #(reduce
                              (fn [r [k v]] (assoc r k (walk v))) {} %))))]
    (walk root)))

;;
;; Helpers
;;

(defn randomize-shade
  [color]
  (if (< 0.5 (Math/random))
    (u/lighten color 0.05)
    (u/darken color 0.05)))

(defn create-rect
  [dims {:keys [x y uid]}]
  (when-let [dim (get dims uid)]
    (into [x y] dim)))

;;
;; Draw
;;

(defn connection-points
  [fx fy fw fh tx ty tw th]
  (let [fw2 (/ fw 2)]
    (if (< (+ tx tw) (+ fx fw2))
      [:left  [fx (+ fy (/ fh 2))] [(+ tx tw) (+ ty (/ th 2))]]
      [:right [(+ fx fw) (+ fy (/ fh 2))] [tx (+ ty (/ th 2))]])))

(defn generate-path
  [[fx fy fw fh] [tx ty tw th]]
  (let [[dir [sx sy] [ex ey]] (connection-points fx fy fw fh tx ty tw th)
        dx (Math/max (Math/abs (/ (- sx ex) 2)) 10)
        dy (Math/max (Math/abs (/ (- sy ey) 2)) 10)
        path (if (= dir :left)
               [(- sx dx) sy (+ ex dx) ey ex ey]
               [(+ sx dx) sy (- ex dx) ey ex ey])]
    (apply str ["M" (apply str 
                           (interpose "," [sx sy]))
                "C" (apply str
                           (interpose "," path))])))

;;
;; Events
;;

(defn move-node
  [state [{:keys [uid] :as node} [dx dy]] tag]
  (om/transact! state :items
                #(apply-match (fn [{:keys [x y] :as root}]
                                (let [delta-x (- x dx)
                                      delta-y (- y dy)]
                                  (apply-tree (fn [n]
                                                (-> n
                                                    (update-in [:x] - delta-x)
                                                    (update-in [:y] - delta-y)))
                                              root)))
                              %
                              (fn [n]
                                (== uid (:uid n))))
                tag))

(defn best-position
  [x y children]
  (if (empty? children)
    [(+ x 40) y]
    (let [[selector incrimentor] (if (> (Math/random) 0.5)
                                   [Math/max +]
                                   [Math/min -])
          [nx ny] (reduce (fn [[x y] [k v]]
                            [(Math/min x (:x v)) (selector y (:y v))])
                          [x y] children)]
      [(+ 40 nx) (incrimentor ny 25)])))

(defn new-node
  [{:keys [uid x y color children] :as parent} new-uid offset]
  (let [[nx ny] (best-position (+ x offset) y children)]
    {:uid      new-uid
     :title    ""
     :x        nx
     :y        ny
     :color    (if (= uid -1)
                 (rand-nth INITIAL-COLORS)
                 (randomize-shade color))
     :children {}}))

(defn add-node
  [state {:keys [uid]}]
  (let [[offset] (get-in @state [:graph uid])]
    (om/transact! state :items
                  #(apply-match (fn [parent]
                                  (let [new-uid (next-uid)]
                                    (update-in parent [:children]
                                               assoc new-uid
                                               (new-node parent new-uid offset))))
                                %
                                (fn [n]
                                  (= uid (:uid n)))) ;; TODO
                  :create-restore-point)))

(defn remove-node
  [state {:keys [uid]}]
  (om/transact! state :items
                #(apply-tree (fn [n]
                               (update-in n [:children] dissoc uid)) %)
                :create-restore-point))

(defn update-tooltip
  [state tooltip]
  (om/transact! state :tooltip (constantly tooltip)))

(defn update-dimensions
  [state [uid dim]]
  (om/transact! state :graph #(assoc % uid dim)))

(defn handle-event
  [type state data]
  (case type
    :move       (move-node   state data :no-restore-point)
    :add        (add-node    state data)
    :remove     (remove-node state data)
    :drag-stop  (move-node   state data :create-restore-point)
    :tooltip    (update-tooltip state data)
    :dim        (update-dimensions state data)
    nil))

;;
;; Component
;;

(defn path-component
  [state owner]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [{:keys [from-rect to-rect color]} state]
        (dom/path #js {:d           (generate-path from-rect to-rect)
                       :strokeWidth "2"
                       :stroke      color
                       :fill        "none"})))))

(defn web-component
  [state owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [events (chan)
            comm   {:events     events
                    :mouse-up   (mouse-up)
                    :mouse-move (mouse-move)
                    :alt-key    (keyboard-alt)}] 
        (om/set-state! owner :comm comm)
        (go
         (while true
           (let [[type data] (<! events)]
             (handle-event type state data))))))

;; TODO: unmount

    om/IRenderState
    (render-state [_ {:keys [comm]}]
      (apply dom/div #js {:id "web-container"}
             (apply dom/svg #js {:width  5000
                                 :height 5000
                                 :style  #js {:overflow "hidden"
                                              :z-index  0}}
                    (let [dim (:graph state)]
                      (map-nodes
                       (fn [from to]
                         (let [from-rect (create-rect dim from)
                               to-rect   (create-rect dim to)]
                           (when (and from-rect to-rect)
                             (om/build path-component {:color     (:color to)
                                                       :from-rect from-rect
                                                       :to-rect   to-rect}
                                       {:react-key (str "link-" (:uid from) "-" (:uid to))}))))
                       (:items state))))
             (map-nodes
              (fn [parent node]
                (om/build node-component (dissoc node :children)
                          {:init-state {:comm comm}
                           :react-key  (:uid node)}))
              (:items state))))))
