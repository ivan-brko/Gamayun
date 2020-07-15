package supervision

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import storage.DataRepository
import java.io.IOException

class BasicTaskSupervisor(private val listener: ResultListener, private val dataRepository: DataRepository) : TaskSupervisor {
    override suspend fun runTask(taskConfig: TaskConfig) {
        runCommand(taskConfig)
    }

    //c/p from so with some coroutine modifications
    private suspend fun runCommand(taskConfig: TaskConfig) =
        withContext(Dispatchers.IO) {
            try {
                val executableWithArgs = mutableListOf<String>().let {
                    it.add(taskConfig.pathToExe)
                    it.addAll(taskConfig.args)
                    it.toList()
                }

                val proc = ProcessBuilder(executableWithArgs)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start() //this line doesn't block until program stops executing, only until program is started

                val result = listener.listenForResult(taskConfig.name, 50000)
                if (result != null){
                    println(result)
                }
                else {
                    println("No result!")
                }
                //todo: kill process if it is still running

            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

}