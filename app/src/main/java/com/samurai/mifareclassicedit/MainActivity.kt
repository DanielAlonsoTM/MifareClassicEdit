package com.samurai.mifareclassicedit

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.samurai.mifareclassicedit.utilities.Utils.leToNumeric
import com.samurai.mifareclassicedit.utilities.Utils.leToNumericString
import com.samurai.mifareclassicedit.utilities.Utils.toastMessage
import com.samurai.mifareclassicedit.exceptions.AuthenticateException
import com.samurai.mifareclassicedit.mifare.MifareManager
import com.samurai.mifareclassicedit.utilities.Utils
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var mFilters: Array<IntentFilter>? = null
    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null
    private var mTechList: Array<Array<String>>? = null

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        this.setContentView(R.layout.activity_main)

        //Initialization of NFC in app
        initNFC()

        //Clear all fields in app
        val buttonClear = findViewById<Button>(R.id.button_clear)
        val editBalance = findViewById<EditText>(R.id.edit_text_balance)
        val editCardNumber = findViewById<EditText>(R.id.edit_text_number_card)

        buttonClear.setOnClickListener {
            setTextViewFields("-","-","-")
            editBalance.text.clear()
            editCardNumber.text.clear()
        }
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter!!.disableForegroundDispatch(this as Activity)
    }

    override fun onResume() {
        super.onResume()
        mNfcAdapter!!.enableForegroundDispatch(this as Activity, mPendingIntent, mFilters, mTechList)
    }

    private fun initNFC() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this as Context)
        if (mNfcAdapter == null) {
            toastMessage(applicationContext, "This device don't support NFC")
            finish()
        } else {
            if (!mNfcAdapter!!.isEnabled) {
                toastMessage(applicationContext, "Your NFC is disable")
            }
            mPendingIntent = PendingIntent.getActivity(
                this as Context,
                0,
                Intent(this as Context, this.javaClass as Class<*>).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0
            )
            val intentFilter = IntentFilter("android.nfc.action.TECH_DISCOVERED")
            try {
                intentFilter.addDataType("*/*")
                mFilters = arrayOf(intentFilter)
                mTechList =
                    arrayOf(arrayOf(MifareClassic::class.java.name))
            } catch (error: ExceptionInInitializerError) {
                throw RuntimeException("error", error as Throwable)
            }
        }
    }

    @Throws(IOException::class, AuthenticateException::class)
    private fun processingOperation(mifareManager: MifareManager) {
        val numberCard = (findViewById<EditText>(R.id.edit_text_number_card)).text.toString()
        val newBalance = (findViewById<EditText>(R.id.edit_text_balance)).text.toString()

        setCardNumber(numberCard, mifareManager)
        setBalance(newBalance, mifareManager)
    }

    @Throws(IOException::class, AuthenticateException::class)
    private fun showDataFromCard(mifareManager: MifareManager) {
        val block = mifareManager.readBlock(1)

        val chipNumber = leToNumericString(
            mifareManager.tag.id,
            4
        )

        val cardNumber = leToNumericString(
            byteArrayOf(
                block[4],
                block[5],
                block[6],
                block[7]
            ), 4
        )

        val currentBalance = Utils.formatCoin(
            leToNumeric(
                mifareManager.readBlock(33),
                2
            )
        )

        setTextViewFields(chipNumber, cardNumber, currentBalance)
    }

    @Throws(IOException::class, AuthenticateException::class)
    private fun setCardNumber(numberCard: String, mifareManager: MifareManager) {
        val long1: Long = if (numberCard.isEmpty()) {
            0L
        } else {
            numberCard.toLong()
        }

        if (numberCard.isNotEmpty()) {
            val block = mifareManager.readBlock(1)
            block[4] = (long1 ushr 0).toByte()
            block[5] = (long1 ushr 8).toByte()
            block[6] = (long1 ushr 16).toByte()
            block[7] = (long1 ushr 24).toByte()
            mifareManager.writeBlock(1, block)
        }
    }

    @Throws(IOException::class, AuthenticateException::class)
    private fun setBalance(newBalance: String, mifareManager: MifareManager) {
        if (newBalance.isNotEmpty()) {
            val balanceInt = Integer.parseInt(newBalance)

            mifareManager.writeValue(33, balanceInt)
            mifareManager.writeValue(34, balanceInt)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if ("android.nfc.action.TECH_DISCOVERED" != intent.action) {
            return
        }
        val mifareManager =
            MifareManager(intent.getParcelableExtra("android.nfc.extra.TAG") as Tag)
        try {
            mifareManager.open()
            setTextViewFields("-","-","-")
            processingOperation(mifareManager)
            showDataFromCard(mifareManager)
        } catch (ex: AuthenticateException) {
            Log.e("ERROR", "Error authenticating sector", ex as Throwable)
            toastMessage(applicationContext, "Error authenticating sector " + ex.sector)
        } catch (ex2: Exception) {
            Log.e("ERROR", "Unknown error", ex2 as Throwable)
            toastMessage(applicationContext, ex2.message!!)
        } finally {
            mifareManager.close()
        }
    }

    private fun setTextViewFields(chipNumber: String, cardNumber: String, currentBalance: String) {
        val textViewChip = findViewById<TextView>(R.id.text_view_chip)
        val textViewNumberCard = findViewById<TextView>(R.id.text_view_current_number_card)
        val textViewCurrentBalance = findViewById<TextView>(R.id.text_view_current_balance)

        textViewChip.text = chipNumber
        textViewNumberCard.text = cardNumber
        textViewCurrentBalance.text = currentBalance
    }
}
