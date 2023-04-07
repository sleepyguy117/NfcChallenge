package com.challenge.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and

class NfcHelper {

    var nfcHelperListener: NFCHelperListener? = null

    interface NFCHelperListener {
        fun onReadResult(tokenRecord: TokenRecord?) { }     // null for invalid or bad / blank token
        fun onWriteResult(success: Boolean) { }
    }

    private var readWriteMode = false     // true is writing, false is reading

    private fun createNdefMessage(tokenRecord: TokenRecord): NdefMessage {
        /*
            3 ndefRecords
            1 - "com.challenge.xeal"
            2 - <users name>
            3 - balance as string
         */
        val firstRecord = createNdefTextRecord(Constants.FIRST_RECORD_STRING)
        val usernameRecord = createNdefTextRecord(tokenRecord.username)
        val balanceRecord = createNdefTextRecord(String.format("%.2f", tokenRecord.balance))

        return NdefMessage(arrayOf(firstRecord, usernameRecord, balanceRecord))
    }

    private fun createNdefTextRecord(
        payload: String,
        locale: Locale = Locale.getDefault(),
        encodeInUtf8: Boolean = true
    ): NdefRecord {
        val langBytes = locale.language.toByteArray(Charsets.US_ASCII)
        val utfEncoding = if (encodeInUtf8) Charsets.UTF_8 else Charsets.UTF_16
        val textBytes = payload.toByteArray(utfEncoding)
        val utfBit: Int = if (encodeInUtf8) 0 else 1 shl 7
        val status = (utfBit + langBytes.size).toChar()
        val data = ByteArray(1 + langBytes.size + textBytes.size)
        data[0] = status.toByte()
        System.arraycopy(langBytes, 0, data, 1, langBytes.size)
        System.arraycopy(textBytes, 0, data, 1 + langBytes.size, textBytes.size)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), data)
    }

    private fun readNdefTextRecord(record: NdefRecord): String? {
        var text: String? = null
        val payload = record.payload
        val utfEncoding: Charset =
            if ((payload[0] and 128.toByte()).toInt() == 0) Charsets.UTF_8 else Charsets.UTF_16 // Get the Text Encoding
        val languageCodeLength: Int =
            (payload[0] and 51).toInt() // Get the Language Code, e.g. "en"
        try {
            // Get the Text
            text = String(
                payload,
                languageCodeLength + 1,
                payload.size - languageCodeLength - 1,
                utfEncoding
            )
        } catch (_: UnsupportedEncodingException) {

        }

        return text
    }

    private fun writeToTag(tag: Tag, message: NdefMessage): Boolean {
        var success = false

        try {
            val ndefTag = Ndef.get(tag)
            if (ndefTag == null) {
                // Let's try to format the Tag in NDEF
                val nForm = NdefFormatable.get(tag)
                if (nForm != null) {
                    nForm.connect()
                    nForm.format(message)
                    nForm.close()
                }
            } else {
                ndefTag.connect()
                ndefTag.writeNdefMessage(message)
                ndefTag.close()
            }
            success = true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return success
    }

    fun setReadMode() {
        readWriteMode = false
    }

    fun setWriteMode() {
        readWriteMode = true
    }

    fun setupReloadTokenRecord(userName: String, balance: Float, reloadAmount: Int) {
        tokenRecordToWrite = TokenRecord(userName, balance + reloadAmount.toFloat())
    }

    private var tokenRecordToWrite: TokenRecord? = null

    fun handleNewIntent(intent: Intent) {
        if (readWriteMode) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let { writeTag ->
                var result = false
                tokenRecordToWrite?.let { tokenRecord ->
                    result = writeToTag(writeTag, createNdefMessage(tokenRecord))
                }
                nfcHelperListener?.onWriteResult(result)
            }
        } else {
            var tokenRecord: TokenRecord? = null
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                if (rawMessages != null && rawMessages.isNotEmpty()) {
                    // our tokens only have 1 ndefMessage
                    val ndefMessage = rawMessages[0] as NdefMessage
                    val ndefRecords = ndefMessage.records

                    if (ndefRecords.size == 3) {
                        val firstRecord = readNdefTextRecord(ndefRecords[0])
                        val userName = readNdefTextRecord(ndefRecords[1])
                        val balanceString = readNdefTextRecord(ndefRecords[2])
                        if (firstRecord == Constants.FIRST_RECORD_STRING && userName != null && balanceString != null) {
                            val balance = try {
                                balanceString.toFloat()
                            } catch (e: NumberFormatException) {
                                0f
                            }
                            tokenRecord = TokenRecord(userName, balance)
                        }
                    }
                }
            }

            nfcHelperListener?.onReadResult(tokenRecord)
        }
    }
}