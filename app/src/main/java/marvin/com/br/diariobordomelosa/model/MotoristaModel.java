package marvin.com.br.diariobordomelosa.model;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;

@Entity(tableName = "motorista_model")
@Data
public class MotoristaModel {

    @PrimaryKey(autoGenerate = true)
    public Integer id;
    public String nome;
    public String placa_melosa;

}
