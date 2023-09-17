package it.smartphonecombo.uecapabilityparser.util

import it.smartphonecombo.uecapabilityparser.extension.custom
import it.smartphonecombo.uecapabilityparser.extension.getString
import it.smartphonecombo.uecapabilityparser.importer.Import0xB0CD
import it.smartphonecombo.uecapabilityparser.importer.Import0xB0CDBin
import it.smartphonecombo.uecapabilityparser.importer.Import0xB826
import it.smartphonecombo.uecapabilityparser.importer.ImportCapabilityInformation
import it.smartphonecombo.uecapabilityparser.importer.ImportLteCarrierPolicy
import it.smartphonecombo.uecapabilityparser.importer.ImportMTKLte
import it.smartphonecombo.uecapabilityparser.importer.ImportNrCapPrune
import it.smartphonecombo.uecapabilityparser.importer.ImportNvItem
import it.smartphonecombo.uecapabilityparser.importer.ImportQctModemCap
import it.smartphonecombo.uecapabilityparser.model.Capabilities
import it.smartphonecombo.uecapabilityparser.model.Rat
import it.smartphonecombo.uecapabilityparser.model.index.IndexLine
import it.smartphonecombo.uecapabilityparser.model.index.LibraryIndex
import it.smartphonecombo.uecapabilityparser.util.ImportCapabilitiesHelpers.convertUeCapabilityToJson
import it.smartphonecombo.uecapabilityparser.util.ImportQcHelpers.parseMultiple0xB826
import it.smartphonecombo.uecapabilityparser.util.ImportQcHelpers.parseMultiple0xBOCD
import java.time.Instant
import java.util.Base64
import kotlin.system.measureTimeMillis
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class Parsing(
    private val input: ByteArray,
    private val inputNR: ByteArray?,
    private val inputENDC: ByteArray?,
    private val defaultNR: Boolean = false,
    private val type: String,
    private val jsonFormat: Json = Json
) {
    private var jsonUeCap: JsonObject? = null
    val capabilities = parseCapabilitiesAndSetMetadata()

    val ueLog: String
        get() = jsonUeCap?.let { jsonFormat.encodeToString(jsonUeCap) } ?: input.decodeToString()

    private fun parseCapabilitiesAndSetMetadata(): Capabilities {
        val capabilities: Capabilities
        val processTime = measureTimeMillis { capabilities = parseCapabilities() }
        capabilities.logType = type
        capabilities.timestamp = Instant.now().toEpochMilli()
        capabilities.setMetadata("processingTime", "${processTime}ms")
        if (defaultNR) capabilities.setMetadata("defaultNR", "true")
        return capabilities
    }

    private fun parseCapabilities(): Capabilities {
        val imports =
            when (type) {
                "E" -> ImportNvItem
                "C" -> ImportLteCarrierPolicy
                "CNR" -> ImportNrCapPrune
                "Q" -> Import0xB0CD
                "QLTE" -> Import0xB0CDBin
                "M" -> ImportMTKLte
                "QNR" -> Import0xB826
                "RF" -> ImportQctModemCap
                "W",
                "N",
                "O",
                "QC",
                "H" -> ImportCapabilityInformation
                else -> return Capabilities()
            }

        if (imports == Import0xB826) {
            return parseMultiple0xB826(input.decodeToString())
        }

        if (imports == Import0xB0CDBin) {
            return parseMultiple0xBOCD(input.decodeToString())
        }

        if (imports == ImportCapabilityInformation) {
            jsonUeCap = convertUeCapabilityToJson(type, input, inputNR, inputENDC, defaultNR)
            val eutra = jsonUeCap?.get(Rat.EUTRA.toString()) as? JsonObject
            val eutraNr = jsonUeCap?.get(Rat.EUTRA_NR.toString()) as? JsonObject
            val nr = jsonUeCap?.get(Rat.NR.toString()) as? JsonObject

            return (imports as ImportCapabilityInformation).parse(eutra, eutraNr, nr)
        }

        return imports.parse(input)
    }

    fun store(libraryIndex: LibraryIndex, path: String, compression: Boolean): Boolean {
        val inputDir = "$path/input"
        val outputDir = "$path/output"
        val id = capabilities.id
        val inputs = arrayOf(input, inputNR, inputENDC)
        val inputsPath = mutableListOf<String>()

        inputs.filterNotNull().filterNot(ByteArray::isEmpty).forEachIndexed { index, data ->
            val fileName = "$id-$index"
            val inputPath = "$inputDir/$fileName"
            IO.outputFile(data, inputPath, compression)
            inputsPath.add(fileName)
        }

        val encodedString = Json.custom().encodeToString(capabilities)
        val outputPath = "$outputDir/$id.json"
        IO.outputFile(encodedString.toByteArray(), outputPath, compression)
        val indexLine =
            IndexLine(
                id,
                capabilities.timestamp,
                capabilities.getStringMetadata("description") ?: "",
                inputsPath,
                compression,
                capabilities.getStringMetadata("defaultNR").toBoolean(),
                capabilities.parserVersion
            )
        libraryIndex.addLine(indexLine)
        return true
    }

    companion object {
        fun fromJsonRequest(request: JsonElement): Parsing? {
            val base64decoder = Base64.getDecoder()
            val input =
                request.getString("input").takeUnless(String?::isNullOrEmpty)?.let {
                    base64decoder.decode(it)
                }
            val inputNR =
                request.getString("inputNR").takeUnless(String?::isNullOrEmpty)?.let {
                    base64decoder.decode(it)
                }
            val inputENDC =
                request.getString("inputENDC").takeUnless(String?::isNullOrEmpty)?.let {
                    base64decoder.decode(it)
                }
            val defaultNR =
                request.getString("defaultNR")?.let { it.toBoolean() } ?: (input == null)
            val type = request.getString("type")

            if (input == null && inputNR == null || type == null) {
                return null
            }

            val parsing =
                Parsing(
                    input ?: inputNR!!,
                    if (defaultNR) null else inputNR,
                    inputENDC,
                    defaultNR,
                    type
                )

            // Set description
            val description = request.getString("description")
            if (!description.isNullOrEmpty()) {
                parsing.capabilities.setMetadata("description", description)
            }

            return parsing
        }
    }
}
