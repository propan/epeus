(ns epeus.history
  (:require [epeus.state :as epeus]))

(def past
  (atom [(get-in @epeus/app-state [:items])]))

(def future
  (atom []))

(defn can-undo?
  []
  (> (count @past) 1))

(defn can-redo?
  []
  (> (count @future) 0))

(defn- save-restore-point
  [state]
  (when-not (= state (last @past))
    (swap! past conj state)))

(defn undo
  []
  (when (can-undo?)
    (swap! future conj (last @past))
    (swap! past pop)
    (reset! epeus/app-state
            (assoc-in @epeus/app-state [:items] (last @past)))))

(defn redo
  []
  (when (can-redo?)
    (reset! epeus/app-state
            (assoc-in @epeus/app-state [:items] (last @future)))
    (save-restore-point (last @future))
    (swap! future pop)))

(defn create-restore-point
  ([]
     (create-restore-point (get-in @epeus/app-state [:items])))
  ([state]
     (reset! future [])
     (save-restore-point state)))

(defn handle-transaction [tx-data root-cursor]
  (when (= (:tag tx-data) :create-restore-point)
    (create-restore-point (get-in (:new-state tx-data) [:items]))))
