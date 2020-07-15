package supervision

import kotlinx.coroutines.withTimeout

class PrimitiveGrpcResultListener(private val gamayunResultServer: GamayunResultServer) : ResultListener {
    //TODO
    //This is really ugly, there is a nicer way of handling it surely
    //Check later
    override suspend fun listenForResult(jobId: String, timeoutMillis: Long): List<String>? {
        val result = gamayunResultServer.getResultsForJobId(jobId)
        return try {
            withTimeout(timeoutMillis) {
                result.await()
            }
        } catch (e: Exception) {
            null
        }
    }
}