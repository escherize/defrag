(ns defrag.core
  (:require
   [defrag.specs :as df]
   [clojure.walk :as walk]
   [clojure.spec.alpha :as s]))

(defn ^:private get-params [{:keys [params] :as body}]
  (let [args (mapv second (:args params))]
    (cond-> args
      (:varargs params) (conj (get-in params [:varargs :form 1])))))

(defn ^:private update-conf-with-args [{[arity] :fn-tail :as conf} body-update-fn]
  (case arity
    :arity-1 (let [parameters (get-params (get-in conf [:fn-tail 1]))]
               (update-in conf [:fn-tail 1 :body 1]
                          (partial body-update-fn parameters)))
    :arity-n (update-in conf [:fn-tail 1 :bodies]
                        (fn [bodies]
                          (map
                           (fn [body]
                             (let [parameters (get-params body)]
                               (update-in body [:body 1]
                                          (partial body-update-fn parameters))))
                           bodies)))))

(defn ^:private defn-update-body-with-args
  [f args-to-defn]
  (let [{:keys [fn-name] :as conf} (s/conform ::df/defn-args args-to-defn)
        new-conf (update-conf-with-args conf (partial f (str fn-name)))
        new-args (s/unform ::df/defn-args new-conf)]
    (cons `defn new-args)))

(defmacro defrag!
  "Creates a new defn-like function with name defn-title which wraps an
  original defn form with defrag-fn"
  [defn-title defrag-fn]
  `(defmacro ~defn-title [& args#] (defn-update-body-with-args ~defrag-fn args#)))

;; samples:

(defn wrap-print
  "Example of using a function to wrap various defn forms, which has
  access to argument forms."
  [name args body]
  `((println (pr-str {:name ~name
                      :arg-list '~args
                      :args ~args
                      :body '~body})) ~@body))

(defrag! pdefn wrap-print)

(comment

  (pdefn g [y] y)

  (pdefn ^:private f
         ([] 1)
         ([x] (+ 2 x))
         ([x & ys] (first ys)))

  ;; macro-expands to:
  #_(def f
      (fn*
       ([] (println (pr-str {:args [], :name "f", :body '[1]})) 1)
       ([x]
        (println (pr-str {:args [x], :name "f", :body '[(+ 2 x)]}))
        (+ 2 x))
       ([x & ys]
        (println (pr-str {:args [x ys], :name "f", :body '[(first ys)]}))
        (first ys))))


  ;; calling it produces:

  (f)
  ;; prints: {:args [], :name "f", :body [1]}
  ;;=> 1

  (f 2)
  ;; prints: {:args [2], :name "f", :body [(+ 2 x)]}
  ;;=>2

  (f 3 "hi" 5)
  ;; prints: {:args [3 ("hi" 5)], :name "f", :body [(first ys)]}
  ;;=> "hi"

  ;; notice our function is private and has the usual defn metadata:

  (:private (meta #'f))
  ;;=> true

  (:arglists (meta #'f))
  ;;=> ([] [x] [x & ys])

  )

(defn def-wrap [name args body]
  `((def ~(symbol (str name "_args")) ~args)
    (defn ~(symbol (str "rerun_" name)) []
      (apply ~(symbol name) ~(symbol (str name "_args"))))
    ~@body))

(defrag! ddefn def-wrap)

(comment

  (ddefn square [x] (* x x))

  ;; call square
  (square -30)
  ;;=> 900

  ;; pretend you forgot square's arguments:
  square_args
  ;;=> [-30]

  ;; rerun square:
  (rerun_square)
  ;;=> 900

  )

(defn inject-pdefn [form]
  (walk/postwalk (fn [x] (if (= x 'defn) `pdefn x)) form))

(defn inject-ddefn [form]
  (walk/postwalk (fn [x] (if (= x 'defn) `ddefn x)) form))

(comment

  #defrag/d
  (defn square [x] (* x x))

  ;; call square
  (square -30)
  ;;=> 900

  ;; pretend you forgot square's arguments:
  square_args
  ;;=> [-30]

  ;; rerun square:
  (rerun_square)
  ;;=> 900

  )
