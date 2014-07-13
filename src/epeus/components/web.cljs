(ns epeus.components.web
  (:require-macros [cljs.core.async.macros :refer [alt! go]])
  (:require [cljs.core.async :as async :refer [chan tap]]
            [epeus.events :refer [mouse-down mouse-move mouse-up keyboard-alt]]
            [epeus.components.node :refer [node-component]]
            [epeus.utils :as u :refer [element-bounds hidden next-uid now]]
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

(defn child-position
  [graph {px :x py :y puid :uid} {cx :x cy :y cuid :uid}]
  (let [[pw ph] (get graph puid)
        [cw ch] (get graph cuid)]
    (if (and pw ph cw ch (< (+ cx cw) (+ px (/ pw 2))))
      :left
      :right)))

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

(defn create-restore-point
  [state]
  (om/update! state [:main :modified] (now) :create-restore-point))

(defn move-node
  [state [{:keys [uid] :as node} [dx dy]] backup?]
  (om/transact! state [:main :items]
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
                                (== uid (:uid n)))))
  (when backup?
    (create-restore-point state)))

(defn rename-node
  [state [uid title]]
  (om/transact! state [:main :items]
                #(apply-match (fn [node]
                                (assoc node :title title))
                              %
                              (fn [n]
                                (== uid (:uid n)))))
  (create-restore-point state))

(defn offsets
  "Generates an infinite sequence of offsests starting with initital value of :init and a step of :step.

   Example:
   (take 5 (offsets 0 10)) => (10 -10 20 -20 30)"
  [init step]
  (let [next (+ init step)]
    (concat
     [next (- next)]
     (lazy-seq
      (offsets next step)))))

(defn- overlap?
  "Checks if a new node at position :y overlaps with any of existing children."
  [y children]
  (some #(< (Math/abs (- y (:y %))) 20) children))

(defn best-position
  "Calculates the best position for a new node."
  [x y width side children]
  (let [predicate   (if (= side :left) < >)
        offset-x-fn (if (= side :left) - +)
        children    (vec (filter #(predicate (:x %) x) (vals children)))
        x-position  (offset-x-fn x (if (= side :left) 140 (+ width 50)))]
    (if-not (seq children)
      [x-position y]
      (loop [[offset & other] (offsets 0 20)]
        (let [y-position (+ y offset)]
          (if (overlap? y-position children)
            (recur other)
            [x-position y-position]))))))

(defn new-node
  [{:keys [uid x y color children] :as parent} new-uid side offset]
  (let [[nx ny] (best-position x y offset side children)]
    (print nx ny)
    {:uid      new-uid
     :title    ""
     :x        nx
     :y        ny
     :color    (if (= uid -1)
                 (rand-nth INITIAL-COLORS)
                 (randomize-shade color))
     :children {}}))

(defn add-node
  [state [{:keys [uid]} side]]
  (let [[offset] (get-in @state [:graph uid])]
    (om/transact! state [:main :items]
                  #(apply-match (fn [parent]
                                  (let [new-uid (next-uid)]
                                    (update-in parent [:children]
                                               assoc new-uid
                                               (new-node parent new-uid side offset))))
                                %
                                (fn [n]
                                  (= uid (:uid n)))))
    (create-restore-point state)))

(defn remove-node
  [state {:keys [uid]}]
  (om/transact! state [:main :items]
                #(apply-tree (fn [n]
                               (update-in n [:children] dissoc uid)) %))
  (create-restore-point state))

(defn update-tooltip
  [state tooltip]
  (om/transact! state :tooltip (constantly tooltip)))

(defn update-dimensions
  [state [uid dim]]
  (om/transact! state :graph #(assoc % uid dim)))

(defn handle-event
  [type state data]
  (case type
    :rename     (rename-node       state data)
    :move       (move-node         state data false)
    :add        (add-node          state data)
    :remove     (remove-node       state data)
    :drop       (move-node         state data true)
    :tooltip    (update-tooltip    state data)
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
      (let [items (get-in state [:main :items])
            graph (:graph state)]
        (apply dom/div #js {:id "web-container"}
               (apply dom/svg #js {:width  5000
                                   :height 5000
                                   :style  #js {:overflow "hidden"
                                                :z-index  0}}
                      (map-nodes
                       (fn [from to]
                         (let [from-rect (create-rect graph from)
                               to-rect   (create-rect graph to)]
                           (when (and from-rect to-rect)
                             (om/build path-component {:color     (:color to)
                                                       :from-rect from-rect
                                                       :to-rect   to-rect}
                                       {:react-key (str "link-" (:uid from) "-" (:uid to))}))))
                       items))
               (map-nodes
                (fn [parent node]
                  (om/build node-component (-> node
                                               (dissoc :children)
                                               (assoc  :position (child-position graph parent node))) ;; I'm not quite sure it's the way to go..
                            {:init-state {:comm comm}
                             :react-key  (:uid node)}))
                items))))))
