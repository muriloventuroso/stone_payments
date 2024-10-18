package dev.ltag.stone_payments.usecases

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.util.Base64
import br.com.stone.posandroid.providers.PosPrintReceiptProvider
import br.com.stone.posandroid.providers.PosTransactionProvider
import stone.providers.CancellationProvider;
import dev.ltag.stone_payments.Result
import dev.ltag.stone_payments.StonePaymentsPlugin
import io.flutter.plugin.common.MethodChannel
import stone.application.enums.*
import stone.application.interfaces.StoneActionCallback
import stone.application.interfaces.StoneCallbackInterface
import stone.database.transaction.TransactionObject
import stone.database.transaction.TransactionDAO
import stone.utils.Stone

class PaymentUsecase(
    private val stonePayments: StonePaymentsPlugin,
) {
    private val context = stonePayments.context;
    

    fun doPayment(
        value: Double,
        type: Int,
        installment: Int,
        print: Boolean?,
        callback: (Result<Boolean>) -> Unit,
    ) {
        try {
            stonePayments.transactionObject = TransactionObject()

            val transactionObject = stonePayments.transactionObject

            transactionObject.instalmentTransaction =
                InstalmentTransactionEnum.getAt(installment - 1);
            transactionObject.typeOfTransaction =
                if (type == 1) TypeOfTransactionEnum.CREDIT else if (type == 2) TypeOfTransactionEnum.PIX else TypeOfTransactionEnum.DEBIT;
            transactionObject.isCapture = true;
            val newValue: Int = (value * 100).toInt();
            transactionObject.amount = newValue.toString();

            stonePayments.providerPosTransaction = PosTransactionProvider(
                context,
                transactionObject,
                Stone.getUserModel(0),
            )

            var provider = stonePayments.providerPosTransaction!!

            provider.setConnectionCallback(object : StoneActionCallback {

                override fun onSuccess() {
                    sendResult(transactionObject)
                    when (val status = provider.transactionStatus) {
                        TransactionStatusEnum.APPROVED -> {
                            if (print == true) {
                                val posPrintReceiptProvider =
                                    PosPrintReceiptProvider(
                                        context, transactionObject,
                                        ReceiptType.MERCHANT,
                                    );

                                posPrintReceiptProvider.connectionCallback = object :
                                    StoneCallbackInterface {

                                    override fun onSuccess() {
                                        Log.d("SUCCESS", "success print")
                                        Log.d("SUCCESS", transactionObject.toString())
                                        
                                    }

                                    override fun onError() {
                                        Log.d("ERRORPRINT", transactionObject.toString())

                                    }
                                }

                                posPrintReceiptProvider.execute()

                            }
                            sendAMessage("APPROVED")

                            callback(Result.Success(true))
                        }
                        TransactionStatusEnum.DECLINED -> {
                            val message = provider.messageFromAuthorize
                            sendAMessage(message ?: "DECLINED")
                            callback(Result.Success(false))
                        }
                        TransactionStatusEnum.REJECTED -> {
                            val message = provider.messageFromAuthorize
                            sendAMessage(message ?: "REJECTED")
                            callback(Result.Success(false))
                        }
                        else -> {
                            val message = provider.messageFromAuthorize
                            sendAMessage(message ?: status.name)
                        }
                    }
                    stonePayments.providerPosTransaction = null
                }

                override fun onError() {

                    Log.d("RESULT", "ERROR")

                    sendAMessage(provider.transactionStatus?.name ?: "ERROR")

                    callback(Result.Error(Exception("ERROR")));
                    stonePayments.providerPosTransaction = null
                }

                override fun onStatusChanged(p0: Action?) {
                    
                    if (p0 == Action.TRANSACTION_WAITING_QRCODE_SCAN) {
                        sendAQRCode(transactionObject.getQRCode())
                    }
                    sendAMessage(p0?.name!!)
                    
                }
            })

            provider.execute()


        } catch (e: Exception) {
            Log.d("ERROR", e.toString())
            callback(Result.Error(e));
        }

    }

    fun abortPayment(callback: (Result<Boolean>) -> Unit) {
        try {
            if (stonePayments.providerPosTransaction == null) {
                callback(Result.Success(false))
                return
            }
            stonePayments.providerPosTransaction?.abortPayment()
            callback(Result.Success(true))

        } catch (e: Exception) {
            Log.d("ERROR", e.toString())
            callback(Result.Error(e));
        }
    }

    fun cancel(
        transactionId: String,
        print: Boolean?,
        callback: (Result<Boolean>) -> Unit,
    ) {
        try {
            val transactionDAO = TransactionDAO(context)
            val selectedTransaction = transactionDAO.findTransactionWithInitiatorTransactionKey(transactionId);

            if(selectedTransaction == null) {
                callback(Result.Error(Exception("NOT FOUND")))
                return
            }

            val provider = CancellationProvider(
                context,
                selectedTransaction,
            )

            provider.setConnectionCallback(object : StoneCallbackInterface {

                override fun onSuccess() {

                    sendResult(selectedTransaction)
                    sendAMessage("CANCELLED")
                    if(print == true) {
                        val posPrintReceiptProvider =
                            PosPrintReceiptProvider(
                                context, selectedTransaction,
                                ReceiptType.MERCHANT,
                            );

                        posPrintReceiptProvider.connectionCallback = object :
                            StoneCallbackInterface {

                            override fun onSuccess() {

                                Log.d("SUCCESS", selectedTransaction.toString())
                                
                            }

                            override fun onError() {
                                Log.d("ERRORPRINT", selectedTransaction.toString())

                            }
                        }

                        posPrintReceiptProvider.execute()
                    }

                    callback(Result.Success(true))

                }

                override fun onError() {

                    Log.d("RESULT", "ERROR")

                    callback(Result.Error(Exception("ERROR")));
                }
            })

            provider.execute()


        } catch (e: Exception) {
            Log.d("ERROR", e.toString())
            callback(Result.Error(e));
        }

    }

    private fun sendAMessage(message: String) {
        Handler(Looper.getMainLooper()).post {
            val channel = MethodChannel(
                StonePaymentsPlugin.flutterBinaryMessenger!!,
                "stone_payments",
            )
            channel.invokeMethod("message", message)
        }
    }

    private fun sendResult(message: TransactionObject) {
        Handler(Looper.getMainLooper()).post {
            val channel = MethodChannel(
                StonePaymentsPlugin.flutterBinaryMessenger!!,
                "stone_payments",
            )
            channel.invokeMethod("transaction", transactionToJson(message))
        }
    }

    private fun sendAQRCode(message: Bitmap) {
        Handler(Looper.getMainLooper()).post {
            val channel = MethodChannel(
                StonePaymentsPlugin.flutterBinaryMessenger!!,
                "stone_payments",
            )
            channel.invokeMethod("qrcode", BitMapToString(message))
        }
    }

    fun BitMapToString(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    private fun transactionToJson(message: TransactionObject) : String {
        var jsonString = "{";
        jsonString = jsonString.plus("\"acquirerTransactionKey\": \"${message.acquirerTransactionKey}\",");
        jsonString = jsonString.plus("\"initiatorTransactionKey\": \"${message.initiatorTransactionKey}\",");
        jsonString = jsonString.plus("\"amount\": \"${message.amount}\",");
        if(message.typeOfTransaction != null){
            jsonString = jsonString.plus("\"typeOfTransaction\": \"${message.typeOfTransaction.name}\",");
        }
        if(message.instalmentTransaction != null){
            jsonString = jsonString.plus("\"instalmentTransaction\": \"${message.instalmentTransaction.name}\",");
        }
        if(message.instalmentType != null){
            jsonString = jsonString.plus("\"instalmentType\": \"${message.instalmentType.name}\",");
        }
        jsonString = jsonString.plus("\"cardHolderNumber\": \"${message.cardHolderNumber}\",");
        jsonString = jsonString.plus("\"cardBrandName\": \"${message.cardBrandName}\",");
        jsonString = jsonString.plus("\"cardHolderName\": \"${message.cardHolderName}\",");
        jsonString = jsonString.plus("\"authorizationCode\": \"${message.authorizationCode}\",");
        if(message.transactionStatus != null){
            jsonString = jsonString.plus("\"transactionStatus\": \"${message.transactionStatus.name}\",");
        }
        jsonString = jsonString.plus("\"date\": \"${message.date}\",");
        jsonString = jsonString.plus("\"time\": \"${message.time}\",");
        if(message.entryMode != null){
            jsonString = jsonString.plus("\"entryMode\": \"${message.entryMode.toString()}\",");
        }
        jsonString = jsonString.plus("\"aid\": \"${message.aid}\",");
        jsonString = jsonString.plus("\"arcq\": \"${message.arcq}\",");
        jsonString = jsonString.plus("\"shortName\": \"${message.shortName}\",");
        if(message.userModel != null){
            jsonString = jsonString.plus("\"userModel\": \"${message.userModel.toString()}\",");
        }
        jsonString = jsonString.plus("\"pinpadUsed\": \"${message.pinpadUsed}\",");
        jsonString = jsonString.plus("\"balance\": \"${message.balance}\",");
        jsonString = jsonString.plus("\"isCapture\": \"${message.isCapture.toString()}\",");
        jsonString = jsonString.plus("\"subMerchantCategoryCode\": \"${message.subMerchantCategoryCode}\",");
        jsonString = jsonString.plus("\"subMerchantAddress\": \"${message.subMerchantAddress}\"");
        jsonString = jsonString.plus("}");
        return jsonString;
    }
}