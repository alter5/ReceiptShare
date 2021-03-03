package com.example.receiptshare.activities.CreateReceipt

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.example.receiptshare.R
import com.example.receiptshare.activities.Main.MainActivity
import com.example.receiptshare.data.PaymentStatus
import com.example.receiptshare.data.Receipt
import com.example.receiptshare.helpers.findFloats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import java.io.IOException
import java.lang.String.format
import java.util.*

class CreateReceiptActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CreateReceiptActivity"
    }

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase

    private lateinit var mReceiptImageIV: ImageView
    private lateinit var mTitleET: EditText
    private lateinit var mRecipientET: EditText
    private lateinit var mTotalEt: EditText
    private lateinit var mOwesET: EditText
    private lateinit var mSubmitBtn: Button

    private lateinit var mReceiptImageUri: Uri
    private var mReceiptTotal: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_receipt)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        mReceiptImageUri = intent.extras?.getParcelable<Uri>(MainActivity.RECEIPT_IMAGE_URI_EXTRA)!!

        bindViews()
    }

    private fun bindViews(){
        mReceiptImageIV = findViewById(R.id.createreceipt_receipt_image_iv)
        mTitleET = findViewById(R.id.createreceipt_title_et)
        mRecipientET = findViewById(R.id.createreceipt_recipient_et)
        mTotalEt = findViewById(R.id.createreceipt_total_et)
        mOwesET = findViewById(R.id.createreceipt_owes_et)

        mReceiptImageIV.setImageURI(mReceiptImageUri)
        mTotalEt.setText(String.format("%.2f", mReceiptTotal))
        getTotalFromReceiptImage(mReceiptImageUri)

        mSubmitBtn = findViewById(R.id.createreceipt_submit_btn)
        mSubmitBtn.setOnClickListener {
            uploadReceipt()
            finish()
        }
    }

    private fun uploadReceipt() {
        val storageRef = FirebaseStorage.getInstance().reference
        val photoUUID = UUID.randomUUID().toString()
        val receiptRef = storageRef.child("receipts").child(photoUUID)
        val uploadTask = receiptRef.putFile(mReceiptImageUri)

        // Upload receipt to Firebase Storage
        uploadTask.addOnFailureListener {
            Log.d("CreateReceiptActivity", it.toString())
        }.addOnSuccessListener { _ ->
            val receipt = Receipt(
                mTitleET.text.toString(),
                mAuth.currentUser?.displayName!!,
                mRecipientET.text.toString(),
                mTotalEt.text.toString().toDouble(),
                mOwesET.text.toString().toDouble(),
                PaymentStatus.UNPAID,
                photoUUID
            )

            val dbRef = mDatabase.reference.child("receipts")
            dbRef.push().setValue(receipt)
        }
    }

    private fun getTotalFromReceiptImage(imageUri: Uri){
        val recognizer = TextRecognition.getClient()
        var image: InputImage? = null
        try {
            image = InputImage.fromFilePath(this.applicationContext, imageUri)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var text = ""
        recognizer.process(image).addOnSuccessListener {
            for (block in it.textBlocks) text += block.text + "\n"
            val receiptTotal = createReceiptFromText(text).receiptTotal
            val s = "%.2f".format(receiptTotal)
            mTotalEt.setText(s)
        }
    }

    private fun createReceiptFromText(text:String): Receipt {
        // Finds all floats in the receipt
        val receiptFloats = text.findFloats()

        val receipt = Receipt()
        if (receiptFloats.isEmpty()) return receipt
        else {
            receipt.receiptTotal = (receiptFloats.maxOrNull() ?: 0.0f).toDouble()
            // TODO: Add tax to Receipt class
            // receipt.receiptTax = receiptFloats.findSecondLargest() ?: 0.0f
            // receipt.title = text.firstLine()

            return receipt
        }
    }
}


