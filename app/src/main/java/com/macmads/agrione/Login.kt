package com.macmads.agrione

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.macmads.agrione.databinding.ActivityLoginBinding
import com.macmads.agrione.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = Firebase.auth
       if( auth.currentUser != null)
       {
           startActivity(Intent(this, MainActivity::class.java))
           finish()
       }
        binding.sendSMSCard.setOnClickListener {
             if(binding.editTextMobile.text.isEmpty()){
                 Toast.makeText(this, "Please Enter Your Mobile Number", Toast.LENGTH_SHORT).show()
             }else{
                 var intent = Intent(this, VerifyOTP::class.java)
                 intent.putExtra("number", binding.editTextMobile.text!!.toString())
                 startActivity(intent)
                 finish()
             }
        }
    }
}