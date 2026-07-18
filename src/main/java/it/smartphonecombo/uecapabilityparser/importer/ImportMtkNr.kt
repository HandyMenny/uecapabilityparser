package it.smartphonecombo.uecapabilityparser.importer

import it.smartphonecombo.uecapabilityparser.extension.mutableListWithCapacity
import it.smartphonecombo.uecapabilityparser.io.InputSource
import it.smartphonecombo.uecapabilityparser.model.BwClass
import it.smartphonecombo.uecapabilityparser.model.Capabilities
import it.smartphonecombo.uecapabilityparser.model.EmptyMimo
import it.smartphonecombo.uecapabilityparser.model.LinkDirection
import it.smartphonecombo.uecapabilityparser.model.combo.ComboEnDc
import it.smartphonecombo.uecapabilityparser.model.combo.ComboNr
import it.smartphonecombo.uecapabilityparser.model.combo.ComboNrDc
import it.smartphonecombo.uecapabilityparser.model.combo.ICombo
import it.smartphonecombo.uecapabilityparser.model.component.ComponentLte
import it.smartphonecombo.uecapabilityparser.model.component.ComponentNr
import it.smartphonecombo.uecapabilityparser.model.component.IComponent
import it.smartphonecombo.uecapabilityparser.model.feature.FeatureIndex
import it.smartphonecombo.uecapabilityparser.model.feature.FeaturePerCCLte
import it.smartphonecombo.uecapabilityparser.model.feature.FeaturePerCCNr
import it.smartphonecombo.uecapabilityparser.model.feature.IFeaturePerCC
import it.smartphonecombo.uecapabilityparser.model.fromLiteral
import it.smartphonecombo.uecapabilityparser.model.modulation.ModulationOrder
import it.smartphonecombo.uecapabilityparser.model.toMimo

/**
 * A parser for MediaTek 5G modem trace messages logs.
 *
 * Parses NR-CA and EN-DC combo information from NRRC capability trace messages, including NR DL/UL
 * FSpCC definitions, EUTRA DL/UL FSpCC definitions, Feature Set to FSpCC mappings, Feature Set
 * Combinations (FSC), and combo band combinations.
 *
 * The output is a [Capabilities] with NR-CA combos in [nrCombos][Capabilities.nrCombos], EN-DC
 * combos in [enDcCombos][Capabilities.enDcCombos], and NR-DC combos in
 * [nrDcCombos][Capabilities.nrDcCombos].
 */
object ImportMtkNr : ImportCapabilities {

    // Optional [MAIN] prefix found in older MTK trace formats
    private const val CAP = """\[CAP\]\s*"""
    private const val MAIN_CAP = """(?:\[MAIN\])?\[CAP\]\s*"""

    // Regex for NR DL FSpCC definitions
    private val reNrDlFspcc =
        Regex(
            MAIN_CAP +
                """NR DL FSpCC\[(\d+)],\s*""" +
                """scs\[(\w+)],\s*bw\[(\w+)],\s*bw90m\[(\w+)],\s*""" +
                """mimo\[(\w+)],\s*modulation\[(\w+)]"""
        )

    // Regex for NR UL FSpCC definitions
    private val reNrUlFspcc =
        Regex(
            MAIN_CAP +
                """NR UL FSpCC\[(\d+)],\s*""" +
                """scs\[(\w+)],\s*bw\[(\w+)],\s*bw90m\[(\w+)],\s*""" +
                """cb_mimo\[(\w+)],.*?modulation\[(\w+)]"""
        )

    // Regex for EUTRA DL FSpCC definitions
    private val reEutraDlFspcc = Regex(MAIN_CAP + """EUTRA DL FSpCC\[(\d+)],.*?mimo\[(\w+)]""")

    // Regex for EUTRA UL FSpCC definitions (may include 256qam field)
    private val reEutraUlFspcc =
        Regex(MAIN_CAP + """EUTRA UL FSpCC\[(\d+)],\s*mimo\[(\w+)](?:,\s*256qam\[(\w+)])?""")

    // Regex for NR DL Feature Set -> FSpCC ID mappings
    // Old format has 2 brackets, new format has 8 brackets
    private val reNrDlFs =
        Regex(
            MAIN_CAP +
                """NR DL FS\[(\d+)],\s*FSDLpCC ID""" +
                """\[([^\]]*)]""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?"""
        )

