(ns epeus.tree-utils)

(defn map-nodes
  [f root]
  (let [walk (fn walk [parent node]
               (lazy-seq
                (cons (f parent node)
                      (mapcat (fn [[k v]] (walk node v))
                              (:children node)))))]
    (walk nil root)))

(defn apply-tree
  [f root]
  (let [walk (fn walk [node]
               (-> (f node)
                   (update-in [:children]
                              #(reduce
                                (fn [r [k v]]
                                  (let [n (walk v)]
                                    (assoc r (:uid n) n))) {} %))))]
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

