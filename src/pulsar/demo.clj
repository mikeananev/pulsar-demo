(ns pulsar.demo
  (:gen-class)
  (:require [taoensso.nippy :refer [freeze thaw]])
  (:import (org.apache.pulsar.client.api PulsarClient Producer MessageId Consumer Message)
           (clojure.lang IFn)))

(defn new-client
  "return ^PulsarClient client"
  ([^String urls]
   (let [builder (PulsarClient/builder)]
     (.build
       (doto builder
        (.serviceUrl urls)))))
  ([]
   (new-client "pulsar://localhost:6650")))

(defn close-client
  [^PulsarClient c]
  (.close c))

(defn new-producer
  "create new producer.
  returns ^Producer instance."
  ^Producer
  [^PulsarClient c ^String topic-name]
  (let [producer (.newProducer c)]
    (.create
      (doto producer
       (.topic topic-name)))))

(defn close-producer
  [^Producer p]
  (.close p))

(defn send-message
  "Send message to topic. msg can be any Clojure data type.
  returns message id."
  ^MessageId
  [^Producer p msg]
  (let [binary-data (freeze msg)]
    (.send p binary-data)))

(defn new-consumer
  "create new consumer.
  returns ^Consumer instance"
  [^PulsarClient c ^String topic-name ^String subs-name]
  (let [consumer (.newConsumer c)]
    (.subscribe
      (doto consumer
        (.topic (into-array [topic-name]))
        (.subscriptionName subs-name)))))

(defn close-consumer
  [^Consumer c]
  (.close c))

(defn start-consumer
  "start receive messages in a new thread and process messages using given f.
  return atom with true value. to stop process just set atom to false."
  [^Consumer c ^IFn f]
  (let [a (atom true)]
    (future
      (println "starting receive messages...")
      (while @a
        (try
          (let [msg (.receive c)]
           (f msg)
           (.acknowledge c msg))
          (catch Exception e
            (println "got exception: " (.getMessage e)))))
      (println "consumer is stopped."))
    a))

(defn print-message
  [^Message m]
  (println "got message: " (pr-str (thaw (.getData m)))))

(comment
  (def c1 (new-client))
  (def t1 "topic1")
  (def p1 (new-producer c1 t1))

  (def msg1 {:a 1 :b "2" :c [1 2 3]})
  (send-message p1 msg1)

  (def subs1 "subscription1")
  (def s1 (new-consumer c1 t1 subs1))

  (def stop-s1 (start-consumer s1 print-message))
  (reset! stop-s1 false)

  (close-consumer s1)
  (close-producer p1)
  (close-client c1)

  )

(defn -main
  "entry point to program."
  [& args]
  (println "apache pulsar client..."))
