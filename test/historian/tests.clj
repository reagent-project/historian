(ns historian.tests
  (:require [clojure.test :as t :refer (is deftest with-test run-tests testing)]
            [historian.core :as hist]))

(def test-atom (atom ""))

(deftest undos-test
  (reset! test-atom "ABC")
  (is (= "ABC" @test-atom))
  (hist/record! test-atom :test-atom) ;; start the recording
  ;; at this point we should have taken the first record
  (reset! test-atom "DEF") ;; change the atom state, add new record
  (is (= "DEF" @test-atom))
  (hist/undo!)     ;; restore the previous state
  (is (not= "DEF" @test-atom))
  (is (= "ABC" @test-atom))
  (reset! test-atom "GHI") ;; change the state
  (is (= "GHI" @test-atom))
  (hist/undo!) ;; now we should return to the previous state
  (is (= "ABC" @test-atom))
  (hist/stop-record! :test-atom)
  ;; we shouldn't be watching this atom anymore
  (let [nb (count (deref @hist/alexandria))]
    (reset! test-atom :new-value)
    (is (= nb (count (deref @hist/alexandria)))))
  (hist/clear-history!))

(deftest with-single-record-test
  (hist/record! test-atom :test-atom)
  (reset! test-atom :before)
  (hist/with-single-record
    (doseq [i (range 10)]
      (reset! test-atom i)))
  (is (= 9 @test-atom))
  (hist/undo!)
  (is (= :before @test-atom))
  (hist/stop-record! :test-atom)
  (hist/clear-history!))

(deftest redos-test
  (reset! test-atom :before)
  (hist/record! test-atom :test-atom) ;; first state
  (reset! test-atom :after) ;; second state
  (hist/undo!)
  (is (= :before @test-atom)) ;; back to first state
  (hist/redo!)
  (is (= :after @test-atom)) ;; now back to the second state
  (hist/undo!) ;; and again back to the first state
  (is (= :before @test-atom))
  (hist/stop-record! :test-atom)
  (hist/clear-history!))
