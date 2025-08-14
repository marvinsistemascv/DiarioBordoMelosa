package marvin.com.br.diariobordomelosa.model;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;

@Entity(tableName = "abastecimento_model")
@Data
public class AbastecimentoModel {

    @PrimaryKey(autoGenerate = true)
    public Integer id;
    public Integer cod_veiculo;
    public String data_abastecimento;
    public String hora_abastecimento;
    public String motorista_melosa;
    public Double qtd_litros;
    public Double km_horimetro;
    public String sit;

}
