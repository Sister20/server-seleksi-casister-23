package `fun`.suggoi.seleksister20.data

import org.ktorm.schema.*

object Answers: Table<Nothing>("answers"){
  val nim = int("nim").primaryKey()
  val part = varchar("part").primaryKey()
  val fullname = varchar("fullname")
  val link = varchar("link")
  val message = varchar("message")
  val submittedAt = varchar("submittedAt")
}