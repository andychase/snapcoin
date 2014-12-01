import jdbm.{PrimaryHashMap, RecordManagerFactory}

object PersistentStorage {
    val fileName = "db/db"
    val recordName = "walletIDs"

    def put(k: String, v: String): Unit = {
        val recMan = RecordManagerFactory.createRecordManager(fileName)
        val hashMap: PrimaryHashMap[String, String] = recMan.hashMap(recordName)
        hashMap.put(k, v)
        recMan.commit()
        recMan.close()
    }

    def get(k: String): Option[String] = {
        val recMan = RecordManagerFactory.createRecordManager(fileName)
        val hashMap: PrimaryHashMap[String, String] = recMan.hashMap(recordName)
        val value = hashMap.get(k)
        recMan.close()
        Option(value)
    }
}
