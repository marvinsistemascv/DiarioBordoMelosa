package marvin.com.br.diariobordomelosa.repository;


import marvin.com.br.diariobordomelosa.model.SincronizacaoRequest;
import marvin.com.br.diariobordomelosa.model.VersaoResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface RetroServiceInterface {

    @GET("/app_obras/versao_app")
    Call<VersaoResponse> verificarVersao();

    @POST("/app_obras/sincronizar_abastecimentos")
    Call<ResponseBody> sincronizarTudo(@Body SincronizacaoRequest request);

}
