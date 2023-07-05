package `fun`.suggoi.seleksister20

import `fun`.suggoi.seleksister20.utils.TimeOTP
import `fun`.suggoi.seleksister20.data.*

import java.time.Clock
import java.time.ZoneId
import io.ktor.server.application.*;
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.http.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import io.github.cdimascio.dotenv.dotenv
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.kodein.di.*
import org.kodein.type.*
import org.kodein.di.ktor.*
import org.flywaydb.core.Flyway
import org.sqlite.SQLiteDataSource
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.main() {
  val dotenv = dotenv()
  val secret = dotenv["SECRET"]
  val db = dotenv["DB_URL"]
  val clock = Clock.systemUTC()
  val config = AppConfig(db, secret, clock)
  main(DI{
    bind<AppConfig>{ singleton { config } }
  })
}

fun Application.main(kodein: DI){
  val config by kodein.instance<AppConfig>()
  val dbUrl = config.databaseUrl
  val secret = config.sharedSecret
  val clock = config.clock
  val step = environment.config.propertyOrNull("totp.step")?.getString()?.toLongOrNull() ?: 30L
  val length = environment.config.propertyOrNull("totp.length")?.getString()?.toIntOrNull() ?: 8
  val serverHeader = environment.config.propertyOrNull("header.server")?.getString() ?: "Kotr"
  val availableParts = environment.config.propertyOrNull("selection.parts")?.getList() ?: emptyList()
  val dataSource = SQLiteDataSource()
  dataSource.setUrl(dbUrl)
  val flyway = Flyway.configure().dataSource(dataSource).load()
  flyway.migrate()
  val db = Database.connect(dataSource)

  install(Authentication){
    basic{
      realm = "Access to uploading"
      validate {credentials ->
        if(credentials.name == "" || credentials.password == ""){
          application.log.info("Empty authorization header")
          null
        }
        else{
          val commonSecret = (secret+credentials.name).toByteArray()
          if((credentials.name.startsWith("13521") or credentials.name.startsWith("18221")) and 
            ((credentials.password == TimeOTP.generateTOTP(commonSecret, Clock.fixed(clock.instant().minusSeconds(step), ZoneId.of("UTC")), 0, step, length)) or
            (credentials.password == TimeOTP.generateTOTP(commonSecret, clock, 0, step, length)) or
            (credentials.password == TimeOTP.generateTOTP(commonSecret, Clock.fixed(clock.instant().plusSeconds(step), ZoneId.of("UTC")), 0, step, length)))
          ){
            application.log.info("${credentials.name} authorized successfully")
            UserIdPrincipal(credentials.name)
          }
          else{
            application.log.info("${credentials.name} failed to authorize")
            null
          }
        }
      }
    }
  }
  install(ContentNegotiation){
    json(Json{
      ignoreUnknownKeys = true
    })
  }
  install(DefaultHeaders){
    header(HttpHeaders.Server, serverHeader)
  }
  routing {
    authenticate{
      post("/submit/{part}"){
        val nim = call.principal<UserIdPrincipal>()?.name?.toIntOrNull()
        val part = call.parameters["part"].toString()
        call.application.environment.log.info("$nim submit for $part")
        if(availableParts.contains(part)){
          try{
            val data = call.receive<SubmitRequest>()
            if(nim != null){
              // do something
              if(data.fullName == null || data.link == null){
                call.application.environment.log.info("$nim submitted with null field. payload = $data")
                call.respondText("Please fix payload.", status = HttpStatusCode.BadRequest)
              }
              else{
                val res = db.from(Answers)
                  .select(Answers.nim)
                  .where{
                    (Answers.nim eq nim) and (Answers.part eq part)
                  }
                if(res.totalRecords == 0){
                  db.insert(Answers){
                    set(it.nim, nim)
                    set(it.part, part)
                    set(it.fullname, data.fullName)
                    set(it.link, data.link)
                    set(it.message, data.message)
                  }
                  call.application.environment.log.info("$nim completed $part")
                  call.respondText("Congratulations on completing part $part!", status = HttpStatusCode.Created)
                }
                else if(res.totalRecords == 1){
                  val name = res.rowSet[Answers.fullname]
                  call.application.environment.log.info("$nim resubmitted for $part")
                  call.respondText("You have already completed part $part, $name.", status = HttpStatusCode.NoContent)
                }
              }
            }
            else{
              call.application.environment.log.warn("$nim probably hacked the server...")
              call.respondText("You shouldn't be able to reach this...", status = HttpStatusCode.Forbidden)
            }
          }
          catch(e: ContentTransformationException){
            call.application.environment.log.info("$nim submit for $part but with wrong payload")
            call.respondText("Fix submission format.", status = HttpStatusCode.BadRequest)
          }
          catch(e: Exception){
            call.application.environment.log.error("$nim submit for $part, but error: $e")
            call.respondText("Unknown error occurred.", status = HttpStatusCode.BadGateway)
          }
        }
        else{
          call.application.environment.log.info("$nim submit for invalid part $part")
          call.respondText("Part invalid", status = HttpStatusCode.Forbidden)
        }
      }
    }
  }
}