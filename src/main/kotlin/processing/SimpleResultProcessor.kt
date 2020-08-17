package processing

import kotlinx.serialization.json.*
import org.bson.BsonArray
import org.bson.BsonDateTime
import org.bson.BsonDocument
import org.bson.BsonString

//todo: This whole part is not ideally written, it needs a rewrite
class SimpleResultProcessor : ResultProcessor {
    override fun toGamayunBson(data: String, tags: List<String>): BsonDocument {
        val json = data.toJson()
        val document = json?.toBsonDocument() ?: data.toBsonDocument()
        document.addGamayunMetadata(tags)
        return document
    }

    private fun String.toJson() =
        try {
            Json(JsonConfiguration.Stable).parseJson(this) //this will throw if it is not json
        } catch (e: Exception) {
            null
        }

    private fun JsonElement.toBsonDocument(): BsonDocument {
        val bsonDocument = BsonDocument()

        if (this is JsonObject) {
            val jsonObject = this.jsonObject
            jsonObject.forEach {
                val key = it.component1()
                val jsonElement = it.component2().primitive.contentOrNull
                bsonDocument[key] = BsonString(jsonElement)
            }
        } else if (this is JsonPrimitive) {
            val jsonElement = this.primitive.contentOrNull
            bsonDocument["body"] = BsonString(jsonElement)
        }

        return bsonDocument
    }

    private fun String.toBsonDocument(): BsonDocument {
        val document = BsonDocument()
        document["body"] = BsonString(this)
        return document
    }

    private fun BsonDocument.addGamayunMetadata(tags: List<String>) {
        this["gamayunTimestamp"] = BsonDateTime(System.currentTimeMillis())
        //todo: optimisation possible if we convert these only on startup and not for every result
        val bsonTags = tags.map {
            BsonString(it)
        }
        this["gamayunTags"] = BsonArray(bsonTags)
    }
}