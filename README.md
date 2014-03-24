historian
=========

<img src="https://raw.githubusercontent.com/Frozenlock/historian/master/472px-Ancientlibraryalex.jpg"
 alt="Historian logo" title="Library of Alexandria" align="right" />


A drop-in atom-state-management (UNDOs!!) for your clojurescript projects.

Also supports clojure in case you would want to make similar applications, or simply for testing.

*Warning: As it is, simply supports undo, not redo.*


## Usage
Add the following dependency in your `project.clj`:
```clj
[historian "0.1.3"]
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


## Roadmap

I would like to have a tree-like history, allowing the user to choose any previous state.

(See emacs' undo-tree or Vim's Gundo for examples)

Feel free to contribute! ;-)



