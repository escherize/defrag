(ns defrag.core-test
  (:require [clojure.test :refer :all]
            [defrag.core :refer :all]))

(defonce ^:private log (atom []))

(defn clear-log-fixture [test-fn]
  (reset! log [])
  (test-fn))

(use-fixtures :each clear-log-fixture)

(defn ^:private def-atom-append [name args body]
  `((swap! log conj
           {:name ~name
            :args ~args
            :body '~body}) ~@body))

(defrag! defn<->atom def-atom-append)

(defn<->atom a-fun [x] [x "?" x])

(deftest single-arity-single-call-test
  (a-fun :hello)
  (is (= '[{:args [:hello], :name "a-fun", :body [[x "?" x]]}]
         @log)))

(deftest single-arity-multi-call-test
  (mapv a-fun [1 2 3])
  (is (= '[{:args [1], :name "a-fun", :body [[x "?" x]]}
           {:args [2], :name "a-fun", :body [[x "?" x]]}
           {:args [3], :name "a-fun", :body [[x "?" x]]}]
         @log)))

(defn<->atom b-fun
  ([] (b-fun 1))
  ([x] (b-fun 1 2))
  ([x y] (b-fun 1 2 3))
  ([x y z] (b-fun 1 2 3 4))
  ([x y z aa] :The_End))

(deftest multi-arity-multi-call-test
  (is (= :The_End (b-fun)))
  (is
   (= '[{:args [], :name "b-fun", :body [(b-fun 1)]}
        {:args [1], :name "b-fun", :body [(b-fun 1 2)]}
        {:args [1 2], :name "b-fun", :body [(b-fun 1 2 3)]}
        {:args [1 2 3], :name "b-fun", :body [(b-fun 1 2 3 4)]}
        {:args [1 2 3 4], :name "b-fun", :body [:The_End]}]
      @log)))

(deftest insane-mode
  (b-fun)
  (b-fun 1)
  (b-fun 1 2)
  (b-fun 1 2 3)
  (b-fun 1 2 3 4)
  (is (= '[{:args [], :name "b-fun", :body [(b-fun 1)]}
           {:args [1], :name "b-fun", :body [(b-fun 1 2)]}
           {:args [1 2], :name "b-fun", :body [(b-fun 1 2 3)]}
           {:args [1 2 3], :name "b-fun", :body [(b-fun 1 2 3 4)]}
           {:args [1 2 3 4], :name "b-fun", :body [:The_End]}
           {:args [1], :name "b-fun", :body [(b-fun 1 2)]}
           {:args [1 2], :name "b-fun", :body [(b-fun 1 2 3)]}
           {:args [1 2 3], :name "b-fun", :body [(b-fun 1 2 3 4)]}
           {:args [1 2 3 4], :name "b-fun", :body [:The_End]}
           {:args [1 2], :name "b-fun", :body [(b-fun 1 2 3)]}
           {:args [1 2 3], :name "b-fun", :body [(b-fun 1 2 3 4)]}
           {:args [1 2 3 4], :name "b-fun", :body [:The_End]}
           {:args [1 2 3], :name "b-fun", :body [(b-fun 1 2 3 4)]}
           {:args [1 2 3 4], :name "b-fun", :body [:The_End]}
           {:args [1 2 3 4], :name "b-fun", :body [:The_End]}]
         @log)))
