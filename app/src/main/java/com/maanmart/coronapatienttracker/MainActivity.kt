package com.maanmart.coronapatienttracker

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.maanmart.coronapatienttracker.shared.SharedState
import com.maanmart.coronapatienttracker.shared.dto.SettingModel


class MainActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginIfUserSave()

        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val login = findViewById<Button>(R.id.login)
        val loading = findViewById<ProgressBar>(R.id.loading)

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        // مشاهده وضعیت فرم لاگین
        loginViewModel.loginFormState.observe(this@MainActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        //مشاهده پاسخ ریافتی لاگین از سرور
        loginViewModel.loginResult.observe(this@MainActivity, Observer {
            val loginResult = it ?: return@Observer

            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
                loading.visibility = View.GONE
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)
        })

        // مشاهده دریافت نتایج تنظیمات از سرور
        loginViewModel.settingResult.observe(this@MainActivity, Observer {
            val settingModel = it ?:return@Observer

            loading.visibility = View.GONE
            if(settingModel.userId == null) return@Observer
            saveUsernameAndPasswordForFirebaseToken(username.text.toString(), password.text.toString())
            rememberUser(settingModel)
            SharedState.personId = settingModel.userId
            SharedState.sendLocationInterval = settingModel.settingResponse?.sendLocationsInterval
            SharedState.getLocationInterval = settingModel.settingResponse?.getLocationInterval
            SharedState.distanceCheckInterval = settingModel.settingResponse?.distanceCheckInterval

            startMap()
        })

        //اپدیت کردن مقدار یوزرنیم در ویومدل
        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            //اپدیت کردن مقدار پسورد در ویومدل
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }
            // فراخوانی لاگین با کلیک روی دکمه تیک در کیبورد
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        initLogin(username, password)
                }
                false
            }

            //فراخوانی لاگین با کلیک روی دکمه لاگین
            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                initLogin(username, password)
            }
        }
//
//        username.setText("09123456789")
//        password.setText("2222222222")
    }

    private fun saveUsernameAndPasswordForFirebaseToken(username: String, password: String) {
        val editor: SharedPreferences.Editor = getSharedPreferences("AppPreffs", MODE_PRIVATE).edit()
        editor.putString("mobile", username)
        editor.putString("identityCode", password)
        editor.apply()
    }

    // رفتن به صفحه نقشه
    private fun startMap() {
        val mapIntent = Intent(this, MapsActivity::class.java)
        mapIntent.putExtra("title",intent.getStringExtra("title"))
        mapIntent.putExtra("message",intent.getStringExtra("message"))
        startActivity(mapIntent)
    }

    // لاگین کردن یوزر در صورتی که قبلا لاگین کرده باشد
    private fun loginIfUserSave() {
        val prefs = getSharedPreferences("AppPreffs", MODE_PRIVATE)
        val userId = prefs.getString("personId", null) ?: return
        SharedState.personId = userId
        SharedState.sendLocationInterval = prefs.getInt("sendLocationInterval",120)
        SharedState.getLocationInterval = prefs.getInt("getLocationInterval",30)
        SharedState.distanceCheckInterval = prefs.getInt("distanceCheckInterval",30)
        startMap()
    }

    // ذخیره یوزر بعد از لاگین
    private fun rememberUser(settingModel: SettingModel) {
        val editor: SharedPreferences.Editor = getSharedPreferences("AppPreffs", MODE_PRIVATE).edit()
        editor.putString("personId", settingModel.userId)
        editor.putInt("sendLocationInterval",settingModel.settingResponse?.sendLocationsInterval?:120)
        editor.putInt("getLocationInterval",settingModel.settingResponse?.getLocationInterval?:30)
        editor.putInt("distanceCheckInterval",settingModel.settingResponse?.distanceCheckInterval?:30)
        editor.apply()
    }

    // استارت فرایند لاگین
    private fun initLogin(
        username: EditText,
        password: EditText
    ) {
        loginViewModel.personLogin(
                username.text.toString(),
                password.text.toString()
        )
    }

    // نمایش پیام خوشامدگویی به کاربر لاگین شده
    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    // نمایش خطای لاگین
    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}



