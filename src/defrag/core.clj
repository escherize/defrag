(ns defrag.core
  (:require
   [defrag.specs :as df]
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

(defn defn-update-body-with-args
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

(comment

  ;; example wrapper functions:
  
  (defn wrap-print
    "Example of using a function to wrap various defn forms, which has
  access to argument forms."
    [name args body]
    `((println (pr-str {:name ~name
                        :arg-list '~args
                        :args ~args
                        :body '~body})) ~@body))


  (defn wrap-def [name args body]
    `((def ~(symbol (str name "_args")) ~args)
      (defn ~(symbol (str "rerun_" name)) []
        (apply ~(symbol name) ~(symbol (str name "_args"))))
      ~@body))

  )

