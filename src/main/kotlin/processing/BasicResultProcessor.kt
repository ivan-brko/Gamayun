package processing

import java.time.LocalDateTime

class BasicResultProcessor : ResultProcessor {
    override fun processRawResults(data: String, tags: List<String>?): ProcessedGamayunResult {
        val data = mutableMapOf<String, String>().also { map ->
            map.addGamayunTimestamp()
            map["body"] = data
        }.toMap()
        val tagsData = mutableMapOf<String, List<String>>().also { map ->
            map.addGamayunTags(tags)
        }.toMap()

        return ProcessedGamayunResult(data, tagsData)
    }

    override fun processMapResults(data: Map<String, String>, tags: List<String>?): ProcessedGamayunResult {
        val data = data.toMutableMap().also { map ->
            map.addGamayunTimestamp()
        }.toMap()

        val tagsData = mutableMapOf<String, List<String>>().also { map ->
            map.addGamayunTags(tags)
        }.toMap()

        return ProcessedGamayunResult(data, tagsData)
    }


    private fun MutableMap<String, String>.addGamayunTimestamp() {
        this["gamayunTimestamp"] = LocalDateTime.now().toString()
    }

    private fun MutableMap<String, List<String>>.addGamayunTags(tags: List<String>?) {
        if (tags != null) {
            this["tags"] = tags
        }
    }
}