package marvin.com.br.diariobordomelosa.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import marvin.com.br.diariobordomelosa.model.ManutencaoItem;
import marvin.com.br.diariobordomelosa.model.ManutencaoModel;

@Dao
public interface ManutencaoDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long inserir(ManutencaoModel m);

    @Query("SELECT * FROM manutencao_model where sit = 'realizado'")
    List<ManutencaoModel> pegar_manutencoes_realizadas();

    @Query("SELECT * FROM manutencao_model order by id desc limit 1")
    ManutencaoModel pegar_ultima_manutencao();

    @Query("DELETE FROM manutencao_model WHERE sit = 'sincronizado' ")
    void apagar_sincronizados();

    @Query("DELETE FROM manutencao_model WHERE id =:id")
    void cancelar_manutencao(Integer id);

    @Update
    void updateAll(List<ManutencaoModel> manutencoes);
}
