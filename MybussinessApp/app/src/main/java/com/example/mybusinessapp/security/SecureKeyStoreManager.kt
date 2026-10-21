package com.example.mybusinessapp.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// ==================================================================
// 🔐 HARDWARE KEYSTORE & ENCRYPTED STORAGE MANAGER
// ==================================================================
class SecureKeyStoreManager private constructor(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MyBusinessApp_MasterAESKey"
        private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        @Volatile
        private var INSTANCE: SecureKeyStoreManager? = null

        fun getInstance(context: Context): SecureKeyStoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureKeyStoreManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 1. Hardware-Backed EncryptedSharedPreferences (AES256_GCM)
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secure_credentials_vault",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to standard context prefs if Keystore hardware is unavailable in emulator test
            context.getSharedPreferences("secure_credentials_fallback", Context.MODE_PRIVATE)
        }
    }

    // 2. Android Keystore Direct Payload Encryption
    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // Allows programmatic storage without continuous prompt
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypt arbitrary string payload directly via Android KeyStore AES-256 GCM
     */
    fun encryptPayload(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combined IV + CipherText encoded in Base64
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            plainText
        }
    }

    /**
     * Decrypt encrypted Base64 string payload via Android KeyStore AES-256 GCM
     */
    fun decryptPayload(encryptedBase64: String): String {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
            if (combined.size < GCM_IV_LENGTH) return encryptedBase64

            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedBase64
        }
    }

    // 3. Encrypted SharedPreferences accessors for Auth Tokens
    fun saveSecureAuthToken(token: String) {
        encryptedPrefs.edit().putString("auth_token_encrypted", token).apply()
    }

    fun getSecureAuthToken(): String? {
        return encryptedPrefs.getString("auth_token_encrypted", null)
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean("biometric_2fa_enabled", enabled).apply()
    }

    fun isBiometricLockEnabled(): Boolean {
        return encryptedPrefs.getBoolean("biometric_2fa_enabled", true)
    }
}
