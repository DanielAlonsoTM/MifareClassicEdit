package com.samurai.mifareclassicedit.mifare

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import com.samurai.mifareclassicedit.exceptions.AuthenticateException
import java.io.IOException
import kotlin.experimental.inv


class MifareManager(tag: Tag?) {
    private var currentAuthSector = 99
    private val mfc: MifareClassic = MifareClassic.get(tag)

    companion object {
        private val KEY_B_SECTOR: Array<ByteArray> = arrayOf(
            byteArrayOf(31, -62, 53, -84, 19, 9),
            byteArrayOf(36, 63, 22, 9, 24, -47),
            byteArrayOf(-102, -4, 66, 55, 42, -15),
            byteArrayOf(104, 45, 64, 26, -69, 9),
            byteArrayOf(6, 125, -76, 84, 84, -87),
            byteArrayOf(21, -4, 76, 118, 19, -2),
            byteArrayOf(104, -45, 2, -120, -111, 10),
            byteArrayOf(-11, -102, 54, -94, 84, 109),
            byteArrayOf(100, -29, -63, 3, -108, -62),
            byteArrayOf(-73, 54, 65, 38, 20, -81),
            byteArrayOf(50, 79, 93, -10, 83, 16),
            byteArrayOf(100, 63, -74, -34, 34, 23),
            byteArrayOf(-126, -12, 53, -34, -33, 1),
            byteArrayOf(2, 99, -34, 18, 120, -13),
            byteArrayOf(81, 40, 76, 54, -122, -90),
            byteArrayOf(106, 71, 13, 84, 18, 124)
        )
        private const val NO_SECTOR = 99
        private const val TOTAL_BLOCKS_BY_SECTOR = 4

    }

    @Throws(IOException::class, AuthenticateException::class)
    fun authenticateSector(n: Int) {
        if (!mfc.authenticateSectorWithKeyB(n, KEY_B_SECTOR[n])) {
            throw AuthenticateException(n)
        }
    }

    @Throws(IOException::class, AuthenticateException::class)
    fun checkAuthSectorByBlock(currentAuthSector: Int) {
        var authSector = currentAuthSector
        authSector /= 4
        if (this.currentAuthSector != authSector) {
            authenticateSector(authSector)
            this.currentAuthSector = authSector
        }
    }

    fun close() {
        try {
            mfc.close()
        } catch (ex: IOException) {
        }
    }

    val tag: Tag
        get() = mfc.tag

    @Throws(IOException::class)
    fun open() {
        mfc.connect()
    }

    @Throws(IOException::class, AuthenticateException::class)
    fun readBlock(n: Int): ByteArray {
        checkAuthSectorByBlock(n)
        return mfc.readBlock(n)
    }

    @Throws(IOException::class, AuthenticateException::class)
    fun writeBlock(n: Int, array: ByteArray?) {
        checkAuthSectorByBlock(n)
        mfc.writeBlock(n, array)
    }

    @Throws(IOException::class, AuthenticateException::class)
    fun writeValue(n: Int, n2: Int) {
        val block = readBlock(n)
        block[0] = (n2 ushr 0).toByte()
        block[1] = (n2 ushr 8).toByte()
        block[2] = (n2 ushr 16).toByte()
        block[3] = (n2 ushr 24).toByte()
        block[4] = block[0].inv()
        block[5] = block[1].inv()
        block[6] = block[2].inv()
        block[7] = block[3].inv()
        block[8] = block[0]
        block[9] = block[1]
        block[10] = block[2]
        block[11] = block[3]
        writeBlock(n, block)
    }

}
