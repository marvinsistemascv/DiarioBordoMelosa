package marvin.com.br.diariobordomelosa.Adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import marvin.com.br.diariobordomelosa.DAO.AppDatabase;
import marvin.com.br.diariobordomelosa.DAO.ManutencaoItemDAO;
import marvin.com.br.diariobordomelosa.R;
import marvin.com.br.diariobordomelosa.model.ManutencaoItem;

public class ItemLctoAdapter extends RecyclerView.Adapter<ItemLctoAdapter.ItemViewHolder> {

    private final List<ManutencaoItem> lista_itens;
    private final Context context;
    ManutencaoItemDAO ItemDAO;

    public ItemLctoAdapter(List<ManutencaoItem> lista_itens, Context context) {
        this.lista_itens = lista_itens; // 👉 sem new ArrayList<>
        this.context = context;
        AppDatabase db = AppDatabase.getInstance(context);
        ItemDAO = db.manutencaoItemDAO();
    }


    public void excluir_item(int id_item, int position) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                ItemDAO.excluir_item(id_item);

                new Handler(Looper.getMainLooper()).post(() -> {
                    lista_itens.remove(position); // remove da lista do adapter
                    notifyItemRemoved(position); // avisa o RecyclerView
                    Toast.makeText(context, "Item excluído", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception ex) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    new AlertDialog.Builder(context)
                            .setTitle("ERRO")
                            .setIcon(R.drawable.ic_atencao)
                            .setMessage("Erro ao excluir item: " + ex.getMessage())
                            .setPositiveButton("ENTENDI", null)
                            .create()
                            .show();
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista_itens.size();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manutencao, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {

        ManutencaoItem item = lista_itens.get(position);

        holder.txt_desc_item.setText(item.descricao);
        holder.txt_qtd_item.setText(item.quantidade.toString());
        holder.txt_un_item.setText(item.unidade);

        holder.btn_excluir_item.setOnClickListener(v -> {

            new AlertDialog.Builder(context)
                    .setTitle("excluir o item?")
                    .setIcon(R.drawable.ic_atencao)
                    .setMessage("confirma a operação?")
                    .setNegativeButton("não", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setPositiveButton("Sim, excluir", (dialog, which) -> {
                        excluir_item(item.id,holder.getAdapterPosition());
                    })
                    .create()
                    .show();
        });
    }


    static class ItemViewHolder extends RecyclerView.ViewHolder {

        TextView txt_desc_item;
        TextView txt_qtd_item;
        TextView txt_un_item;
        Button btn_excluir_item;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            txt_desc_item = itemView.findViewById(R.id.txt_descricao_item_lcto);
            txt_qtd_item = itemView.findViewById(R.id.txt_qtd_item_lcto);
            txt_un_item = itemView.findViewById(R.id.txt_un_item_lcto);
            btn_excluir_item = itemView.findViewById(R.id.btnExcluiItem);

        }
    }


}
