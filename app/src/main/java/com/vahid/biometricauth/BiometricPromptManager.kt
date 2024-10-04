package com.vahid.biometricauth

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class BiometricPromptManager(
    private val activity: AppCompatActivity
) {
    private val resultChannel = Channel<BiometricResult>()
    val promtResults = resultChannel.receiveAsFlow()
    fun showBiometricPromt(
        title: String,
        description: String
    ) {

        val manager = BiometricManager.from(activity)
        val authenticator = if (Build.VERSION.SDK_INT >= 30) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_STRONG
        }
        val promtInfo = PromptInfo.Builder()
            .setTitle(title)
            .setDescription(description)
            .setAllowedAuthenticators(authenticator)

        if (Build.VERSION.SDK_INT < 30) {
            promtInfo.setNegativeButtonText("cancel")
        }
        when (manager.canAuthenticate(authenticator)) {
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                resultChannel.trySend(BiometricResult.HardwareUnavailable)

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                resultChannel.trySend(BiometricResult.FeatureUnavailable)

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                resultChannel.trySend(BiometricResult.AuthenticationNotSet)

            else ->
                Unit
        }
        val prompt = BiometricPrompt(activity,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    resultChannel.trySend(BiometricResult.AuthenticationFailed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    resultChannel.trySend(BiometricResult.AuthenticationError(error = errString.toString()))
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    resultChannel.trySend(BiometricResult.AuthenticationSuccess)
                }
            }
        )
        prompt.authenticate(promtInfo.build())
    }


}