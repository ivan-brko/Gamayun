package di

import config.ConfigurationReader
import config.TomlConfigurationReader
import notification.NotificationSetup.setupNotificationSupport
import notification.Notifier
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import processing.BasicResultProcessor
import processing.ResultProcessor
import scheduling.GamayunInitializer
import scheduling.QuartzScheduler
import scheduling.Scheduler
import storage.DataRepository
import storage.mongo.MongoDataRepository
import storage.mongo.MongoDbSettings
import supervision.*

object KodeinSetup {
    fun setupDi(tomlConfigurationRoot: String) =
            DI {
                bind<ConfigurationReader>() with singleton { TomlConfigurationReader(tomlConfigurationRoot) }
                bind<ResultProcessor>() with singleton { BasicResultProcessor() }
                bind<Scheduler>() with singleton { QuartzScheduler(di) }
                bind<GamayunInitializer>() with singleton { GamayunInitializer(di) }
                bind<MongoDbSettings>() with singleton { MongoDbSettings(di) }
                bind<DataRepository>() with singleton { MongoDataRepository(di) }
                bind<ResultListener>() with singleton { GamayunGrpcResultListener(GrpcResultServer()) }
                bind<List<Notifier>>() with singleton { setupNotificationSupport(di) }
                bind<TaskSupervisor>() with singleton { BasicTaskSupervisor(di) }
            }


}