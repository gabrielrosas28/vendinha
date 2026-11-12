package br.com.santachiara.vendinha.domain

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Senha {
    const val MIN_DIGITOS = 4
    const val MAX_DIGITOS = 8
    private const val ALGORITMO = "PBKDF2WithHmacSHA1"
    private const val ITERACOES = 12_000
    private const val BITS = 256

    fun formatoValido(senha: String): Boolean =
        senha.length in MIN_DIGITOS..MAX_DIGITOS && senha.all { it in '0'..'9' }

    fun gerarSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt.toHex()
    }

    fun hash(senha: String, saltHex: String): String {
        val spec = PBEKeySpec(senha.toCharArray(), saltHex.hexToBytes(), ITERACOES, BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    fun confere(senha: String, saltHex: String, hashHex: String): Boolean {
        if (!formatoValido(senha)) return false
        return MessageDigest.isEqual(hash(senha, saltHex).hexToBytes(), hashHex.hexToBytes())
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
