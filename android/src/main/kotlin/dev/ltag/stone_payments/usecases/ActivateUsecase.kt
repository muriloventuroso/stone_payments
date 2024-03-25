package dev.ltag.stone_payments.usecases

import android.content.Context
import android.util.Log
import dev.ltag.stone_payments.Result
import stone.application.StoneStart
import stone.application.interfaces.StoneCallbackInterface
import stone.providers.ActiveApplicationProvider
import stone.user.UserModel
import stone.utils.Stone
import stone.utils.keys.StoneKeyType
import java.lang.Exception

class ActivateUsecase(
    private val context: Context,
) {
    fun doActivate(appName: String, stoneCode: String, stoneKeys: List<String>, callback: (Result<Boolean>) -> Unit) {
        Stone.setAppName(appName);
        
        if (stoneKeys.size != 0 && stoneKeys.size != 2) {
            callback(Result.Error(Exception("Chaves inválidas")))
        }

        var stoneKeysHashed: HashMap<StoneKeyType, String> = HashMap<StoneKeyType, String>()
        if(stoneKeys.size == 2){
            stoneKeysHashed.put(StoneKeyType.QRCODE_AUTHORIZATION, stoneKeys[0]);
            stoneKeysHashed.put(StoneKeyType.QRCODE_PROVIDERID, stoneKeys[1]);
        }
        
        val userList: List<UserModel>? = StoneStart.init(context, stoneKeysHashed)

        if (userList == null) {
            val activeApplicationProvider = ActiveApplicationProvider(context)
            activeApplicationProvider.dialogMessage = "Ativando o Stone Code"
            activeApplicationProvider.dialogTitle = "Aguarde"
            activeApplicationProvider.connectionCallback = object : StoneCallbackInterface {

                override fun onSuccess() {
                    // SDK ativado com sucesso

                    callback(Result.Success(true))
                    Log.d("SUCESSO", "SUCESSO")
                }

                override fun onError() {
                    // Ocorreu algum erro na ativação

                    Log.d("ERROR", "ERRO")
                    callback(Result.Error(Exception("Erro ao Ativar")))
                }
            }
            activeApplicationProvider.activate(stoneCode)
        } else {
            // O SDK já foi ativado.

            callback(Result.Success(true))
        }
    }
}
