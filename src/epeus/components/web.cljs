(ns epeus.components.web
  (:require-macros [cljs.core.async.macros :refer [alt! go]])
  (:require [cljs.core.async :as async :refer [chan tap]]
            [epeus.events :refer [mouse-down mouse-move mouse-up keyboard-alt]]
            [epeus.components.node :refer [node-component]]
            [epeus.history :refer [create-restore-point]]
            [epeus.utils :refer [element-bounds hidden next-uid]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn map-nodes
  [f root]
  (let [walk (fn walk [node]
               (lazy-seq
                (cons (f node)
                      (mapcat (fn [[ k v]] (walk v))
                              (:children node)))))]
    (walk root)))

(defn map-connections
  [f root]
  (let [walk (fn walk [node]
               (lazy-seq
                (concat
                 (map (fn [[uid child]]
                        (f node child)) (:children node))
                 (mapcat (fn [[ k v]] (walk v))
                         (:children node)))))]
    (walk root)))

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
;; Draw
;;

(defn connection-points
  [xf yf [wf hf] xt yt [wt ht]]
  (let [wf2 (/ wf 2)]
    (if (< (+ xt wt) (+ xf wf2))
      [:left  [xf (+ yf (/ hf 2))] [(+ xt wt) (+ yt (/ ht 2))]]
      [:right [(+ xf wf) (+ yf (/ hf 2))] [xt (+ yt (/ ht 2))]])))

(defn generate-path
  [from from-rect to to-rect]
  (let [fx (:x from)
        fy (:y from)
        tx (:x to)
        ty (:y to)
        [dir [sx sy] [ex ey]] (connection-points fx fy from-rect tx ty to-rect)
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
  [{:keys [x y color children] :as parent} new-uid offset]
  (let [[nx ny] (best-position (+ x offset) y children)]
    {:uid      new-uid
     :title    ""
     :x        nx
     :y        ny
     :color    color
     :children {}}))

(defn add-node
  [state [{:keys [uid]} offset]]
  (om/transact! state :items
                #(apply-match (fn [parent]
                                (let [new-uid (next-uid)]
                                  (update-in parent [:children]
                                             assoc new-uid
                                             (new-node parent new-uid offset))))
                              %
                              (fn [n]
                                (= uid (:uid n)))) ;; TODO
                :create-restore-point))

(defn remove-node
  [state {:keys [uid]}]
  (om/transact! state :items
                #(apply-tree (fn [n]
                               (update-in n [:children] dissoc uid)) %)
                :create-restore-point))

(defn handle-event
  [type state data]
  (case type
    :move       (move-node   state data :no-restore-point)
    :add        (add-node    state data)
    :remove     (remove-node state data)
    :drag-stop  (move-node   state data :create-restore-point)
    nil))

;;
;; Component
;;

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

    om/IRenderState
    (render-state [_ {:keys [comm]}]
      (apply dom/div #js {:id "web-container"}
             (apply dom/svg #js {:width  5000
                                 :height 5000
                                 :style  #js {:overflow "hidden"
                                              :z-index  0}}
                    (let [dim @(om/get-shared owner :dim)]
                      (map-connections
                       (fn [from to]
                         (let [from-rect (get dim (:uid from))
                               to-rect   (get dim (:uid to) [0 20])]
                           (when (and from-rect to-rect)
                             (dom/path #js {:d           (generate-path from from-rect to to-rect)
                                            :strokeWidth "2"
                                            :stroke      (:color to)
                                            :fill        "none"}))))
                       (:items state))))
             (map-nodes
              (fn [node]
                (om/build node-component node
                          {:init-state {:comm comm}
                           :react-key  (:uid node)}))
              (:items state))))))
