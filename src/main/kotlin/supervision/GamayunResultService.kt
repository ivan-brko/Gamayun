package supervision

import io.grpc.ServerBuilder
import io.grpc.examples.helloworld.EmptyResponse
import io.grpc.examples.helloworld.ResultGrpcKt
import io.grpc.examples.helloworld.TaskResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

class GamayunResultServer {
    private class GamayunResultService(private val resultMap: MutableMap<String, List<String>>) :
        ResultGrpcKt.ResultCoroutineImplBase() {
        override suspend fun reportResult(request: TaskResult): EmptyResponse {
            resultMap[request.jobId] = request.resultsList
//            println(request.resultsList)
            return EmptyResponse.getDefaultInstance()
        }
    }

    private val resultMap = mutableMapOf<String, List<String>>()
    val server = ServerBuilder.forPort(16656).addService(GamayunResultService(resultMap)).build()

    fun start() {
        println("Starting server!")
        server.start()
    }

    suspend fun getResultsForJobId(jobId: String): Deferred<List<String>> =
        GlobalScope.async {
            while (!resultMap.containsKey(jobId)) {
                delay(50)
            }

            val result = resultMap[jobId]!!
            resultMap.remove(jobId)
            result
        }

}