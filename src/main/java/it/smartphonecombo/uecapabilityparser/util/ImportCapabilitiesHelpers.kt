package it.smartphonecombo.uecapabilityparser.util

import com.ericsson.mts.asn1.KotlinJsonFormatWriter
import com.ericsson.mts.asn1.converter.AbstractConverter
import com.ericsson.mts.asn1.converter.ConverterNSG
import com.ericsson.mts.asn1.converter.ConverterOsix
import com.ericsson.mts.asn1.converter.ConverterQcat
import com.ericsson.mts.asn1.converter.ConverterWireshark
import it.smartphonecombo.uecapabilityparser.extension.indexOf
import it.smartphonecombo.uecapabilityparser.model.LogType
import it.smartphonecombo.uecapabilityparser.model.Rat
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object ImportCapabilitiesHelpers {
    fun convertUeCapabilityToJson(
        type: LogType,
        input: ByteArray,
        inputNR: ByteArray?,
        inputENDC: ByteArray?,
        defaultNR: Boolean
    ): JsonObject {
        val inputMainText = input.decodeToString()
        val inputNRText = inputNR?.decodeToString()
        val inputENDCText = inputENDC?.decodeToString()

        val ratContainerMap =
            if (type == LogType.H) {
                jsonFromHex(inputMainText, inputNRText, inputENDCText, defaultNR)
            } else {
                var combined = inputMainText
                // Combine all inputs
                inputENDCText?.let { combined += it }
                inputNRText?.let { combined += it }
                jsonFromText(combined, type)
            }
        return JsonObject(ratContainerMap)
    }

    private fun jsonFromText(input: String, type: LogType): Map<String, JsonElement> {

        lateinit var eutraIdentifier: Regex
        lateinit var nrIdentifier: Regex
        lateinit var mrdcIdentifier: Regex
        lateinit var converter: AbstractConverter

        when (type) {
            LogType.W -> {
                eutraIdentifier = "${Rat.EUTRA.ratCapabilityIdentifier}\\s".toRegex()
                nrIdentifier = "${Rat.NR.ratCapabilityIdentifier}\\s".toRegex()
                mrdcIdentifier = "${Rat.EUTRA_NR.ratCapabilityIdentifier}\\s".toRegex()
                converter = ConverterWireshark()
            }
            LogType.N -> {
                eutraIdentifier = "rat-Type : ${Rat.EUTRA}\\s".toRegex()
                nrIdentifier = "rat-Type : ${Rat.NR}\\s".toRegex()
                mrdcIdentifier = "rat-Type : ${Rat.EUTRA_NR}\\s".toRegex()
                converter = ConverterNSG()
            }
            LogType.O -> {
                eutraIdentifier = "${Rat.EUTRA.ratCapabilityIdentifier}\\s".toRegex()
                nrIdentifier = "${Rat.NR.ratCapabilityIdentifier}\\s".toRegex()
                mrdcIdentifier = "${Rat.EUTRA_NR.ratCapabilityIdentifier}\\s".toRegex()
                converter = ConverterOsix()
            }
            LogType.QC -> {
                eutraIdentifier = "value ${Rat.EUTRA.ratCapabilityIdentifier} ::=\\s".toRegex()
                nrIdentifier = "value ${Rat.NR.ratCapabilityIdentifier} ::=\\s".toRegex()
                mrdcIdentifier = "value ${Rat.EUTRA_NR.ratCapabilityIdentifier} ::=\\s".toRegex()
                converter = ConverterQcat()
            }
            else -> {
                throw IllegalArgumentException()
            }
        }
        val formatWriter = KotlinJsonFormatWriter()
        val ratContainerMap = mutableMapOf<String, JsonElement>()

        val list =
            listOf(
                    Rat.EUTRA to input.indexOf(eutraIdentifier),
                    Rat.EUTRA_NR to input.indexOf(mrdcIdentifier),
                    Rat.NR to input.indexOf(nrIdentifier),
                )
                .filter { it.second != -1 }
                .sortedBy { it.second }
        var eutra = ""
        var eutraNr = ""
        var nr = ""
        for (i in list.indices) {
            val (rat, start) = list[i]
            val end = list.getOrNull(i + 1)?.second ?: input.length
            when (rat) {
                Rat.EUTRA ->
                    eutra =
                        input.substring(
                            start + eutraIdentifier.toString().length - 1,
                            end,
                        )
                Rat.EUTRA_NR ->
                    eutraNr =
                        input.substring(
                            start + mrdcIdentifier.toString().length - 1,
                            end,
                        )
                Rat.NR -> nr = input.substring(start + nrIdentifier.toString().length - 1, end)
                else -> {
                    // Do nothing
                }
            }
        }
        if (eutra.isNotBlank()) {
            MtsAsn1Helpers.getAsn1Converter(Rat.EUTRA, converter)
                .convert(
                    Rat.EUTRA.ratCapabilityIdentifier,
                    eutra.byteInputStream(),
                    formatWriter,
                )
            formatWriter.jsonNode?.let { ratContainerMap.put(Rat.EUTRA.toString(), it) }
        }
        if (eutraNr.isNotBlank() || nr.isNotBlank()) {
            val nrConverter = MtsAsn1Helpers.getAsn1Converter(Rat.NR, converter)
            if (eutraNr.isNotBlank()) {
                nrConverter.convert(
                    Rat.EUTRA_NR.ratCapabilityIdentifier,
                    eutraNr.byteInputStream(),
                    formatWriter,
                )
                formatWriter.jsonNode?.let { ratContainerMap.put(Rat.EUTRA_NR.toString(), it) }
            }
            if (nr.isNotBlank()) {
                nrConverter.convert(
                    Rat.NR.ratCapabilityIdentifier,
                    nr.byteInputStream(),
                    formatWriter,
                )
                formatWriter.jsonNode?.let { ratContainerMap.put(Rat.NR.toString(), it) }
            }
        }
        return ratContainerMap
    }

    private fun jsonFromHex(
        inputMainText: String,
        inputNRText: String?,
        inputENDCText: String?,
        defaultNR: Boolean
    ): Map<String, JsonElement> {
        val ratContainerMap = mutableMapOf<String, JsonElement>()
        val defaultRat = if (defaultNR) Rat.NR else Rat.EUTRA
        ratContainerMap += MtsAsn1Helpers.getUeCapabilityJsonFromHex(defaultRat, inputMainText)
        if (inputNRText != null) {
            ratContainerMap += MtsAsn1Helpers.getUeCapabilityJsonFromHex(Rat.NR, inputNRText)
        }
        if (inputENDCText != null) {
            ratContainerMap +=
                MtsAsn1Helpers.getUeCapabilityJsonFromHex(Rat.EUTRA_NR, inputENDCText)
        }

        return ratContainerMap
    }
}
