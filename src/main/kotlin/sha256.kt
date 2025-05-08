package dev.jhyub

import java.security.MessageDigest

fun sha256(str: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(str.toByteArray())
    return bytesToHex(md.digest())
}

fun bytesToHex(byteArray: ByteArray): String {
    val digits = "0123456789ABCDEF"
    val hexChars = CharArray(byteArray.size * 2)
    for(i in byteArray.indices) {
        val v = byteArray[i].toInt() and 0xff
        hexChars[i * 2] = digits[v shr 4]
        hexChars[i * 2 + 1] = digits[v and 0xf]
    }
    return String(hexChars)
}