(ns defrag.injections
  (:require
   [clojure.walk :as walk]
   [defrag.core :as dfg]))

(defn inject-ddefn [form]
  (walk/postwalk
   (fn [x] (if (= x 'defn) `dfg/ddefn x))
   form))


(comment

  #def/argz
  (defn square [x] (* x x))

  ;; macro expand:
  #_(def square
      (fn*
       ([x]
        (def square_args [x])
        (def rerun_square (fn* ([] (apply square square_args))))
        (* x x))))

  #def/argz
  (defn count-args
    ([] 0)
    ([_] 1)
    ([_ & many] (count many)))


  #def/argz
  (defn ff [& xs] xs)

  
  )
