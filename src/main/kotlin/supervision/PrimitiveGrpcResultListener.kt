package supervision

class PrimitiveGrpcResultListener : ResultListener {
    override fun listenForResult(jobId: String, timeoutMillis: Long): String? {
        return ""
    }
}