package supervision

import arrow.core.Either
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import org.gamayun.proto.EmptyResponse
import org.gamayun.proto.JobError
import org.gamayun.proto.JobResult
import org.gamayun.proto.ResultGrpcKt

private val logger = KotlinLogging.logger {}

class GrpcResultServer {
    private class GamayunResultService(
            private val resultMap: MutableMap<String, List<String>>,
            private val errorMap: MutableMap<String, String>,
            private val listeningForResults: MutableSet<String>
    ) :
            ResultGrpcKt.ResultCoroutineImplBase() {

        override suspend fun reportResult(request: JobResult): EmptyResponse {
            if (listeningForResults.contains(request.name)) {
                resultMap[request.name] = request.resultsList
            } else {
                logger.warn { "Received result for jobId ${request.name} for which listening is not active. Will ignore!" }
            }

            return EmptyResponse.getDefaultInstance()
        }

        override suspend fun reportError(request: JobError): EmptyResponse {
            if (listeningForResults.contains(request.name)) {
                errorMap[request.name] = request.error
            } else {
                logger.warn { "Received an error for jobId ${request.name} for which listening is not active. Will ignore!" }
            }

            return EmptyResponse.getDefaultInstance()
        }
    }

    private val resultMap = mutableMapOf<String, List<String>>()
    private val errorMap = mutableMapOf<String, String>()
    private val listeningForResults = mutableSetOf<String>()
    val server: Server =
            ServerBuilder
                    .forPort(16656)
                    .addService(GamayunResultService(resultMap, errorMap, listeningForResults))
                    .build()

    init {
        logger.info { "Starting Gamayun GRPC Server" }
        server.start()
    }

    //todo: collections used here (resultMap and listeningForResults) are not thread safe
    //and this could be accessed from different threads
    suspend fun getResultsForJobId(jobId: String, timeoutMillis: Long): Either<String, List<String>>? =
            if (listeningForResults.contains(jobId)) {
                null
            } else {
                clearReportReceivingMaps(jobId)
                listeningForResults.add(jobId)

                val result = getReportOrTimeout(timeoutMillis, jobId)

                listeningForResults.remove(jobId)
                clearReportReceivingMaps(jobId)

                if (result == null) {
                    logger.warn { "Did not receive result for $jobId!" }
                }

                result
            }

    private fun clearReportReceivingMaps(jobId: String) {
        resultMap.remove(jobId)
        errorMap.remove(jobId)
    }

    private suspend fun getReportOrTimeout(
            timeoutMillis: Long,
            jobId: String
    ): Either<String, List<String>>? =
            withTimeoutOrNull(timeoutMillis) {
                while (!resultMap.containsKey(jobId) && !errorMap.containsKey(jobId)) {
                    delay(20)
                }

                if (resultMap.containsKey(jobId)) {
                    val result = Either.Right(resultMap[jobId]!!)
                    result
                } else {
                    val error = Either.Left(errorMap[jobId]!!)
                    error
                }
            }

}