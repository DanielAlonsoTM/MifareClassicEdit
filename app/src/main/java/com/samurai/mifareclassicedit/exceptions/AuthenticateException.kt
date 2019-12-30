package com.samurai.mifareclassicedit.exceptions

class AuthenticateException(var sector: Int) : Exception() {
    companion object {
        private const val serialVersionUID = 1L
    }
}
