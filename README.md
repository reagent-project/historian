Historian
=========

<img src="https://raw.githubusercontent.com/Frozenlock/historian/master/472px-Ancientlibraryalex.jpg"
 alt="Historian logo" title="Library of Alexandria"/>


A drop-in atom-state-management (UNDOs!!) for your clojurescript projects.

Also supports clojure in case you would want to make similar applications, or simply for testing.

*Warning: As it is, simply supports undo, not redo.*


## Usage
Add the following dependency in your `project.clj`:
```clj
[historian "0.1.4"]
```

And require historian in your namespace:
```clj
(ns my-ns
  (:require [historian.core :as hist]))
```

Suppose you have your state in an atom `my-state`:
```clj
(def my-state (atom "ABC"))
```

To keep an history of all changes, simply add your atom to historian:

```clj
(hist/record! my-state :my-state)

;; then change the state of your atom

(reset! my-state "DEF")

@my-state
=> "DEF"

(hist/restore-last!)

@my-state
=> "ABC"

;; tada!
```

Of course, sometimes we want to do some things without anyone noticing...
```clj
;; our current state is "ABC"

(hist/off-the-record
 (reset! my-state "GHI"))  ;; <--- this change won't be added to the undo history

(reset! my-state "ZZZ")

(hist/restore-last!)

@my-state
=> "ABC"
```

If you have a bunch of operations initiated by a single user action:

```clj

(hist/with-single-record
 (doseq [i (range 200)]
  (reset! my-state i)))
;; We've just done 200 operations on the atom, but only the last state is recorded.

(hist/restore-last!)

@my-state
=> "ABC"
```

To check if any undo history is available, use `can-undo?`.

When loading an app with multiple atoms, you should use `clear-history!` and `trigger-record!` to start with a clean slate.

## Use with [Reagent] (https://github.com/holmsand/reagent)

*Reagent* atoms remember where they've been derefed. In order for *Historian*'s atom to behave the same, simply replace it with one of your *Reagent* atom:
```clj
(ns some-ns (:require [reagent.core :refer [atom]]
                      [historian.core :as hist]))

(hist/replace-library! (atom [])) ; <----- the new atom must be a vector.
```

## Roadmap

I would like to have a tree-like history, allowing the user to choose any previous state.

(See emacs' undo-tree or Vim's Gundo for examples)

Feel free to contribute! ;-)



