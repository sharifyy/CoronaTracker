package com.maanmart.coronapatienttracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.maanmart.coronapatienttracker.data.DataSource
import com.maanmart.coronapatienttracker.shared.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsViewModel(private val dataSource: DataSource) : ViewModel() {

    // ایجاد لایو دیتا برای موقعیت بیماران کرونایی
    private val _coronaLocations = MutableLiveData<List<List<LatLng>>?>()
    val coronaLocations: LiveData<List<List<LatLng>>?> = _coronaLocations

    //دریافت موقعیت بیماران کرونایی از سرور
    fun getCoronaPatientsLocation(personId:String){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                val patientLocationsResult = dataSource.checkPatientsLocation(personId)
                if(patientLocationsResult is Result.Success){
                    patientLocationsResult.data.forEachIndexed { index, list -> list.forEach { println("$it patient $index") }}
                    _coronaLocations.postValue(patientLocationsResult.data)
                }else{
                    _coronaLocations.postValue(null)
                }
            }
        }
    }
}