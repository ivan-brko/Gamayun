package processing

import org.bson.BsonDocument

interface ResultProcessor {
    fun toGamayunBson(data: String, tags: List<String>?): BsonDocument
}