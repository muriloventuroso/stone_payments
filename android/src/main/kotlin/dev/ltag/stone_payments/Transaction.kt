package dev.ltag.stone_payments

import com.beust.klaxon.*

        private val klaxon = Klaxon()

data class Transaction (
    val acquirerTransactionKey: String?,
    val initiatorTransactionKey: String?,
    val amount: String?,
    val typeOfTransaction: String?,
    val instalmentTransaction: String?,
    val instalmentType: String?,
    val cardHolderNumber: String?,
    val cardBrandName: String?,
    val cardHolderName: String?,
    val authorizationCode: String?,
    val transactionStatus: String?,
    val date: String?,
    val time: String?,
    val entryMode: String?,
    val aid: String?,
    val arcq: String?,
    val shortName: String?,
    val userModel: String?,
    val pinpadUsed: String?,
    val balance: String?,
    val capture: Boolean?,
    val subMerchantCategoryCode: String?,
    val subMerchantAddress: String?,
    
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Transaction>(json)
    }
}