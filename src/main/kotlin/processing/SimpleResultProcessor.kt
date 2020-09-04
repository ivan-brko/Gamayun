package processing

import kotlinx.serialization.json.*
import org.bson.*

//todo: This whole part is not ideally written, it needs a rewrite
class SimpleResultProcessor : ResultProcessor {
    override fun toGamayunBson(data: String, tags: List<String>?): BsonDocument {
        val json = data.toJson()
        val document = json?.toBsonDocument() ?: data.toBsonDocument()
        if (tags != null) {
            document.addGamayunMetadata(tags)
        }
        return document
    }

    private fun String.toJson(): JsonElement? =
        try {
            Json(JsonConfiguration.Stable).parseJson(this) //this will throw if it is not json
        } catch (e: Exception) {
            null
        }

    private fun JsonElement.toBsonDocument(): BsonDocument =
        BsonDocument().also { bsonDocument ->
            if (this is JsonObject) {
                val jsonObject = this.jsonObject
                jsonObject.forEach {
                    val key = it.component1()
                    val jsonElement = it.component2().primitive.contentOrNull
                    if (jsonElement != null) {
                        bsonDocument[key] = BsonString(jsonElement)
                    } else {
                        bsonDocument[key] = BsonNull()
                    }
                }
            } else if (this is JsonPrimitive) {
                val jsonElement = this.primitive.contentOrNull
                bsonDocument["body"] = BsonString(jsonElement)
            }
        }


    private fun String.toBsonDocument(): BsonDocument =
        BsonDocument().also { bsonDocument ->
            bsonDocument["body"] = BsonString(this)
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