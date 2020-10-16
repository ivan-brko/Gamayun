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

        if (result != null) {
            when (result) {
                is Either.Left -> {
                    logger.warn { "Error reported for job ${taskConfig.name}\n${result.a}" }
                    notifiers.forEach { it.reportErrorForJob(taskConfig.name, result.a) }
                }
                is Either.Right -> {
                    val processedRawData = result.b.rawResults.map { resultProcessor.processRawResults(it, taskConfig.tags) }
                    val processedMapData = result.b.mapResults.map { resultProcessor.processMapResults(it, taskConfig.tags) }
                    val allProcessedData = processedMapData + processedRawData

                    dataRepository.storeResult(taskConfig.name, allProcessedData)
                }
            }
        } else {
            logger.warn { "No result received in TaskSupervisor for jobId ${taskConfig.name}" }
            notifiers.forEach { it.reportErrorForJob(taskConfig.name) }
        }

    } catch (e: IOException) {
        logger.warn { "IO error while listening for result for jobId ${taskConfig.name}: \n${e.stackTrace}" }
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