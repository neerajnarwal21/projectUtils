package com.merchant.paul.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.merchant.paul.R
import com.merchant.paul.activity.BaseActivity
import java.util.*

/**
 * Created by neeraj on 29/9/17.
 */
object PermissionsManager {

    private lateinit var pCallback: PCallback
    private var reqCode: Int = 0
    private const val PERMISSION_REQUEST_CODE = 99

    open class PCallback(private val success: ((reqCode: Int) -> Unit), private val failure: ((reqCode: Int) -> Unit)?=null) {
        fun onSuccess(reqCode: Int) {
            success.invoke(reqCode)
        }

        fun onError(reqCode: Int) {
            failure?.invoke(reqCode)
        }
    }

    fun checkPermissions(activity: Activity, perms: Array<String>, requestCode: Int, pCallback: PCallback): Boolean {
        PermissionsManager.pCallback = pCallback
        PermissionsManager.reqCode = requestCode
        val permsArray = ArrayList(Arrays.asList(*perms))
        for (perm in perms) {
            if (ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED)
                permsArray.remove(perm)
        }
        if (permsArray.size > 0)
            ActivityCompat.requestPermissions(activity, permsArray.toTypedArray(), PERMISSION_REQUEST_CODE)
        return permsArray.size == 0
    }

    fun onPermissionsResult(activity: BaseActivity, requestCode: Int, grantResults: IntArray) {

        var permGrantedBool = false
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                for (grantResult in grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        try {
                            activity.showToast(activity.getString(R.string.not_sufficient_permissions, activity.getString(R.string.app_name)), true)
                        } catch (e: Exception) {
                            Toast.makeText(activity, activity.getString(R.string.not_sufficient_permissions, activity.getString(R.string.app_name)), Toast.LENGTH_SHORT).show()
                        }

                        permGrantedBool = false
                        break
                    } else {
                        permGrantedBool = true
                    }
                }
                if (permGrantedBool) {
                    pCallback.onSuccess(reqCode)
                } else {
                    pCallback.onError(reqCode)
                }
            }
        }
    }

//    interface PermissionCallback {
//        fun onPermissionsGranted(resultCode: Int)
//
//        fun onPermissionsDenied(resultCode: Int)
//    }

}
