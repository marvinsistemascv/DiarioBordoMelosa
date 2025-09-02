package marvin.com.br.diariobordomelosa.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import marvin.com.br.diariobordomelosa.model.AbastecimentoModel;
import marvin.com.br.diariobordomelosa.model.ManutencaoItem;
import marvin.com.br.diariobordomelosa.model.ManutencaoModel;

@Dao
public interface ManutencaoItemDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long inserir(ManutencaoItem m);

    @Query("SELECT * FROM manutencao_item where manutencaoId =:id_manutencao")
    List<ManutencaoItem> pegar_itens_manutencao(Integer id_manutencao);

    @Query("SELECT * FROM manutencao_item where sit ='lancado'")
    List<ManutencaoItem> pegar_todas_realizadas();

    @Query("DELETE FROM manutencao_item WHERE id =:id")
    void excluir_item(Integer id);

    @Query("DELETE FROM manutencao_item WHERE sit ='lancado'")
    void apagar_sincronizados();

    @Query("DELETE FROM manutencao_item WHERE id IN (:ids)")
    void apagar_itens_manutencao(List<Integer> ids);

    @Query("SELECT * FROM manutencao_item WHERE sit = 'lancado' AND manutencaoId IN (:ids)")
    List<ManutencaoItem> pegar_itens_manutencoes_realizadas(List<Integer> ids);


    @Update
    void updateAll(List<ManutencaoItem> itens);
}
