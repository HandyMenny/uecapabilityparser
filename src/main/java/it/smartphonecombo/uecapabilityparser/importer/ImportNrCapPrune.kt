package it.smartphonecombo.uecapabilityparser.importer

import it.smartphonecombo.uecapabilityparser.bean.Capabilities
import it.smartphonecombo.uecapabilityparser.bean.IComponent
import it.smartphonecombo.uecapabilityparser.bean.lte.ComponentLte
import it.smartphonecombo.uecapabilityparser.bean.nr.ComboNr
import it.smartphonecombo.uecapabilityparser.bean.nr.ComponentNr
import it.smartphonecombo.uecapabilityparser.extension.component6
import it.smartphonecombo.uecapabilityparser.extension.component7
import java.io.InputStream
import java.io.InputStreamReader

/** A parser for NR Combinations as found in Qualcomm cap_prune */
object ImportNrCapPrune : ImportCapabilities {

    /**
     * This parser take as [input] an [InputStream] containing NR Combinations using the same format
     * used in Qualcomm cap_prune.
     *
     * In this format each combination is separated from the other by a ";".
     *
     * This combination consists of carrier components separated by "-".
     *
     * Each carrier component is composed of a character that is b for LTE combos and n for Combos,
     * followed by a number representing the band, followed by a character representing the downlink
     * bandwidth class, followed by an array representing the number of RX antennas (ex. [4,2]),
     * followed by a character representing the uplink bandwidth class, followed by an array
     * representing the number of TX antennas (ex. [2,2]),
     *
     * The number of RX antennas, the uplink bandwidth class and the number of TX antennas are
     * optional.
     *
     * The output is a [Capabilities] with the list of parsed NR CA combos stored in
     * [nrCombos][Capabilities.nrCombos] and the list of parsed EN DC combos stored in
     * [enDcCombos][Capabilities.enDcCombos].
     *
     * This parser doesn't support NR DC or SUL combos, if you have an example of cap_prune
     * containing such combos, please share it with info at smartphonecombo dot it
     */
    override fun parse(input: InputStream): Capabilities {
        val caBandCombosString = input.reader().use(InputStreamReader::readText)

        val listCombo =
            caBandCombosString
                .split(';')
                .filter(String::isNotBlank)
                .mapNotNull(::parseCombo)

        val cap = Capabilities()
        val (enDcCombos, nrCombos) = listCombo.partition { it.componentsLte.isNotEmpty() }
        cap.enDcCombos = enDcCombos
        cap.nrCombos = nrCombos
        return cap
    }

    /** Converts the given comboString to a [ComboNr] Returns null if parsing fails */
    private fun parseCombo(comboString: String): ComboNr? {
        val lteBands = mutableListOf<IComponent>()
        val nrBands = mutableListOf<IComponent>()
        val components = comboString.split('-')

        for (componentString in components) {
            when (val component = parseComponent(componentString)) {
                is ComponentLte -> lteBands.add(component)
                is ComponentNr -> nrBands.add(component)
            }
        }

        val lteBandsArray = lteBands.toTypedArray()
        lteBandsArray.sortWith(IComponent.defaultComparator.reversed())
        val nrBandsArray = nrBands.toTypedArray()
        nrBandsArray.sortWith(IComponent.defaultComparator.reversed())

        return if (lteBandsArray.isEmpty() && nrBandsArray.isEmpty()) {
            null
        } else if (lteBandsArray.isEmpty()) {
            ComboNr(nrBandsArray)
        } else {
            ComboNr(lteBandsArray, nrBandsArray)
        }
    }

    // Regex used to extract the various parts of a component
    private val componentsRegex =
        """([bn])(\d{1,3})([A-Q]?)\[?([\d,]{0,8})]?([A-Q]?)\[?([\d,]{0,8})]?""".toRegex()
    /** Converts the given componentString to a [IComponent]. Returns null if parsing fails */
    private fun parseComponent(componentString: String): IComponent? {
        val result = componentsRegex.find(componentString) ?: return null

        val (_, type, baseBand, classDL, mimoDL, classUL, mimoUL) = result.groupValues

        if (type == "b") {
            return ComponentLte(
                baseBand.toInt(),
                bwClassParsing(classDL),
                bwClassParsing(classUL),
                mimoParsing(mimoDL),
                null,
                null
            )
        } else {
            return ComponentNr(
                baseBand.toInt(),
                bwClassParsing(classDL),
                bwClassParsing(classUL),
                mimoParsing(mimoDL),
                mimoParsing(mimoUL),
                null,
                null
            )
        }
    }

    /** Extract mimo from the given string. Only the first mimo found is returned. */
    private fun mimoParsing(mimo: String): Int {
        return if (mimo.isEmpty()) {
            0
        } else {
            mimo.split(",").first().toInt()
        }
    }

    /** Returns the first character of [bwClass] or '0' if empty. */
    private fun bwClassParsing(bwClass: String): Char {
        return if (bwClass.isEmpty()) {
            '0'
        } else {
            bwClass[0]
        }
    }
}