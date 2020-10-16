package supervision

import arrow.core.Either
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import org.gamayun.proto.*

private val logger = KotlinLogging.logger {}

class GrpcResultServer {
    private class GamayunResultService(
            private val resultMap: MutableMap<String, GamayunResult>,
            private val errorMap: MutableMap<String, String>,
            private val listeningForResults: MutableSet<String>
    ) : ResultGrpcKt.ResultCoroutineImplBase() {
        override suspend fun reportResultWithRawStringsOnly(request: JobResultWithRawStringsOnly): EmptyResponse {
            if (listeningForResults.contains(request.name)) {
                resultMap[request.name] = GamayunResult(request.resultsList, listOf())
            } else {
                logger.warn { "Received result for jobId ${request.name} for which listening is not active. Will ignore!" }
            }

            return EmptyResponse.getDefaultInstance()
        }

        override suspend fun reportResultWithMapOnly(request: JobResultWithMapOnly): EmptyResponse {
            if (listeningForResults.contains(request.name)) {
                request.resultsList.map { grpcInnerType -> grpcInnerType.mapResultMap }
                resultMap[request.name] = GamayunResult(listOf(), request.resultsList.map { grpcInnerType -> grpcInnerType.mapResultMap })
            } else {
                logger.warn { "Received result for jobId ${request.name} for which listening is not active. Will ignore!" }
            }

            return EmptyResponse.getDefaultInstance()
        }

        override suspend fun reportResultWithMapAndStrings(request: JobResultWithMapAndStrings): EmptyResponse {
            if (listeningForResults.contains(request.name)) {
                resultMap[request.name] = GamayunResult(request.stringResultsList, request.mapResultsList.map { grpcInnerType -> grpcInnerType.mapResultMap })
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


    private val resultMap = mutableMapOf<String, GamayunResult>()
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
    suspend fun getResultsForJobId(jobId: String, timeoutMillis: Long): Either<String, GamayunResult>? =
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
    ): Either<String, GamayunResult>? =
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