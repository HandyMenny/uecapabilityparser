package it.smartphonecombo.uecapabilityparser.importer

import it.smartphonecombo.uecapabilityparser.extension.Band
import it.smartphonecombo.uecapabilityparser.extension.BwMap
import it.smartphonecombo.uecapabilityparser.extension.asArrayOrNull
import it.smartphonecombo.uecapabilityparser.extension.asIntOrNull
import it.smartphonecombo.uecapabilityparser.extension.getArray
import it.smartphonecombo.uecapabilityparser.extension.getArrayAtPath
import it.smartphonecombo.uecapabilityparser.extension.getInt
import it.smartphonecombo.uecapabilityparser.extension.getObject
import it.smartphonecombo.uecapabilityparser.extension.getObjectAtPath
import it.smartphonecombo.uecapabilityparser.extension.getString
import it.smartphonecombo.uecapabilityparser.extension.isSUL
import it.smartphonecombo.uecapabilityparser.extension.merge
import it.smartphonecombo.uecapabilityparser.extension.mutableListWithCapacity
import it.smartphonecombo.uecapabilityparser.extension.typedList
import it.smartphonecombo.uecapabilityparser.io.IOUtils.echoSafe
import it.smartphonecombo.uecapabilityparser.io.InputSource
import it.smartphonecombo.uecapabilityparser.model.BCS
import it.smartphonecombo.uecapabilityparser.model.BwClass
import it.smartphonecombo.uecapabilityparser.model.Capabilities
import it.smartphonecombo.uecapabilityparser.model.Duplex
import it.smartphonecombo.uecapabilityparser.model.EmptyMimo
import it.smartphonecombo.uecapabilityparser.model.LinkDirection
import it.smartphonecombo.uecapabilityparser.model.Mimo
import it.smartphonecombo.uecapabilityparser.model.PowerClass
import it.smartphonecombo.uecapabilityparser.model.Rat
import it.smartphonecombo.uecapabilityparser.model.SingleBCS
import it.smartphonecombo.uecapabilityparser.model.UplinkTxSwitchConfig
import it.smartphonecombo.uecapabilityparser.model.UplinkTxSwitchOption
import it.smartphonecombo.uecapabilityparser.model.UplinkTxSwitchType
import it.smartphonecombo.uecapabilityparser.model.band.BandBoxed
import it.smartphonecombo.uecapabilityparser.model.band.BandLteDetails
import it.smartphonecombo.uecapabilityparser.model.band.BandNrDetails
import it.smartphonecombo.uecapabilityparser.model.band.DuplexBandTable
import it.smartphonecombo.uecapabilityparser.model.band.IBandDetails
import it.smartphonecombo.uecapabilityparser.model.bandwidth.BwTableNr
import it.smartphonecombo.uecapabilityparser.model.bandwidth.BwsBitMap
import it.smartphonecombo.uecapabilityparser.model.bandwidth.BwsNr
import it.smartphonecombo.uecapabilityparser.model.combo.ComboEnDc
import it.smartphonecombo.uecapabilityparser.model.combo.ComboLte
import it.smartphonecombo.uecapabilityparser.model.combo.ComboNr
import it.smartphonecombo.uecapabilityparser.model.combo.ComboNrDc
import it.smartphonecombo.uecapabilityparser.model.combo.ICombo
import it.smartphonecombo.uecapabilityparser.model.component.ComponentLte
import it.smartphonecombo.uecapabilityparser.model.component.ComponentNr
import it.smartphonecombo.uecapabilityparser.model.component.IComponent
import it.smartphonecombo.uecapabilityparser.model.feature.FeatureIndex
import it.smartphonecombo.uecapabilityparser.model.feature.FeaturePerCCLte
import it.smartphonecombo.uecapabilityparser.model.feature.FeaturePerCCNr
import it.smartphonecombo.uecapabilityparser.model.feature.FeatureSet
import it.smartphonecombo.uecapabilityparser.model.feature.FeatureSets
import it.smartphonecombo.uecapabilityparser.model.filter.BandFilterLte
import it.smartphonecombo.uecapabilityparser.model.filter.BandFilterNr
import it.smartphonecombo.uecapabilityparser.model.filter.IUeCapabilityFilter
import it.smartphonecombo.uecapabilityparser.model.filter.UeCapabilityFilterLte
import it.smartphonecombo.uecapabilityparser.model.filter.UeCapabilityFilterNr
import it.smartphonecombo.uecapabilityparser.model.fromLiteral
import it.smartphonecombo.uecapabilityparser.model.json.UEEutraCapabilityJson
import it.smartphonecombo.uecapabilityparser.model.json.UEMrdcCapabilityJson
import it.smartphonecombo.uecapabilityparser.model.json.UENrCapabilityJson
import it.smartphonecombo.uecapabilityparser.model.json.UENrRrcCapabilityJson
import it.smartphonecombo.uecapabilityparser.model.modulation.EmptyModulation
import it.smartphonecombo.uecapabilityparser.model.modulation.Modulation
import it.smartphonecombo.uecapabilityparser.model.modulation.ModulationOrder
import it.smartphonecombo.uecapabilityparser.model.modulation.toModulation
import it.smartphonecombo.uecapabilityparser.model.ratcapabilities.IRatCapabilities
import it.smartphonecombo.uecapabilityparser.model.ratcapabilities.RatCapabilitiesLte
import it.smartphonecombo.uecapabilityparser.model.ratcapabilities.RatCapabilitiesNr
import it.smartphonecombo.uecapabilityparser.model.ratcapabilities.UeType
import it.smartphonecombo.uecapabilityparser.model.toMimo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object ImportCapabilityInformation : ImportCapabilities {
    override fun parse(input: InputSource): Capabilities {
        val caBandCombosString = input.readText()
        val caBandCombosJson =
            try {
                Json.parseToJsonElement(caBandCombosString) as? JsonObject
            } catch (_: SerializationException) {
                null
            }

        val eutraCapability = caBandCombosJson?.get(Rat.EUTRA.toString()) as? JsonObject
        val eutraNrCapability = caBandCombosJson?.get(Rat.EUTRA_NR.toString()) as? JsonObject
        val nrCapability = caBandCombosJson?.get(Rat.NR.toString()) as? JsonObject

        return parse(eutraCapability, eutraNrCapability, nrCapability)
    }

    fun parse(
        eutraCapability: JsonObject?,
        eutraNrCapability: JsonObject?,
        nrCapability: JsonObject?,
    ): Capabilities {
        val comboList = Capabilities()
        var lteFeatures: FeatureSets? = null
        var nrFeatures: FeatureSets? = null
        var nrBandsMap: Map<Band, BandNrDetails>? = null
        var lteBandsMap: Map<Band, BandLteDetails>? = null
        val filterList = mutableListWithCapacity<IUeCapabilityFilter>(3)
        val ratCapabilitiesList = mutableListWithCapacity<IRatCapabilities>(3)

        if (eutraCapability != null) {
            val eutra = UEEutraCapabilityJson(eutraCapability)
            val (lteCategoryDL, lteCategoryUL) = getLTECategory(eutra)
            comboList.lteCategoryDL = lteCategoryDL
            comboList.lteCategoryUL = lteCategoryUL

            lteBandsMap = getLteBands(eutra).associateBy({ it.band }, { it })
            comboList.nrNSAbands = getNrBands(eutra, true).sorted()
            comboList.nrSAbands = getNrBands(eutra, false).sorted()

            if (debug) {
                echoSafe(
                    "NR NSA bands (from eutra capability): [${comboList.nrNSAbands.joinToString(transform = {it.band.toString()})}]"
                )
                echoSafe(
                    "NR SA bands (from eutra capability): [${comboList.nrSAbands.joinToString(transform = {it.band.toString()})}]"
                )
            }

            val listCombo = getBandCombinations(eutra, lteBandsMap)
            val listComboAdd = getBandCombinationsAdd(eutra, lteBandsMap)
            val listComboReduced = getBandCombinationsReduced(eutra, lteBandsMap)
            val totalLteCombos = listCombo + listComboAdd + listComboReduced
            comboList.lteCombos = totalLteCombos

            if (eutraNrCapability != null) {
                // Don't parse lte features if no mrdc capability is available
                lteFeatures = getLteFeatureSet(eutra)
            }
            filterList.add(getUeLteCapabilityFilters(eutra))
            comboList.altTbsIndexes = parseAltTbsIndexes(eutra)
            val release = parseAccessRelease(eutraCapability)
            val segSupported = parseSegSupportedEutra(eutra, release)
            ratCapabilitiesList.add(RatCapabilitiesLte(release, segSupported))
        }

        if (nrCapability != null) {
            val nr = UENrCapabilityJson(nrCapability)
            val ueType = getUeType(nr)
            nrBandsMap = getNrBands(nr, ueType).associateBy({ it.band }, { it })
            nrFeatures = getNRFeatureSet(nr)
            val featureSetCombination = getFeatureSetCombinations(nr)
            val nrCaCombos = getNrBandCombinations(nr).typedList<ComboNr>()
            val saUlTxSwitchCombos = getUlTxSwitchBandCombinations(nr)
            val saCombos = nrCaCombos + saUlTxSwitchCombos
            comboList.nrCombos =
                linkFeaturesAndCarrier(
                        saCombos,
                        featureSetCombination,
                        null,
                        nrFeatures,
                        null,
                        nrBandsMap,
                    )
                    .typedList<ComboNr>()
                    .map { combo ->
                        // R17_2T2T / R17_1T2T require 2 UL layers on every switching band.
                        // After feature linking the actual MIMO is known, so drop 2T2T/1T2T
                        // configs from any combo that has a switching band with < 2 UL layers.
                        if (combo.uplinkTxSwitch.none {
                                it.type == UplinkTxSwitchType.R17_2T2T ||
                                    it.type == UplinkTxSwitchType.R17_1T2T
                            }
                        ) {
                            combo
                        } else {
                            val all2T =
                                combo.componentsNr.count { c -> c.ulTxSwitch && c.mimoUL.max() >= 2 } >= 2
                            if (all2T) combo
                            else {
                                val filtered =
                                    combo.uplinkTxSwitch.filter {
                                        it.type != UplinkTxSwitchType.R17_2T2T &&
                                            it.type != UplinkTxSwitchType.R17_1T2T
                                    }
                                combo.copy(uplinkTxSwitch = filtered).copy(combo.featureSet)
                            }
                        }
                    }
                    .typedList()

            val nrDcCombos = getNrDcBandCombinations(nr, nrCaCombos)
            val nrDcComboWithFeatures =
                linkFeaturesAndCarrier(
                        nrDcCombos,
                        featureSetCombination,
                        null,
                        nrFeatures,
                        null,
                        nrBandsMap,
                    )
                    .typedList<ComboNr>()
            comboList.nrDcCombos =
                nrDcComboWithFeatures.map { combo ->
                    val (fr2, fr1) = combo.masterComponents.partition { it.isFR2 }
                    ComboNrDc(fr1, fr2, combo.featureSet, combo.bcs)
                }

            filterList.add(getUeNrCapabilityFilters(nr))
            val release = parseAccessRelease(nrCapability)
            val segSupported = parseSegSupportedNr(nr, release)
            ratCapabilitiesList.add(RatCapabilitiesNr(release, segSupported, ueType))
        }

        if (eutraNrCapability != null) {
            val mrdc = UEMrdcCapabilityJson(eutraNrCapability)
            val featureSetCombination = getFeatureSetCombinations(mrdc)
            val nsaCombos = getNrBandCombinations(mrdc)
            comboList.enDcCombos =
                linkFeaturesAndCarrier(
                        nsaCombos,
                        featureSetCombination,
                        lteFeatures,
                        nrFeatures,
                        lteBandsMap,
                        nrBandsMap,
                    )
                    .typedList()
            filterList.add(getUeNrCapabilityFilters(mrdc))
        }

        if (lteBandsMap != null) {
            updateLteBandsCapabilities(lteBandsMap, comboList.lteCombos)
            comboList.lteBands = lteBandsMap.values.sorted()
        }

        if (nrBandsMap != null) {
            updateNrBandsCapabilities(
                nrBandsMap,
                comboList.enDcCombos,
                comboList.nrCombos,
                comboList.nrDcCombos,
            )
            comboList.nrBands = nrBandsMap.values.sorted()
            if (debug) {
                comboList.nrBands.forEach { echoSafe(it.bwsToString()) }
            }
        }

        comboList.ueCapFilters = filterList
        comboList.ratCapabilities = ratCapabilitiesList

        return comboList
    }

    private fun getBandCombinations(
        eutraCapability: UEEutraCapabilityJson,
        bandList: Map<Band, BandLteDetails>,
    ): List<ComboLte> {
        val combinations =
            eutraCapability.eutraCapabilityV1020
                ?.getArrayAtPath("rf-Parameters-v1020.supportedBandCombination-r10")
                ?.mapNotNull { bandCombination ->
                    if (bandCombination is JsonArray) {
                        bandCombination.map { parseBandParameters(it, 10) }
                    } else {
                        null
                    }
                } ?: return emptyList()

        // Bands ext
        eutraCapability.eutraCapabilityV1090
            ?.getArrayAtPath("rf-Parameters-v1090.supportedBandCombination-v1090")
            ?.forEachIndexed { i, bandParameters ->
                if (bandParameters is JsonArray) {
                    bandParameters.forEachIndexed { j, bandParameter ->
                        bandParameter.getInt("bandEUTRA-v1090")?.let {
                            combinations[i][j].band = it
                        }
                    }
                }
            }

        // Some devices don't report 4layers in supportedMIMO-CapabilityDL-r10
        // Use CA-MIMO-ParametersDL-v10i0 if we haven't yet found any bands with 4rx or 8rx
        if (!combinations.hasHighMimo()) {
            eutraCapability.eutraCapabilityV10i0
                ?.getArrayAtPath("rf-Parameters-v10i0.supportedBandCombination-v10i0")
                ?.let { parseCaMimoV10i0(it, combinations) }
        }

        // parse mimo-perCC ("mixed mimo")
        eutraCapability.eutraCapabilityV1270
            ?.getArrayAtPath("rf-Parameters-v1270.supportedBandCombination-v1270")
            ?.let { parseCaMimoV1270(it, combinations) }

        // Basic Modulation - set 256/64qam DL or 64/16qam UL from bandList
        setModulationFromBandList(combinations, bandList)

        // Advanced Modulation - set 1024qam DL or 256qam UL per combo
        eutraCapability.eutraCapabilityV1430
            ?.getArrayAtPath("rf-Parameters-v1430.supportedBandCombination-v1430")
            ?.let { set256qamUL(it, combinations) }
        eutraCapability.eutraCapabilityV1530
            ?.getArrayAtPath("rf-Parameters-v1530.supportedBandCombination-v1530")
            ?.let { set1024qam(it, combinations) }

        val supportedBandCombinationExtR10 =
            eutraCapability.eutraCapabilityV1060?.getArrayAtPath(
                "rf-Parameters-v1060.supportedBandCombinationExt-r10"
            ) ?: emptyList()
        val bcsList =
            supportedBandCombinationExtR10.map { combinationParameters ->
                combinationParameters.getString("supportedBandwidthCombinationSet-r10")?.let {
                    BCS.fromBinaryString(it)
                } ?: SingleBCS(0)
            }

        return combinations.mergeBcs(bcsList)
    }

    private fun getBandCombinationsAdd(
        eutraCapability: UEEutraCapabilityJson,
        bandList: Map<Band, BandLteDetails>,
    ): List<ComboLte> {
        val combinationsArray =
            eutraCapability.eutraCapabilityV1180?.getArrayAtPath(
                "rf-Parameters-v1180.supportedBandCombinationAdd-r11"
            ) ?: return emptyList()
        val bcsList = mutableListWithCapacity<BCS>(combinationsArray.size)

        val combinations =
            combinationsArray.mapNotNull { bandCombination ->
                val bcs =
                    bandCombination.getString("supportedBandwidthCombinationSet-r11")
                        ?: "1" // 1 -> only bcs 0
                bcsList.add(BCS.fromBinaryString(bcs))
                val bandParametersList = bandCombination.getArrayAtPath("bandParameterList-r11")

                bandParametersList?.map { parseBandParameters(it, 11) }
            }

        // Some devices don't report 4layers in supportedMIMO-CapabilityDL-r10
        // Use CA-MIMO-ParametersDL-v10i0 if we haven't yet found any bands with 4rx or 8rx
        if (!combinations.hasHighMimo()) {
            eutraCapability.eutraCapabilityV11d0
                ?.getArrayAtPath("rf-Parameters-v11d0.supportedBandCombinationAdd-v11d0")
                ?.let { parseCaMimoV10i0(it, combinations) }
        }

        // parse mimo-perCC ("mixed mimo")
        eutraCapability.eutraCapabilityV1270
            ?.getArrayAtPath("rf-Parameters-v1270.supportedBandCombinationAdd-v1270")
            ?.let { parseCaMimoV1270(it, combinations) }

        // Basic Modulation - set 256qam or 64qam from bandList
        setModulationFromBandList(combinations, bandList)

        // Advanced Modulation - set 1024qam DL or 256qam UL per combo
        eutraCapability.eutraCapabilityV1430
            ?.getArrayAtPath("rf-Parameters-v1430.supportedBandCombinationAdd-v1430")
            ?.let { set256qamUL(it, combinations) }
        eutraCapability.eutraCapabilityV1530
            ?.getArrayAtPath("rf-Parameters-v1530.supportedBandCombinationAdd-v1530")
            ?.let { set1024qam(it, combinations) }

        return combinations.mergeBcs(bcsList)
    }

    private fun getBandCombinationsReduced(
        eutraCapability: UEEutraCapabilityJson,
        bandList: Map<Band, BandLteDetails>,
    ): List<ComboLte> {
        val combinationsArray =
            eutraCapability.eutraCapabilityV1310?.getArrayAtPath(
                "rf-Parameters-v1310.supportedBandCombinationReduced-r13"
            ) ?: return emptyList()
        val bcsList = mutableListWithCapacity<BCS>(combinationsArray.size)
        val combinations =
            combinationsArray.mapNotNull { bandCombination ->
                val bcs =
                    bandCombination.getString("supportedBandwidthCombinationSet-r13")
                        ?: "1" // 1 -> only bcs 0
                bcsList.add(BCS.fromBinaryString(bcs))

                val bandParametersList = bandCombination.getArray("bandParameterList-r13")
                bandParametersList?.map { parseBandParameters(it, 13) }
            }

        // Basic Modulation - set 256qam or 64qam from bandList
        setModulationFromBandList(combinations, bandList)

        // Advanced Modulation - set 1024qam DL or 256qam UL per combo
        eutraCapability.eutraCapabilityV1430
            ?.getArrayAtPath("rf-Parameters-v1430.supportedBandCombinationReduced-v1430")
            ?.let { set256qamUL(it, combinations) }
        eutraCapability.eutraCapabilityV1530
            ?.getArrayAtPath("rf-Parameters-v1530.supportedBandCombinationReduced-v1530")
            ?.let { set1024qam(it, combinations) }

        return combinations.mergeBcs(bcsList)
    }

    private fun setModulationFromBandList(
        combinations: List<List<ComponentLte>>,
        bandList: Map<Band, BandLteDetails>,
    ) {
        combinations.flatten().forEach {
            it.modDL = bandList[it.band]?.modDL ?: EmptyModulation
            if (it.classUL != BwClass.NONE) {
                it.modUL = bandList[it.band]?.modUL ?: EmptyModulation
            }
        }
    }

    private fun set256qamUL(
        supportedBandCombinationV1430: JsonArray,
        combinations: List<List<ComponentLte>>,
    ) {
        supportedBandCombinationV1430.forEachIndexed { i, bandParameterList ->
            bandParameterList.getArray("bandParameterList-v1430")?.forEachIndexed { j, bandParameter
                ->
                if (bandParameter.getString("ul-256QAM-r14") != null) {
                    combinations[i][j].modUL = ModulationOrder.QAM256.toModulation()
                } else {
                    val perCCInfoList = bandParameter.getArray("ul-256QAM-perCC-InfoList-r14")
                    val defaultMod = combinations[i][j].modUL.maxModulationOrder()
                    val mixedModulation =
                        perCCInfoList?.map {
                            if (it.getString("ul-256QAM-perCC-r14") != null) {
                                ModulationOrder.QAM256
                            } else {
                                defaultMod
                            }
                        }

                    if (!mixedModulation.isNullOrEmpty()) {
                        combinations[i][j].modUL = Modulation.from(mixedModulation)
                    }
                }
            }
        }
    }

    private fun set1024qam(
        supportedBandCombinationV1530: JsonArray,
        combinations: List<List<ComponentLte>>,
    ) {
        supportedBandCombinationV1530.forEachIndexed { i, bandParameterList ->
            bandParameterList.getArray("bandParameterList-v1530")?.forEachIndexed { j, bandParameter
                ->
                if (bandParameter.getString("dl-1024QAM-r15") != null) {
                    combinations[i][j].modDL = ModulationOrder.QAM1024.toModulation()
                }
            }
        }
    }

    private fun parseCaMimoV10i0(
        supportedBandCombinationV10i0: List<JsonElement>,
        combinations: List<List<ComponentLte>>,
    ) {
        supportedBandCombinationV10i0.forEachIndexed { i, bandParameterList ->
            bandParameterList.getArray("bandParameterList-v10i0")?.forEachIndexed { j, it ->
                it.getArray("bandParametersDL-v10i0")
                    ?.get(0)
                    ?.getString("fourLayerTM3-TM4-r10")
                    ?.let { combinations[i][j].mimoDL = Mimo.from(4) }
            }
        }
    }

    private fun parseCaMimoV1270(
        supportedBandCombinationV1270: List<JsonElement>,
        combinations: List<List<ComponentLte>>,
    ) {
        for (i in supportedBandCombinationV1270.indices) {
            val bandParameterList =
                supportedBandCombinationV1270[i].getArray("bandParameterList-v1270") ?: continue

            for (j in bandParameterList.indices) {
                bandParameterList[j]
                    .getArray("bandParametersDL-v1270")
                    ?.get(0)
                    ?.getArray("intraBandContiguousCC-InfoList-r12")
                    ?.let {
                        combinations[i][j].mimoDL = parseMimoR12(it, combinations[i][j].mimoDL)
                    }
            }
        }
    }

    private fun updateLteBandsCapabilities(
        bandList: Map<Band, BandLteDetails>,
        listCombo: List<ComboLte>,
    ) {
        val lteComponents = listCombo.flatMap { it.masterComponents.toList() }.toHashSet()
        lteComponents.forEach {
            val band = bandList[it.band]
            if (band != null) {
                if (it.mimoDL > band.mimoDL) {
                    band.mimoDL = it.mimoDL
                }
                if (it.mimoUL > band.mimoUL) {
                    band.mimoUL = it.mimoUL
                }
                if (it.modDL > band.modDL) {
                    band.modDL = it.modDL
                }
                if (it.modUL > band.modUL) {
                    band.modUL = it.modUL
                }
            }
        }
    }

    private fun updateNrBandsCapabilities(
        bandList: Map<Int, BandNrDetails>,
        enDcCombos: List<ComboEnDc>,
        nrCombos: List<ComboNr>,
        nrDcCombos: List<ComboNrDc>,
    ) {
        val nrComponents =
            enDcCombos.flatMap { it.componentsNr } +
                nrCombos.flatMap { it.componentsNr } +
                nrDcCombos.flatMap { it.componentsNr + it.componentsNrDc }

        for (component in nrComponents.toHashSet()) {
            val band = bandList[component.band] ?: continue
            if (component.channelBW90mhz && !band.bw90MHzSupported()) {
                val newBandwidths =
                    band.bandwidths.map { bwsNr ->
                        val (scs, bwsDL, bwsUL) = bwsNr
                        if (scs == 30 || scs == 60) {
                            val newBwsDL = bwsDL.takeIf(IntArray::isNotEmpty)?.plus(90)
                            val newBwsUL = bwsUL.takeIf(IntArray::isNotEmpty)?.plus(90)
                            newBwsDL?.sortDescending()
                            newBwsUL?.sortDescending()

                            if (newBwsDL != null || newBwsUL != null) {
                                return@map BwsNr(scs, newBwsDL ?: bwsDL, newBwsUL ?: bwsUL)
                            }
                        }
                        return@map bwsNr
                    }

                band.bandwidths = newBandwidths
            }
            if (component.mimoDL > band.mimoDL) {
                band.mimoDL = component.mimoDL
            }
            if (component.mimoUL > band.mimoUL) {
                band.mimoUL = component.mimoUL
            }
        }
    }

    private fun getLTECategory(eutraCapability: UEEutraCapabilityJson): Pair<Int, Int> {
        var dlCategory = 0
        var ulCategory = 0

        // ue-Category
        eutraCapability.rootJson.getInt("ue-Category")?.let {
            dlCategory = it
            ulCategory = it
        }

        // ue-Category-v1020
        eutraCapability.eutraCapabilityV1020?.getInt("ue-Category-v1020")?.let {
            dlCategory = it
            ulCategory = it
        }

        // ue-Category-v1170
        eutraCapability.eutraCapabilityV1170?.getInt("ue-Category-v1170")?.let {
            dlCategory = it
            ulCategory = it
        }

        // ue-Category-v11a0
        eutraCapability.eutraCapabilityV11a0?.getInt("ue-Category-v11a0")?.let {
            dlCategory = it
            ulCategory = it
        }

        // ue-Category-v1250
        eutraCapability.eutraCapabilityV1250?.getInt("ue-CategoryDL-r12")?.let { dlCategory = it }
        eutraCapability.eutraCapabilityV1250?.getInt("ue-CategoryUL-r12")?.let { ulCategory = it }

        // ue-Category-v1260
        eutraCapability.eutraCapabilityV1260?.getInt("ue-CategoryDL-v1260")?.let { dlCategory = it }

        // ue-Category-v1310
        eutraCapability.eutraCapabilityV1310?.getString("ue-CategoryDL-v1310")?.let {
            // n17, m1
            dlCategory = it.drop(1).toInt()
        }
        eutraCapability.eutraCapabilityV1310?.getString("ue-CategoryUL-v1310")?.let {
            // n14, m1
            ulCategory = it.drop(1).toInt()
        }

        // ue-Category-v1330
        eutraCapability.eutraCapabilityV1330?.getInt("ue-CategoryDL-v1330")?.let { dlCategory = it }

        // ue-Category-v1340
        eutraCapability.eutraCapabilityV1340?.getInt("ue-CategoryUL-v1340")?.let { ulCategory = it }

        // ue-Category-v1350
        eutraCapability.eutraCapabilityV1350?.getInt("ue-CategoryDL-v1350")?.let { dlCategory = it }
        eutraCapability.eutraCapabilityV1350?.getInt("ue-CategoryUL-v1350")?.let { ulCategory = it }

        // ue-Category-v1430
        eutraCapability.eutraCapabilityV1430?.getString("ue-CategoryDL-v1430")?.let {
            // m2
            dlCategory = it.drop(1).toInt()
        }
        eutraCapability.eutraCapabilityV1430?.getString("ue-CategoryUL-v1430")?.let {
            // n16, n17, n18, n19, n20, m2
            ulCategory = it.drop(1).toInt()
        }
        eutraCapability.eutraCapabilityV1430?.getString("ue-CategoryUL-v1430b")?.let {
            // n21
            ulCategory = it.drop(1).toInt()
        }

        // ue-Category-v1450
        eutraCapability.eutraCapabilityV1450?.getInt("ue-CategoryDL-v1450")?.let { dlCategory = it }

        // ue-Category-v1460
        eutraCapability.eutraCapabilityV1460?.getInt("ue-CategoryDL-v1460")?.let { dlCategory = it }

        // ue-Category-v1530
        eutraCapability.eutraCapabilityV1530?.getInt("ue-CategoryDL-v1530")?.let { dlCategory = it }
        eutraCapability.eutraCapabilityV1530?.getInt("ue-CategoryUL-v1530")?.let { ulCategory = it }

        return Pair(dlCategory, ulCategory)
    }

    private fun getLteBands(eutraCapability: UEEutraCapabilityJson): List<BandLteDetails> {
        val supportedBandListEutra =
            eutraCapability.rootJson.getArrayAtPath("rf-Parameters.supportedBandListEUTRA")

        val lteBands =
            supportedBandListEutra?.mapNotNull {
                it.getInt("bandEUTRA")?.let { band ->
                    val duplex = DuplexBandTable.getLteDuplex(band)
                    val defaultModUL =
                        if (duplex == Duplex.SDL) ModulationOrder.NONE else ModulationOrder.QAM16
                    val mimoUl = if (duplex == Duplex.SDL) 0 else 1
                    val defaultPowerClass =
                        if (duplex == Duplex.SDL) PowerClass.NONE else PowerClass.PC3

                    BandLteDetails(
                        band,
                        mimoDL = 2.toMimo(),
                        mimoUL = mimoUl.toMimo(),
                        modDL = ModulationOrder.QAM64.toModulation(),
                        modUL = defaultModUL.toModulation(),
                        powerClass = defaultPowerClass,
                    )
                }
            } ?: return emptyList()

        eutraCapability.eutraCapabilityV9e0
            ?.getArrayAtPath("rf-Parameters-v9e0.supportedBandListEUTRA-v9e0")
            ?.forEachIndexed { i, it ->
                it.getInt("bandEUTRA-v9e0")?.let { band -> lteBands[i].band = band }
            }

        eutraCapability.eutraCapabilityV1250
            ?.getArrayAtPath("rf-Parameters-v1250.supportedBandListEUTRA-v1250")
            ?.forEachIndexed { i, it ->
                if (it.getString("dl-256QAM-r12") != null) {
                    lteBands[i].modDL = ModulationOrder.QAM256.toModulation()
                }
                if (it.getString("ul-64QAM-r12") != null) {
                    lteBands[i].modUL = ModulationOrder.QAM64.toModulation()
                }
            }

        eutraCapability.eutraCapabilityV1310
            ?.getArrayAtPath("rf-Parameters-v1310.supportedBandListEUTRA-v1310")
            ?.forEachIndexed { i, it ->
                if (it.getString("ue-PowerClass-5-r13") != null) {
                    lteBands[i].powerClass = PowerClass.PC5
                }
            }

        eutraCapability.eutraCapabilityV1320
            ?.getArrayAtPath("rf-Parameters-v1320.supportedBandListEUTRA-v1320")
            ?.forEachIndexed { i, it ->
                it.getString("ue-PowerClass-N-r13")?.let {
                    lteBands[i].powerClass = PowerClass.valueOf(it.replace("class", "PC"))
                }
            }

        return lteBands
    }

    private fun getNrBands(eutraCapability: UEEutraCapabilityJson, endc: Boolean): List<BandBoxed> {

        val supportedBandListNR =
            if (endc) {
                eutraCapability.eutraCapabilityV1510?.getArrayAtPath(
                    "irat-ParametersNR-r15.supportedBandListEN-DC-r15"
                )
            } else {
                eutraCapability.eutraCapabilityV1540?.getArrayAtPath(
                    "irat-ParametersNR-v1540.supportedBandListNR-SA-r15"
                )
            }

        return supportedBandListNR?.mapNotNull {
            it.getInt("bandNR-r15")?.let { band -> BandBoxed(band) }
        } ?: emptyList()
    }

    private fun List<List<ComponentLte>>.hasHighMimo() = any { bands ->
        bands.any { it.mimoDL.average() > 2 }
    }

    private fun List<List<ComponentLte>>.mergeBcs(bcsList: List<BCS>) =
        zip(bcsList) { bands, bcs -> ComboLte(bands.sortedDescending(), bcs) }

    private fun linkFeaturesAndCarrier(
        combos: List<ICombo>,
        featureSetCombinations: List<List<List<FeatureIndex>>>,
        lteFeatures: FeatureSets?,
        nrFeatures: FeatureSets?,
        lteBands: Map<Band, BandLteDetails>?,
        nrBands: Map<Band, BandNrDetails>?,
    ): List<ICombo> {
        val list = mutableListWithCapacity<ICombo>(combos.size)

        for (combo in combos) {
            val featureSetsPerBand = featureSetCombinations.getOrNull(combo.featureSet) ?: continue
            val indices = featureSetsPerBand.firstOrNull()?.indices ?: IntRange.EMPTY
            val mergedCombos =
                indices.map { index ->
                    mergeComboNrAndIndexedFeature(
                        combo,
                        featureSetsPerBand,
                        index,
                        nrFeatures,
                        lteFeatures,
                        lteBands,
                        nrBands,
                    )
                }
            list.addAll(mergedCombos)
        }
        return list
    }

    private fun mergeComboNrAndIndexedFeature(
        combo: ICombo,
        featureSetsPerBand: List<List<FeatureIndex>>,
        index: Int,
        nrFeatures: FeatureSets?,
        lteFeatures: FeatureSets?,
        lteBands: Map<Band, BandLteDetails>?,
        nrBands: Map<Band, BandNrDetails>?,
    ): ICombo {
        val newNrComponents: MutableList<ComponentNr>
        val newLteComponents: MutableList<ComponentLte>

        val lteIterator: Iterator<ComponentLte>
        val nrIterator: Iterator<ComponentNr>

        when (combo) {
            is ComboEnDc -> {
                lteIterator = combo.componentsLte.iterator()
                nrIterator = combo.componentsNr.iterator()
                newLteComponents = mutableListWithCapacity(combo.componentsLte.size)
                newNrComponents = mutableListWithCapacity(combo.componentsNr.size)
            }
            is ComboNr -> {
                lteIterator = emptyList<ComponentLte>().iterator()
                nrIterator = combo.componentsNr.iterator()
                newLteComponents = mutableListOf()
                newNrComponents = mutableListWithCapacity(combo.componentsNr.size)
            }
            else -> {
                throw IllegalArgumentException("Unsupported combo type ${combo.javaClass}")
            }
        }

        for (featureSetBand in featureSetsPerBand) {
            val featureSet = featureSetBand[index]
            val oldComponent: IComponent
            val features: FeatureSets?
            val bandDetails: IBandDetails?

            if (featureSet.isNR) {
                oldComponent = nrIterator.next()
                features = nrFeatures
                bandDetails = nrBands?.get(oldComponent.band)
            } else {
                oldComponent = lteIterator.next()
                features = lteFeatures
                bandDetails = lteBands?.get(oldComponent.band)
            }

            val newComponent =
                mergeComponentAndFeature(
                    featureSet,
                    oldComponent,
                    features,
                    bandDetails ?: BandLteDetails(0),
                )

            if (newComponent is ComponentLte) {
                newLteComponents.add(newComponent)
            } else if (newComponent is ComponentNr) {
                newNrComponents.add(newComponent)
            }
        }

        newNrComponents.sortDescending()

        return if (newLteComponents.isNotEmpty()) {
            newLteComponents.sortDescending()
            val comboEnDc = combo as ComboEnDc
            val (bcsNr, bcsEutra, bcsIntraEnDc) =
                mergeAndSplitEnDcBCS(
                    newLteComponents,
                    newNrComponents,
                    comboEnDc.bcsNr,
                    comboEnDc.bcsEutra,
                    combo.bcsIntraEnDc,
                )
            combo.copy(
                masterComponents = newLteComponents,
                secondaryComponents = newNrComponents,
                bcsNr = bcsNr,
                bcsEutra = bcsEutra,
                bcsIntraEnDc = bcsIntraEnDc,
            )
        } else {
            (combo as ComboNr).copy(masterComponents = newNrComponents)
        }
    }

    private fun mergeComponentAndFeature(
        featureSet: FeatureIndex,
        component: IComponent,
        features: FeatureSets?,
        bandDetails: IBandDetails,
    ): IComponent? {
        val dlIndex = featureSet.downlinkIndex - 1
        val ulIndex = featureSet.uplinkIndex - 1

        if (dlIndex < 0 && ulIndex < 0) {
            // Fallback combination
            return null
        }

        val dlFeature =
            if (dlIndex >= 0) {
                features?.downlink?.getOrNull(dlIndex)?.featureSetsPerCC
            } else {
                null
            }
        val ulFeature =
            if (ulIndex >= 0) {
                features?.uplink?.getOrNull(ulIndex)?.featureSetsPerCC
            } else {
                null
            }

        if (
            features == null ||
                dlFeature.isNullOrEmpty() && dlIndex >= 0 ||
                ulFeature.isNullOrEmpty() && ulIndex >= 0
        ) {
            // Return a copy without any editing
            return component.clone()
        }

        return mergeComponentAndFeaturePerCC(component, dlFeature, ulFeature, bandDetails)
    }

    private fun getFeatureSetCombinations(
        nrCapability: UENrRrcCapabilityJson
    ): List<List<List<FeatureIndex>>> {
        val featureSetCombinations = nrCapability.rootJson.getArray("featureSetCombinations")
        val list =
            featureSetCombinations?.mapNotNull { featureSetCombination ->
                (featureSetCombination as? JsonArray)?.mapNotNull { featureSetsPerBand ->
                    (featureSetsPerBand as? JsonArray)?.mapNotNull { featureSet ->
                        val eutra = featureSet.getObject("eutra")
                        val nr = featureSet.getObject("nr")
                        if (nr != null) {
                            val dl = nr.getInt("downlinkSetNR") ?: 0
                            val ul = nr.getInt("uplinkSetNR") ?: 0

                            FeatureIndex(true, dl, ul)
                        } else if (eutra != null) {
                            val dl = eutra.getInt("downlinkSetEUTRA") ?: 0
                            val ul = eutra.getInt("uplinkSetEUTRA") ?: 0

                            FeatureIndex(false, dl, ul)
                        } else {
                            null
                        }
                    }
                }
            }
        return list ?: emptyList()
    }

    private fun getNrBandCombinations(nrCapability: UENrRrcCapabilityJson): List<ICombo> {
        val endc = nrCapability is UEMrdcCapabilityJson
        val bandCombinationsPath =
            if (endc) {
                "rf-ParametersMRDC.supportedBandCombinationList"
            } else {
                "rf-Parameters.supportedBandCombinationList"
            }
        val bandCombinationsListv1590 =
            nrCapability.rootJson
                .getArrayAtPath("rf-ParametersMRDC.supportedBandCombinationList-v1590")
                ?.iterator()
        val bandCombinationsList = nrCapability.rootJson.getArrayAtPath(bandCombinationsPath)
        val list =
            bandCombinationsList?.mapNotNull { bandCombination ->
                val bandList = bandCombination.getArray("bandList") ?: return@mapNotNull null
                val lteBands = mutableListWithCapacity<ComponentLte>(bandList.size)
                val nrBands = mutableListWithCapacity<ComponentNr>(bandList.size)
                for (bandParameters in bandList) {
                    val component = parse5gBandParameters(bandParameters)
                    if (component is ComponentNr) {
                        nrBands.add(component)
                    } else if (component is ComponentLte) {
                        lteBands.add(component)
                    }
                }
                val featureSetCombination = bandCombination.getInt("featureSetCombination") ?: 0
                val bcsString = bandCombination.getString("supportedBandwidthCombinationSet") ?: "1"
                val bcs = BCS.fromBinaryString(bcsString)
                if (endc) {
                    val bcsEutraString =
                        bandCombination
                            .getObject("ca-ParametersEUTRA")
                            ?.getString("supportedBandwidthCombinationSetEUTRA-v1530") ?: "1"
                    val bcsEutra = BCS.fromBinaryString(bcsEutraString)
                    val bcsIntraEnDcString =
                        bandCombinationsListv1590
                            ?.next()
                            ?.getString("supportedBandwidthCombinationSetIntraENDC") ?: "1"
                    val bcsIntraEnDc = BCS.fromBinaryString(bcsIntraEnDcString)
                    ComboEnDc(lteBands, nrBands, featureSetCombination, bcs, bcsEutra, bcsIntraEnDc)
                } else {
                    ComboNr(nrBands, featureSetCombination, bcs)
                }
            }
        return list ?: emptyList()
    }

    private fun parse5gBandParameters(bandParameters: JsonElement): IComponent? {
        val nr = bandParameters.getObject("nr")
        if (nr != null) {
            val band = nr.getInt("bandNR") ?: 0
            val dlClass = BwClass.valueOf(nr.getString("ca-BandwidthClassDL-NR"))
            val ulClass = BwClass.valueOf(nr.getString("ca-BandwidthClassUL-NR"))
            return ComponentNr(band, dlClass, ulClass)
        }

        val lte = bandParameters.getObject("eutra")
        if (lte != null) {
            val band = lte.getInt("bandEUTRA") ?: 0
            val dlClass = BwClass.valueOf(lte.getString("ca-BandwidthClassDL-EUTRA"))
            val ulClass = BwClass.valueOf(lte.getString("ca-BandwidthClassUL-EUTRA"))
            return ComponentLte(band, dlClass, ulClass)
        }
        return null
    }

    private fun getNrDcBandCombinations(
        nrCapability: UENrCapabilityJson,
        nrCombos: List<ComboNr>,
    ): List<ComboNr> {
        val bandCombinationsPath = "rf-Parameters.supportedBandCombinationList-v1560"
        val bandCombinationsList = nrCapability.rootJson.getArrayAtPath(bandCombinationsPath)

        val list =
            bandCombinationsList?.mapIndexedNotNull { i, bandCombination ->
                val nrCombo = nrCombos.getOrNull(i)
                val nrdc = bandCombination.getObject("ca-ParametersNRDC")

                // if nrdc is absent, nrdc isn't supported
                if (nrCombo == null || nrdc == null) {
                    return@mapIndexedNotNull null
                }

                // if featureSetDc isn't set, use standard featureSet
                val featureSetDc = nrdc.getInt("featureSetCombinationDC") ?: nrCombo.featureSet

                nrCombo.copy(featureSet = featureSetDc)
            } ?: emptyList()
        return list
    }

    private fun getNrBands(nrCapability: UENrCapabilityJson, ueType: UeType): List<BandNrDetails> {
        val phyParametersFr1 =
            nrCapability.rootJson.getObjectAtPath("phy-Parameters.phy-ParametersFR1")
        val qam256Fr1DL = phyParametersFr1?.getString("pdsch-256QAM-FR1") != null

        val scs60Fr1 = phyParametersFr1?.getString("scs-60kHz") != null

        val bandsList =
            nrCapability.rootJson.getArrayAtPath("rf-Parameters.supportedBandListNR")?.mapNotNull {
                supportedBandNr ->
                val componentNr =
                    supportedBandNr.getInt("bandNR")?.let { BandNrDetails(it) }
                        ?: return@mapNotNull null

                val duplex = DuplexBandTable.getNrDuplex(componentNr.band)
                val qam256Dl =
                    if (!componentNr.isFR2) {
                        qam256Fr1DL
                    } else {
                        supportedBandNr.getString("pdsch-256QAM-FR2") != null
                    }

                val qam1024Dl =
                    !componentNr.isFR2 && supportedBandNr.getString("pdsch-1024QAM-FR1-r17") != null

                componentNr.modDL =
                    when {
                        duplex == Duplex.SUL -> ModulationOrder.NONE
                        qam1024Dl -> ModulationOrder.QAM1024
                        qam256Dl -> ModulationOrder.QAM256
                        else -> ModulationOrder.QAM64
                    }.toModulation()

                if (supportedBandNr.getString("pusch-256QAM") != null) {
                    componentNr.modUL = ModulationOrder.QAM256.toModulation()
                } else if (duplex != Duplex.SDL) {
                    componentNr.modUL = ModulationOrder.QAM64.toModulation()
                }

                componentNr.powerClass =
                    when {
                        duplex == Duplex.SDL -> {
                            PowerClass.NONE
                        }
                        supportedBandNr.getString("ue-PowerClass-v1610") != null -> {
                            PowerClass.PC1dot5
                        }
                        else -> {
                            PowerClass.valueOf(
                                supportedBandNr.getString("ue-PowerClass")?.uppercase() ?: "PC3"
                            )
                        }
                    }

                val maxUplinkDutyCycleKey =
                    when {
                        componentNr.isFR2 -> {
                            "maxUplinkDutyCycle-FR2"
                        }
                        componentNr.powerClass == PowerClass.PC2 -> {
                            // default is 50% according to TS 38.306
                            componentNr.maxUplinkDutyCycle = 50
                            "maxUplinkDutyCycle-PC2-FR1"
                        }
                        componentNr.powerClass == PowerClass.PC1dot5 -> {
                            "maxUplinkDutyCycle-PC1dot5-MPE-FR1-r16"
                        }
                        else -> {
                            null
                        }
                    }

                if (maxUplinkDutyCycleKey != null) {
                    supportedBandNr
                        .getString(maxUplinkDutyCycleKey)
                        ?.removePrefix("n")
                        ?.toInt()
                        ?.let { componentNr.maxUplinkDutyCycle = it }
                }

                componentNr.rateMatchingLteCrs =
                    supportedBandNr.getString("rateMatchingLTE-CRS") != null

                parseNRChannelBWs(supportedBandNr, componentNr, scs60Fr1, ueType)

                componentNr
            }

        return bandsList ?: emptyList()
    }

    private fun parseNRChannelBWs(
        supportedBandNr: JsonElement,
        componentNr: BandNrDetails,
        scs60Fr1Supported: Boolean,
        ueType: UeType,
    ) {
        val channelBWsDL = supportedBandNr.getObject("channelBWs-DL")
        val channelBWsUL = supportedBandNr.getObject("channelBWs-UL")
        val channelBWsDlV1590 = supportedBandNr.getObject("channelBWs-DL-v1590")
        val channelBWsUlV1590 = supportedBandNr.getObject("channelBWs-UL-v1590")

        val bandwidthsDL = parseNrBw(channelBWsDL, componentNr, false, ueType)
        val bandwidthsUL = parseNrBw(channelBWsUL, componentNr, false, ueType)

        val scsRange =
            when {
                componentNr.isFR2 -> listOf(60, 120)
                scs60Fr1Supported -> listOf(15, 30, 60)
                else -> listOf(15, 30)
            }

        /*
         * According to TS 38.306 v16.6.0, the UE can omit channelBWs or specific SCS in channelBWs
         * if it supports all BWs defined in 38.101-1 and 38.101-2 v15.7.0.
         * So we add default BWs here when a specific SCS is missing.
         */
        for (scs in scsRange) {
            val bws = BwTableNr.getDLBws(componentNr.band, scs)
            if (!bandwidthsDL.containsKey(scs)) {
                bandwidthsDL[scs] = bws.bwsDL
            }
            if (!bandwidthsUL.containsKey(scs)) {
                bandwidthsUL[scs] = bws.bwsUL
            }
        }

        bandwidthsDL.merge(parseNrBw(channelBWsDlV1590, componentNr, true, ueType))
        bandwidthsUL.merge(parseNrBw(channelBWsUlV1590, componentNr, true, ueType))

        // Sort bws array and add to bwsList
        val bwsList = mutableListWithCapacity<BwsNr>(scsRange.count())
        for (scs in scsRange) {
            val bwsDl = bandwidthsDL[scs] ?: intArrayOf()
            var bwsUl = bandwidthsUL[scs] ?: intArrayOf()

            /* Don't add empty bws */
            if (bwsDl.isEmpty() && bwsUl.isEmpty()) {
                continue
            }

            /* if equal, preserve only one array */
            if (bwsDl.contentEquals(bwsUl)) {
                bwsUl = bwsDl
                bwsDl.sortDescending()
            } else {
                bwsDl.sortDescending()
                bwsUl.sortDescending()
            }

            bwsList.add(BwsNr(scs, bwsDl, bwsUl))
        }

        componentNr.bandwidths = bwsList
    }

    private fun parseNrBw(
        channelBWs: JsonObject?,
        componentNr: BandNrDetails,
        isV1590: Boolean,
        ueType: UeType,
    ): BwMap {
        val freqRange = if (componentNr.isFR2) "fr2" else "fr1"

        val bandWidthMap = mutableMapOf<Int, IntArray>()
        channelBWs?.getObject(freqRange)?.forEach { (scsKey, element) ->
            val scs = scsKey.removePrefix("scs-").removeSuffix("kHz").toInt()
            (element as? JsonPrimitive)?.contentOrNull?.let { bwString ->
                bandWidthMap[scs] = BwsBitMap(bwString, componentNr.band, scs, isV1590, ueType).bws
            }
        }
        return bandWidthMap
    }

    private fun getLteFeatureSet(eutraCapability: UEEutraCapabilityJson): FeatureSets {
        var downlink = emptyList<FeatureSet>()
        var uplink = emptyList<FeatureSet>()

        eutraCapability.eutraCapabilityV1510?.getObject("featureSetsEUTRA-r15")?.let { featureSets
            ->
            val downlinkPerCC =
                featureSets.getArray("featureSetsDL-PerCC-r15")?.map {
                    val mimoLayers = it.getString("supportedMIMO-CapabilityDL-MRDC-r15")
                    val mimo = maxOf(Int.fromLiteral(mimoLayers), 2)
                    FeaturePerCCLte(mimo = mimo.toMimo())
                }

            downlink =
                featureSets.getArray("featureSetsDL-r15")?.mapNotNull { featureSetPerCCList ->
                    val list =
                        featureSetPerCCList.getArray("featureSetPerCC-ListDL-r15")?.mapNotNull {
                            index ->
                            index.asIntOrNull()?.let { downlinkPerCC?.getOrNull(it) }
                        }
                    if (list != null) {
                        FeatureSet(list, LinkDirection.DOWNLINK)
                    } else {
                        null
                    }
                } ?: downlink

            val uplinkPerCC =
                featureSets.getArray("featureSetsUL-PerCC-r15")?.map {
                    val qam =
                        if (it.getString("ul-256QAM-r15") != null) {
                            ModulationOrder.QAM256
                        } else {
                            ModulationOrder.NONE
                        }
                    val mimoLayers = it.getString("supportedMIMO-CapabilityUL-r15")
                    val mimo = maxOf(Int.fromLiteral(mimoLayers), 1)
                    FeaturePerCCLte(LinkDirection.UPLINK, mimo = mimo.toMimo(), qam = qam)
                }

            // featureSets.getArray("featureSetsDL-v1550") - Never seen on the wild
            uplink =
                featureSets.getArray("featureSetsUL-r15")?.mapNotNull { featureSetPerCCList ->
                    val list =
                        featureSetPerCCList.getArray("featureSetPerCC-ListUL-r15")?.mapNotNull {
                            index ->
                            index.asIntOrNull()?.let { uplinkPerCC?.getOrNull(it) }
                        }
                    if (list != null) {
                        FeatureSet(list, LinkDirection.UPLINK)
                    } else {
                        null
                    }
                } ?: uplink
        }

        if (debug) {
            echoSafe("\nLTE FeatureSets")
            echoSafe(downlink.joinToString(separator = "\n"))
            echoSafe(uplink.joinToString(separator = "\n"))
        }
        return FeatureSets(downlink, uplink)
    }

    private fun getNRFeatureSet(nrCapability: UENrCapabilityJson): FeatureSets {
        var downlink = emptyList<FeatureSet>()
        var uplink = emptyList<FeatureSet>()

        nrCapability.rootJson.getObject("featureSets")?.let { featureSets ->
            val downlinkPerCC = mutableListOf<FeaturePerCCNr>()
            val uplinkPerCC = mutableListOf<FeaturePerCCNr>()

            val downlinkPerCCr15 =
                featureSets.getArray("featureSetsDownlinkPerCC")?.map {
                    val scs =
                        it.getString("supportedSubcarrierSpacingDL")
                            ?.removePrefix("kHz")
                            ?.split("-")
                            ?.first()
                            ?.toIntOrNull() ?: 0
                    val supportedBandwidthDL = it.getObject("supportedBandwidthDL")
                    val bwFr1OrFr2 =
                        supportedBandwidthDL?.getString("fr1")
                            ?: supportedBandwidthDL?.getString("fr2")
                    var bw = parseBw(bwFr1OrFr2)
                    // Ignore bw 90MHz if max bw is < 80
                    val channelBW90mhz = it.getString("channelBW-90mhz") != null && bw >= 80
                    // Set maxBw to 90MHz if maxBw is 80
                    if (channelBW90mhz && bw == 80) bw = 90

                    val mimoLayers = it.getString("maxNumberMIMO-LayersPDSCH")
                    val mimo = maxOf(Int.fromLiteral(mimoLayers), 2)

                    FeaturePerCCNr(
                        mimo = mimo.toMimo(),
                        // Modulation in NR features means something else (see TS 38 306)
                        qam = ModulationOrder.NONE,
                        scs = scs,
                        bw = bw,
                        channelBW90mhz = channelBW90mhz,
                    )
                }

            downlinkPerCCr15?.let { downlinkPerCC.addAll(it) }

            // r17 bw
            featureSets.getArray("featureSetsDownlinkPerCC-v1700")?.forEachIndexed { index, it ->
                val supportedBandwidthDL = it.getObject("supportedBandwidthDL-v1710")
                val bwFr1OrFr2 =
                    supportedBandwidthDL?.getString("fr1-r17")
                        ?: supportedBandwidthDL?.getString("fr2-r17")

                val bwR17 = parseBw(bwFr1OrFr2)

                if (bwR17 != 0) {
                    val curr = downlinkPerCC[index]
                    val bw90MHzSupported = bwR17 >= 90 && curr.channelBW90mhz
                    val new = curr.copy(bw = bwR17, channelBW90mhz = bw90MHzSupported)
                    downlinkPerCC[index] = new
                }
            }

            downlink =
                featureSets.getArray("featureSetsDownlink")?.mapNotNull { featureSetPerCCList ->
                    val list =
                        featureSetPerCCList.getArray("featureSetListPerDownlinkCC")?.mapNotNull {
                            index ->
                            index.asIntOrNull()?.let {
                                // NR PerCC-ID is 1..1024 while LTE PerCC-ID is 0..32
                                downlinkPerCC?.getOrNull(it - 1)
                            }
                        }
                    if (list != null) {
                        FeatureSet(list, LinkDirection.DOWNLINK)
                    } else {
                        null
                    }
                } ?: downlink

            val uplinkPerCCr15 =
                featureSets.getArray("featureSetsUplinkPerCC")?.map {
                    val scs =
                        it.getString("supportedSubcarrierSpacingUL")
                            ?.removePrefix("kHz")
                            ?.split("-")
                            ?.first()
                            ?.toIntOrNull() ?: 0
                    val supportedBandwidthUL = it.getObject("supportedBandwidthUL")
                    val bwFr1OrFr2 =
                        supportedBandwidthUL?.getString("fr1")
                            ?: supportedBandwidthUL?.getString("fr2")
                    var bw = parseBw(bwFr1OrFr2)
                    // Ignore bw 90MHz if max bw is < 80
                    val channelBW90mhz = it.getString("channelBW-90mhz") != null && bw >= 80
                    // Set maxBw to 90MHz if maxBw is 80
                    if (channelBW90mhz && bw == 80) bw = 90

                    val mimoCbLayers =
                        it.getObject("mimo-CB-PUSCH")?.getString("maxNumberMIMO-LayersCB-PUSCH")
                    val mimoNonCbLayers = it.getString("maxNumberMIMO-LayersNonCB-PUSCH")

                    val mimo =
                        maxOf(Int.fromLiteral(mimoCbLayers), Int.fromLiteral(mimoNonCbLayers), 1)
                    FeaturePerCCNr(
                        type = LinkDirection.UPLINK,
                        mimo = mimo.toMimo(),
                        // Modulation in NR features means something else
                        qam = ModulationOrder.NONE,
                        scs = scs,
                        bw = bw,
                        channelBW90mhz = channelBW90mhz,
                    )
                }

            uplinkPerCCr15?.let { uplinkPerCC.addAll(it) }

            // r17 bw
            featureSets.getArray("featureSetsUplinkPerCC-v1700")?.forEachIndexed { index, it ->
                val supportedBandwidthDL = it.getObject("supportedBandwidthUL-v1710")
                val bwFr1OrFr2 =
                    supportedBandwidthDL?.getString("fr1-r17")
                        ?: supportedBandwidthDL?.getString("fr2-r17")

                val bwR17 = parseBw(bwFr1OrFr2)

                if (bwR17 != 0) {
                    val curr = uplinkPerCC[index]
                    val bw90MHzSupported = bwR17 >= 90 && curr.channelBW90mhz
                    val new = curr.copy(bw = bwR17, channelBW90mhz = bw90MHzSupported)
                    uplinkPerCC[index] = new
                }
            }

            uplink =
                featureSets.getArray("featureSetsUplink")?.mapNotNull { featureSetPerCCList ->
                    val list =
                        featureSetPerCCList.getArray("featureSetListPerUplinkCC")?.mapNotNull {
                            index ->
                            index.asIntOrNull()?.let {
                                // NR PerCC-ID is 1..1024 while LTE PerCC-ID is 0..32
                                uplinkPerCC?.getOrNull(it - 1)
                            }
                        }
                    if (list != null) {
                        FeatureSet(list, LinkDirection.UPLINK)
                    } else {
                        null
                    }
                } ?: uplink
        }

        if (debug) {
            echoSafe("\nNr FeatureSets")
            echoSafe(downlink.joinToString(separator = "\n"))
            echoSafe(uplink.joinToString(separator = "\n"))
        }
        return FeatureSets(downlink, uplink)
    }

    private fun parseBandParametersDL(
        bandParameters: JsonElement,
        release: Int,
    ): Pair<BwClass, Mimo> {
        val bandParametersDL =
            when (release) {
                14 -> bandParameters
                13 -> bandParameters.getObject("bandParametersDL-r13")
                else -> bandParameters.getArrayAtPath("bandParametersDL-r$release")?.first()
            }

        if (bandParametersDL == null) {
            return Pair(BwClass.NONE, EmptyMimo)
        }

        // both r10 and r11 uses ca-BandwidthClassDL/supportedMIMO-CapabilityDL -r10
        val subRelease = if (release >= 13) release.toString() else "10"
        val dlClassString = bandParametersDL.getString("ca-BandwidthClassDL-r$subRelease")
        val dlClass = BwClass.valueOf(dlClassString)

        var dlMimo = 0
        // BandCombination-r14/BandIndication-r14 doesn't support mimo
        if (release < 14) {
            val mimoLayers = bandParametersDL.getString("supportedMIMO-CapabilityDL-r$subRelease")
            dlMimo = Int.fromLiteral(mimoLayers)

            // Some devices only reports fourLayerTM3-TM4-rXX or only reports 4rx in
            // fourLayerTM3-TM4-rXX
            // For r11 and r10 the check is done in parseCaMimoV10i0()
            if (release == 13 && dlMimo < 4) {
                bandParametersDL.getString("fourLayerTM3-TM4-r13")?.let { dlMimo = 4 }
            }

            // Some devices don't report supportedMIMO-CapabilityDL-rXX for twoLayers
            if (dlClass != BwClass.NONE && dlMimo == 0) {
                dlMimo = 2
            }
        }

        var resultMimo: Mimo = dlMimo.toMimo()
        if (release == 13) {
            // if release 13 check intraBandContiguousCC-InfoList-r13
            bandParametersDL.getArray("intraBandContiguousCC-InfoList-r13")?.let {
                resultMimo = parseMimoR12(it, resultMimo)
            }
        }

        return Pair(dlClass, resultMimo)
    }

    private fun parseMimoR12(intraBandCCInfoList: JsonArray, defaultMimo: Mimo): Mimo {
        if (intraBandCCInfoList.size < 2) {
            return defaultMimo
        }

        var allDefault = true
        val default = defaultMimo.average().toInt()
        val mixedMimoList =
            intraBandCCInfoList.map {
                val str = it.getString("supportedMIMO-CapabilityDL-r12")
                if (str == null) {
                    default
                } else {
                    allDefault = false
                    Int.fromLiteral(str)
                }
            }

        // Don't override mimo if all are = to default
        return if (allDefault) defaultMimo else Mimo.from(mixedMimoList)
    }

    private fun parseBandParametersUL(
        bandParameters: JsonElement,
        release: Int,
    ): Pair<BwClass, Mimo> {
        val bandParametersUL =
            when (release) {
                14 -> bandParameters
                13 -> bandParameters.getObject("bandParametersUL-r13")
                else -> bandParameters.getArrayAtPath("bandParametersUL-r$release")?.first()
            }

        if (bandParametersUL == null) {
            return Pair(BwClass.NONE, EmptyMimo)
        }

        // both r10, r11 and r13 uses supportedMIMO-CapabilityUL -r10
        val subReleaseBw = if (release >= 14) release.toString() else "10"
        val subReleaseMimo = if (release >= 13) release.toString() else "10"

        val ulClassString = bandParametersUL.getString("ca-BandwidthClassUL-r$subReleaseBw")
        val ulClass = BwClass.valueOf(ulClassString)
        val mimoLayers = bandParametersUL.getString("supportedMIMO-CapabilityUL-r$subReleaseMimo")

        var ulMimo = 0
        // BandCombination-r14/BandIndication-r14 doesn't support mimo
        if (release < 14) {
            ulMimo = Int.fromLiteral(mimoLayers)
            if (ulMimo == 0) {
                // supportedMIMO-CapabilityUL isn't reported if ulMimo = 1
                ulMimo = 1
            }
        }

        return Pair(ulClass, ulMimo.toMimo())
    }

    private fun parseBandParameters(bandParameters: JsonElement, release: Int): ComponentLte {
        val band = bandParameters.getInt("bandEUTRA-r$release") ?: 0
        val (dlClass, dlMimo) = parseBandParametersDL(bandParameters, release)
        val (ulClass, ulMimo) = parseBandParametersUL(bandParameters, release)
        return ComponentLte(band, dlClass, ulClass, dlMimo, ulMimo)
    }

    private fun getUeNrCapabilityFilters(capability: UENrRrcCapabilityJson): UeCapabilityFilterNr {
        val rat =
            if (capability is UEMrdcCapabilityJson) {
                Rat.EUTRA_NR
            } else {
                Rat.NR
            }
        val ueCapFilter = UeCapabilityFilterNr(rat)

        val receivedFilters = capability.nrRrcCapabilityV1560?.getObjectAtPath("receivedFilters")
        if (receivedFilters != null) {
            parseReceivedFilters(receivedFilters, ueCapFilter)
        }

        val bandListFilterPath =
            if (rat == Rat.EUTRA_NR) {
                "rf-ParametersMRDC.appliedFreqBandListFilter"
            } else {
                "rf-Parameters.appliedFreqBandListFilter"
            }
        val freqBandFilter = capability.rootJson.getArrayAtPath(bandListFilterPath)
        val freqBandFilterList =
            freqBandFilter?.mapNotNull { bandInfo ->
                val eutra = bandInfo.getObject("bandInformationEUTRA")
                val nr = bandInfo.getObject("bandInformationNR")
                if (nr != null) {
                    val band = nr.getInt("bandNR") ?: 0
                    val maxBwDl = parseBw(nr.getString("maxBandwidthRequestedDL"))
                    val maxBwUl = parseBw(nr.getString("maxBandwidthRequestedUL"))
                    val maxCCsDl = nr.getInt("maxCarriersRequestedDL") ?: 0
                    val maxCCsUl = nr.getInt("maxCarriersRequestedUL") ?: 0
                    BandFilterNr(band, maxBwDl, maxBwUl, maxCCsDl, maxCCsUl)
                } else if (eutra != null) {
                    val band = eutra.getInt("bandEUTRA") ?: 0
                    val classDl = BwClass.valueOf(eutra.getString("ca-BandwidthClassDL-EUTRA"))
                    val classUl = BwClass.valueOf(eutra.getString("ca-BandwidthClassUL-EUTRA"))
                    BandFilterLte(band, classDl, classUl)
                } else {
                    null
                }
            }

        if (freqBandFilterList != null) {
            val (lteBands, nrBands) = freqBandFilterList.partition { it is BandFilterLte }
            ueCapFilter.lteBands = lteBands.typedList()
            ueCapFilter.nrBands = nrBands.typedList()
        }

        // if rat is NR and eutra-nr-only is set, the UE should omit appliedFreqBandListFilter
        // but eutra-nr-only with omitEnDc or includeNrDc doesn't really make sense
        if (
            rat == Rat.NR &&
                freqBandFilterList == null &&
                !ueCapFilter.omitEnDc &&
                !ueCapFilter.includeNrDc
        ) {
            // Check bandList to make sure we don't have a strange unfiltered request
            // (unfiltered request aren't supported by 3gpp)
            val bandList =
                capability.rootJson.getArrayAtPath("rf-Parameters.supportedBandCombinationList")

            ueCapFilter.eutraNrOnly = bandList == null
        }

        return ueCapFilter
    }

    private fun getUeLteCapabilityFilters(
        capability: UEEutraCapabilityJson
    ): UeCapabilityFilterLte {
        val ueCapFilter = UeCapabilityFilterLte()

        val requestedBands =
            capability.eutraCapabilityV1180?.getArrayAtPath(
                "rf-Parameters-v1180.requestedBands-r11"
            ) ?: emptyList()
        val requestedBandsList =
            requestedBands.mapNotNull { band -> band.asIntOrNull()?.let { BandBoxed(it) } }
        ueCapFilter.lteBands = requestedBandsList

        val enbRequestR13 =
            capability.eutraCapabilityV1310?.getObjectAtPath(
                "rf-Parameters-v1310.eNB-RequestedParameters-r13"
            )
        if (enbRequestR13 != null) {
            ueCapFilter.reducedIntNonContComb =
                enbRequestR13.getString("reducedIntNonContCombRequested-r13") != null
            ueCapFilter.maxCCsDl = enbRequestR13.getInt("requestedCCsDL-r13") ?: 0
            ueCapFilter.maxCCsUl = enbRequestR13.getInt("requestedCCsUL-r13") ?: 0
            ueCapFilter.skipFallbackCombRequested =
                enbRequestR13.getString("skipFallbackCombRequested-r13") != null
        }

        val requestedDiffFallbackCombList =
            capability.eutraCapabilityV1430?.getArrayAtPath(
                "rf-Parameters-v1430.eNB-RequestedParameters-v1430.requestedDiffFallbackCombList-r14"
            )

        val diffFallbackCombList =
            requestedDiffFallbackCombList?.mapNotNull { combination ->
                val componentsArray = combination.asArrayOrNull() ?: return@mapNotNull null
                val components = componentsArray.map { parseBandParameters(it, 14) }
                ComboLte(components.sortedDescending())
            }

        ueCapFilter.diffFallbackCombList = diffFallbackCombList ?: emptyList()

        val filterCommon =
            capability.eutraCapabilityV1560?.getObject("appliedCapabilityFilterCommon-r15")
        if (filterCommon != null) {
            parseUeFilterCommon(filterCommon, ueCapFilter)
        }

        if (
            capability.eutraCapabilityV1310?.getArrayAtPath(
                "rf-Parameters-v1310.supportedBandCombinationReduced-r13"
            ) != null
        ) {
            ueCapFilter.reducedFormat = true
        }

        return ueCapFilter
    }

    // R16/R17/R18 ul tx switch NRCA parsing. NRDC and ENDC not supported for lack of samples.
    private fun getUlTxSwitchBandCombinations(nrCapability: UENrCapabilityJson): List<ComboNr> {
        val bandCombinationsPath = "rf-Parameters.supportedBandCombinationList-UplinkTxSwitch-r16"
        val bandCombinationsList = nrCapability.rootJson.getArrayAtPath(bandCombinationsPath)

        if (bandCombinationsList == null) return emptyList()

        val v1700List =
            nrCapability.rootJson.getArrayAtPath(
                "rf-Parameters.supportedBandCombinationList-UplinkTxSwitch-v1700"
            )
        val v1720List =
            nrCapability.rootJson.getArrayAtPath(
                "rf-Parameters.supportedBandCombinationList-UplinkTxSwitch-v1720"
            )
        val v1800List =
            nrCapability.rootJson.getArrayAtPath(
                "rf-Parameters.supportedBandCombinationList-UplinkTxSwitch-v1800"
            )

        val list = mutableListWithCapacity<ComboNr>(bandCombinationsList.size)
        for ((comboIndex, bandCombination) in bandCombinationsList.withIndex()) {
            val bandCombinationR16 = bandCombination.getObject("bandCombination-r16")
            val bandList = bandCombinationR16?.getArray("bandList") ?: continue
            val nrBands = mutableListWithCapacity<ComponentNr>(bandList.size)

            for (bandParameters in bandList) {
                val component = parse5gBandParameters(bandParameters)
                if (component is ComponentNr) {
                    nrBands.add(component)
                }
            }

            val featureSetCombination = bandCombinationR16.getInt("featureSetCombination") ?: 0
            val bcsString = bandCombinationR16.getString("supportedBandwidthCombinationSet") ?: "1"
            val bcs = BCS.fromBinaryString(bcsString)

            val bandPairs = bandCombination.getArray("supportedBandPairListNR-r16") ?: continue
            val optionR16Str = bandCombination.getString("uplinkTxSwitching-OptionSupport-r16")
            // Default to SWITCHED_UL when the field is absent: option 1 (switchedUL) is the
            // implicit baseline per TS 38.306; some UEs omit the field when only switchedUL
            // is supported.
            val optionR16 =
                if (optionR16Str != null) UplinkTxSwitchOption.valueOf(optionR16Str as String?)
                else UplinkTxSwitchOption.SWITCHED_UL

            // Collect R17 2T2T info for this combo index.
            // Two ASN.1 structures are used across UE implementations:
            // - v1700 with uplinkTxSwitchingBandParametersList-v1700 (PUSCH coherence per band)
            // - v1700 with supportedBandPairListNR-v1700 (period 2T2T per pair) + v1720 option
            val v1700Entry = v1700List?.getOrNull(comboIndex)
            val r17BandParamsList = v1700Entry?.getArray("uplinkTxSwitchingBandParametersList-v1700")
            val r17BandPairList = v1700Entry?.getArray("supportedBandPairListNR-v1700")
            val hasR17 = r17BandParamsList != null || r17BandPairList != null

            // v1720 carries uplinkTxSwitching-OptionSupport2T2T-r17 for the newer structure
            val v1720Entry = v1720List?.getOrNull(comboIndex)
            val r17OptionStr = v1720Entry?.getString("uplinkTxSwitching-OptionSupport2T2T-r17")

            // Collect R18 band pairs for this combo index, keyed by (ul1, ul2)
            val v1800Entry = v1800List?.getOrNull(comboIndex)
            val r18BandPairs = v1800Entry?.getArray("supportedBandPairListNR-r18")

            for (pairs in bandPairs) {
                // -1 because index start from 1
                val ul1 = pairs.getInt("bandIndexUL1-r16")?.minus(1)
                val ul2 = pairs.getInt("bandIndexUL2-r16")?.minus(1)

                if (ul1 == null || ul2 == null) continue

                var isSUL = false
                val newBands =
                    nrBands.mapIndexed { index, band ->
                        if (index == ul1 || index == ul2) {
                            if (band.isSUL) isSUL = true
                            band.copy(ulTxSwitch = true)
                        } else {
                            band
                        }
                    }

                // sul supports only option 1
                val ulTxSwitchOption = if (isSUL) UplinkTxSwitchOption.SWITCHED_UL else optionR16

                val ulTxSwitchConfigs = mutableListOf<UplinkTxSwitchConfig>()

                // R16 config (always present when band pair exists)
                ulTxSwitchConfigs.add(UplinkTxSwitchConfig(UplinkTxSwitchType.R16, ulTxSwitchOption))

                // R17 2T2T: present when either v1700 structure indicates 2T2T support.
                // Option comes from v1720 uplinkTxSwitching-OptionSupport2T2T-r17 if present,
                // otherwise falls back to the R16 option.
                if (hasR17) {
                    val r17Option =
                        if (r17OptionStr != null) UplinkTxSwitchOption.valueOf(r17OptionStr as String?)
                        else ulTxSwitchOption
                    ulTxSwitchConfigs.add(
                        UplinkTxSwitchConfig(UplinkTxSwitchType.R17_2T2T, r17Option)
                    )
                }

                // R18: per band pair matched by (bandIndexUL1-r18, bandIndexUL2-r18) using 0-based
                // indices
                if (r18BandPairs != null) {
                    for (r18Pair in r18BandPairs) {
                        val r18ul1 = r18Pair.getInt("bandIndexUL1-r18")?.minus(1)
                        val r18ul2 = r18Pair.getInt("bandIndexUL2-r18")?.minus(1)
                        if (r18ul1 != ul1 || r18ul2 != ul2) continue

                        val r18OptionStr =
                            r18Pair.getString("uplinkTxSwitchingOptionForBandPair-r18")
                        val r18Option = UplinkTxSwitchOption.valueOf(r18OptionStr as String?)
                        val periodObj =
                            r18Pair.getObject("uplinkTxSwitchingPeriodForBandPair-r18")
                        val has2T = periodObj?.getString("switchingPeriodFor2T-r18") != null
                        val r18Type =
                            if (has2T) UplinkTxSwitchType.R18_2T else UplinkTxSwitchType.R18_1T
                        ulTxSwitchConfigs.add(UplinkTxSwitchConfig(r18Type, r18Option))
                    }
                }

                val combo = ComboNr(newBands, featureSetCombination, bcs, ulTxSwitchConfigs)
                list.add(combo)
            }
        }
        return list
    }

    private fun parseReceivedFilters(
        receivedFilters: JsonObject,
        ueCapFilter: IUeCapabilityFilter,
    ) {
        val filterCommon = receivedFilters.getObject("capabilityRequestFilterCommon")
        if (filterCommon != null) {
            parseUeFilterCommon(filterCommon, ueCapFilter)
        }
        val capabilityEnquiryV1610 = receivedFilters.getObject("nonCriticalExtension")
        ueCapFilter.segAllowed = capabilityEnquiryV1610?.getString("rrc-SegAllowed-r16") != null
    }

    private fun parseUeFilterCommon(filterCommon: JsonObject, ueCapFilter: IUeCapabilityFilter) {
        val mrdcRequest = filterCommon.getObject("mrdc-Request")
        if (mrdcRequest != null) {
            ueCapFilter.omitEnDc = mrdcRequest.getString("omitEN-DC") != null
            ueCapFilter.includeNeDc = mrdcRequest.getString("includeNE-DC") != null
            ueCapFilter.includeNrDc = mrdcRequest.getString("includeNR-DC") != null
        }
        ueCapFilter.uplinkTxSwitchRequest =
            filterCommon.getString("uplinkTxSwitchRequest-r16") != null
    }

    // remove mhz prefix and convert to int
    // return 0 if input is invalid or null
    private fun parseBw(bandwidth: String?): Int {
        return bandwidth?.removePrefix("mhz")?.toIntOrNull() ?: 0
    }

    private fun parseAltTbsIndexes(eutraCapability: UEEutraCapabilityJson): List<String> {
        val tbsAltIndex = mutableListWithCapacity<String>(3)

        val a26a33 =
            eutraCapability.eutraCapabilityV1280
                ?.getObject("phyLayerParameters-v1280")
                ?.getString("alternativeTBS-Indices-r12")

        val b33 =
            eutraCapability.eutraCapabilityV1430
                ?.getObject("phyLayerParameters-v1430")
                ?.getString("alternativeTBS-Index-r14")

        if (a26a33 == "supported") {
            tbsAltIndex.add("26a")
            tbsAltIndex.add("33a")
        }
        if (b33 == "supported") {
            tbsAltIndex.add("33b")
        }

        return tbsAltIndex
    }

    private fun parseAccessRelease(capability: JsonObject): Int? {
        val release = capability.getString("accessStratumRelease")

        val releaseInt = release?.removePrefix("rel")?.toIntOrNull()

        return releaseInt
    }

    private fun parseSegSupportedEutra(capability: UEEutraCapabilityJson, release: Int?): Boolean? {
        val otherV1690 = capability.eutraCapabilityV1690?.getObject("other-Parameters-v1690")
        return parseSegSupported(otherV1690, release)
    }

    private fun parseSegSupportedNr(capability: UENrCapabilityJson, release: Int?): Boolean? {
        return parseSegSupported(capability.nrRrcCapabilityV1690, release)
    }

    private fun parseSegSupported(capabilityV1690: JsonObject?, release: Int?): Boolean? {
        // For rel16, absence of ul-RRC-Segmentation-r16 doesn't indicate the UE doesn't support it
        val default = if (compareValues(release, 16) > 0) false else null
        val segSupported = capabilityV1690?.getString("ul-RRC-Segmentation-r16") != null

        return if (segSupported) true else default
    }

    private fun getUeType(cap: UENrCapabilityJson): UeType {
        val defaultType = UeType.EMBB
        val redCapParameters = cap.nrRrcCapabilityV1700?.getObject("redCapParameters-r17")
        val isRedCap = redCapParameters?.getString("supportOfRedCap-r17") != null

        return if (isRedCap) UeType.RED_CAP_R17 else defaultType
    }
}
