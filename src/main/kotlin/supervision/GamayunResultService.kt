package supervision

import io.grpc.ServerBuilder
import io.grpc.examples.helloworld.EmptyResponse
import io.grpc.examples.helloworld.ResultGrpcKt
import io.grpc.examples.helloworld.TaskResult

class GamayunResultServer {
    val server = ServerBuilder.forPort(16656).addService(GamayunResultService()).build()

    fun start() {
        println("Starting server!")
        server.start()
    }
}

class GamayunResultService : ResultGrpcKt.ResultCoroutineImplBase() {

    override suspend fun reportResult(request: TaskResult): EmptyResponse {
        println(request.resultsList)
        return EmptyResponse.getDefaultInstance()
    }
}