package it.smartphonecombo.uecapabilityparser.util

import kotlinx.serialization.json.Json

class MultiParsing(
    private val inputsList: List<List<ByteArray>>,
    private val typeList: List<String>,
    private val subTypesList: List<List<String>>,
    private val jsonFormat: Json = Json
) {
    val parsingList = parseCapabilities()

    private fun parseCapabilities(): List<Parsing> {
        val parsedCapabilities = mutableListOf<Parsing>()
        val subTypeIterator = subTypesList.iterator()

        for (i in inputsList.indices) {
            val inputs = inputsList[i]
            val type = typeList[i]
            var inputArray = ByteArray(0)
            var inputENDCArray: ByteArray? = null
            var inputNRArray: ByteArray? = null
            var defaultNr = false

            if (type != "H") {
                inputArray = inputs.fold(inputArray) { acc, it -> acc + it }
            } else {
                val subTypes = subTypeIterator.next()
                for (j in inputs.indices) {
                    val subType = subTypes[j]
                    val input = inputs[j]
                    when (subType) {
                        "LTE" -> inputArray = input
                        "ENDC" -> inputENDCArray = input
                        "NR" -> inputNRArray = input
                    }
                }

                if (inputNRArray?.isNotEmpty() == true && inputArray.isEmpty()) {
                    inputArray = inputNRArray
                    defaultNr = true
                }
            }

            val parsing =
                Parsing(inputArray, inputENDCArray, inputNRArray, defaultNr, type, jsonFormat)

            parsedCapabilities.add(parsing)
        }
        return parsedCapabilities
    }
}
