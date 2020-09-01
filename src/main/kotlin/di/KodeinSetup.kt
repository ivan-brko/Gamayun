package di

import config.ConfigurationReader
import config.TomlConfigurationReader
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import processing.ResultProcessor
import processing.SimpleResultProcessor
import scheduling.QuartzScheduler
import scheduling.Scheduler
import storage.DataRepository
import storage.mongo.MongoDataRepository
import storage.mongo.MongoDbSettings
import supervision.*
import supervision.errorReport.ErrorReportSetup.setupErrorSupport
import supervision.errorReport.ErrorReporter

object KodeinSetup {
    fun setupDi(tomlConfigurationRoot: String) =
        DI {
            bind<ConfigurationReader>() with singleton { TomlConfigurationReader(tomlConfigurationRoot) }
            bind<ResultProcessor>() with singleton { SimpleResultProcessor() }
            bind<Scheduler>() with singleton { QuartzScheduler(di) }
            bind<MongoDbSettings>() with singleton { MongoDbSettings(di) }
            bind<DataRepository>() with singleton { MongoDataRepository(di) }
            bind<ResultListener>() with singleton { GamayunGrpcResultListener(GrpcResultServer()) }
            bind<List<ErrorReporter>>() with singleton { setupErrorSupport(di) }
            bind<TaskSupervisor>() with singleton { BasicTaskSupervisor(di) }
        }


}