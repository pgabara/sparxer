Sparxer [![Build Status][build-badge]][build-link]
==============
Submit your spark jobs using rest api.

## Features
- Submit spark job.
- Resubmit spark job using its id.
- Get user jobs.
- Get job details.
- Token based authentication.

## Future
Ideas on how to improve the project:

1. Create a front end, maybe ScalaJS/React?
2. Schedule spark jobs e.g. run every Monday at 1PM.
3. Send emails with job statuses.
4. Setup a test module to test spark and db integration (TravisCI).
5. Improve README :)

## API
Here is the Sparxer API description that will help you to submit a spark job, resubmit it, get your submitted jobs and to find a job details.

### How to sign in
Sparxer uses token based authentication (Bearer). Generated token will be active for one hour.

```
TOKEN=$(curl -s -H 'Content-Type: application/json' --data '{email: "{email}", password: "{password}"}' -X POST http://{hostname}:{port}/auth/sign-in)
```

### How to submit a spark job
Sparxer uses api that is close to native spark-submit tool.

```
curl --data {json} -H 'Content-Type: application/json' -H 'Authorization: Bearer ${TOKEN}' -X POST http://{hostname}:{port}/spark/submit
```

Json format:
```
{
    "mainClass": "org.apache.spark.examples.SparkPi",
    "master": "local[*]",
    "deployMode": "client",
    "jar": "/opt/spark/examples/jars/spark-examples_2.11-2.4.3.jar",
    "sparkConf": {},
    "args": [
        "100"
    ],
    "envs": {
        "SPARK_HOME": "/opt/spark"
    }
}
```

### How to resubmit a job
Sparxer allows to resubmit existing job providing its id number.

```
curl -H 'Content-Type: application/json' --data '{"id": 12345}' -H 'Authorization: Bearer ${TOKEN}' -X POST http://{hostname}:{port}/spark/resubmit
```

### How to get user jobs
You can access all jobs that were submitted by the logged in user.

```
curl -H 'Accept: application/json' -H 'Authorization: Bearer ${TOKEN}' -X GET http://{hostname}:{port}/jobs
```

### How to get a spark job details
You can get data about a submitted job. It will provide information about a job configuration and a list of statuses.

```
curl -H 'Accept: application/json' -H 'Content-Type: application/json' -H 'Authorization: Bearer ${TOKEN}' -X GET http://{hostname}:port/jobs/12345
```

## How to run
todo...

## Technology stack
- [ZIO](https://zio.dev/) - Type-safe, composable asynchronous and concurrent programming for Scala.
- [Http4s](https://http4s.org/) - Type-full, functional, streaming HTTP for Scala.
- [Doobie](https://tpolecat.github.io/doobie/) - A functional JDBC layer for Scala.
- [Circe](https://circe.github.io/circe/) - A JSON library for Scala powered by Cats.
- [TSec](https://jmcardon.github.io/tsec/) - A Type-Safe General Cryptography Library on the JVM.

[build-badge]: https://travis-ci.org/pgabara/sparxer.svg?branch=master
[build-link]: https://travis-ci.org/pgabara/sparxer