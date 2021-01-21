package com.maanmart.coronapatienttracker.shared

enum class ResultCode(val code: Int) {
    OK(code = 0),
    UNKNOWN_ERROR(code= -1),
    INVALID_DATA (code= -300),
    DISABLED(code= -600),
    NOT_FOUND (code= -1000)
}