package `fun`.suggoi.seleksister20

import kotlin.test.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.*
import `fun`.suggoi.seleksister20.utils.TimeOTP
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.math.BigInteger
import kotlin.random.Random

object TOTPTest: Spek({
  describe("HOTP Truncate function"){
    describe("sanity tests"){
      it("should be of length requested (6)"){
        val hmac = ByteArray(20)
        hmac.fill(127, 0, 20)
        val length = 6
        val totp = TimeOTP.Truncate(hmac, length)
        assertEquals(length, totp.length)
      }

      it("should be of length requested (8)"){
        val hmac = ByteArray(20)
        hmac.fill(127, 0, 20)
        val length = 8 
        val totp = TimeOTP.Truncate(hmac, length)
        assertEquals(length, totp.length)
      }
    }
    describe("RFC4226 test vectors"){
      it("should equal to truncated for count 0"){
        val parseHmac = BigInteger("0cc93cf18508d94934c64b65d8ba7667fb7cde4b0", 16).toByteArray()
        val actualHmac = parseHmac.slice(IntRange(1, parseHmac.size-1))
        val totp = TimeOTP.Truncate(actualHmac.toByteArray(), 6)
        assertEquals("755224", totp)
      }
      it("should equal to truncated for count 3"){
        val parseHmac = BigInteger("066c28227d03a2d5529262ff016a1e6ef76557ece", 16)
        val actualHmac = parseHmac
        val totp = TimeOTP.Truncate(actualHmac.toByteArray(), 6)
        assertEquals("969429", totp)
      }
      it("should equal to truncated for count 6"){
        val parseHmac = BigInteger("0bc9cd28561042c83f219324d3c607256c03272ae", 16).toByteArray()
        val actualHmac = parseHmac.slice(IntRange(1, parseHmac.size-1))
        val totp = TimeOTP.Truncate(actualHmac.toByteArray(), 6)
        assertEquals("287922", totp)
      }
      it("should equal to truncated for count 9"){
        val parseHmac = BigInteger("01637409809a679dc698207310c8c7fc07290d9e5", 16)
        val actualHmac = parseHmac
        val totp = TimeOTP.Truncate(actualHmac.toByteArray(), 6)
        assertEquals("520489", totp)
      }
    }
  }

  describe("TOTP generation, fixed clock, RFC6238 test vectors"){
    val key = "12345678901234567890123456789012".toByteArray(Charsets.US_ASCII)
    it("should generate correct TOTP for time 59"){
      val totp = TimeOTP.generateTOTP(key, Clock.fixed(Instant.ofEpochSecond(59), ZoneId.of("UTC")), 0, 30, 8)
      assertEquals("46119246", totp)
    }
    it("should generate correct TOTP for time 1111111109"){
      val totp = TimeOTP.generateTOTP(key, Clock.fixed(Instant.ofEpochSecond(1111111109), ZoneId.of("UTC")), 0, 30, 8)
      assertEquals("68084774", totp)
    }
    it("should generate correct TOTP for time 1111111111"){
      val totp = TimeOTP.generateTOTP(key, Clock.fixed(Instant.ofEpochSecond(1111111111), ZoneId.of("UTC")), 0, 30, 8)
      assertEquals("67062674", totp)
    }
    it("should generate correct TOTP for time 1234567890"){
      val totp = TimeOTP.generateTOTP(key, Clock.fixed(Instant.ofEpochSecond(1234567890), ZoneId.of("UTC")), 0, 30, 8)
      assertEquals("91819424", totp)
    }
    it("should generate correct TOTP for time 2000000000"){
      val totp = TimeOTP.generateTOTP(key, Clock.fixed(Instant.ofEpochSecond(2000000000), ZoneId.of("UTC")), 0, 30, 8)
      assertEquals("90698825", totp)
    }
    it("should generate correct TOTP for time 20000000000"){
      val totp = TimeOTP.generateTOTP(key, Clock.fixed(Instant.ofEpochSecond(20000000000), ZoneId.of("UTC")), 0, 30, 8)
      assertEquals("77737706", totp)
    }
  }
})