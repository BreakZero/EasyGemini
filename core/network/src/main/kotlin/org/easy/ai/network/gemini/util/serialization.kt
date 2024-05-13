package org.easy.ai.network.gemini.util

import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import timber.log.Timber

/**
 * Serializer for enums that defaults to the first ordinal on unknown types.
 *
 * Convention is that the first enum be named `UNKNOWN`, but any name is valid.
 *
 * When an unknown enum value is found, the enum itself will be logged to stderr with a message
 * about opening an issue on GitHub regarding the new enum value.
 */
internal class FirstOrdinalSerializer<T : Enum<T>>(private val enumClass: KClass<T>) :
    KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FirstOrdinalSerializer")

    override fun deserialize(decoder: Decoder): T {
        val name = decoder.decodeString()
        val values = enumClass.enumValues()

        return values.firstOrNull { it.serialName == name }
            ?: values.first().also { printWarning(name) }
    }

    private fun printWarning(name: String) {
        Timber.tag("FirstOrdinalSerializer").e(
            """
        |Unknown enum value found: $name"
        |This usually means the backend was updated, and the SDK needs to be updated to match it.
        |Check if there's a new version for the SDK, otherwise please open an issue on our
        |GitHub to bring it to our attention:
        |https://github.com/google/google-ai-android
       """.trimMargin()
        )
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.serialName)
    }
}

/**
 * Provides the name to be used in serialization for this enum value.
 *
 * By default an enum is serialized to its [name][Enum.name], and can be overwritten by providing a
 * [SerialName] annotation.
 */
internal val <T : Enum<T>> T.serialName: String
    get() = declaringJavaClass.getField(name).getAnnotation(SerialName::class.java)?.value ?: name

/**
 * Variant of [kotlin.enumValues] that provides support for [KClass] instances of enums.
 *
 * @throws SerializationException if the class is not a valid enum. Beyond runtime emily magic, this
 *   shouldn't really be possible.
 */
internal fun <T : Enum<T>> KClass<T>.enumValues(): Array<T> =
    java.enumConstants ?: throw SerializationException("$simpleName is not a valid enum type.")