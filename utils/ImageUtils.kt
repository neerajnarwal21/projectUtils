package com.merchant.paul.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.widget.Toast
import com.merchant.paul.BuildConfig
import com.merchant.paul.R
import com.soundcloud.android.crop.Crop
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/**
 * Created by Neeraj Narwal on 17/10/16.
 */
class ImageUtils private constructor(builder: Builder) {
    private val onlyCamera: Boolean
    private val onlyGallery: Boolean
    private val doCrop: Boolean

    init {
        activity = builder.activity
        reqCode = builder.reqCode
        myCallback = builder.myCallBack
        this.onlyCamera = builder.onlyCamera
        this.onlyGallery = builder.onlyGallery
        this.doCrop = builder.doCrop
        width = builder.width
        height = builder.height
    }

    class Builder(internal val activity: Activity, internal val reqCode: Int, internal val myCallBack: (imagePath: String?, resultCode: Int) -> Unit) {
        internal var onlyCamera: Boolean = false
        internal var onlyGallery: Boolean = false
        internal var doCrop: Boolean = false
        internal var width: Int = 0
        internal var height: Int = 0


        fun onlyCamera(onlyCamera: Boolean): Builder {
            this.onlyCamera = onlyCamera
            return this
        }

        fun onlyGallery(onlyGallery: Boolean): Builder {
            this.onlyGallery = onlyGallery
            return this
        }

        fun crop(): Builder {
            this.doCrop = true
            return this
        }

        fun aspectRatio(width: Int, height: Int): Builder {
            this.width = width
            this.height = height
            this.doCrop = true
            return this
        }

        fun start() {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, "Write External storage Permission not specified", Toast.LENGTH_LONG).show()
                return
            }
            imageUtils = ImageUtils(this)
            if (imageUtils!!.onlyCamera) {
                captureCameraImage()
            } else if (imageUtils!!.onlyGallery) {
                selectGalleryImage()
            } else {
                selectImageDialog()
            }
        }
    }

    companion object {

        val GALLERY_REQ = 1
        val CAMERA_REQ = 2
        private var outputUri: Uri? = null
        private var activity: Activity? = null
        private var imageUtils: ImageUtils? = null
        lateinit var myCallback: (imagePath: String?, resultCode: Int) -> Unit
        private var reqCode: Int = 0
        private var width: Int = 0
        private var height: Int = 0
        private var inputUri: Uri? = null
        private var inputUriCamera: Uri? = null

        private fun selectImageDialog() {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(activity!!.getString(R.string.choose_image_source))
            builder.setItems(arrayOf<CharSequence>("Gallery", "Camera")) { _, which ->
                when (which) {
                    0 -> selectGalleryImage()
                    1 -> captureCameraImage()
                }
            }
            builder.show()
        }

        private fun captureCameraImage() {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val storageDir = activity!!.cacheDir
            val imageFile: File
            imageFile = File(storageDir, System.currentTimeMillis().toString() + ".jpg")
            if (takePictureIntent.resolveActivity(activity!!.packageManager) != null) {
                inputUriCamera = Uri.fromFile(imageFile)
                inputUri = FileProvider.getUriForFile(activity!!, BuildConfig.APPLICATION_ID, imageFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, inputUri)
                activity!!.startActivityForResult(takePictureIntent, CAMERA_REQ)
            }
        }

        private fun selectGalleryImage() {
            val intent1 = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            intent1.type = "image/*"
            activity!!.startActivityForResult(intent1, GALLERY_REQ)
        }

        fun activityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == GALLERY_REQ && resultCode == Activity.RESULT_OK) {
                inputUri = data?.data
//                Log.e("ImageUtils", "start")
//                createOrientedImage(inputUri)
//                Log.e("ImageUtils", "end")
                if (imageUtils!!.doCrop) {
                    val file = File("" + activity!!.cacheDir + System.currentTimeMillis() + ".jpg")
                    outputUri = Uri.fromFile(file)
                    Crop()
                } else {
                    sendBackImagePath(inputUri, reqCode)
                }

            } else if (requestCode == CAMERA_REQ && resultCode == Activity.RESULT_OK) {
//                createOrientedImage(inputUriCamera)
                if (imageUtils!!.doCrop) {
                    val file = File("" + activity!!.cacheDir + System.currentTimeMillis() + ".jpg")
                    outputUri = Uri.fromFile(file)
                    Crop()
                } else {
                    sendBackImagePath(inputUriCamera, reqCode)
                }

            } else if (requestCode == Crop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
                sendBackImagePath(outputUri, reqCode)
            } else if (requestCode == Crop.REQUEST_CROP && resultCode == Crop.RESULT_ERROR) {
                Toast.makeText(activity, "Write external storage permission not specified", Toast.LENGTH_SHORT).show()
            }
        }

        private fun sendBackImagePath(inputUri: Uri?, reqCode: Int) {
            val path = getRealPath(activity, inputUri)
            myCallback.invoke(path, reqCode)
        }

        private fun Crop() {
            if (width != 0 && width > 0) {
                Crop.of(inputUri, outputUri).withAspect(width, height).start(activity)
            } else
                Crop.of(inputUri, outputUri).asSquare().start(activity)
        }

        fun bitmapToFile(bitmap: Bitmap, activity: Activity): File {
            val f = File(activity.cacheDir, System.currentTimeMillis().toString() + ".jpg")
            try {
                f.createNewFile()
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
                val bitmapdata = bos.toByteArray()

                val fos = FileOutputStream(f)
                fos.write(bitmapdata)
                fos.flush()
                fos.close()
            } catch (ioexception: IOException) {
                ioexception.printStackTrace()
            }

            return f
        }
    }
}

