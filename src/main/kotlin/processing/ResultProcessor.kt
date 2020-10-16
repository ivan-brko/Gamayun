package processing

data class ProcessedGamayunResult(
        val stringData: Map<String, String>,
        val stringListData: Map<String, List<String>>
)

interface ResultProcessor {
    fun processRawResults(data: String, tags: List<String>?): ProcessedGamayunResult
    fun processMapResults(data: Map<String, String>, tags: List<String>?): ProcessedGamayunResult
}