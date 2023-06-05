package it.smartphonecombo.uecapabilityparser.model.band

import it.smartphonecombo.uecapabilityparser.extension.Band
import it.smartphonecombo.uecapabilityparser.model.Duplex

object DuplexBandTable {

    // From 3gpp 36.101 V17.8.0
    private val lteData: Map<Band, Duplex> =
        mapOf(
            1 to Duplex.FDD,
            2 to Duplex.FDD,
            3 to Duplex.FDD,
            4 to Duplex.FDD,
            5 to Duplex.FDD,
            7 to Duplex.FDD,
            8 to Duplex.FDD,
            9 to Duplex.FDD,
            10 to Duplex.FDD,
            11 to Duplex.FDD,
            12 to Duplex.FDD,
            13 to Duplex.FDD,
            14 to Duplex.FDD,
            17 to Duplex.FDD,
            18 to Duplex.FDD,
            19 to Duplex.FDD,
            20 to Duplex.FDD,
            21 to Duplex.FDD,
            22 to Duplex.FDD,
            24 to Duplex.FDD,
            25 to Duplex.FDD,
            26 to Duplex.FDD,
            27 to Duplex.FDD,
            28 to Duplex.FDD,
            29 to Duplex.SDL,
            30 to Duplex.FDD,
            31 to Duplex.FDD,
            32 to Duplex.SDL,
            33 to Duplex.TDD,
            34 to Duplex.TDD,
            35 to Duplex.TDD,
            36 to Duplex.TDD,
            37 to Duplex.TDD,
            38 to Duplex.TDD,
            39 to Duplex.TDD,
            40 to Duplex.TDD,
            41 to Duplex.TDD,
            42 to Duplex.TDD,
            43 to Duplex.TDD,
            44 to Duplex.TDD,
            45 to Duplex.TDD,
            46 to Duplex.TDD,
            47 to Duplex.TDD,
            48 to Duplex.TDD,
            49 to Duplex.TDD,
            50 to Duplex.TDD,
            51 to Duplex.TDD,
            52 to Duplex.TDD,
            53 to Duplex.TDD,
            65 to Duplex.FDD,
            66 to Duplex.FDD,
            67 to Duplex.SDL,
            68 to Duplex.FDD,
            69 to Duplex.SDL,
            70 to Duplex.FDD,
            71 to Duplex.FDD,
            72 to Duplex.FDD,
            73 to Duplex.FDD,
            74 to Duplex.FDD,
            75 to Duplex.SDL,
            76 to Duplex.SDL,
            85 to Duplex.FDD,
            87 to Duplex.FDD,
            88 to Duplex.FDD,
        )

    // From 3gpp 38.104 V17.8.0
    private val nrData: Map<Band, Duplex> =
        mapOf(
            1 to Duplex.FDD,
            2 to Duplex.FDD,
            3 to Duplex.FDD,
            5 to Duplex.FDD,
            7 to Duplex.FDD,
            8 to Duplex.FDD,
            12 to Duplex.FDD,
            13 to Duplex.FDD,
            14 to Duplex.FDD,
            18 to Duplex.FDD,
            20 to Duplex.FDD,
            24 to Duplex.FDD,
            25 to Duplex.FDD,
            26 to Duplex.FDD,
            28 to Duplex.FDD,
            29 to Duplex.SDL,
            30 to Duplex.FDD,
            34 to Duplex.TDD,
            38 to Duplex.TDD,
            39 to Duplex.TDD,
            40 to Duplex.TDD,
            41 to Duplex.TDD,
            46 to Duplex.TDD,
            48 to Duplex.TDD,
            50 to Duplex.TDD,
            51 to Duplex.TDD,
            53 to Duplex.TDD,
            65 to Duplex.FDD,
            66 to Duplex.FDD,
            67 to Duplex.SDL,
            70 to Duplex.FDD,
            71 to Duplex.FDD,
            74 to Duplex.FDD,
            75 to Duplex.SDL,
            76 to Duplex.SDL,
            77 to Duplex.TDD,
            78 to Duplex.TDD,
            79 to Duplex.TDD,
            80 to Duplex.SUL,
            81 to Duplex.SUL,
            82 to Duplex.SUL,
            83 to Duplex.SUL,
            84 to Duplex.SUL,
            85 to Duplex.FDD,
            86 to Duplex.SUL,
            89 to Duplex.SUL,
            90 to Duplex.TDD,
            91 to Duplex.FDD,
            92 to Duplex.FDD,
            93 to Duplex.FDD,
            94 to Duplex.FDD,
            95 to Duplex.SUL,
            96 to Duplex.TDD,
            97 to Duplex.SUL,
            98 to Duplex.SUL,
            99 to Duplex.SUL,
            100 to Duplex.FDD,
            101 to Duplex.TDD,
            102 to Duplex.TDD,
            104 to Duplex.TDD,
            257 to Duplex.TDD,
            258 to Duplex.TDD,
            259 to Duplex.TDD,
            260 to Duplex.TDD,
            261 to Duplex.TDD,
            262 to Duplex.TDD,
            263 to Duplex.TDD,
        )

    fun getLteDuplex(band: Band): Duplex = lteData[band] ?: Duplex.NONE

    fun getNrDuplex(band: Band): Duplex = nrData[band] ?: Duplex.NONE
}
