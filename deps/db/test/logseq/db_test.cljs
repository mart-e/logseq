(ns logseq.db-test
  (:require [cljs.test :refer [deftest is]]
            [logseq.db.frontend.schema :as db-schema]
            [datascript.core :as d]
            [logseq.db :as ldb]
            [logseq.db.sqlite.create-graph :as sqlite-create-graph]))


;;; datoms
;;; - 1 <----+
;;;   - 2    |
;;;     - 3 -+
(def broken-outliner-data-with-cycle
  [{:db/id 1
    :block/uuid #uuid"e538d319-48d4-4a6d-ae70-c03bb55b6fe4"
    :block/parent 3}
   {:db/id 2
    :block/uuid #uuid"c46664c0-ea45-4998-adf0-4c36486bb2e5"
    :block/parent 1}
   {:db/id 3
    :block/uuid #uuid"2b736ac4-fd49-4e04-b00f-48997d2c61a2"
    :block/parent 2}])

(deftest get-block-children-ids-on-bad-outliner-data
  (let [db (d/db-with (d/empty-db db-schema/schema)
                      broken-outliner-data-with-cycle)]
    (is (= "bad outliner data, need to re-index to fix"
           (try (ldb/get-block-children-ids db #uuid "e538d319-48d4-4a6d-ae70-c03bb55b6fe4")
                (catch :default e
                  (ex-message e)))))))

(def class-parents-data
  [{:block/type "class"
    :block/title "x"
    :block/name "x"
    :block/uuid #uuid "6c353967-f79b-4785-b804-a39b81d72461"}
   {:block/type "class"
    :block/title "y"
    :block/name "y"
    :block/uuid #uuid "7008db08-ba0c-4aa9-afc6-7e4783e40a99"
    :logseq.property/parent [:block/uuid #uuid "6c353967-f79b-4785-b804-a39b81d72461"]}
   {:block/type "class"
    :block/title "z"
    :block/name "z"
    :block/uuid #uuid "d95f2912-a7af-41b9-8ed5-28861f7fc0be"
    :logseq.property/parent [:block/uuid #uuid "7008db08-ba0c-4aa9-afc6-7e4783e40a99"]}])

(deftest get-class-parents-test
  (let [conn (d/create-conn db-schema/schema-for-db-based-graph)]
    (d/transact! conn (sqlite-create-graph/build-db-initial-data "{}"))
    (d/transact! conn class-parents-data)
    (is (= #{"x" "y"}
           (->> (ldb/get-class-parents (ldb/get-page @conn "z"))
                (map #(:block/title (d/entity @conn %)))
                set)))))
