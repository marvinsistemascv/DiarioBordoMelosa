package marvin.com.br.diariobordomelosa.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import marvin.com.br.diariobordomelosa.model.MotoristaModel;

@Dao
public interface MotoristaDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserir(MotoristaModel e);

    @Query("SELECT * FROM motorista_model where id =1")
    MotoristaModel pegar_motorista();

}
