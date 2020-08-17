package supervision

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.examples.helloworld.EmptyResponse
import io.grpc.examples.helloworld.ResultGrpcKt
import io.grpc.examples.helloworld.TaskResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class GrpcResultServer {
    private class GamayunResultService(
        private val resultMap: MutableMap<String, List<String>>,
        private val listeningForResults: MutableSet<String>
    ) :
        ResultGrpcKt.ResultCoroutineImplBase() {

        override suspend fun reportResult(request: TaskResult): EmptyResponse {
            if (listeningForResults.contains(request.jobId)) {
                resultMap[request.jobId] = request.resultsList
            } else {
                logger.warn { "Received result for jobId ${request.jobId} for which listening is not active. Will ignore!" }
            }

            return EmptyResponse.getDefaultInstance()
        }
    }

    private val resultMap = mutableMapOf<String, List<String>>()
    private val listeningForResults = mutableSetOf<String>()
    val server: Server =
        ServerBuilder
            .forPort(16656)
            .addService(GamayunResultService(resultMap, listeningForResults))
            .build()

    init {
        logger.info { "Starting Gamayun GRPC Server" }
        server.start()
    }

    //todo: collections used here (resultMap and listeningForResults) are not thread safe
    //and this could be accessed from different threads
    suspend fun getResultsForJobId(jobId: String, timeoutMillis: Long): List<String>? =
        if (listeningForResults.contains(jobId)) {
            null
        } else {
            listeningForResults.add(jobId)
            val result = withTimeoutOrNull(timeoutMillis) {
                while (!resultMap.containsKey(jobId)) {
                    delay(20)
                }

                val result = resultMap[jobId]!!
                result
            }

            if (result == null) {
                logger.warn { "Did not receive result for $jobId!" }
            }

            resultMap.remove(jobId)
            listeningForResults.remove(jobId)

            result
        }
}