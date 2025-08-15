package marvin.com.br.diariobordomelosa.DAO;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import marvin.com.br.diariobordomelosa.model.AbastecimentoModel;


@Dao
public interface AbastecimentoDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserir(AbastecimentoModel a);

    @Update
    void updateAll(List<AbastecimentoModel> abastecimentos);

    @Query("SELECT * FROM abastecimento_model where sit = 'realizado'")
    List<AbastecimentoModel> pegar_abastecimentos_realizados();

    @Query("DELETE FROM abastecimento_model WHERE sit = 'sincronizado' ")
    void apagar_sincronizados();
}
