(ns historian.core)

(def alexandria 
  "The great library... store your stuff here."
  (atom []))

(def overseer
  "Who should we record?"
  (atom {}))

(defn- register-atom! [atom k]
  (swap! overseer assoc k atom))

(defn- de-register-atom! [k]
  (swap! overseer dissoc k))

(defn- snapshot [k]
  {:atom (deref (get @overseer k))
   :key k
   :timestamp 
   #+cljs (goog.now) 
   #+clj (System/currentTimeMillis)
   })

(defn- take-snapshots []
  (map snapshot (keys @overseer)))

(defn- different-from-last? [new]
  (let [remove-ts-fn #(dissoc % :timestamp)
        old (peek @alexandria)]
    (not= (map remove-ts-fn old)
          (map remove-ts-fn new))))

(defn- save-snapshots! [snaps]
  (swap! alexandria conj snaps))

(defn- save-if-different! [snaps]
  (when (different-from-last? snaps)
    (save-snapshots! snaps)))

(def ^:dynamic *record-active* true)

(defn- watch! [atm]
  (add-watch atm ::historian-watch
             (fn [_ _ _ _]
               (when *record-active*
                 (save-if-different! (take-snapshots))))))

(defn- remove-watch! [a]
  (remove-watch a ::historian-watch))

(defn- can-undo?* [records]
  (>= (count records) 1))  ;; must have at least a past state


;;;; main API


(defn trigger-record!
  "Trigger a record to history. The current state of at least one atom
  must be different from the previous one for the record to be
  included into history."[]
  (when *record-active*
    (save-if-different! (take-snapshots))))


(defn replace-library!
  "The library atom (where all records are kept) will be replaced by
  the new-atom. Useful if you've already done some modifications to
  the new-atom (like added some watchers). Depending on where you use
  this function, you might want to fire a `trigger-record!' just
  after."[new-atom] 
  #+cljs (set! historian.core/alexandria new-atom)
  #+clj (intern 'historian.core 'alexandria new-atom))

(defn record!
  "Add the atom to the overseer watch. When any of the atom under its
  watch is modified, it triggers a save of every atom to history (if
  any of the atom is different form the last historic state. Return
  the atom." [atm k]
  (register-atom! atm k)
  (watch! atm)
  (trigger-record!)
  atm)

(defn stop-record! 
  "Remove the atom associated to this key from the overseer watch.
  This atom will no longer be watched, nor its state saved to
  history."[k]
  (remove-watch (get @overseer k) k)
  (de-register-atom! k))

(defn can-undo?
  "Do we have enough history to undo?"[]
  (can-undo?* @alexandria))


(defn restore! [snaps]
  (binding [*record-active* false]
    (doseq [s snaps]
      (reset! (get @overseer (:key s)) (:atom s)))))

(defn restore-last! []
  (let [alex @alexandria]
    (when (can-undo?* alex)
      (->> alex
           pop 
           (reset! alexandria) 
           peek 
           restore!))))

(defn clear-history! []
  (reset! alexandria []))

#+clj
(defmacro off-the-record
  "Temporarily deactivate the watches write to history."
  [& content]
  `(binding [*record-active* false]
     ~@content))

#+clj
(defmacro with-single-record
  "Temporarily deactivate the watches write to history. A single write
   is triggered at the end of the macro, assuming at least one of the
   atoms watched by the overseer has changed." [& content]
   `(do (off-the-record ~@content)
        (trigger-record!)))
