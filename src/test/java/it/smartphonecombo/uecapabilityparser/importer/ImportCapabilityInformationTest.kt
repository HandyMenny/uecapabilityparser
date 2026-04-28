package it.smartphonecombo.uecapabilityparser.importer

import org.junit.jupiter.api.Test

internal class ImportCapabilityInformationTest :
    AbstractImportCapabilities(
        ImportCapabilityInformation,
        "src/test/resources/capabilityInformation/",
    ) {
    @Test
    fun ueCapEutraCombinationAdd() {
        parse("ueCapEutraCombinationAdd.json", "ueCapEutraCombinationAdd.json")
    }

    @Test
    fun ueCapEutraCombinationReduced() {
        parse("ueCapEutraCombinationReduced.json", "ueCapEutraCombinationReduced.json")
    }

    @Test
    fun ueCapEutra1024qam() {
        parse("ueCapEutra1024qam.json", "ueCapEutra1024qam.json")
    }

    @Test
    fun ueCapEutraCombinationReduced1024qam() {
        parse(
            "ueCapEutraCombinationReduced1024qam.json",
            "ueCapEutraCombinationReduced1024qam.json",
        )
    }

    @Test
    fun ueCapEutra64qamDLMimoUL() {
        parse("ueCapEutra64qamDLMimoUL.json", "ueCapEutra64qamDLMimoUL.json")
    }

    @Test
    fun ueCapEutra256qamDLMimoUL() {
        parse("ueCapEutra256qamDLMimoUL.json", "ueCapEutra256qamDLMimoUL.json")
    }

    @Test
    fun ueCapEutraCombinationReducedMimoPerCC() {
        parse(
            "ueCapEutraCombinationReducedMimoPerCC.json",
            "ueCapEutraCombinationReducedMimoPerCC.json",
        )
    }

    @Test
    fun ueCapEutraOmitEnDc() {
        parse("ueCapEutraOmitEnDc.json", "ueCapEutraOmitEnDc.json")
    }

    @Test
    fun ueCapEutraRequestDiffFallback() {
        parse("ueCapEutraRequestDiffFallback.json", "ueCapEutraRequestDiffFallback.json")
    }

    @Test
    fun ueCapEutraSegSupported() {
        parse("ueCapEutraSegSupported.json", "ueCapEutraSegSupported.json")
    }

    @Test
    fun ueCapEutraSegNotSupported() {
        parse("ueCapEutraSegNotSupported.json", "ueCapEutraSegNotSupported.json")
    }

    @Test
    fun ueCapNrOneCC() {
        parse("ueCapNrOneCC.json", "ueCapNrOneCC.json")
    }

    @Test
    fun ueCapNrThreeCC() {
        parse("ueCapNrThreeCC.json", "ueCapNrThreeCC.json")
    }

    @Test
    fun ueCapMrdcDefaultBws() {
        parse("ueCapMrdcDefaultBws.json", "ueCapMrdcDefaultBws.json")
    }

    @Test
    fun ueCapMrdcFR2() {
        parse("ueCapMrdcFR2.json", "ueCapMrdcFR2.json")
    }

    @Test
    fun ueCapMrdcExynos() {
        parse("ueCapMrdcExynos.json", "ueCapMrdcExynos.json")
    }

    @Test
    fun ueCapMrdcIntraEnDc() {
        parse("ueCapMrdcIntraEnDc.json", "ueCapMrdcIntraEnDc.json")
    }

    @Test
    fun ueCapMrdcIntraEnDcV1590() {
        parse("ueCapMrdcIntraEnDcV1590.json", "ueCapMrdcIntraEnDcV1590.json")
    }

    @Test
    fun ueCapMrdcFake90mhz() {
        parse("ueCapMrdcFake90mhz.json", "ueCapMrdcFake90mhz.json")
    }

    @Test
    fun ueCapEutraNrOnlyIntraBcsAll() {
        parse("ueCapEutraNrOnlyIntraBcsAll.json", "ueCapEutraNrOnlyIntraBcsAll.json")
    }

    @Test
    fun ueCapNrDc() {
        parse("ueCapNrDc.json", "ueCapNrDc.json")
    }

    @Test
    fun ueCapNrDcShannon() {
        parse("ueCapNrDcShannon.json", "ueCapNrDcShannon.json")
    }

    @Test
    fun ueCapNrCaMmWave() {
        parse("ueCapNrCaMmWave.json", "ueCapNrCaMmWave.json")
    }

    @Test
    fun ueCapNrSUL() {
        parse("ueCapNrSUL.json", "ueCapNrSUL.json")
    }

    @Test
    fun ueCapNrOmitEnDc() {
        parse("ueCapNrOmitEnDc.json", "ueCapNrOmitEnDc.json")
    }

    @Test
    fun ueCapNrBwRequested() {
        parse("ueCapNrBwRequested.json", "ueCapNrBwRequested.json")
    }

    @Test
    fun ueCapNrUnfiltered() {
        parse("ueCapNrUnfiltered.json", "ueCapNrUnfiltered.json")
    }

    @Test
    fun ueCapNrSegAllowed() {
        parse("ueCapNrSegAllowed.json", "ueCapNrSegAllowed.json")
    }

    @Test
    fun ueCapNrDc1024qam() {
        parse("ueCapNrDc1024qam.json", "ueCapNrDc1024qam.json")
    }

    @Test
    fun ueCapNr35MHzR15() {
        parse("ueCapNr35MHzR15.json", "ueCapNr35MHzR15.json")
    }

    @Test
    fun ueRedCapSegNotSupported() {
        parse("ueRedCapSegNotSupported.json", "ueRedCapSegNotSupported.json")
    }

    @Test
    fun ueRedCapFDD() {
        parse("ueRedCapFDD.json", "ueRedCapFDD.json")
    }

    @Test
    fun ueRedCapTDD() {
        parse("ueRedCapTDD.json", "ueRedCapTDD.json")
    }

    @Test
    fun ueCapNrUlTxSwitchR16() {
        parse("ueCapNrUlTxSwitchR16.json", "ueCapNrUlTxSwitchR16.json")
    }

    @Test
    fun ueCapNrUlTxSwitchR16_2() {
        parse("ueCapNrUlTxSwitchR16_2.json", "ueCapNrUlTxSwitchR16_2.json")
    }

    @Test
    fun ueCapNrUlTxSwitchR17R18() {
        parse("ueCapNrUlTxSwitchR17R18.json", "ueCapNrUlTxSwitchR17R18.json")
    }

    @Test
    fun ueCapNrWithEutraAndMrdc() {
        parse("ueCapNrWithEutraAndMrdc.json", "ueCapNrWithEutraAndMrdc.json")
    }
}
