@file:OptIn(ExperimentalSerializationApi::class)

package it.smartphonecombo.uecapabilityparser.model.shannon

import it.smartphonecombo.uecapabilityparser.model.BCS
import it.smartphonecombo.uecapabilityparser.model.PowerClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@SerialName("ComboGroup")
data class ComboGroup(
    /** Some features that applies to the whole combo group. */
    @ProtoNumber(1) val comboFeatures: ComboFeatures,
    /** List of combos that share the same [ComboFeatures]. */
    @ProtoNumber(2) val combos: List<ShannonCombo> = emptyList(),
)

@Serializable
@SerialName("Combo")
data class ShannonCombo(
    /** List of Components. */
    @ProtoNumber(1) val components: List<ShannonComponent> = emptyList(),
    /** A bit mask stored as unsigned int that enables or disables some features. */
    @ProtoNumber(2) val bitMask: UInt
)

@Serializable
@SerialName("ComboFeatures")
data class ComboFeatures(
    /**
     * The supportedBandwidthCombinationSet that applies to the Nr Components.
     *
     * It's stored as a 32bit unsigned int, each of its bits has the same value of the corresponding
     * bit in the BitString.
     */
    @ProtoNumber(1) @SerialName("bcsNr") private val rawBcsNr: UInt?,

    /**
     * The supportedBandwidthCombinationSet that applies to the IntraEnDc Components
     * (supportedBandwidthCombinationSetIntraENDC).
     *
     * It's stored as a 32bit unsigned int, each of its bits has the same value of the corresponding
     * bit in the BitString.
     */
    @ProtoNumber(2) @SerialName("bcsIntraEndc") private val rawBcsIntraEndc: UInt?,

    /**
     * The supported Bandwidth Combination Set that applies to the Eutra Components
     * (supportedBandwidthCombinationSetEUTRA-v1530).
     *
     * It's stored as a 32bit unsigned int, each of its bits has the same value of the corresponding
     * bit in the BitString.
     */
    @ProtoNumber(3) @SerialName("bcsEutra") private val rawBcsEutra: UInt?,

    /**
     * Power Class of the whole combination, it's stored as an enum.
     *
     * Note that this doesn't override the powerclass of the uplink bands.
     *
     * For FR1 0 -> Default, 1 -> PC2, 2 -> PC1.5
     *
     * For FR2 0 -> Default
     */
    @ProtoNumber(4) @SerialName("powerClass") private val rawPowerClass: Int?,

    /**
     * intraBandENDC-Support is stored as an enum.
     *
     * 0 -> contiguous, 1 -> non-contiguous, 2 -> both.
     */
    @ProtoNumber(5) @SerialName("intraBandEnDcSupport") private val rawIntraBandEnDcSupport: Int?
) {
    val bcsNr
        get() = BCS.fromBinaryString(rawBcsNr?.toString(2) ?: "0")

    val bcsIntraEndc
        get() = BCS.fromBinaryString(rawBcsIntraEndc?.toString(2) ?: "0")

    val bcsEutra
        get() = BCS.fromBinaryString(rawBcsIntraEndc?.toString(2) ?: "0")

    val powerClass
        get() =
            when (rawPowerClass) {
                1 -> PowerClass.PC2
                2 -> PowerClass.PC1dot5
                else -> PowerClass.NONE
            }
}
