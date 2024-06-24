package com.clone.sickbar

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object TestSerializer : Serializer<Test> {
    private val cryptoManager = CryptoManager()
    override val defaultValue: Test
        get() = Test()

    override suspend fun readFrom(input: InputStream): Test {
        return try {
            val string = cryptoManager.decrypt(input)
            Json.decodeFromString(
                deserializer = Test.serializer(),
                string = string.decodeToString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: Test, output: OutputStream) {
        println("datastore2 $t")
        cryptoManager.encrypt(
            byteArray = Json.encodeToString(
                serializer = Test.serializer(),
                value = t
            ).encodeToByteArray(),
            outputStream = output
        )

    }
}