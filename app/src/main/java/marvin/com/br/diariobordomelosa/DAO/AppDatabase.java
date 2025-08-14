package marvin.com.br.diariobordomelosa.DAO;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import marvin.com.br.diariobordomelosa.model.AbastecimentoModel;
import marvin.com.br.diariobordomelosa.model.MotoristaModel;


@Database(entities = {AbastecimentoModel.class, MotoristaModel.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AbastecimentoDAO abastecimentoDAO();

    public abstract MotoristaDAO motoristaDAO();
}
