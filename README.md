Historian
=========

<img src="https://raw.githubusercontent.com/Frozenlock/historian/master/472px-Ancientlibraryalex.jpg"
 alt="Historian logo" title="Library of Alexandria"/>

> "The present is the least important time we live in" --Alan Kay

A drop-in atom-state-management (UNDOs!!) for your clojurescript projects.

Also supports clojure in case you would want to make similar applications, or simply for testing.

*Warning: Breaking change in the API for version 1.0.0.*
`restore-last!` is now `undo!`. The command is much clearer this way,
especially now that we support redos.

## Table of contents
[Usage](#usage)  
[Shortcuts](#shortcuts)  
[Reagent](#reagent)  

<a name="usage"/>
## Usage
Add the following dependency in your `project.clj`:
```clj

[historian "1.0.0"]
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

(hist/undo!)

@my-state
=> "ABC"

(hist/redo!)

@my-state
=> "DEF"

;; tada!
```

Of course, sometimes we want to do some things without anyone noticing...
```clj
;; our current state is "ABC"

(hist/off-the-record
 (reset! my-state "GHI"))  ;; <--- this change won't be added to the undo history

(reset! my-state "ZZZ")

(hist/undo!)

@my-state
=> "ABC"
```

If you have a bunch of operations initiated by a single user action:

```clj

(hist/with-single-record
 (doseq [i (range 200)]
  (reset! my-state i)))
;; We've just done 200 operations on the atom, but only the last state is recorded.

(hist/undo!)

@my-state
=> "ABC"
```

To check if any undo/redo history is available, use `can-undo?` and `can-redo?`.

When loading an app with multiple atoms, you should use `clear-history!` and `trigger-record!` to start with a clean slate.

<a name="shortcuts"/>
## Keyboard Shortcuts

You can bind `ctrl-z` and `ctrl-y` to undo and redo by using
`bind-keys` in the `historian.keys` namespace.


<a name="reagent"/>
## Use with [Reagent] (https://github.com/holmsand/reagent)

*Reagent* atoms remember where they've been derefed. In order for *Historian*'s atom to behave the same, simply replace it with one of your *Reagent* atom:
```clj
(ns some-ns (:require [reagent.core :refer [atom]]
                      [historian.core :as hist]))

;; for undos:
(hist/replace-library! (atom [])) ; <----- the new atom must be a vector.

;; for redos:
(hist/replace-prophecy! (atom [])) ; <----- the new atom must be a vector.
```


