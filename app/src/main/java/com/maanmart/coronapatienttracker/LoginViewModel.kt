package com.maanmart.coronapatienttracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maanmart.coronapatienttracker.data.DataSource
import com.maanmart.coronapatienttracker.shared.dto.SettingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.maanmart.coronapatienttracker.shared.Result
import com.maanmart.coronapatienttracker.shared.SharedPrefs
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: DataSource) : ViewModel() {

    // ایجاد لایو دیتا برای فرم لاگین
    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    // ایجاد لایو دیتا برای وضعیت لاگین
    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    //ایجاد لایو دیتا برای وضعیت تنظیمات
    private val _settingResult = MutableLiveData<SettingModel>()
    val settingResult: LiveData<SettingModel> = _settingResult


    // لاگین کردن یوزر وآپدیت لایو دیتای مربوطه و لایو دیتای تنظیمات
    fun personLogin(identityCode: String, mobile: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val personLogin = loginRepository.personLogin(mobile, identityCode)
            if (personLogin is Result.Success) {
                _loginResult.postValue(
                        LoginResult(success = LoggedInUserView(displayName = personLogin.data.id)))
                val settings = loginRepository.getSettings(personLogin.data.id)
                if (settings is Result.Success) {
                    _settingResult.postValue(SettingModel(personLogin.data.id, settings.data))
                } else {
                    _settingResult.postValue(SettingModel(null, null))
                    println("getting settings failed")
                }
            } else {
                _loginResult.postValue(LoginResult(error = R.string.login_failed))
            }
        }
    }

    // اعتبار سنجی فرم لاگین بعد از تغییر هر فیلد
    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
//        return if (username.contains('@')) {
//            Patterns.EMAIL_ADDRESS.matcher(username).matches()
//        } else {
//            username.isNotBlank()
//        }
        return "^09\\d{9}".toRegex().matches(username)
//        return username.isNotBlank()
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length == 10
    }
}