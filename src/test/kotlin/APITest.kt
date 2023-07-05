package `fun`.suggoi.seleksister20

import `fun`.suggoi.seleksister20.AppConfig
import `fun`.suggoi.seleksister20.*
import `fun`.suggoi.seleksister20.utils.TimeOTP
import `fun`.suggoi.seleksister20.data.SubmitRequest

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import java.nio.file.Path
import java.io.File
import kotlin.test.*
import kotlin.io.path.createTempFile
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.*
import io.ktor.server.testing.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.server.application.*;
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.github.cdimascio.dotenv.*
import org.kodein.di.*
import org.kodein.type.*
import org.ktorm.database.Database
import org.spekframework.spek2.dsl.Root
import kotlinx.serialization.Serializable

var dbName: Path? = null
val sharedSecret = "seleksister20"
var db = ""
val currentServerSeconds = 420L
lateinit var appConfig: AppConfig
lateinit var testDI: DI
fun reset(){
  if(dbName != null){
    File(dbName.toString()).delete()
  }
  dbName = createTempFile()
  db = "jdbc:sqlite:${dbName!!.toAbsolutePath().toString()}"
  testDI = DI {
    bind<AppConfig>{ singleton { appConfig } }
  }
  appConfig = AppConfig(db, sharedSecret, Clock.fixed(Instant.ofEpochSecond(currentServerSeconds), ZoneId.of("UTC")))
}

