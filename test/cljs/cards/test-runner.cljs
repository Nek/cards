(ns cards.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [cards.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'cards.core-test))
    0
    1))
