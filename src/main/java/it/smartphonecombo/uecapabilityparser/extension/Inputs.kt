package it.smartphonecombo.uecapabilityparser.extension

import io.javalin.http.UploadedFile
import it.smartphonecombo.uecapabilityparser.io.ByteArrayInputSource
import it.smartphonecombo.uecapabilityparser.io.FileInputSource
import it.smartphonecombo.uecapabilityparser.io.GzipFileInputSource
import it.smartphonecombo.uecapabilityparser.io.InputSource
import it.smartphonecombo.uecapabilityparser.io.StringInputSource
import it.smartphonecombo.uecapabilityparser.io.UploadedFileInputSource
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Read an unsigned byte from the stream. Throw [EOFException] if end of stream has been reached.
 */
internal fun InputStream.readUByte(): Int {
    val res = read()
    if (res == -1) throw EOFException()
    return res
}

/**
 * Read a low endian unsigned short from the stream. Throw [EOFException] if end of stream has been
 * reached.
 */
internal fun InputStream.readUShortLE(): Int {
    val bytes = readNBytes(2)
    if (bytes.size != 2) throw EOFException()
    val down = bytes[0].toUnsignedInt()
    val up = bytes[1].toUnsignedInt() shl 8
    return up or down
}

/**
 * Invokes [InputStream.skip] repeatedly with its parameter equal to the remaining number of bytes
 * to skip until the requested number of bytes has been skipped or an error condition occurs.
 *
 * Inspired by [InputStream.skipNBytes]
 */
internal fun InputStream.skipBytes(n: Long) {
    var bytesLeft = n
    while (bytesLeft > 0) {
        when (val skipped = skip(bytesLeft)) {
            0L -> {
                readUByte()
                // one byte read so decrement bytesLeft
                bytesLeft--
            }
            in 1..bytesLeft -> bytesLeft -= skipped
            else -> throw IOException("Unable to skip exactly")
        }
    }
}

fun ByteArray.toInputSource() = ByteArrayInputSource(this)

fun String.toInputSource() = StringInputSource(this)

fun File.toInputSource(gzip: Boolean = false) =
    if (gzip) GzipFileInputSource(this) else FileInputSource(this)

fun UploadedFile.toInputSource() = UploadedFileInputSource(this)

@Suppress("NOTHING_TO_INLINE") inline fun InputSource.isEmpty() = size() == 0L
