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
import dev.ltag.stone_payments.Transaction
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

            val provider = PosTransactionProvider(
                context,
                transactionObject,
                Stone.getUserModel(0),
            )

            provider.setConnectionCallback(object : StoneActionCallback {

                override fun onSuccess() {

                    when (val status = provider.transactionStatus) {
                        TransactionStatusEnum.APPROVED -> {

                            sendResult(transactionObject)
                            if (print == true) {
                                val posPrintReceiptProvider =
                                    PosPrintReceiptProvider(
                                        context, transactionObject,
                                        ReceiptType.MERCHANT,
                                    );

                                posPrintReceiptProvider.connectionCallback = object :
                                    StoneCallbackInterface {

                                    override fun onSuccess() {

                                        Log.d("SUCCESS", transactionObject.toString())
                                        
                                    }

                                    override fun onError() {
                                        val e = "Erro ao imprimir"
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

                }

                override fun onError() {

                    Log.d("RESULT", "ERROR")

                    sendAMessage(provider.transactionStatus?.name ?: "ERROR")

                    callback(Result.Error(Exception("ERROR")));
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
                selectedTransaction!!,
            )

            provider.setConnectionCallback(object : StoneCallbackInterface {

                override fun onSuccess() {

                    sendResult(selectedTransaction)
                    sendAMessage("CANCELLED")
                    if(print == true) {
                        val posPrintReceiptProvider =
                            PosPrintReceiptProvider(
                                context, selectedTransaction!!,
                                ReceiptType.MERCHANT,
                            );

                        posPrintReceiptProvider.connectionCallback = object :
                            StoneCallbackInterface {

                            override fun onSuccess() {

                                Log.d("SUCCESS", selectedTransaction!!.toString())
                                
                            }

                            override fun onError() {
                                val e = "Erro ao imprimir"
                                Log.d("ERRORPRINT", selectedTransaction!!.toString())

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
        var tr = Transaction(
            message.acquirerTransactionKey,
            message.initiatorTransactionKey,
            message.amount,
            message.typeOfTransaction.name,
            message.instalmentTransaction.name,
            message.instalmentType.name,
            message.cardHolderNumber,
            message.cardBrandName,
            message.cardHolderName,
            message.authorizationCode,
            message.transactionStatus.name,
            message.date,
            message.shortName,
            message.userModel.toString(),
            message.pinpadUsed,
            message.balance,
            message.isCapture,
            message.subMerchantCategoryCode,
            message.subMerchantAddress,
        )
        return tr.toJson()
    }
}