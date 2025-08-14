package marvin.com.br.diariobordomelosa.model;

import java.util.List;

public class SincronizacaoRequest {
    private List<AbastecimentoModel> abastecimentos;

    public SincronizacaoRequest(List<AbastecimentoModel> abastecimentos) {
        this.abastecimentos = abastecimentos;
    }
    public List<AbastecimentoModel> getCadastros() { return abastecimentos; }

}
