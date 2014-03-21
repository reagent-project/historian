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
   :timestamp #+cljs (goog.now) #+clj (System/currentTimeMillis)
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

(def ^:dynamic *watch-active* true)

(defn- watch! [atom]
  (add-watch ::historian-watch
             #(when *watch-active*
                (save-if-different! (take-snapshots)))))

(defn- remove-watch! [atom]
  (remove-watch atom ::historian-watch))


;;;; main API

(defn record! 
  "Add the atom to the overseer watch. When any of the atom under its
  watch is modified, it triggers a save of every atom to history (if
  any of the atom is different form the last historic state."[atom k]
  (register-atom! atom k)
  (watch! atom))

(defn stop-record! 
  "Remove the atom associated to this key from the overseer watch.
  This atom will no longer be watched, nor its state saved to
  history."[k]
  (remove-watch (get @overseer k))
  (de-register-atom! k))

#+clj
(defmacro single-record
  "Temporarily deactivate the watches write to history. A single write
   is triggered at the end of the macro, assuming at least one of the
   atoms watched by the overseer has changed." [&content]
   `(binding [*watch-active* false]
      ~@content
      (save-if-different! (take-snapshots))))

(defn restore! [snaps]
  (binding [*watch-active* false]
    (doseq [{:keys [atom key]} snaps]
      (reset! (get overseer key) atom))))

(defn restore-last! []
  (when-not (empty? @alexandria)
    (restore! (peek (pop alexandria)))))

(defn clear-history! []
  (reset! alexandria []))
