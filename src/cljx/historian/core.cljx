(ns ^{:doc "Manage states for your atoms. (Easy undo/redo)"
      :author "Frozenlock"
      :quote "The present is the least important time we live in. --Alan Kay"}
  historian.core)

(def alexandria 
  "The great library... store your stuff here."
  (atom []))

(def nostradamus 
  "What will happen in the future..."
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
  (mapv snapshot (keys @overseer)))

(defn- different-from-last? [new]
  (let [remove-ts-fn #(dissoc % :timestamp)
        old (peek @alexandria)]
    (not= (map remove-ts-fn old)
          (map remove-ts-fn new))))

(defn- save-snapshots! [snaps]
  (swap! alexandria conj snaps))

(defn- save-if-different! [snaps]
  (when (different-from-last? snaps)
    (save-snapshots! snaps)
    (reset! nostradamus [])))

(defn- save-prophecies! [snaps]
  (swap! nostradamus conj snaps))

(def ^:dynamic *record-active* true)

(defn- restore!
  "Restore all the atoms being watched to a previous/different state."
  [snaps]
  (binding [*record-active* false]
    (doseq [s snaps]
      (reset! (get @overseer (:key s)) (:atom s)))))

(defn- watch! [atm]
  (add-watch atm ::historian-watch
             (fn [_ _ _ _]
               (when *record-active*
                 (save-if-different! (take-snapshots))))))

(defn- remove-watch! [a]
  (remove-watch a ::historian-watch))

(defn- can-undo?* [records]
  (when (next records) true)) ;; because the CURRENT state is the
                              ;; first in the list of states, we need
                              ;; to have at least 2 (the current, plus
                              ;; a previous one) to be able to undo.

(defn- can-redo?* [records]
  (when (first records) true)) ;; contrary to undo, a single state is
                               ;; enough to redo.


;;;; main API


(defn trigger-record!
  "Trigger a record to history. The current state of at least one atom
  must be different from the previous one for the record to be
  included into history."[]
  (when *record-active*
    (save-if-different! (take-snapshots))))


(defn replace-library!
  "The library atom (where all records are kept to enable 'undo') will
  be replaced by the new-atom. Useful if you've already done some
  modifications to the new-atom (like added some watchers). Depending
  on where you use this function, you might want to fire a
  `trigger-record!' just after.
  
  Usually, one would also want to use `replace-prophecy!' in order to
  replace the 'redo' atom."
  [new-atom] 
  #+cljs (set! historian.core/alexandria new-atom) 
  #+clj (intern 'historian.core 'alexandria new-atom))

(defn replace-prophecy!
  "The prophecy atom (where all records are kept to enable 'redo')
  will be replaced by the new-atom. Useful if you've already done some
  modifications to the new-atom (like added some watchers).

  Usually used with `replace-library'."
  [new-atom]  
  #+cljs (set! historian.core/nostradamus new-atom) 
  #+clj (intern 'historian.core 'nostradamus new-atom))

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
  (remove-watch! (get @overseer k))
  (de-register-atom! k))

(defn can-undo?
  "Do we have enough history to undo?"[]
  (can-undo?* @alexandria))

(defn can-redo?
  "Can we redo?"[]
  (can-redo?* @nostradamus))

(defn undo! []
  (let [alex @alexandria]
    (when (can-undo?* alex)
      (save-prophecies! (peek alex)) ;; add current state to the list
                                     ;; of 'redos'
      (->> alex
           pop                       ;; discard the current state
           (reset! alexandria)
           peek
           restore!))))

(defn redo! []
  (let [nos @nostradamus]
    (when (can-redo?* nos)
      (save-snapshots! (peek nos)) ;; add the state as 'current' in
                                   ;; the undo atom.
      (reset! nostradamus (pop nos)) ;; Remove the prophecy
      (restore! (peek nos))))) ;; Set the prophecy as the current state.

(defn clear-history! []
  (reset! alexandria [])
  (reset! nostradamus []))

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
