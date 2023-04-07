package com.challenge.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.challenge.nfc.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NfcHelper.NFCHelperListener {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()


    private val nfcHelper = NfcHelper()
    private var nfcAdapter: NfcAdapter? = null
    private val nfcIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
        try {
            addDataType(Constants.TEXT_MIME_TYPE)
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }
    }
    private val filters = arrayOf(nfcIntentFilter, IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
    private val techListsArray = arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))
    private lateinit  var pendingIntent: PendingIntent

    private fun setupNfc() {
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_MUTABLE)
        } else PendingIntent.getActivity(this, 0, intent, 0)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcHelper.nfcHelperListener = this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNfc()
        setSelectorLabels()
        setObservers()
        setClickListeners()
    }


    override fun onResume() {
        super.onResume()

        nfcHelper.setReadMode()
        mainViewModel.resetAll()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techListsArray)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        nfcHelper.handleNewIntent(intent)
    }

    private fun launchSuccess() {
        val intent = Intent(this@MainActivity, SuccessActivity::class.java).apply {
            putExtra(SuccessActivity.RELOAD_AMOUNT, mainViewModel.reloadAmount.value!!)
        }
        startActivity(intent)
    }

    private fun setSelectorLabels() {
        binding.reloadSelector1.text = String.format(getString(R.string.dollars_format, Constants.RELOAD_AMOUNT1))
        binding.reloadSelector2.text = String.format(getString(R.string.dollars_format, Constants.RELOAD_AMOUNT2))
        binding.reloadSelector3.text = String.format(getString(R.string.dollars_format, Constants.RELOAD_AMOUNT3))
    }

    private fun setClickListeners() {
        binding.reloadSelector1.setOnClickListener {
            mainViewModel.toggleReloadAmount(Constants.RELOAD_AMOUNT1)
        }
        binding.reloadSelector2.setOnClickListener {
            mainViewModel.toggleReloadAmount(Constants.RELOAD_AMOUNT2)
        }
        binding.reloadSelector3.setOnClickListener {
            mainViewModel.toggleReloadAmount(Constants.RELOAD_AMOUNT3)
        }
        binding.payNowButton.setOnClickListener {
            mainViewModel.startWriting()
        }
        binding.fundsAvailableHolder.setOnClickListener {
            // tapping funds available, will disable waiting spinner for writing
            nfcHelper.setReadMode()
            mainViewModel.resetAll()
        }
    }

    private fun setObservers() {
        mainViewModel.userName.observe(this) {
            binding.username.text = it
        }

        mainViewModel.balance.observe(this) {
            binding.fundAvailableValue.text = String.format(getString(R.string.balance_format), it)
        }

        mainViewModel.reloadAmount.observe(this) {
            when (it) {
                Constants.RELOAD_AMOUNT1 -> {
                    binding.reloadSelector1.isSelected = true
                    binding.reloadSelector2.isSelected = false
                    binding.reloadSelector3.isSelected = false
                    binding.payNowButton.isEnabled = true
                }
                Constants.RELOAD_AMOUNT2 -> {
                    binding.reloadSelector1.isSelected = false
                    binding.reloadSelector2.isSelected = true
                    binding.reloadSelector3.isSelected = false
                    binding.payNowButton.isEnabled = true
                }
                Constants.RELOAD_AMOUNT3 -> {
                    binding.reloadSelector1.isSelected = false
                    binding.reloadSelector2.isSelected = false
                    binding.reloadSelector3.isSelected = true
                    binding.payNowButton.isEnabled = true
                }
                else -> {
                    binding.reloadSelector1.isSelected = false
                    binding.reloadSelector2.isSelected = false
                    binding.reloadSelector3.isSelected = false
                    binding.payNowButton.isEnabled = false
                }
            }
        }

        mainViewModel.isWriting.observe(this) {
            if (it) {
                binding.payNowButton.isEnabled = false
                binding.payNowLabel.visibility = View.INVISIBLE
                binding.spinner.visibility = View.VISIBLE
                binding.spinner.repeatCount = LottieDrawable.INFINITE
                binding.spinner.playAnimation()

                nfcHelper.setupReloadTokenRecord(mainViewModel.userName.value ?: "Default Name", mainViewModel.balance.value ?: 0f, mainViewModel.reloadAmount.value!!)
                nfcHelper.setWriteMode()
                lifecycleScope.launch {
                    delay(1500)

                    Toast.makeText(this@MainActivity, getString(R.string.tap_nfc_tag), Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.payNowButton.isEnabled = mainViewModel.reloadAmount.value != null
                binding.payNowLabel.visibility = View.VISIBLE
                if (binding.spinner.isAnimating) {
                    binding.spinner.pauseAnimation()
                }
                binding.spinner.visibility = View.INVISIBLE
            }
        }
    }

    override fun onReadResult(tokenRecord: TokenRecord?) {
        tokenRecord?.let {
            mainViewModel.setUserName(it.username)
            mainViewModel.setBalance(it.balance)
        }
    }

    override fun onWriteResult(success: Boolean) {
        if (success) {
            binding.spinner.pauseAnimation()
            launchSuccess()
        } else {
            mainViewModel.resetAll()
            Toast.makeText(
                this@MainActivity,
                getString(R.string.nfc_tag_load_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}