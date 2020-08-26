package supervision

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.instance
import processing.ResultProcessor
import storage.DataRepository
import supervision.errorReport.ErrorReporter
import java.io.IOException


private val logger = KotlinLogging.logger {}

class BasicTaskSupervisor(private val kodein: DI) : TaskSupervisor {
    private val listener: ResultListener by kodein.instance()
    private val dataRepository: DataRepository by kodein.instance()
    private val errorReporter: ErrorReporter by kodein.instance()
    private val resultProcessor: ResultProcessor by kodein.instance()

    override suspend fun runTask(taskConfig: TaskConfig) = try {
        val executableWithArgs = taskConfig.toExecutableWithArgs()

        val process = withContext(Dispatchers.IO) {
            ProcessBuilder(executableWithArgs)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start() //this line doesn't block until program stops executing, only until program is started
        }

        val result = listener.listenForResult(taskConfig.name, taskConfig.resultWaitTimeoutMillis)

        process.destroyAsync() //in case something is left hanging

        if (result != null) {
            val documents = result.map { resultProcessor.toGamayunBson(it, taskConfig.tags) }
            dataRepository.storeResult(taskConfig.name, documents)
        } else {
            logger.warn { "No result received in TaskSupervisor for jobId ${taskConfig.name}" }
            errorReporter.reportErrorForJob(taskConfig.name)
        }

    } catch (e: IOException) {
        logger.warn { "IO error while listening for result for jobId ${taskConfig.name}: \n${e.stackTrace}" }
    }

    private fun TaskConfig.toExecutableWithArgs(): List<String> =
        mutableListOf<String>().run {
            add(pathToExe)
            addAll(args)
            toList()
        }

    private suspend fun Process.destroyAsync() {
        coroutineScope {
            async {
                destroy() //todo: check if this is enough to kill everything
            }
        }
    }
}