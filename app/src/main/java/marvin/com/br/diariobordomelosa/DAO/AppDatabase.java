package marvin.com.br.diariobordomelosa.DAO;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import marvin.com.br.diariobordomelosa.model.AbastecimentoModel;
import marvin.com.br.diariobordomelosa.model.ManutencaoItem;
import marvin.com.br.diariobordomelosa.model.ManutencaoModel;
import marvin.com.br.diariobordomelosa.model.MotoristaModel;


@Database(entities = {AbastecimentoModel.class, MotoristaModel.class,
        ManutencaoModel.class, ManutencaoItem.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AbastecimentoDAO abastecimentoDAO();

    public abstract MotoristaDAO motoristaDAO();

    public abstract ManutencaoDAO manutencaoDAO();

    public abstract ManutencaoItemDAO manutencaoItemDAO();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 10;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "app-db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }


}
