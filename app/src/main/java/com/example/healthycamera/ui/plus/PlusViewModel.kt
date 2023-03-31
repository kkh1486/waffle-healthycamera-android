package com.example.healthycamera.ui.plus

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlusViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Nutrient"
    }
    val text: LiveData<String> = _text
}