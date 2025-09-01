package marvin.com.br.diariobordomelosa.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;

@Entity(tableName = "manutencao_item")
@Data
public class ManutencaoItem {

    @PrimaryKey(autoGenerate = true)
    public Integer id;

    public Integer manutencaoId;
    public String descricao;
    public Double quantidade;
    public String unidade;
    public String sit;
}
