(ns ^{:doc "Manage states for your atoms. (Easy undo/redo)"
      :author "Frozenlock"
      :quote "The present is the least important time we live in. --Alan Kay"}
  historian.core)

(def alexandria 
  "The great library... store your stuff here."
  (atom (atom [])))

(defn get-library-atom []
  @alexandria)

(def nostradamus 
  "What will happen in the future..."
  (atom (atom [])))

(defn get-prophecy-atom []
  @nostradamus)

(def overseer
  "Who should we record?"
  (atom {}))

(defn- register-atom! 
  ([atom k] (register-atom! atom k nil))
  ([atom k passive?]
   (swap! overseer assoc k {:atom atom :passive? passive?})))

(defn- de-register-atom! [k]
  (swap! overseer dissoc k))

(defn- snapshot [k]
  (let [{:keys [atom passive?]} (get @overseer k)]
    {:value (deref atom)
     :passive? passive?
     :key k
     :timestamp 
     #?(:cljs (goog.now)
        :clj (System/currentTimeMillis))}))

(defn- take-snapshots []
  (mapv snapshot (keys @overseer)))

(defn- different-from?
  "Check if any non-passive snapshot is different."
  [new old]
  (let [clean-maps #(when-not (:passive? %)
                      (dissoc % :timestamp))]
    (not= (map clean-maps old)
          (map clean-maps new))))

(defn- different-from-last? [new]
  (different-from? new (peek @(get-library-atom))))

(defn- save-snapshots! [snaps]
  (swap! (get-library-atom) conj snaps))

(defn- save-if-different! [snaps]
  (when (different-from-last? snaps)
    (save-snapshots! snaps)
    (reset! (get-prophecy-atom) [])))

(defn- save-prophecies! [snaps]
  (swap! (get-prophecy-atom) conj snaps))

(def ^:dynamic *record-active* true)

(defn- restore!
  "Restore all the atoms being watched to a previous/different state."
  [snaps]
  (binding [*record-active* false]
    (doseq [s snaps]
      (reset! (get-in @overseer [(:key s) :atom])
              (:value s)))))

(defn- watch! [atm]
  (add-watch atm ::historian-watch
             (fn [k _ old-value new-value]
               (when (not= old-value new-value) ;; really modified?
                 (when *record-active*
                   (save-if-different! (take-snapshots)))))))

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

(defn overwrite-record!
  "Overwrite the last historic entry with a new one." 
  ([] (overwrite-record! (take-snapshots)))
  ([snaps]
   (when *record-active*
     (swap! (get-library-atom) pop) ;; last snapshots
     (save-snapshots! snaps))))

(defn replace-library!
  "The library atom (where all records are kept to enable 'undo') will
  be replaced by the new-atom. Useful if you've already done some
  modifications to the new-atom (like added some watchers). Depending
  on where you use this function, you might want to fire a
  `trigger-record!' just after.
  
  Usually, one would also want to use `replace-prophecy!' in order to
  replace the 'redo' atom."
  [new-atom]
  (reset! alexandria new-atom))

(defn replace-prophecy!
  "The prophecy atom (where all records are kept to enable 'redo')
  will be replaced by the new-atom. Useful if you've already done some
  modifications to the new-atom (like added some watchers).

  Usually used with `replace-library'."
  [new-atom]  
  (reset! nostradamus new-atom))

(defn record!
  "Add the atom to the overseer watch. When any of the atom under its
  watch is modified, it triggers a save of every atom to history (if
  any of the atom is different form the last historic state). Return
  the atom.
  
  If `passive?' is true, the atom will NOT trigger any new save and
  will only be saved when another atom under watch is modified."
  ([atm k] (record! atm k nil))
  ([atm k passive?]
   (register-atom! atm k passive?)
   (when-not passive? (watch! atm))
   (trigger-record!)
   atm))

(defn stop-record! 
  "Remove the atom associated to this key from the overseer watch.
  This atom will no longer be watched, nor its state saved to
  history."[k]
  (remove-watch! (get-in @overseer [k :atom]))
  (de-register-atom! k))

(defn stop-all-records!
  "Remove all the atoms from the overseer watch. The atoms will no
  longer be watched, nor any of their state saved to history."
  []
  (let [ks (keys @overseer)]
    (doseq [k ks]
      (stop-record! k))))

(defn can-undo?
  "Do we have enough history to undo?"[]
  (can-undo?* @(get-library-atom)))

(defn can-redo?
  "Can we redo?"[]
  (can-redo?* @(get-prophecy-atom)))

(defn undo! []
  (let [alex @(get-library-atom)]
    (when (can-undo?* alex)
      (save-prophecies! (peek alex)) ;; add current state to the list
                                     ;; of 'redos'
      (->> alex
           pop                       ;; discard the current state
           (reset! (get-library-atom))
           peek
           restore!))))

(defn redo! []
  (let [nos @(get-prophecy-atom)]
    (when (can-redo?* nos)
      (save-snapshots! (peek nos)) ;; add the state as 'current' in
                                   ;; the undo atom.
      (reset! (get-prophecy-atom) (pop nos)) ;; Remove the prophecy
      (restore! (peek nos))))) ;; Set the prophecy as the current state.

(defn clear-history! []
  (reset! (get-library-atom) [])
  (reset! (get-prophecy-atom) []))

#?(:clj
(defmacro off-the-record
  "Temporarily deactivate the watches write to history."
  [& content]
  `(binding [*record-active* false]
     ~@content)))

#?(:clj
(defmacro with-single-record
  "Temporarily deactivate the watches write to history. A single write
   is triggered at the end of the macro, assuming at least one of the
   atoms watched by the overseer has changed." [& content]
   `(do (off-the-record ~@content)
        (trigger-record!))))

(:clj
(defmacro with-single-before-and-after
  "Deactivate the watches write to history and execute the body. If
  any non-passive atom is modified, replace the last history with a
  snapshot taken just before executing the body and then take another
  snapshot." [& content]
  `(let [before-snaps# (take-snapshots)]
     (off-the-record ~@content)
     (let [after-snaps# (take-snapshots)]
       (when (different-from? after-snaps# before-snaps#)
         (overwrite-record! before-snaps#)
         (trigger-record!))))))