    // Regex for NR UL Feature Set -> FSpCC ID mappings
    private val reNrUlFs =
        Regex(
            MAIN_CAP +
                """NR UL FS\[(\d+)],\s*FSULpCC ID""" +
                """\[([^\]]*)]""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?"""
        )

    // Regex for EUTRA DL Feature Set -> FSpCC ID mappings
    private val reEutraDlFs =
        Regex(
            MAIN_CAP +
                """EUTRA DL FS\[(\d+)],\s*FSDLpCC ID""" +
                """\[([^\]]*)]""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?"""
        )

    // Regex for EUTRA UL Feature Set -> FSpCC ID mappings
    private val reEutraUlFs =
        Regex(
            MAIN_CAP +
                """EUTRA UL FS\[(\d+)],\s*FSULpCC ID""" +
                """\[([^\]]*)]""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?""" +
                """(?:\[([^\]]*)])?"""
        )

    // Regex for FSC definitions
    // Old format: [MAIN][CAP] FSC[1], band num[1], D/U[...][...]
    // New format: [CAP] FSC[1], D/U[...][...]
    private val reFsc =
        Regex(MAIN_CAP + """FSC\[(\d+)],\s*(?:band num\[\d+],\s*)?D/U((?:\[[^\]]+/[^\]]+])+)""")
    private val reDuPair = Regex("""\[([^/]+)/([^\]]+)]""")

    // Regex for combo lines - supports old, new, and realme 7 5G trace formats:
    // New: [CAP]CA idx [0] NL1 bc, num[1] DL: ... UL: ... FSC[1], BC info[...]
    // Old: [MAIN][CAP] CA idx [1], NL1 CA band comb, band num[1], DL: ..., UL: ..., FSC[2]
    // realme 7 5G: [MAIN][CAP] NL1 CA band comb, band num[1], DL: ..., UL: ..., FSC[1]
    private val reComboNew =
        Regex(
            CAP +
                """CA idx \[(\d+)] NL1 bc, num\[(\d+)]\s+""" +
                """DL:\s+([\w_]+)\s+UL:\s+([\w_]+)\s+FSC\[(\d+)]"""
        )
    private val reComboOld =
        Regex(
            MAIN_CAP +
                """CA idx \[(\d+)],\s*NL1 CA band comb,\s*band num\[(\d+)],\s*""" +
                """DL:\s+([\w_]+),\s*UL:\s+([\w_]+),\s*FSC\[(\d+)]"""
        )
    private val reComboNoIdx =
        Regex(
            MAIN_CAP +
                """()NL1 CA band comb,\s*band num\[(\d+)],\s*""" +
                """DL:\s+([\w_]+),\s*UL:\s+([\w_]+),\s*FSC\[(\d+)]"""
        )

    // Regex for individual band component in DL/UL strings
    private val reBandComponent = Regex("""(B|N)(\d+)([A-Z])""")

    // Regex for FS reference strings like 'N7', 'E1', '_0'
    private val reFsRef = Regex("""([NE])(\d+)""")

    override fun parse(input: InputSource): Capabilities {
        val capabilities = Capabilities()
        val lines = input.readLines()

        val parsed = parseTraceLog(lines)
        val combos = buildCombos(parsed)

        capabilities.enDcCombos = combos.filterIsInstance<ComboEnDc>()
        capabilities.nrCombos = combos.filterIsInstance<ComboNr>()
        capabilities.nrDcCombos = combos.filterIsInstance<ComboNrDc>()

        return capabilities
    }

    /**
     * Data class holding a parsed FSC entry with variants.
     *
     * Some devices emit multiple FSC lines with the same FSC number but different FS references.
     * Each variant is a FeatureIndex
     */
    private data class FscEntry(val variants: List<List<FeatureIndex>>)

    /** Container for all parsed trace data. */
    private data class ParsedTrace(
        val nrDlFspcc: Map<Int, FeaturePerCCNr>,
        val nrUlFspcc: Map<Int, FeaturePerCCNr>,
        val eutraDlFspcc: Map<Int, FeaturePerCCLte>,
        val eutraUlFspcc: Map<Int, FeaturePerCCLte>,
        val nrDlFs: Map<Int, List<Int>>,
        val nrUlFs: Map<Int, List<Int>>,
        val eutraDlFs: Map<Int, List<Int>>,
        val eutraUlFs: Map<Int, List<Int>>,
        val fscDefs: Map<Int, FscEntry>,
        val combos: List<ICombo>,
    )

