package supervision.grpc

import arrow.core.Either
import supervision.ResultListener

data class GamayunResult(
    val rawResults: List<String>,
    val mapResults: List<Map<String, String>>
)

class GamayunGrpcResultListener(private val grpcResultServer: GrpcResultServer) : ResultListener {
    override suspend fun listenForResult(jobId: String, timeoutMillis: Long): Either<String, GamayunResult>? =
        grpcResultServer.getResultsForJobId(jobId, timeoutMillis)
}