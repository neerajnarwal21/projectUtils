package com.merchant.paul.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Spinner
import com.merchant.paul.BuildConfig
import com.merchant.paul.activity.SplashActivity
import com.merchant.paul.data.DocumentData
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun getNonEmptyText(text: String?): String {
    return if (text == null || text.isEmpty()) "Not Provided" else text
}

fun changeDateFormat(dateString: String?, sourceDateFormat: String, targetDateFormat: String): String {
    if (dateString == null || dateString.isEmpty()) {
        return ""
    }
    val date: Date
    try {
        val inputDateFormat = SimpleDateFormat(sourceDateFormat, Locale.getDefault())
        date = inputDateFormat.parse(dateString)
    } catch (e: Exception) {
        e.printStackTrace()
        return dateString
    }

    return SimpleDateFormat(targetDateFormat, Locale.getDefault()).format(date)
}

fun changeDateFormatFromDate(sourceDate: Date?, targetDateFormat: String?): String {
    return if (sourceDate == null || targetDateFormat == null || targetDateFormat.isEmpty()) {
        ""
    } else SimpleDateFormat(targetDateFormat, Locale.getDefault()).format(sourceDate)
}

fun getDateFromStringDate(dateString: String?, sourceDateFormat: String): Date? {
    if (dateString == null || dateString.isEmpty()) {
        return null
    }
    val inputDateFromat = SimpleDateFormat(sourceDateFormat, Locale.ENGLISH)
    var date = Date()
    try {
        date = inputDateFromat.parse(dateString)
    } catch (e: ParseException) {
        e.printStackTrace()
    }

    return date
}

fun getUniqueDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

fun isValidMail(email: String): Boolean {
    return email.matches("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex())
}

fun isValidPassword(password: String): Boolean {
    return password.matches("^(?=\\S+$).{8,}$".toRegex())
}

fun isValidPan(pan: String): Boolean {
    return pan.matches("[A-Z]{5}[0-9]{4}[A-Z]".toRegex())
}

fun changeStatusBarColor(activity: Activity, colorId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(activity, colorId)
    }
}

fun setStatusBarTranslucent(activity: Activity, makeTranslucent: Boolean) {
    if (makeTranslucent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }
}

fun getAddress(location: Location, context: Context): String {
    try {
        val geocoder = Geocoder(context)
        val addresses: List<Address>
        return if (location.latitude != 0.0 || location.longitude != 0.0) {
            addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses[0].getAddressLine(0)
            val address1 = addresses[0].getAddressLine(1)
            val country = addresses[0].countryName
            address + ", " + address1 + ", " + (country ?: "")
        } else {
            "Unable to get address of this location"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return "Unable to get address of this location"
    }

}

fun getAddress(lat: Double, lng: Double, context: Context): String {
    try {
        val geocoder = Geocoder(context)
        val addresses: List<Address>
        return if (lat != 0.0 || lng != 0.0) {
            addresses = geocoder.getFromLocation(lat, lng, 1)
            val address = addresses[0].getAddressLine(0)
            val address1 = addresses[0].getAddressLine(1)
            val country = addresses[0].countryName
            address + ", " + address1 + ", " + (country ?: "")
        } else {
            ""
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }

}

fun debugLog(tag: String, s: String) {
    if (BuildConfig.DEBUG)
        Log.e(tag, s)
}

fun keyHash(context: Context) {
    try {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        for (signature in info.signatures) {
            val md = MessageDigest.getInstance("SHA")
            md.update(signature.toByteArray())
            Log.e("KeyHash:>>>>>>>>>>>>>>>", "" + Base64.encodeToString(md.digest(), Base64.DEFAULT))
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }

}

fun logoutUser(context: Context) {
    val store = PrefStore(context)
    store.saveString(Const.SESSION_KEY, "")
    val intent = Intent(context, SplashActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

fun getDocumentName(documents: List<DocumentData>): String {
    var docNames = ""
    for (docName in documents) {
        docNames += docName.document.name + ", "
    }
    return docNames.substring(0, docNames.length - 2)
}

fun getDocName(docName: String?): String {
    return when (docName) {
        "passport" -> "Passport"
        "aadhar_card" -> "Adhaar card"
        "pan_card" -> "Pan card"
        "voter_id" -> "Voter Id"
        "driving_license" -> "Driving licence"
        "bank_statement" -> "Bank statement"
        else -> docName!!
    }
}

fun Spinner.mySpinnerCallback(callBack: (Int) -> Unit) {
    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            callBack.invoke(position)
        }
    }
}

fun <T : Parcelable> copy(orig: T): T? {
    val p = Parcel.obtain()
    orig.writeToParcel(p, 0)
    p.setDataPosition(0)
    var copy: T? = null
    try {
        copy = orig.javaClass.getDeclaredConstructor(Parcel::class.java).newInstance(p) as T
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return copy
}