    /** Parse all capability structures from the trace log lines. */
    private fun parseTraceLog(lines: List<String>): ParsedTrace {
        val nrDlFspcc = mutableMapOf<Int, FeaturePerCCNr>()
        val nrUlFspcc = mutableMapOf<Int, FeaturePerCCNr>()
        val eutraDlFspcc = mutableMapOf<Int, FeaturePerCCLte>()
        val eutraUlFspcc = mutableMapOf<Int, FeaturePerCCLte>()
        val nrDlFs = mutableMapOf<Int, List<Int>>()
        val nrUlFs = mutableMapOf<Int, List<Int>>()
        val eutraDlFs = mutableMapOf<Int, List<Int>>()
        val eutraUlFs = mutableMapOf<Int, List<Int>>()
        val fscDefs = mutableMapOf<Int, FscEntry>()
        val combos = mutableListOf<ICombo>()

        for (line in lines) {
            val tag = extractTag(line)

            when (tag) {
                "CA idx" -> parseComboLine(line)?.let { combos.add(it) }
                "NR DL FSpCC" ->
                    parseNrFspccLine(line, LinkDirection.DOWNLINK)?.let { nrDlFspcc += it }
                "NR UL FSpCC" ->
                    parseNrFspccLine(line, LinkDirection.UPLINK)?.let { nrUlFspcc += it }
                "EUTRA DL FSpCC" ->
                    parseEutraFspccLine(line, LinkDirection.DOWNLINK)?.let { eutraDlFspcc += it }
                "EUTRA UL FSpCC" ->
                    parseEutraFspccLine(line, LinkDirection.UPLINK)?.let { eutraUlFspcc += it }
                "NR DL FS" -> parseNrFsLine(line, LinkDirection.DOWNLINK)?.let { nrDlFs += it }
                "NR UL FS" -> parseNrFsLine(line, LinkDirection.UPLINK)?.let { nrUlFs += it }
                "EUTRA DL FS" ->
                    parseEutraFsLine(line, LinkDirection.DOWNLINK)?.let { eutraDlFs += it }
                "EUTRA UL FS" ->
                    parseEutraFsLine(line, LinkDirection.UPLINK)?.let { eutraUlFs += it }
                "FSC" ->
                    parseFscLine(line)?.let { (key, value) ->
                        // Merge current variants with new variants
                        val currentVariants = fscDefs[key]?.variants ?: emptyList()
                        fscDefs[key] = FscEntry(currentVariants + listOf(value))
                    }
                else -> {
                    // do nothing
                }
            }
        }

        return ParsedTrace(
            nrDlFspcc,
            nrUlFspcc,
            eutraDlFspcc,
            eutraUlFspcc,
            nrDlFs,
            nrUlFs,
            eutraDlFs,
            eutraUlFs,
            fscDefs,
            combos,
        )
    }

    /**
     * Extract the tag, that is reported in the line after the &#91;CAP&#93; header and before the
     * index number. If the line doesn't contain the word &#91;CAP&#93; it returns an empty string.
     *
     * Example: "211987, 156011, 9956508, 16:33:37:615 2026/03/14, 16:33:37:716544 2026/03/14,
     * MOD_NRRC_MAIN, MOD_NRRC_MAIN_BASELINE_TRACE_INFO_H, &#91;MAIN&#93; &#91;CAP&#93; CA idx [17]"
     * -> tag CA idx
     */
    private fun extractTag(line: String): String {
        // Extract text After [MAIN][CAP]/[CAP]
        var tag = line.substringAfter("[CAP]", "")

        // remove [index number] and text after index number
        tag = tag.split("[").first()

        // trim
        tag = tag.trim()

        // realme 7 5G variant omits the leading "CA idx [N]" prefix
        return if (tag.startsWith("NL1 CA band comb")) "CA idx" else tag
    }

    /**
     * Parse a NR DL/UL FSpCC definition line.
     *
     * Return the Pair(Idx, FeaturePerCCNr) or null.
     */
    private fun parseNrFspccLine(
        line: String,
        direction: LinkDirection,
    ): Pair<Int, FeaturePerCCNr>? {
        val regex = if (direction == LinkDirection.DOWNLINK) reNrDlFspcc else reNrUlFspcc
        val m = regex.find(line) ?: return null
        val idx = m.groupValues[1].toInt()
        val scs = parseMtkScs(m.groupValues[2])
        val bwRaw = parseMtkBw(m.groupValues[3])
        val bw90 = m.groupValues[4] == "NL1_CAP_SUPPORT"
        val bw = if (bw90 && bwRaw == 80) 90 else bwRaw
        val feature =
            FeaturePerCCNr(
                type = direction,
                // XXX_ONE_LAYER -> 1, XXX_TWO_LAYER -> 2...
                mimo = Int.fromLiteral(m.groupValues[5]).toMimo(),
                qam = ModulationOrder.of(m.groupValues[6]),
                bw = bw,
                scs = scs,
                channelBW90mhz = bw90,
            )

        return idx to feature
    }

