package com.macmads.agrione

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.macmads.agrione.databinding.ActivityMainBinding
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var mClassifier: Classifier
    private lateinit var mBitmap: Bitmap

    private val mCameraRequestCode = 0
    private val mGalleryRequestCode = 2

    private val mInputSize = 224
    private val mModelPath = "plant_disease_model.tflite"
    private val mLabelPath = "plant_labels.txt"
    private val mSamplePath = "soybean.JPG"
    private var detection = false

    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)

        resources.assets.open(mSamplePath).use {
            mBitmap = BitmapFactory.decodeStream(it)
            mBitmap = Bitmap.createScaledBitmap(mBitmap, mInputSize, mInputSize, true)
            binding.mPhotoImageView.setImageBitmap(mBitmap)
        }

        binding.mCameraButton.setOnClickListener {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(callCameraIntent, mCameraRequestCode)
        }

        binding.mGalleryButton.setOnClickListener {
            val callGalleryIntent = Intent(Intent.ACTION_PICK)
            callGalleryIntent.type = "image/*"
            startActivityForResult(callGalleryIntent, mGalleryRequestCode)
        }
        binding.mDetectButton.setOnClickListener {
            processImage()
//            val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
//            binding.mResultTextView.text= results?.title+"\n Confidence:"+results?.confidence

        }
        binding.logoutCard.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Sign Out")
            builder.setMessage("Do you really want to Sign Out. To Use app you need to Login.")
            builder.setPositiveButton("Sign Out") { dialog, which ->
                auth.signOut()
                startActivity(Intent(this, Login::class.java))
                finish()
                dialog.dismiss()
            }
            builder.setNegativeButton(android.R.string.no) { dialog, which ->
                Toast.makeText(
                    applicationContext,
                    android.R.string.no, Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            builder.show()

        }
    }

    private fun processImage() {

        val imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(mBitmap!!, 0)
        imageLabeler.process(inputImage).addOnSuccessListener { imageLabels ->
            var stringImageRecognition = ""
            for (imageLabel in imageLabels) {
                val stingLabel = imageLabel.text
                val floatConfidence = imageLabel.confidence
                val index = imageLabel.index
                stringImageRecognition += "$index\n $stingLabel:\n $floatConfidence "
                if (index == 266 && floatConfidence > 0.7) {
                    detection = true
                }
            }
            if (detection) {
                val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
                binding.mResultTextView.text =
                    results?.title + "\n Confidence:" + results?.confidence
                detection = false
            } else {
                binding.mResultTextView.text = stringImageRecognition
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Not a plant")
                builder.setMessage("Given image is invalid steal do you want to continue to check disease")
                builder.setPositiveButton("Detect") { dialog, which ->

                    val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
                    binding.mResultTextView.text =
                        results?.title + "\n Confidence:" + results?.confidence
                    dialog.dismiss()

                }
                builder.setNegativeButton(android.R.string.no) { dialog, which ->
                    Toast.makeText(
                        applicationContext,
                        "Other object detected", Toast.LENGTH_SHORT
                    ).show()
                    binding.mResultTextView.text = stringImageRecognition
                    dialog.dismiss()
                }
                builder.show()
                detection = false
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mCameraRequestCode) {
            //Considérons le cas de la caméra annulée
            if (resultCode == Activity.RESULT_OK && data != null) {
                mBitmap = data.extras!!.get("data") as Bitmap
                mBitmap = scaleImage(mBitmap)
                val toast = Toast.makeText(
                    this,
                    ("Image crop to: w= ${mBitmap.width} h= ${mBitmap.height}"),
                    Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.BOTTOM, 0, 20)
                toast.show()
                binding.mPhotoImageView.setImageBitmap(mBitmap)
                binding.mResultTextView.text = "Your photo image set now."
            } else {
                Toast.makeText(this, "Camera cancel..", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == mGalleryRequestCode) {
            if (data != null) {
                val uri = data.data

                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                println("Success!!!")
                mBitmap = scaleImage(mBitmap)
                binding.mPhotoImageView.setImageBitmap(mBitmap)

            }
        } else {
            Toast.makeText(this, "Unrecognized request code", Toast.LENGTH_LONG).show()

        }
    }


    private fun scaleImage(bitmap: Bitmap?): Bitmap {
        val orignalWidth = bitmap!!.width
        val originalHeight = bitmap.height
        val scaleWidth = mInputSize.toFloat() / orignalWidth
        val scaleHeight = mInputSize.toFloat() / originalHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, orignalWidth, originalHeight, matrix, true)
    }

}