object APITest: Spek({
  reset()
  describe("Test API"){
    describe("authorization fail"){
      it("should decline request without authorization header"){
        testApplication{
          application {
            main(testDI)
          }
          environment{
            config = ApplicationConfig("test.conf")
          }

          val response = client.post("/submit/a")
          assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
      }

      it("should decline request with empty authorization header"){
        testApplication{
          application {
            main(testDI)
          }
          environment{
            config = ApplicationConfig("test.conf")
          }

          val response = client.post("/submit/a"){
            headers{
              append(HttpHeaders.Authorization, "")
            }
          }
          assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
      }

      it("should decline request with wrong password"){
        testApplication{
          var step: Long = 30
          var length: Int = 8
          application {
            main(testDI)
          }
          environment{
            config = ApplicationConfig("test.conf")
            length = config.propertyOrNull("totp.length")?.getString()?.toIntOrNull() ?: length
            step = config.propertyOrNull("totp.length")?.getString()?.toLongOrNull() ?: step
          }

          val username = "13520001"
          val password = TimeOTP.generateTOTP(
            (sharedSecret+username).toByteArray(),
            Clock.fixed(Instant.ofEpochSecond(currentServerSeconds+step*2), ZoneId.of("UTC")),
            0, step, length
          )
          val response = client.post("/submit/a"){
            basicAuth(username, password)
          }
          assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
      }

      it("should decline request with NIM outside of `13520xxx` or `18220xxx`"){
        testApplication{
          var step: Long = 30
          var length: Int = 8
          application {
            main(testDI)
          }
          environment{
            config = ApplicationConfig("test.conf")
            length = config.propertyOrNull("totp.length")?.getString()?.toIntOrNull() ?: length
            step = config.propertyOrNull("totp.length")?.getString()?.toLongOrNull() ?: step
          }

          val usernameFail = "16520001"
          val password = TimeOTP.generateTOTP(
            (sharedSecret+usernameFail).toByteArray(),
            Clock.fixed(Instant.ofEpochSecond(currentServerSeconds), ZoneId.of("UTC")),
            0, step, length
          )
          val responseFail = client.post("/submit/a"){
            basicAuth(usernameFail, password)
          }
          assertEquals(HttpStatusCode.Unauthorized, responseFail.status)
        }
      }
    }

    describe("authorization pass"){
      it("should respond with bad request to empty submission"){
        testApplication{
          var step: Long = 30
          var length: Int = 8
          application {
            main(testDI)
          }
          environment{
            config = ApplicationConfig("test.conf")
            length = config.propertyOrNull("totp.length")?.getString()?.toIntOrNull() ?: length
            step = config.propertyOrNull("totp.length")?.getString()?.toLongOrNull() ?: step
          }

          val username = "13520001"
          val password = TimeOTP.generateTOTP(
            (sharedSecret+username).toByteArray(),
            Clock.fixed(Instant.ofEpochSecond(currentServerSeconds), ZoneId.of("UTC")),
            0, step, length
          )
          val response = client.post("/submit/a"){
            basicAuth(username, password)
          }
          assertEquals(HttpStatusCode.BadRequest, response.status)
        }
      }

      it("should return created to non-empty submission"){
        reset()
        testApplication{
          var step: Long = 30
          var length: Int = 8
          application {
            main(testDI)
          }
          environment{
            config = ApplicationConfig("test.conf")
            length = config.propertyOrNull("totp.length")?.getString()?.toIntOrNull() ?: length
            step = config.propertyOrNull("totp.length")?.getString()?.toLongOrNull() ?: step
          }
          val client = createClient{
            install(ContentNegotiation){
              json()
            }
          }

          val username = "13520001"
          val password = TimeOTP.generateTOTP(
            (sharedSecret+username).toByteArray(),
            Clock.fixed(Instant.ofEpochSecond(currentServerSeconds), ZoneId.of("UTC")),
            0, step, length
          )
          val response = client.post("/submit/a"){
            basicAuth(username, password)
            contentType(ContentType.Application.Json)
            setBody(SubmitRequest("Full Name", "link", "message"))
          }
          assertEquals(HttpStatusCode.Created, response.status)
        }
      }

      it("should not overwrite last submission"){
        reset()
        testApplication{
          var step: Long = 30
          var length: Int = 8
          application {
            main(testDI)
          }
          environment{
            config = ApplicationConfig("test.conf")
            length = config.propertyOrNull("totp.length")?.getString()?.toIntOrNull() ?: length
            step = config.propertyOrNull("totp.length")?.getString()?.toLongOrNull() ?: step
          }
          val client = createClient{
            install(ContentNegotiation){
              json()
            }
          }

          val username = "13520001"
          val password = TimeOTP.generateTOTP(
            (sharedSecret+username).toByteArray(),
            Clock.fixed(Instant.ofEpochSecond(currentServerSeconds), ZoneId.of("UTC")),
            0, step, length
          )
          val firstResponse = client.post("/submit/a"){
            basicAuth(username, password)
            contentType(ContentType.Application.Json)
            setBody(SubmitRequest("Full Name", "link", "message"))
          }
          assertEquals(HttpStatusCode.Created, firstResponse.status)
          val secondResponse = client.post("/submit/a"){
            basicAuth(username, password)
            contentType(ContentType.Application.Json)
            setBody(SubmitRequest("Full Name", "link", "message"))
          }
          assertEquals(HttpStatusCode.NoContent, secondResponse.status)
        }
      }

      it("should respond with bad request due to null value"){
        testApplication{
          var step: Long = 30
          var length: Int = 8
          application {
            main(testDI)
          }
          environment{
            config = ApplicationConfig("test.conf")
            length = config.propertyOrNull("totp.length")?.getString()?.toIntOrNull() ?: length
            step = config.propertyOrNull("totp.length")?.getString()?.toLongOrNull() ?: step
          }
          val client = createClient{
            install(ContentNegotiation){
              json()
            }
          }

          val username = "13520001"
          val password = TimeOTP.generateTOTP(
            (sharedSecret+username).toByteArray(),
            Clock.fixed(Instant.ofEpochSecond(currentServerSeconds), ZoneId.of("UTC")),
            0, step, length
          )
          val response = client.post("/submit/a"){
            basicAuth(username, password)
            contentType(ContentType.Application.Json)
            setBody(SubmitRequest())
          }
          assertEquals(HttpStatusCode.BadRequest, response.status)
        }
      }
    }
  }
})