    /**
     * Parse a EUTRA DL/UL FSpCC definition line.
     *
     * Return the Pair(Idx, FeaturePerCCLte) or null.
     */
    private fun parseEutraFspccLine(
        line: String,
        direction: LinkDirection,
    ): Pair<Int, FeaturePerCCLte>? {
        val regex = if (direction == LinkDirection.DOWNLINK) reEutraDlFspcc else reEutraUlFspcc
        val m = regex.find(line) ?: return null
        val idx = m.groupValues[1].toInt()
        // XXX_ONE_LAYER -> 1, XXX_TWO_LAYER -> 2...
        val mimo = Int.fromLiteral(m.groupValues[2]).toMimo()
        val feature = FeaturePerCCLte(type = direction, mimo = mimo)

        return idx to feature
    }

    /**
     * Parse a NR DL/UL FS -> FSpCC ID mapping line.
     *
     * Return the Pair(FSNum, id List) or null.
     */
    private fun parseNrFsLine(line: String, direction: LinkDirection): Pair<Int, List<Int>>? {
        val regex = if (direction == LinkDirection.DOWNLINK) reNrDlFs else reNrUlFs
        val endGroup = if (direction == LinkDirection.DOWNLINK) 9 else 5
        val m = regex.find(line) ?: return null
        val fsNum = m.groupValues[1].toInt()
        val ids = extractFspccIds(m, startGroup = 2, endGroup = endGroup)

        return fsNum to ids
    }

    /**
     * Parse an EUTRA DL/UL FS -> FSpCC ID mapping line.
     *
     * Return the Pair(FSNum, id List) or null.
     */
    private fun parseEutraFsLine(line: String, direction: LinkDirection): Pair<Int, List<Int>>? {
        val regex = if (direction == LinkDirection.DOWNLINK) reEutraDlFs else reEutraUlFs
        val m = regex.find(line) ?: return null
        val fsNum = m.groupValues[1].toInt()
        val ids = extractFspccIds(m, startGroup = 2, endGroup = 6)

        return fsNum to ids
    }

    /**
     * Parse an FSC definition line.
     *
     * Return a Pair FSC Num -> FeatureIndex list.
     */
    private fun parseFscLine(line: String): Pair<Int, List<FeatureIndex>>? {
        val m = reFsc.find(line) ?: return null
        val fscNum = m.groupValues[1].toInt()

        val pairsStr = m.groupValues[2]
        val indexesList = mutableListOf<FeatureIndex>()
        for (pm in reDuPair.findAll(pairsStr)) {
            val dl = pm.groupValues[1].trim()
            val ul = pm.groupValues[2].trim()
            if (dl == "_0" && ul == "_0") continue // Skip empty slots
            val (dlType, dlIndex) = parseFsRef(dl)
            val (ulType, ulIndex) = parseFsRef(ul)
            val isNr = dlType == 'N' || ulType == 'N'

            val featureIndex = FeatureIndex(isNr, dlIndex, ulIndex)
            indexesList.add(featureIndex)
        }

        return fscNum to indexesList
    }

    /**
     * Parse a combo definition line. Tries new format first, then old format, then the no-CA-idx
     * format.
     *
     * Return the parsed ICombo or null.
     */
    private fun parseComboLine(line: String): ICombo? {
        val m =
            reComboNew.find(line) ?: reComboOld.find(line) ?: reComboNoIdx.find(line) ?: return null
        val dlStr = m.groupValues[3]
        val ulStr = m.groupValues[4]
        val fscNum = m.groupValues[5].toInt()

        val components = parseComboComponents(dlStr, ulStr)
        if (components.isNotEmpty()) {
            val lteComponents = components.filterIsInstance<ComponentLte>()
            val nrComponents = components.filterIsInstance<ComponentNr>()
            val combo =
                if (lteComponents.isEmpty()) {
                    ComboNr(nrComponents, featureSet = fscNum)
                } else {
                    ComboEnDc(lteComponents, nrComponents, featureSet = fscNum)
                }
            return combo
        }
        return null
    }

