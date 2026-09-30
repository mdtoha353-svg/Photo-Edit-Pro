package com.photoeditpro.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var emptyText: TextView
    private var original: Bitmap? = null
    private var rotation = 0f
    private var brightness = 0f
    private var contrast = 1f
    private var saturation = 1f

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        contentResolver.openInputStream(uri)?.use { original = BitmapFactory.decodeStream(it) }
        rotation = 0f; brightness = 0f; contrast = 1f; saturation = 1f
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        emptyText = findViewById(R.id.emptyText)
        findViewById<MaterialButton>(R.id.openButton).setOnClickListener { picker.launch("image/*") }
        findViewById<MaterialButton>(R.id.rotateButton).setOnClickListener {
            if (original != null) { rotation = (rotation + 90f) % 360f; render() }
        }
        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener { save() }
        findViewById<Slider>(R.id.brightnessSlider).addOnChangeListener { _, v, _ -> brightness = v; render() }
        findViewById<Slider>(R.id.contrastSlider).addOnChangeListener { _, v, _ -> contrast = v; render() }
        findViewById<Slider>(R.id.saturationSlider).addOnChangeListener { _, v, _ -> saturation = v; render() }
    }

    private fun render() {
        val source = original ?: return
        emptyText.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        val cm = ColorMatrix()
        cm.setSaturation(saturation)
        val t = (-0.5f * contrast + 0.5f) * 255f + brightness * 255f
        cm.postConcat(ColorMatrix(floatArrayOf(
            contrast,0f,0f,0f,t, 0f,contrast,0f,0f,t,
            0f,0f,contrast,0f,t, 0f,0f,0f,1f,0f
        )))
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(rotation) }, true)
        val result = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(rotated, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) })
        if (rotated !== source) rotated.recycle()
        imageView.setImageBitmap(result)
    }

    private fun save() {
        if (original == null) { Toast.makeText(this, "আগে একটি ছবি নির্বাচন করুন", Toast.LENGTH_SHORT).show(); return }
        val bitmap = Bitmap.createBitmap(imageView.width.coerceAtLeast(1), imageView.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        imageView.draw(Canvas(bitmap))
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoEditPro_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoEditPro")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            Toast.makeText(this, "ছবি Save হয়েছে", Toast.LENGTH_LONG).show()
        } else Toast.makeText(this, "Save করা যায়নি", Toast.LENGTH_SHORT).show()
    }
}
