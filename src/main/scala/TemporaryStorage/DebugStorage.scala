package TemporaryStorage

class DebugStorage extends TemporaryStorage {
    val data = new collection.mutable.HashMap[String, String]()

    override def put(key: String, value: String): Unit = data.put(key, value)

    override def get(key: String) = data.get(key)
}
