package com.maanmart.coronapatienttracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maanmart.coronapatienttracker.data.CoronaTrackerApi
import com.maanmart.coronapatienttracker.data.DataSource

class MapsViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
            return MapsViewModel(DataSource(CoronaTrackerApi.invoke())) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}