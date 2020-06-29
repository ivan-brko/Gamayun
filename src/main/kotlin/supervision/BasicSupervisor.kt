package supervision

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import storage.DataRepository
import java.io.IOException

class BasicSupervisor(private val listener: ResultListener, private val dataRepository: DataRepository) : Supervisor {
    override suspend fun runTask(taskInfo: TaskInfo) {
        runCommand(taskInfo.pathToExe, taskInfo.args)
    }

    //c/p from so with some coroutine modifications
    suspend fun runCommand(executable: String, args: List<String>): String? =
        withContext(Dispatchers.IO) {
            try {
                val executableWithArgs = mutableListOf<String>().let {
                    it.add(executable)
                    it.addAll(args)
                    it.toList()
                }

                val proc = ProcessBuilder(executableWithArgs)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start() //this line doesn't block until program stops executing, only until program is started

                val result = listener.listenForResult("abc", 1000)
                //todo: kill process if it is still running

                dataRepository.storeResult("abc", result ?: "test")

                proc.inputStream.bufferedReader().readText()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

}