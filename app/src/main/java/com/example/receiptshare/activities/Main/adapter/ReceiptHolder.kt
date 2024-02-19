package com.example.receiptshare.activities.Main.adapter

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptshare.R
import com.example.receiptshare.data.PaymentStatus
import com.example.receiptshare.data.Receipt
import java.text.SimpleDateFormat

class ReceiptHolder(val customView: View, var receipt: Receipt? = null) : RecyclerView.ViewHolder(
    customView
) {

    fun bind(receipt: Receipt){
        with(receipt) {
            customView.findViewById<TextView>(R.id.rv_receipts_creator_tv).text = receiptCreator
            customView.findViewById<TextView>(R.id.rv_receipts_title_tv).text = receiptTitle
            customView.findViewById<TextView>(R.id.rv_receipts_recipient_tv).text = receiptRecipient
            customView.findViewById<TextView>(R.id.rv_receipts_total_tv).text = receiptTotal.toString()
            customView.findViewById<TextView>(R.id.rv_receipts_owe_tv).text = recipientOwes.toString()
            customView.findViewById<TextView>(R.id.rv_receipts_date_tv).text = getDate(receiptTime!!)
            customView.findViewById<TextView>(R.id.rv_receipts_status_tv).text = paymentStatus.status

            when (paymentStatus) {
                PaymentStatus.UNPAID -> {
                    customView.findViewById<TextView>(R.id.rv_receipts_status_tv).setTextColor(Color.RED)
                }
                PaymentStatus.VERIFYPAYMENT -> {
                    customView.findViewById<TextView>(R.id.rv_receipts_status_tv).setTextColor(Color.YELLOW)
                }
                PaymentStatus.PAID -> {
                    customView.findViewById<TextView>(R.id.rv_receipts_status_tv).setTextColor(Color.GREEN)
                }
            }
        }
    }

    private fun getDate(milliSeconds: Long): String? {
        val simpleDateFormat = SimpleDateFormat("M/d")
        return simpleDateFormat.format(milliSeconds)
    }

}