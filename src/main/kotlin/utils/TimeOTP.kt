package `fun`.suggoi.seleksister20.utils

import kotlin.math.pow
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer
import java.time.Clock
import java.math.BigInteger

object TimeOTP {
  /**
   * Generates a TOTP based on current time.
   * 
   * @param secret The secret used for HMAC
   * @param clock Clock used for counter in TOTP
   * @param T0 Unix time to start counting steps, in seconds
   * @param X Time step, in seconds
   * @param length OTP digits generated
   * @return the TOTP formatted as string with {@code length} digits
   */
  fun generateTOTP(secret: ByteArray, clock: Clock, T0: Long, X: Long, length: Int): String{
    val hs256 = Mac.getInstance("HmacSHA256")
    val key = SecretKeySpec(secret, "RAW")
    hs256.init(key)

    val T = ((clock.millis()/1000) - T0)/X
    val bytes = ByteBuffer.allocate(8).putLong(T).array()

    val hmac = hs256.doFinal(bytes)
    return Truncate(hmac, length)
  }

  /**
   * Truncates the hmac digest to {@code digits} long
   * 
   * @param hmac The digest to be truncated
   * @param digits length of truncated digest
   * @return truncated {@code hmac}
   */
  fun Truncate(hmac: ByteArray, digits: Int): String{
    val offset = (hmac[hmac.size - 1].toInt() and 0xf) // get low nibble of last byte
    return "%0${digits}d".format(
            (((hmac[offset+0].toInt() and 0x7F) shl 24) or
             ((hmac[offset+1].toInt() and 0xFF) shl 16) or
             ((hmac[offset+2].toInt() and 0xFF) shl 8) or
              (hmac[offset+3].toInt() and 0xFF))
              .mod((10.0.pow(digits.toDouble())).toInt()))
  }
}