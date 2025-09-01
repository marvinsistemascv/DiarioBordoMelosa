package marvin.com.br.diariobordomelosa.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;
@Entity(tableName = "manutencao_model")
@Data
public class ManutencaoModel {

    @PrimaryKey(autoGenerate = true)
    public Integer id;
    public Integer cod_veiculo;
    public String data_manutencao;
    public String hora_manutencao;
    public String motorista_melosa;
    public String melosa;
    public Double km_horimetro;
    public String sit;

}
