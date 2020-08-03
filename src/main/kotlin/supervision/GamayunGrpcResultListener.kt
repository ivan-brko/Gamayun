package supervision

class GamayunGrpcResultListener(private val grpcResultServer: GrpcResultServer) : ResultListener {
    override suspend fun listenForResult(jobId: String, timeoutMillis: Long): List<String>? =
        grpcResultServer.getResultsForJobId(jobId, timeoutMillis)
}