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
import com.example.receiptshare.data.User
import com.example.receiptshare.helpers.findFloats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import java.io.IOException
import java.util.*

/**
 * Displays a form to create a receipt and uploads it to the Firebase Database
 * The receipt image is provided through the calling activity's intent data
 */
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

    // Receipt instance variables
    private lateinit var mReceiptImageUri: Uri
    private var mReceiptTotal: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_receipt)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        // Get the selected receipt's image from intent data
        mReceiptImageUri = intent.extras?.getParcelable<Uri>(MainActivity.RECEIPT_IMAGE_URI_EXTRA)!!

        bindViews()
    }

    private fun bindViews() {
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
            // Return to Main Activity
            finish()
        }
    }

    /**
     * Uploads a Receipt instance to the Firebase Database.
     * Receipt image is stored in Firebase Storage.
     */
    private fun uploadReceipt() {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageUUID = UUID.randomUUID().toString()
        val receiptImageRef = storageRef.child("receipts").child(imageUUID)
        val uploadTask = receiptImageRef.putFile(mReceiptImageUri)

        // Upload receipt image to Firebase Storage
        uploadTask
            .addOnFailureListener {
                Log.d("CreateReceiptActivity", it.toString())
            }
            .addOnSuccessListener { _ ->
                // Upload Receipt instance to Firebase Database
                val receipt = Receipt(
                    mTitleET.text.toString(),
                    mAuth.currentUser?.displayName!!,
                    mRecipientET.text.toString(),
                    mTotalEt.text.toString().toDouble(),
                    mOwesET.text.toString().toDouble(),
                    PaymentStatus.UNPAID,
                    imageUUID
                )

                getRecipientUIDandUploadReceiptToDatabase(receipt)
            }
    }

    /**
     * Finds the UID of a user by using their display name.
     * Then, uploads the receipt to the database which
     * the sender of the receipt and the receiver are both able to access.
     */
    private fun getRecipientUIDandUploadReceiptToDatabase(receipt: Receipt){
        // Get the UID of the current user
        val usersRef = mDatabase.reference.child("users")
        val recipientName = mRecipientET.text.toString()
        // Find user with the requested name
        usersRef.orderByChild("name").equalTo(recipientName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (userSnapshot in dataSnapshot.children) {
                        // Convert userSnapshot into a User instance
                        val recipient = userSnapshot.getValue(User::class.java)!!
                        Log.d(TAG, "loadRecipient:Retrieved user $recipient")
                        // The resulting receipt filename
                        val receiptFileName = "${mAuth.currentUser!!.uid}_${recipient.uid}_${UUID.randomUUID()}"
                        uploadReceiptToDatabase(receipt, receiptFileName)
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "loadRecipient:onCancelled", databaseError.toException())
                }
            })
    }

    private fun uploadReceiptToDatabase(receipt: Receipt, fileName: String) {
        val dbReceiptRef = mDatabase.reference.child("receipts/$fileName")
        dbReceiptRef.setValue(receipt)
            .addOnCompleteListener {
                Log.d(TAG, "uploadReceipt:Successfully uploaded receipt $receipt to $fileName")
            }
            .addOnFailureListener {
                Log.e(TAG, "uploadReceipt:Failed uploading receipt $receipt", it)
            }
    }

    /**
     * Returns the purchase total from an image of a receipt
     */
    private fun getTotalFromReceiptImage(imageUri: Uri) {
        val recognizer = TextRecognition.getClient()
        var image: InputImage? = null
        try {
            image = InputImage.fromFilePath(this.applicationContext, imageUri)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var text = ""
        // Get receipt total
        recognizer.process(image).addOnSuccessListener {
            for (block in it.textBlocks) text += block.text + "\n"

            // Create a receipt using the extracted image text
            val receipt = createReceiptFromText(text)

            val receiptTotal = receipt.receiptTotal
            val t = "%.2f".format(receiptTotal)

            // Set the total EditText to receiptTotal
            mTotalEt.setText(t)
        }
    }

    /**
     * Returns an instance of a Receipt created by
     * extracting information from the text of a receipt image
     */
    private fun createReceiptFromText(text: String): Receipt {
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


