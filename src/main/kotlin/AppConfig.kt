package `fun`.suggoi.seleksister20

import java.time.Clock
import org.ktorm.database.Database

data class AppConfig(
  val databaseUrl: String,
  val sharedSecret: String,
  val clock: Clock
)