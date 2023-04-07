package com.challenge.nfc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    private val _userName = MutableLiveData<String>()
    private val _balance = MutableLiveData<Float>()
    private val _reloadAmount = MutableLiveData<Int?>().apply {
        value = null
    }
    private val _isWriting = MutableLiveData<Boolean>().apply {
        value = false
    }

    fun setUserName(newUserName: String) {
        _userName.value = newUserName
    }

    fun setBalance(newBalance: Float) {
        _balance.value = newBalance
    }

    fun startWriting() {
        _isWriting.value = true
    }

    fun toggleReloadAmount(newReloadAmount: Int) {
        if (_reloadAmount.value != newReloadAmount) {
            _reloadAmount.value = newReloadAmount
        } else {
            _reloadAmount.value = null
        }
    }

    fun resetAll() {
        _reloadAmount.value = null
        _isWriting.value = false
    }

    val userName: LiveData<String>
        get() = _userName

    val balance: LiveData<Float>
        get() = _balance

    val reloadAmount: LiveData<Int?>
        get() = _reloadAmount

    val isWriting: LiveData<Boolean>
        get() = _isWriting

}