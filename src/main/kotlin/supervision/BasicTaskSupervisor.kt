package supervision

import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import notification.Notifier
import org.kodein.di.DI
import org.kodein.di.instance
import processing.ResultProcessor
import storage.DataRepository
import supervision.grpc.GamayunResult
import java.io.IOException


private val logger = KotlinLogging.logger {}

//todo: rewrite this class, the code is ugly

class BasicTaskSupervisor(private val kodein: DI) : TaskSupervisor {
    private val listener: ResultListener by kodein.instance()
    private val dataRepository: DataRepository by kodein.instance()
    private val notifiers: List<Notifier> by kodein.instance()
    private val resultProcessor: ResultProcessor by kodein.instance()

    override suspend fun runTask(taskConfig: TaskConfig) = try {
        val executableWithArgs = taskConfig.toExecutableWithArgs()

        val process = withContext(Dispatchers.IO) {
            val processBuilder = ProcessBuilder(executableWithArgs)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)

            processBuilder.environment()["GAMAYUN_JOB_NAME"] = taskConfig.name

            processBuilder.start() //this line doesn't block until program stops executing, only until program is started
        }               //ignore the warning, this is wrapped in Dispatchers.IO so it's not a problem that it is blocking


        val result = listener.listenForResult(taskConfig.name, taskConfig.resultWaitTimeoutMillis)

        process.destroyAsync() //in case something is left hanging

        handleTaskResults(result, taskConfig)

    } catch (e: IOException) {
        logger.warn { "IO error while listening for result for jobId ${taskConfig.name}: \n${e.stackTrace}" }
    }

    private suspend fun handleTaskResults(
        result: Either<String, GamayunResult>?,
        taskConfig: TaskConfig
    ) {
        when {
            result != null -> { //in case we received some result
                handleReceivedTaskResult(result, taskConfig)
            }
            taskConfig.producesResult -> { //if this task should have produced a result but it didn't
                handleMissingTaskResults(taskConfig)
            }
        }
    }

    private fun handleMissingTaskResults(taskConfig: TaskConfig) {
        logger.warn { "No result received in TaskSupervisor for jobId ${taskConfig.name}" }
        notifiers.forEach { it.reportErrorForJob(taskConfig.name) }
    }

    private suspend fun handleReceivedTaskResult(
        result: Either<String, GamayunResult>,
        taskConfig: TaskConfig
    ) {
        when (result) {
            is Either.Left -> {
                logger.warn { "Error reported for job ${taskConfig.name}\n${result.a}" }
                notifiers.forEach { it.reportErrorForJob(taskConfig.name, result.a) }
            }
            is Either.Right -> {
                if (taskConfig.producesResult) { //if we received a result and this task should report results
                    val processedRawData =
                        result.b.rawResults.map { resultProcessor.processRawResults(it, taskConfig.tags) }
                    val processedMapData =
                        result.b.mapResults.map { resultProcessor.processMapResults(it, taskConfig.tags) }
                    val allProcessedData = processedMapData + processedRawData

                    dataRepository.storeResult(taskConfig.name, allProcessedData)
                } else { //if we received a result but this task shouldn't produce results
                    logger.warn("Received a result for task ${taskConfig.name} which should not report results. Will ignore this result!")
                }
            }
        }
    }

    private fun TaskConfig.toExecutableWithArgs(): List<String> =
        mutableListOf<String>().run {
            add(pathToExe)
            if (args != null) {
                addAll(args)
            }
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