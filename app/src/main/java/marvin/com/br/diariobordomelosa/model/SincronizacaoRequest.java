package marvin.com.br.diariobordomelosa.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SincronizacaoRequest {

    private List<AbastecimentoModel> abastecimentos;
    private List<ManutencaoModel> manutencoes;

    @SerializedName("itensManutencao") // 👈 garante compatibilidade com o backend
    private List<ManutencaoItem> itensManutencao;

    public SincronizacaoRequest(List<AbastecimentoModel> abastecimentos,
                                List<ManutencaoModel> manutencoes,
                                List<ManutencaoItem> itensManutencao) {
        this.abastecimentos = abastecimentos;
        this.manutencoes = manutencoes;
        this.itensManutencao = itensManutencao;
    }

    public List<AbastecimentoModel> getAbastecimentos() { return abastecimentos; }

    public List<ManutencaoModel> getManutencoes() { return manutencoes; }

    public List<ManutencaoItem> getItensManutencao() { return itensManutencao; }
}

