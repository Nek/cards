(ns save_state.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [save_state.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'save_state.core-test))
    0
    1))
