# openai-api-proxy

A proxy for the [OpenAI chat completion API](https://platform.openai.com/docs/api-reference/chat/create) that keeps
track of used [tokens](https://help.openai.com/en/articles/4936856-what-are-tokens-and-how-to-count-them) and the
resulting [costs](https://openai.com/pricing). Metadata about each query is stored in a database (SQLite by default).
Note that openai-api-proxy does **absolutely no authentication nor authorization**, so never expose it outside of a
secure internal network.

This project uses [Spring Boot Web](https://docs.spring.io/spring-boot/docs/3.2.2/reference/html/web.html) to build
the REST service. [Spring Boot Data JPA](https://docs.spring.io/spring-boot/docs/3.2.2/reference/html/data.html),
[sqlite-jdbc](https://github.com/xerial/sqlite-jdbc), and
[hibernate-community-dialects](https://github.com/hibernate/hibernate-orm/blob/6.4/dialects.adoc) are  used for the
SQLite database access. [JTokkit ](https://github.com/knuddelsgmbh/jtokkit) is used for estimating the tokens of queries
before forwarding the API call.

## API endpoints

For illustration purposes, we assume that openai-api-proxy runs on localhost port 8080.

### Chat completion

The chat completion endpoint has (almost) the same specification as the
[OpenAI API](https://platform.openai.com/docs/api-reference/chat/create). This makes it possible to use openai-api-proxy
as a transparent proxy for applications. There are three notable differences:

* You don't have to set the `Authorization` header with the OpenAI API token. openai-api-proxy will inject this on the
  forwarded API calls.
* You must set the additional HTTP header `x-user`. This is because openai-api-proxy associates all queries with a user.
  Failing to do so will result in a 401 Unauthorized HTTP response.
* Tool calls are not supported.

Example usage:
```bash
curl -H "content-type: application/json" -H "x-user: adam" http://localhost:8080/v1/chat/completions -d '{"model":"gpt-4-1106-preview","messages":[{"role":"user","content":"I am using a proxy to talk to you."}]}'
```

### Model metadata

openai-api-proxy needs to know [pricing metadata](https://openai.com/pricing) about the models. The prices are given in
dollars per 1 thousand used tokens for both query tokens and answer tokens. The pricing of models may also change over
time.

Add pricing metadata for a specific model:
```bash
curl -H "content-type: application/json" http://localhost:8080/model/gpt-4-1106-preview -d '{"validFrom":"2023-11-06T00:00:00Z","per1KQueryTokens": 0.01,"per1KAnswerTokens": 0.03}'
```

Show a specific model:
```bash
curl http://localhost:8080/model/gpt-4-1106-preview
```
Example output:
```json
{
  "name": "gpt-4-1106-preview",
  "costs": [
    {
      "validFrom": "2023-11-06T00:00:00Z",
      "per1KQueryTokens": 0.01,
      "per1KAnswerTokens": 0.03
    }
  ]
}
```

Show all models:
```bash
curl http://localhost:8080/models
```
Returns a JSON list of model metadata (see above).

### Usage

Get aggregated information on all queries:
```bash
curl http://localhost:8080/usage
```
Example output:
```json
{
  "numQueries": 649,
  "queryTokens": 868292,
  "answerTokens": 302305,
  "cost": 17.752929999999985,
  "averageCostPerQuery": 0.02735428351309705
}
```

The usage can be filtered by user (parameter `user`) and/or timestamp (parameters `before` and `after`). The timestamp
must be given with a timezone, e.g. `Z` at the end for GMT or `+01` for UTC+1 (the `+` is URL-encoded as `%2B`). 
```bash
curl 'http://localhost:8080/usage?user=adam&after=2024-01-24T14:50%2B01'
```

Get the metadata about all queries:
```bash
curl http://localhost:8080/queries
```
Example output:
```json
[
  {
    "user": "adam",
    "model": "gpt-4-1106-preview",
    "timestamp": "2024-01-25T13:18:05.205Z",
    "queryTokens": 3243,
    "answerTokens": 1399,
    "cost": 0.07440000000000001
  },
  {
    "user": "adam",
    "model": "gpt-4-1106-preview",
    "timestamp": "2024-01-25T13:22:25.648Z",
    "queryTokens": 1218,
    "answerTokens": 477,
    "cost": 0.02649
  }
]
```

This can be filtered in the same way as the `/usage` endpoint.

### Ratelimits

openai-api-proxy stores the [ratelimit information](https://platform.openai.com/docs/guides/rate-limits) of the latest
call to the OpenAI API. It can be retrieved via:
```bash
curl http://localhost:8080/ratelimits
```
Example output:
```json
{
  "tokens": {
    "maximum": 500000,
    "remaining": 486895,
    "reset": "2024-01-25T14:04:47.719Z"
  },
  "requests": {
    "maximum": 500,
    "remaining": 499,
    "reset": "2024-01-25T13:27:03.44Z"
  }
}
```

## Configuration

You can set configuration options in either of these places:

* An `application.yaml` in the current working directory.
* Via system properties in the JVM (`-D` command line switch of the `java` process).
* Via environment variables in `UPPER_CASE_FORMAT`.

Useful configuration options:
| Option                  |  Default Value                               | Description                                 |
|-------------------------|----------------------------------------------|---------------------------------------------|
| `forward.url`           | `https://api.openai.com/v1/chat/completions` | The URL to forward API calls to.            |
| `forward.token`         | not set                                      | The OpenaAI API token to use for forwarded API calls. |
| `spring.datasource.url` | `jdbc:sqlite:openai-queries.db`              | The path to the SQLite database (after the `jdbc:sqlite:`). Default is the relative path `openai-queries.db` in the current working directory |
| `server.port`           | `8080`                                       | The TCP port to listen on.                  |

We recommend to set `forward.token` as an environment variable (`FORWARD_TOKEN`), as to not easily leak secrets in
open configuration files.

See also [the spring documentation](https://docs.spring.io/spring-boot/docs/3.2.2/reference/html/application-properties.html) 
for additional configuration options.

## Running

Run the main class `net.ssehub.openai_api_proxy.OpenAiApiProxy`, for example like this:
```bash
java -cp openai-api-proxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar net.ssehub.openai_api_proxy.OpenAiApiProxy
```

For the first startup, you have to set the configuration option `spring.jpa.hibernate.ddl-auto` to `update` in order to
create the correct schema for the SQLite database.

### As a systemd service

You can set up openai-api-proxy as a systemd service roughly like this:

1. Create directory `/opt/openai-api-proxy/`, put the jar-with-dependencies there.
2. Create a `run.sh` in `/opt/openai-api-proxy/`:
```bash
#!/bin/bash

export FORWARD_TOKEN="YOUR_TOKEN"
export SPRING_DATASOURCE_URL="jdbc:sqlite:/var/lib/openai-api-proxy/queries.db"

java -cp /opt/openai-api-proxy/openai-api-proxy.jar net.ssehub.openai_api_proxy.OpenAiApiProxy
```
Make this only accessible by `root` and `www-data`:
```
chown root:www-data /opt/openai-api-proxy/run.sh
chmod u=rwx,g=rx,o= /opt/openai-api-proxy/run.sh
```
3. Create `/var/lib/openai-api-proxy/` and set owner `chown www-data:www-data /var/lib/openai-api-proxy/`
4. Create `/etc/systemd/system/openai-api-proxy.service`:
```ini
[Unit]
Description=Proxy for the OpenAI API that keeps track of tokens and cost
After=network.target

[Service]
User=www-data
Group=www-data
ExecStart=/usr/bin/bash /opt/openai-api-proxy/run.sh

[Install]
WantedBy=multi-user.target
```
5. Reload systemd `systemctl daemon-reload`.

You can start the service with `systemctl start openai-api-proxy.service` and/or enable it for automatic startup with
`systemctl enable openai-api-proxy.service`. Check logs with `journalctl --unit openai-api-proxy.service`.

Remember that for the first startup, the SQLite database has to be setup with the correct schema. So either copy an
existing database to `/var/lib/openai-api-proxy/queries.db` or set `spring.jpa.hibernate.ddl-auto` to `update` for the
first startup.

## Compiling

This project uses [Maven](https://maven.apache.org/) for dependency management and the build process. To simply build
jars, run:
```
mvn package
```

This creates two jar files in the `target` folder (`$version` is the version that was built, e.g. `0.0.1-SNAPSHOT`):

* `openai-api-proxy-$version.jar` just includes the class files of this program.
* `openai-api-proxy-$version-jar-with-dependencies.jar` includes the class files of this program, plus all
dependencies. This means that this jar can be used when you don't want to manually provide all dependencies of this
program each time you execute it.
