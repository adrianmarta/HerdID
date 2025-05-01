import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREFS_NAME = "UserPrefs"
    private const val TOKEN_KEY = "userToken"

    // Save token to SharedPreferences
    fun saveToken(context: Context, token: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply()
    }

    // Get the saved token from SharedPreferences
    fun getToken(context: Context): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(TOKEN_KEY, null)
    }

    // Remove token from SharedPreferences
    fun removeToken(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(TOKEN_KEY).apply()
    }
}