@JvmOverloads
fun blur(context: Activity, image: Bitmap, bitmapScale: Float = 0.4f, blurRadius: Float = 5f): Bitmap? {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        val width = Math.round(image.width * bitmapScale)
        val height = Math.round(image.height * bitmapScale)

        val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(context)
        var theIntrinsic: ScriptIntrinsicBlur? = null
        theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)
        theIntrinsic!!.setRadius(blurRadius)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)
        return outputBitmap
    }
    return null
}

fun getRealPath(context: Context?, uri: Uri?): String? {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
        if (isExternalStorageDocument(uri!!)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            if ("primary".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
        } else if (isDownloadsDocument(uri)) {

            val id = DocumentsContract.getDocumentId(uri)
            val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))

            return getDataColumn(context!!, contentUri, null, null)
        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            var contentUri: Uri? = null
            if ("image" == type) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else if ("video" == type) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else if ("audio" == type) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])

            return getDataColumn(context!!, contentUri, selection, selectionArgs)
        }
    } else if ("content".equals(uri!!.scheme, ignoreCase = true)) {
        return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context!!, uri, null, null)

    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }

    return null
}

private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

private fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
}

private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {

    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(column)

    try {
        cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(column)
            if (File(cursor.getString(index)).length() > 0)
                return cursor.getString(index)
            else
                return null
        }
    } finally {
        cursor?.close()
    }
    return null
}

@JvmOverloads
fun imageCompress(picturePath: String?, maxHeight: Float = 816.0f, maxWidth: Float = 612.0f): Bitmap {

    var scaledBitmap: Bitmap? = null
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true

    var bmp = BitmapFactory.decodeFile(picturePath, options)

    var actualHeight = options.outHeight
    var actualWidth = options.outWidth
    var imgRatio = (actualWidth / actualHeight).toFloat()
    val maxRatio = maxWidth / maxHeight

    if (actualHeight > maxHeight || actualWidth > maxWidth) {
        if (imgRatio < maxRatio) {
            imgRatio = maxHeight / actualHeight
            actualWidth = (imgRatio * actualWidth).toInt()
            actualHeight = maxHeight.toInt()
        } else if (imgRatio > maxRatio) {
            imgRatio = maxWidth / actualWidth
            actualHeight = (imgRatio * actualHeight).toInt()
            actualWidth = maxWidth.toInt()
        } else {
            actualHeight = maxHeight.toInt()
            actualWidth = maxWidth.toInt()
        }
    } else {
        var bitmap: Bitmap
        bitmap = BitmapFactory.decodeFile(picturePath)
        bitmap = Bitmap.createScaledBitmap(bitmap, actualWidth, actualHeight, true)
        return bitmap
    }

    options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)
    options.inJustDecodeBounds = false

    options.inPurgeable = true
    options.inInputShareable = true
    options.inTempStorage = ByteArray(16 * 1024)

    try {
        bmp = BitmapFactory.decodeFile(picturePath, options)
    } catch (exception: OutOfMemoryError) {
        exception.printStackTrace()
    }

    try {
        scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
    } catch (exception: OutOfMemoryError) {
        exception.printStackTrace()
    }

    val ratioX = actualWidth / options.outWidth.toFloat()
    val ratioY = actualHeight / options.outHeight.toFloat()
    val middleX = actualWidth / 2.0f
    val middleY = actualHeight / 2.0f

    val scaleMatrix = Matrix()
    scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)

    val canvas = Canvas(scaledBitmap!!)
    canvas.matrix = scaleMatrix
    canvas.drawBitmap(bmp, middleX - bmp.width / 2, middleY - bmp.height / 2, Paint(Paint.FILTER_BITMAP_FLAG))
    bmp.recycle()
    val exif: ExifInterface
    try {
        exif = ExifInterface(picturePath)

        val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, 0)
        val matrix = Matrix()
        if (orientation == 6) {
            matrix.postRotate(90f)
        } else if (orientation == 3) {
            matrix.postRotate(180f)
        } else if (orientation == 8) {
            matrix.postRotate(270f)
        }
        scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                scaledBitmap.width, scaledBitmap.height, matrix,
                true)
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return scaledBitmap!!
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
        val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
        inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
    }
    val totalPixels = (width * height).toFloat()
    val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
    while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
        inSampleSize++
    }

    return inSampleSize
}