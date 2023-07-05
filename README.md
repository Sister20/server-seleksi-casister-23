# Challenge Server
Author:
- suggoitanoshi

## Why
To challenge the author, this server is made using a tech stack
not usually used in the author's circle. Also, this challenge was
first proposed just to make candidate's life harder.

## How to run
The application is dockerized, so you could run it using docker
if you wish to.

### Configuration
The main configuration is in `src/main/resources/application.conf`
It contains configurations for:
- Server port (`ktor.deployment.port`)
- TOTP parameters (`totp.*`)
- Selection part names (`selection.parts`)
- Server header (`server.name`)

Then, the environment variable `.env` must contain the same keys as in
`.env.example` provided in this repository.
The key `DB_URL` is a JDBC database URL.
The key `SECRET` is the shared secret for TOTP

### Docker Instructions
Build the image:
`docker build -t challenge .`

Run it:
`docker run -v ./database.db:/bin/runner/database.db:rw challenge`

### Manual Instructions
Run using gradle:
`./gradlew run`
