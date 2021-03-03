package com.example.receiptshare.activities.Main

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptshare.R
import com.example.receiptshare.activities.CreateReceipt.CreateReceiptActivity
import com.example.receiptshare.activities.CreateReceipt.adapter.ReceiptHolder
import com.example.receiptshare.activities.ViewReceipt.ViewReceiptActivity
import com.example.receiptshare.data.Receipt
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val RECEIPT_KEY = "ReceiptKey"
        const val RECEIPT_IMAGE_URI_EXTRA = "receiptImageUriExtra"
        const val PICK_IMAGE = 200
        const val CREATE_RECEIPT = 201
    }

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase

    private lateinit var mReceiptsRV: RecyclerView
    private lateinit var mActionBtn: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        bindViews()
    }

    private fun bindViews(){
        mActionBtn = findViewById(R.id.main_action_btn)
        mReceiptsRV = findViewById(R.id.main_receipts_rv)
        mActionBtn.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, PICK_IMAGE)
        }

        mReceiptsRV.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        bindItemsToRV()
    }

    private fun bindItemsToRV(){
        // Create Firebase query to get receipts, and bind to recycler view
        val query = mDatabase.reference.child("receipts")
            .orderByChild("receiptTime")

        val options: FirebaseRecyclerOptions<Receipt?> = FirebaseRecyclerOptions.Builder<Receipt>()
            .setQuery(query, Receipt::class.java)
            .setLifecycleOwner(this)
            .build()

        val receiptsRVadapter = object : FirebaseRecyclerAdapter<Receipt, ReceiptHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptHolder {
                return ReceiptHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.rv_receipts_list_item, parent, false)
                )
            }

            override fun onBindViewHolder(
                holder: ReceiptHolder,
                position: Int,
                model: Receipt
            ) {
                holder.bind(model)
                holder.itemView.setOnClickListener {
                    val key = getRef(itemCount - 1 - position).key
                    // TODO: Check if context is correct
                    val viewReceipt = Intent(this@MainActivity, ViewReceiptActivity::class.java)
                    viewReceipt.putExtra(RECEIPT_KEY, key)
                    startActivity(viewReceipt)
                }
            }

            override fun getItem(position: Int): Receipt {
                // Sort Firebase query by ascending order
                return super.getItem(itemCount - 1 - position)
            }
        }

        mReceiptsRV.layoutManager = LinearLayoutManager(this)
        mReceiptsRV.adapter = receiptsRVadapter
        mReceiptsRV.setOnClickListener {

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            val imageUri = data?.data
            val createReceipt = Intent(this, CreateReceiptActivity::class.java)
            createReceipt.putExtra(RECEIPT_IMAGE_URI_EXTRA, imageUri)
            startActivityForResult(createReceipt, CREATE_RECEIPT)
        }
    }
}