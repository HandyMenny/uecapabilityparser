package it.smartphonecombo.uecapabilityparser.importer.nr

import it.smartphonecombo.uecapabilityparser.bean.Capabilities
import it.smartphonecombo.uecapabilityparser.bean.IComponent
import it.smartphonecombo.uecapabilityparser.bean.lte.ComponentLte
import it.smartphonecombo.uecapabilityparser.bean.nr.ComboNr
import it.smartphonecombo.uecapabilityparser.bean.nr.ComponentNr
import it.smartphonecombo.uecapabilityparser.extension.readUnsignedByte
import it.smartphonecombo.uecapabilityparser.extension.readUnsignedShort
import it.smartphonecombo.uecapabilityparser.extension.skipBytes
import it.smartphonecombo.uecapabilityparser.extension.toBwClass
import it.smartphonecombo.uecapabilityparser.importer.ImportCapabilities
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Import0xB826 : ImportCapabilities {
    override fun parse(input: InputStream): Capabilities {
        val combos = Capabilities()
        val listCombo = ArrayList<ComboNr>()
        val byteArray = input.use(InputStream::readBytes)
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        var fileSize = byteBuffer.readUnsignedShort()
        if (fileSize == byteBuffer.limit()) {
            val logItem = "0x" + Integer.toHexString(byteBuffer.readUnsignedShort()).uppercase()
            combos.setMetadata("logItem", logItem)
            if (debug) {
                println("Log Item: $logItem")
            }
            byteBuffer.skipBytes(8)
        } else {
            fileSize = byteBuffer.limit()
            byteBuffer.rewind()
        }
        combos.setMetadata("logSize", fileSize)
        if (debug) {
            println("Log file size: $fileSize bytes")
        }

        val version = byteBuffer.readUnsignedShort()
        combos.setMetadata("version", version)
        if (debug) {
            println("Version $version\n")
        }

        byteBuffer.skipBytes(2)
        var numCombos = byteBuffer.readUnsignedShort()
        if (version > 3) {
            if (debug) {
                println("Total Numb Combos $numCombos\n")
            }
            combos.setMetadata("totalCombos", numCombos)
            val index = byteBuffer.readUnsignedShort()
            combos.setMetadata("index", index)
            if (debug) {
                println("Index $index\n")
            }
            numCombos = byteBuffer.readUnsignedShort()
        }
        if (debug) {
            println("Num Combos $numCombos\n")
        }
        combos.setMetadata("numCombos", numCombos)

        var source: String? = null
        if (version > 3) {
            // Parse source field
            val sourceIndex = byteBuffer.readUnsignedByte()
            source = getSourceFromIndex(sourceIndex)
            combos.setMetadata("source", source)
            if (debug) {
                println("source $source\n")
            }
        }

        var comboN = 0
        while (comboN < numCombos && byteBuffer.remaining() > 0) {
            val combo = parseCombo(byteBuffer, version, source)
            listCombo.add(combo)
            comboN++
        }

        if (debug) {
            println(listCombo)
        }

        if (listCombo.isNotEmpty()) {
            if (listCombo.first().isEnDc) {
                combos.enDcCombos = listCombo
            } else if (listCombo.first().isNrDc) {
                combos.nrDcCombos = listCombo
            } else {
                combos.nrCombos = listCombo
            }
        }
        return combos
    }

    private fun parseCombo(
        byteBuffer: ByteBuffer,
        version: Int,
        source: String?,
    ): ComboNr {
        var endc = false
        var nrdc = false
        if (version >= 8) {
            byteBuffer.skipBytes(3)
        }
        var numBands = byteBuffer.readUnsignedByte()
        numBands =
            when (version) {
                1,
                2 -> numBands
                in 3..7 -> numBands ushr 1
                else -> (numBands ushr 3) and 0x0F
            }
        val bands = mutableListOf<IComponent>()
        var nrbands = mutableListOf<IComponent>()
        var nrdcbands = mutableListOf<IComponent>()
        when (version) {
            6,
            8 -> byteBuffer.skipBytes(1)
            7 -> byteBuffer.skipBytes(3)
            in 9..13 -> byteBuffer.skipBytes(9)
            14 -> byteBuffer.skipBytes(25)
        }
        for (i in 0 until numBands) {
            var band: Int
            val mixed = byteBuffer.readUnsignedShort()
            var temp: Int
            if (version >= 8) {
                temp = mixed ushr 9 and 0x1F
                band = mixed and 0x1FF
            } else {
                band = mixed
                temp = byteBuffer.readUnsignedByte()
            }
            val bwclass = (temp ushr 1).toBwClass()
            val isNr = temp % 2 == 1

            val component =
                if (isNr) {
                    ComponentNr(band)
                } else {
                    ComponentLte(band)
                }
            component.classDL = bwclass
            if (version >= 8) {
                temp = byteBuffer.readUnsignedByte()
                var mimo = temp shl 1
                mimo = mimo and 0x7F
                mimo += mixed ushr 15
                component.mimoDL = getMimoFromIndex(mimo)
                temp = temp ushr 6
                val temp2 = byteBuffer.readUnsignedByte()
                temp += temp2 shl 2
                temp = temp and 0x1F
                val mimoUL = (temp2 ushr 3) and 0x7F
                component.classUL = temp.toBwClass()
                component.mimoUL = getMimoFromIndex(mimoUL)
                temp = byteBuffer.readUnsignedByte()
                val modUL = (temp shr 1) and 0x3
                component.modUL = getQamFromIndex(modUL)
            } else {
                component.mimoDL = getMimoFromIndex(byteBuffer.readUnsignedByte())
                temp = byteBuffer.readUnsignedByte() ushr 1
                component.classUL = temp.toBwClass()
                val mimoUL = byteBuffer.readUnsignedByte()
                component.mimoUL = getMimoFromIndex(mimoUL)
                temp = byteBuffer.readUnsignedByte()
                val modUL = temp
                component.modUL = getQamFromIndex(modUL)
            }

            if (isNr) {
                val nrband = component as ComponentNr
                if (version >= 8) {
                    var scsIndex = temp
                    temp = byteBuffer.readUnsignedByte()
                    scsIndex = scsIndex ushr 7
                    scsIndex += temp and 3 shl 1
                    nrband.scs = 15 * (1 shl (scsIndex and 0x000F) - 1)
                    val maxBWindex = temp shr 2 and 0x1F
                    nrband.maxBandwidth = getBWFromIndexV8(maxBWindex)
                    byteBuffer.skipBytes(2)
                } else if (version >= 6) {
                    byteBuffer.skipBytes(1)
                    temp = byteBuffer.readUnsignedShort()
                    val scsIndex = temp
                    nrband.scs = 15 * (1 shl (scsIndex and 0x000F) - 1)
                    nrband.maxBandwidth = getBWFromIndex(temp ushr 6 and 0x1F)
                } else if (version > 2) {
                    byteBuffer.skipBytes(1)
                    nrband.scs = 15 * (1 shl byteBuffer.readUnsignedByte() - 1)
                    nrband.maxBandwidth = byteBuffer.readUnsignedByte() shl 2
                } else {
                    byteBuffer.skipBytes(1)
                    nrband.scs = 15 * (1 + byteBuffer.readUnsignedByte())
                    nrband.maxBandwidth = byteBuffer.readUnsignedByte() shl 2
                }
                nrbands.add(component)
            } else {
                endc = true
                byteBuffer.skipBytes(3)
                bands.add(component)
            }
        }
        /*
         * We assume that 0xb826 without explicit combo type in source
         * don't support NR CA FR1-FR2.
         */
        if (!endc && !source.equals("RF_NRCA")) {
            val (fr2bands, fr1bands) = nrbands.partition { (it as ComponentNr).isFR2 }

            if (fr2bands.isNotEmpty() && fr1bands.isNotEmpty()) {
                nrdc = true
                nrbands = fr1bands.toMutableList()
                nrdcbands = fr2bands.toMutableList()
            }
        }

        val bandArray = bands.sortedWith(IComponent.defaultComparator.reversed()).toTypedArray()
        val nrbandsArray =
            nrbands.sortedWith(IComponent.defaultComparator.reversed()).toTypedArray()
        val nrdcbandsArray =
            nrdcbands.sortedWith(IComponent.defaultComparator.reversed()).toTypedArray()
        return if (endc) {
            ComboNr(bandArray, nrbandsArray)
        } else if (nrdc) {
            ComboNr(nrbandsArray, nrdcbandsArray)
        } else {
            ComboNr(nrbandsArray)
        }
    }

    private fun getMimoFromIndex(index: Int): Int {
        return when (index) {
            0 -> 0
            1,
            25,
            16,
            9,
            4 -> 1
            2,
            42,
            56,
            72,
            26,
            27,
            28,
            29,
            30,
            17,
            18,
            19,
            20,
            10,
            11,
            12,
            5,
            6 -> 2
            3,
            31,
            32,
            33,
            34,
            35,
            21,
            22,
            23,
            24,
            13,
            14,
            15,
            7,
            8 -> 4
            else -> index
        }
    }

    private fun getQamFromIndex(index: Int): String {
        return when (index) {
            2,
            5 -> "256qam"
            3,
            6 -> "1024qam"
            else -> "64qam"
        }
    }

    private fun getBWFromIndexV8(index: Int): Int {
        return when (index) {
            0 -> 5
            1,
            2 -> 10
            3 -> 15
            4,
            5 -> 20
            8,
            9 -> 25
            10 -> 30
            11 -> 40
            12,
            13 -> 50
            17 -> 60
            18 -> 70
            19,
            20 -> 80
            21,
            22,
            23,
            24,
            25,
            26,
            27,
            28,
            29,
            30,
            31 -> 100
            else -> index
        }
    }

    private fun getBWFromIndex(index: Int): Int {
        return when (index) {
            4 -> 5
            5 -> 10
            6 -> 15
            7 -> 20
            8 -> 25
            9 -> 30
            10 -> 40
            11,
            15 -> 50
            12 -> 60
            13 -> 80
            14,
            20,
            21,
            22,
            23,
            24,
            25,
            26 -> 100
            else -> index
        }
    }

    private fun getSourceFromIndex(index: Int): String {
        return when (index) {
            0 -> "RF"
            1 -> "PM"
            3 -> "RF_ENDC"
            4 -> "RF_NRCA"
            5 -> "RF_NRDC"
            else -> index.toString()
        }
    }
}
