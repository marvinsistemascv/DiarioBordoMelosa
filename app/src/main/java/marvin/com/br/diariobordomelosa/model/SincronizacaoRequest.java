package marvin.com.br.diariobordomelosa.model;

import java.util.List;

public class SincronizacaoRequest {

    private List<AbastecimentoModel> abastecimentos;
    private List<ManutencaoModel> manutencoes;
    private List<ManutencaoItem> intes_manutencao;

    public SincronizacaoRequest(List<AbastecimentoModel> abastecimentos,
                                List<ManutencaoModel> manutencoes,
                                List<ManutencaoItem> intes_manutencao) {
        this.abastecimentos = abastecimentos;
        this.manutencoes = manutencoes;
        this.intes_manutencao = intes_manutencao;
    }

    public List<AbastecimentoModel> getCadastros() { return abastecimentos; }

    public List<ManutencaoModel> getManutencoes() {return manutencoes;}

    public List<ManutencaoItem> getIntes_manutencao() {return intes_manutencao;}
}
