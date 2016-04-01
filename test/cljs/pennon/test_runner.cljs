(ns pennon.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [pennon.core-test]))

(enable-console-print!)

(doo-tests 'pennon.core-test)
