package com.example.receiptshare.activities.ViewReceipt

import android.graphics.Color
import android.icu.text.SimpleDateFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.receiptshare.R
import com.example.receiptshare.activities.Main.MainActivity
import com.example.receiptshare.data.PaymentStatus
import com.example.receiptshare.data.Receipt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

/**
 * Displays the contents of a Receipt. Includes its image, total, sender, and etc.
 */
class ViewReceiptActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ViewReceiptActivity"
    }

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var mStorage: FirebaseStorage

    private lateinit var mReceiptImageIV: ImageView
    private lateinit var mTitleTV: TextView
    private lateinit var mCreatorTV: TextView
    private lateinit var mRecipientTV: TextView
    private lateinit var mTotalTV: TextView
    private lateinit var mOweTV: TextView
    private lateinit var mDateTV: TextView
    private lateinit var mStatusTV: TextView
    private lateinit var mActionBtn: Button

    private lateinit var mReceipt: Receipt
    private var mReceiptDatabaseKey: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_receipt)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        mStorage = FirebaseStorage.getInstance()

        mReceiptDatabaseKey = intent.extras?.getString(MainActivity.RECEIPT_KEY)

        bindViews()
    }

    private fun bindViews(){
        mReceiptImageIV = findViewById(R.id.viewreceipt_receipt_image_iv)
        mTitleTV = findViewById(R.id.viewreceipt_title_tv)
        mCreatorTV = findViewById(R.id.viewreceipt_creator_tv)
        mRecipientTV = findViewById(R.id.viewreceipt_recipient_tv)
        mTotalTV = findViewById(R.id.viewreceipt_total_tv)
        mOweTV = findViewById(R.id.viewreceipt_owe_tv)
        mDateTV = findViewById(R.id.viewreceipt_date_tv)
        mStatusTV = findViewById(R.id.viewreceipt_status_tv)
        mActionBtn = findViewById(R.id.viewreceipt_action_btn)

        getReceiptFromDatabaseAndBindToViews()
    }

    /**
     * Retrieves a Receipt from the Firebase Database and
     * binds its member variables to the activity's views, including its image
     */
    private fun getReceiptFromDatabaseAndBindToViews() {
        val receiptRef = mDatabase.reference.child("receipts").child(mReceiptDatabaseKey!!)
        receiptRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                mReceipt = snapshot.getValue(Receipt::class.java)!!
                bindReceiptToViews()
                Log.d(TAG, mReceipt.receiptTitle)
            }
        })
    }

    private fun bindReceiptToViews() {
        with(mReceipt) {
            mTitleTV.text = receiptTitle
            mCreatorTV.text = receiptCreator
            mRecipientTV.text = receiptRecipient
            mTotalTV.text = receiptTotal.toString()
            mOweTV.text = recipientOwes.toString()
            mDateTV.text = getDate(receiptTime!!)
            mStatusTV.text = paymentStatus.status
        }
        setActionButtonFunctionality()
        bindReceiptImageWithImageView()
    }

    private fun bindReceiptImageWithImageView() {
        val imageRef: StorageReference =
            mStorage.reference.child("receipts").child("${mReceipt.receiptPhotoUUID}")

        imageRef.downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).into(mReceiptImageIV)
        }
    }

    /**
     * Sets the functionality for the ViewReceiptActivity button.
     * Functionality depends on whether the current user is the receipt's sender or recipient.
     * Functionality also depends on whether the recipient has listed the receipt as paid.
     */
    private fun setActionButtonFunctionality(){
        with (mReceipt) {
            // Determine action button functionality
            val currUser = mAuth.currentUser?.displayName
            var text = ""
            if (currUser == receiptCreator) {
                if (paymentStatus == PaymentStatus.UNPAID) {
                    text = "Awaiting payment"
                    mActionBtn.isClickable = false
                    mActionBtn.setBackgroundColor(Color.GRAY)
                } else {
                    text = "Confirm payment"
                    mActionBtn.setOnClickListener {
                        paymentStatus = PaymentStatus.PAID
                        mActionBtn.text = paymentStatus.status
                        mActionBtn.isClickable = false
                        mActionBtn.setBackgroundColor(Color.GRAY)
                    }
                }
            } else if (currUser == receiptRecipient) {
                text = "Pay Now"
                mActionBtn.setOnClickListener {
                    paymentStatus = PaymentStatus.VERIFYPAYMENT
                    mActionBtn.isClickable = false
                    mActionBtn.setBackgroundColor(Color.GRAY)
                }
            } else {
                // Hide button
                mActionBtn.visibility = View.GONE
            }
            Log.d(TAG, "Button text = $text")
            mActionBtn.text = text
        }
    }

    private fun getDate(milliSeconds: Long): String? {
        val simpleDateFormat = SimpleDateFormat("M/d")
        return simpleDateFormat.format(milliSeconds)
    }
}