    /**
     * Parse DL and UL component strings into a merged list of components.
     *
     * DL and UL strings are underscore-separated with positional matching. For example: DL:
     * B1A_B3A_N78A__0_... UL: B1A__0__N78A__0_...
     */
    private fun parseComboComponents(dlStr: String, ulStr: String): List<IComponent> {
        val dlParts = dlStr.split("_")
        val ulParts = ulStr.split("_")

        // Parse DL components with their positions
        data class DlEntry(val isNr: Boolean, val band: Int, val bwClass: Char, val pos: Int)

        val dlEntries = mutableListOf<DlEntry>()
        for ((i, part) in dlParts.withIndex()) {
            if (part.isEmpty() || part == "0") continue
            val bm = reBandComponent.matchEntire(part) ?: continue
            dlEntries.add(
                DlEntry(
                    isNr = bm.groupValues[1] == "N",
                    band = bm.groupValues[2].toInt(),
                    bwClass = bm.groupValues[3][0],
                    pos = i,
                )
            )
        }

        // Parse UL components with their positions
        data class UlEntry(val isNr: Boolean, val band: Int, val bwClass: Char, val pos: Int)

        val ulEntries = mutableListOf<UlEntry>()
        for ((i, part) in ulParts.withIndex()) {
            if (part.isEmpty() || part == "0") continue
            val bm = reBandComponent.matchEntire(part) ?: continue
            ulEntries.add(
                UlEntry(
                    isNr = bm.groupValues[1] == "N",
                    band = bm.groupValues[2].toInt(),
                    bwClass = bm.groupValues[3][0],
                    pos = i,
                )
            )
        }

        // Merge: for each DL component, find matching UL by position first, then by band
        val result = mutableListOf<IComponent>()
        val usedUl = mutableSetOf<Int>() // indices into ulEntries that have been matched

        for (dl in dlEntries) {
            // Try positional match first
            var ulBwClass: Char? = null
            val posMatch =
                ulEntries.indexOfFirst {
                    it.pos == dl.pos && it !in usedUl.map { idx -> ulEntries[idx] }
                }
            if (
                posMatch >= 0 &&
                    ulEntries[posMatch].band == dl.band &&
                    ulEntries[posMatch].isNr == dl.isNr
            ) {
                ulBwClass = ulEntries[posMatch].bwClass
                usedUl.add(posMatch)
            } else {
                // Try matching by band (first unmatched UL with same band and type)
                val bandMatch =
                    ulEntries.withIndex().firstOrNull { (idx, ul) ->
                        idx !in usedUl && ul.band == dl.band && ul.isNr == dl.isNr
                    }
                if (bandMatch != null) {
                    ulBwClass = bandMatch.value.bwClass
                    usedUl.add(bandMatch.index)
                }
            }

            val classDlBw = BwClass.valueOf(dl.bwClass.toString())
            val classUlBw =
                if (ulBwClass != null) BwClass.valueOf(ulBwClass.toString()) else BwClass.NONE

            if (dl.isNr) {
                result.add(ComponentNr(dl.band, classDlBw, classUlBw))
            } else {
                val mimoUl = if (classUlBw != BwClass.NONE) 1.toMimo() else EmptyMimo
                result.add(ComponentLte(dl.band, classDlBw, classUlBw, mimoUL = mimoUl))
            }
        }

        return result
    }

    /** Extract FSpCC IDs from a regex match, filtering out zeros. */
    private fun extractFspccIds(match: MatchResult, startGroup: Int, endGroup: Int): List<Int> {
        val ids = mutableListOf<Int>()
        for (i in startGroup..endGroup) {
            val g = match.groupValues.getOrNull(i)
            if (!g.isNullOrEmpty()) {
                val value = g.toIntOrNull() ?: continue
                if (value > 0) ids.add(value)
            }
        }
        return ids
    }

    /** Convert MTK SCS enum string to SCS value in kHz. */
    private fun parseMtkScs(scsStr: String): Int {
        return when (scsStr) {
            "NL1_CAP_SCS_15KHZ" -> 15
            "NL1_CAP_SCS_30KHZ" -> 30
            "NL1_CAP_SCS_60KHZ" -> 60
            "NL1_CAP_SCS_120KHZ" -> 120
            else -> 15
        }
    }

