package com.example.receiptshare.data

import java.util.*

enum class PaymentStatus(val status : String) {
    UNPAID("Unpaid"), VERIFYPAYMENT("Verify payment"), PAID("Paid")
}

data class Receipt(
    val receiptTitle: String = "",
    val receiptCreator: String = "",
    val receiptRecipient: String = "",
    var receiptTotal: Double = 0.0,
    val recipientOwes: Double = 0.0,
    var paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    var receiptPhotoUUID: String = "",
    val receiptTime: Long? = Date().time){

}