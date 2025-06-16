import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREFS_NAME = "UserPrefs"
    private const val TOKEN_KEY = "userToken"


    fun saveToken(context: Context, token: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME,
            Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply()
    }
    fun getToken(context: Context): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME,
            Context.MODE_PRIVATE)
        return sharedPreferences.getString(TOKEN_KEY, null)
    }
    fun removeToken(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME,
            Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(TOKEN_KEY).apply()
    }
}
