# Gamayun data collector

Gamayun is an application intended to ease data collection processes. 

## Contents
* [Basic idea](#basic-idea)
* [Name](#name)
* [Usage](#usage)
  * [Docker prerequisites](#docker-prereqs)
  * [Docker usage](#docker-usage)
* [Configuration](#configuration) 
  * [Application Configuration](#app-configuration)
    * [Email Configuration](#app-email-configuration)
    * [MongoDB Configuration](#app-mongo-configuration)
    * [Logging Configuration](#app-logging-configuration)
  * [Job Configuration](#job-configuration)
    * [Path expansion](#job-path-expansion-configuration)
* [Reporting the result of a job](#job-result-report)
* [License](#license)
* [Contributing](#contributing)

<a name="basic-idea"></a>
## Basic idea
The idea behind the application is simplification of data collection. To start collecting some new data only the minimal code needed for data collection should be written (for example a python script that collects the data). Everything else (triggering the script at wanted times, storing the data somewhere, error notifications in case of errors while collecting...) is handled by Gamayun.

Most important concept in Gamayun is job, or gamayun-job, and using the Gamayun basically means writing the configuration for all the jobs you need and then running the application with that configuration. An example of a gamayun-job would be collecting some system resources data every morning at 7AM, or scrapping some website every ten minutes. More information about job configuration can be found in TODO.

If you are not too sure you understand how the application works after reading this documentation, check the sample configuration project which is well commented and documented (can be found [here](https://github.com/ivan-brko/GamayunConfigurationSample)).     

<a name="name"></a>
## Name 
Gamayun is a prophetic bird of Slavic folklore, a symbol of wisdom and knowledge.

<a name="usage"></a>
## Usage
Although you can clone this project, build it and use it like that, the easiest way to set things up is with Docker.

<a name="docker-prereqs"></a>
### Docker prerequisites 
If you don't know anything about Docker, there is a small tutorial on how the things are set up in the sample configuration project (can be found [here](https://github.com/ivan-brko/GamayunConfigurationSample)). 

<a name="docker-usage"></a>
### Docker usage
There is a sample repo which shows how to set configuration and then easily deploy it with Docker and it can be found [here](https://github.com/ivan-brko/GamayunConfigurationSample).

This sample can be used as a base for building a new Gamayun configuration. The TLDR is to create a repo which contains Gamayun configuration in valid format, create a Docker image (let's call this the configuration image) with Gamayun as the base image. In that configuration image just copy the entire configuration and set environment variable to point to the location where the configuration is stored in the container. After that, using docker-compose we run this image and MongoDB as two containers and that should be it, Gamayun should be running and storing the data into MongoDB.   
Note that you should change the MongoDB credentials to something more secure from what is set in the sample repo.
Also, note that this sample doesn't expose the MongoDB container directly, so you can't connect to it.

<a name="configuration"></a>
## Configuration
There are two basic parts of Gamayun configuration:
<a name="app-configuration"></a>
### Application configuration
This configuration is about all the things that are not specific to a single job and at the moment the following is supported:
 * Email configuration (if you want Gamayun to send you emails when some jobs aren't working properly or when critical errors happen in the application), usage of this is optional
 * MongoDB configuration, as MongoDB is the only supported repository at now, but this will change later
 * Logging configuration (Gamayun uses log4j 2, note that if you are using docker you need to copy this over Gamayun default log configuration)

<a name="app-email-configuration"></a>
#### Email configuration 
Email configuration ATM is just a POC and it is verified to work only with gmail, this will be changed in the future so the configuration won't be explained in detail here as it is mostly self explanatory. Maybe the best approach to using this feature ATM is to create a new GMail account that will be used just for sending these error reports. If you plan to send reports from Gmail account you don't have to modify ```smtpHost``` and ```smtpPort``` fields. You also might need to configure Gmail account options to allow insecure 3pp apps.
Email configuration should be stored in a file called _gamayun-email.config.toml_ and that file should be in the directory pointed to with envvar _GAMAYUN_CONF_ROOT_. You can see the sample of the file in the configuration example repo.
<a name="app-mongo-configuration"></a> 
#### MongoDB configuration
This configuration (ATM) has only 2 properties, _connectionString_ and _databaseName_.
MongoDB configuration should be stored in a file called _gamayun-mongo.config.toml_ and that file should be in the directory pointed to with envvar _GAMAYUN_CONF_ROOT_. You can see the sample of the file in the configuration example repo.
<a name="app-logging-configuration"></a>
#### Logging configuration
Consult with Log4j2 documentation for information on how to configure logging.

<a name="job-configuration"></a>
### Job configuration
Job configuration is specific for each job that needs to be run, and it consists of (at least) one file, which has a name that ends with _gamayun-job.config.toml_.

In that file, the following properties are available: 
* _**name**_: mandatory string field which is the name of the job. Each job needs to have a unique name or the application will not start ([***read the comment about path expansion](#job-path-expansion-configuration))
* _**pathToExecutable**_: mandatory string field which is the path to the executable that will be run when the job is triggered ([***read the comment about path expansion](#job-path-expansion-configuration))
* _**cronString**_: mandatory string which contains a valid cronstring. This cronstring defines the schedule for triggering the job. Quartz format for crons is used
* _**arguments**_: optional string list which contains the arguments that will be given to the executable when running it ([***read the comment about path expansion](#job-path-expansion-configuration)) 
* _**tags**_: optional string list which contains the tags to be associated with this job in the database
* _**resultWaitTimeoutMillis**_: optional unsigned value which defines for how long the results for this job will be awaited before deciding that the job has failed (default is 1000)
* _**uniqueIds**_: optional string list which defines what keys need to be unique for this task. For example, say you write a web scrapper which collects ads from some site. When running this crawler multiple times you will end up with some duplicate ads from different job runs. What you can do here is add a ```uniqueId = ["link"]``` and for each ad also store its link under this key. This will ensure that only one document in the database can have that link which means no duplicates will be stored, even though the crawler will report them. If you want to use multiple fields as keys, consult with MongoDB documentation (part about compound indexes). 
* **_randomTriggerOffsetSeconds_**: optional unsigned value which defines the random interval (in seconds) in which the job will be run after the next trigger by cron is hit (default is 0). For example, if you set cron to run the job every day at 07:00 PM and set _randomTriggerOffsetSeconds_ to 60, then the job will run each day somewhere between 7:00 PM and 7:01 PM.

<a name="job-path-expansion-configuration"></a>
#### Path expansion
***Some variables support path expansion, which will replace all occurrences of _${CONFIGURATION_FILE_PATH}_ with full path to the current job configuration toml job (without the filename, just the path!).

<a name="job-result-report"></a>
## Reporting the result of a job
Results of the jobs are reported to Gamayun over GRPC. Gamayun is listening on localhost, port 16656. Proto file used for reporting result can be found under resources in the project. The sample project has example scripts which report results to the server. 

To simplify job reports and remove explicit dependency on GRPC for the user of the application, python package will be added which will contain a simple API for reporting results. Also, in the future there will be an option to return errors so that error messages get more informative. At the moment, nothing should be return in case of error and the job should just timeout.

<a name="license"></a>
## License
The project is under a permissive (commercial and non-commercial usage) MIT license.

<a name="contributing"></a>
## Contributing 
All contributions, bug reports and feature requests are welcome. 