    /** Extract bandwidth integer from strings like 'NL1_CAP_BW50'. */
    private fun parseMtkBw(bwStr: String): Int {
        val m = Regex("""NL1_CAP_BW(\d+)""").find(bwStr)
        return m?.groupValues?.get(1)?.toInt() ?: 0
    }

    /** Parse an FS reference string like 'N7', 'E1', '_0' into type and number. */
    private fun parseFsRef(fsStr: String): Pair<Char?, Int> {
        if (fsStr == "_0" || fsStr == "0" || fsStr == "N0") return Pair(null, 0)
        val m = reFsRef.matchEntire(fsStr) ?: return Pair(null, 0)
        return Pair(m.groupValues[1][0], m.groupValues[2].toInt())
    }

    /** Build Capabilities combos from parsed trace data. */
    private fun buildCombos(parsed: ParsedTrace): List<ICombo> {
        val combos = mutableListWithCapacity<ICombo>(parsed.combos.size)

        for (combo in parsed.combos) {
            val fscEntry = parsed.fscDefs[combo.featureSet] ?: continue
            // Master is before secondary in FSC lines
            val components = combo.masterComponents + combo.secondaryComponents

            // Emit one combo per FSC variant (all variants are listed)
            for (variant in fscEntry.variants) {
                // FSC pairs might not exactly match component count; align by min
                val count = minOf(variant.size, components.size)
                if (count == 0) continue

                val newComponents = mutableListOf<IComponent>()
                for (i in 0 until count) {
                    val comp = components[i]
                    val featureIndex = variant[i]
                    val newComponent = mergeFeatureAndComponent(comp, featureIndex, parsed)
                    newComponents.add(newComponent)
                }

                val newLteComponents = newComponents.filterIsInstance<ComponentLte>()
                val newNrComponents = newComponents.filterIsInstance<ComponentNr>()
                val newCombo = assembleCombo(newLteComponents, newNrComponents)

                if (newCombo != null) combos.add(newCombo)
            }
        }

        return combos
    }

    /** Build an IComponent from parsed component data, resolving features via FSC. */
    private fun mergeFeatureAndComponent(
        comp: IComponent,
        featureIndex: FeatureIndex,
        parsed: ParsedTrace,
    ): IComponent {
        val downlinkIndex = featureIndex.downlinkIndex
        val uplinkIndex = featureIndex.uplinkIndex

        var dlFeatures: List<IFeaturePerCC>? = null
        var ulFeatures: List<IFeaturePerCC>? = null

        if (featureIndex.isNR && comp is ComponentNr) {
            // Resolve DL per-CC features: FS -> FSpCC IDs -> FeaturePerCCNr
            dlFeatures = parsed.nrDlFs[downlinkIndex]?.mapNotNull { parsed.nrDlFspcc[it] }
            // Resolve UL per-CC features
            ulFeatures = parsed.nrUlFs[uplinkIndex]?.mapNotNull { parsed.nrUlFspcc[it] }
        } else if (!featureIndex.isNR && comp is ComponentLte) {
            // Resolve DL features from EUTRA FS
            dlFeatures = parsed.eutraDlFs[downlinkIndex]?.mapNotNull { parsed.eutraDlFspcc[it] }

            // Resolve UL features from EUTRA FS
            ulFeatures = parsed.eutraUlFs[uplinkIndex]?.mapNotNull { parsed.eutraUlFspcc[it] }
        }

        // The function return a copy of component
        return mergeComponentAndFeaturePerCC(comp, dlFeatures, ulFeatures, null)
    }

    /** Assemble the final combo (NR-CA, NR-DC, or EN-DC) from component lists. */
    private fun assembleCombo(
        lteComponents: List<IComponent>,
        nrComponents: List<IComponent>,
    ): ICombo? {
        if (nrComponents.isEmpty() && lteComponents.isEmpty()) return null

        val sortedNr = nrComponents.sortedDescending().filterIsInstance<ComponentNr>()
        val sortedLte = lteComponents.sortedDescending().filterIsInstance<ComponentLte>()

        return if (sortedLte.isEmpty()) {
            // NR-CA or NR-DC
            if (sortedNr.none { it.isFR2 } || sortedNr.none { !it.isFR2 }) {
                ComboNr(sortedNr)
            } else {
                // Both FR1 and FR2 present -> NR-DC
                val (fr2, fr1) = sortedNr.partition { it.isFR2 }
                ComboNrDc(fr1, fr2)
            }
        } else {
            // EN-DC
            ComboEnDc(sortedLte, sortedNr)
        }
    }
}
