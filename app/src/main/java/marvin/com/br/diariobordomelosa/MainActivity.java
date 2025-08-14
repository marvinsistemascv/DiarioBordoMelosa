package marvin.com.br.diariobordomelosa;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marvin.com.br.diariobordomelosa.DAO.ApiClient;
import marvin.com.br.diariobordomelosa.DAO.AppDatabase;
import marvin.com.br.diariobordomelosa.model.AbastecimentoModel;
import marvin.com.br.diariobordomelosa.model.MotoristaModel;
import marvin.com.br.diariobordomelosa.repository.RetroServiceInterface;
import marvin.com.br.diariobordomelosa.util.DataHora;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private AlertDialog dialog;                         // <— diálogo atual
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean pediuCadastroEncarregado = false;   // evita abrir 2x
    private RetroServiceInterface service;
    Retrofit retrofit;
    private TextView txtVersao;
    private TextView txtMotorista;
    private TextView txtPlacaMelosa;

    private final ActivityResultLauncher<Intent> qrLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String qr = result.getData().getStringExtra("qr_text");
                    Integer veiculoId = extrairIdVeiculo(qr);
                    if (veiculoId != null) {
                        MediaPlayer mp = MediaPlayer.create(this, R.raw.camera_som);
                        mp.setOnCompletionListener(MediaPlayer::release);
                        mp.start();
                        fazer_novo_abastecimento(veiculoId);
                    } else {
                        Toast.makeText(this, "QR inválido", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        retrofit = ApiClient.getClient(this);
        service = retrofit.create(RetroServiceInterface.class);


        txtVersao = findViewById(R.id.txtVersao);
        txtMotorista = findViewById(R.id.txt_nome_motorista);
        txtPlacaMelosa = findViewById(R.id.txt_placa_melosa);

        try {
            db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                    .fallbackToDestructiveMigration()
                    .build();
        } catch (Exception e) {
            showToastErro("ERRO DB: " + e.getMessage());
        }

        // Carrega encarregado e decide se dá boas-vindas ou pede cadastro.
        io.execute(() -> {
            MotoristaModel e = null;
            try {
                e = db.motoristaDAO().pegar_motorista();
            } catch (Exception ex) {
                MotoristaModel finalE = e;
                runOnUiThread(() -> showToast("Erro ao ler encarregado: " + ex.getMessage()));
            }

            MotoristaModel finalE = e;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (finalE != null) {
                    txtMotorista.setText(finalE.nome);
                    txtPlacaMelosa.setText(finalE.placa_melosa);
                    // segue o fluxo normal da Main sem finalizar aqui
                } else if (!pediuCadastroEncarregado) {
                    pediuCadastroEncarregado = true;
                    abrir_cadastro_motorista();
                }
            });
        });
    }

    private void abrir_cadastro_motorista() {

        if (isFinishing() || isDestroyed()) return;

        View viewInflated = LayoutInflater.from(this)
                .inflate(R.layout.dialog_cad_motorista, null);

        EditText inputNome = viewInflated.findViewById(R.id.inputNomeMotorista);
        EditText inputPlacaMelosa = viewInflated.findViewById(R.id.inputPlacaMelosa);
        Button btnAdicionar = viewInflated.findViewById(R.id.btnGravaMotorista);
        Button btnCancelar = viewInflated.findViewById(R.id.btnCancelarMotorista);

        MotoristaModel motor = pegar_motorista();
        if (motor != null) {
            inputNome.setText(motor.nome);
            inputPlacaMelosa.setText(motor.placa_melosa);
        }

        dialog = new AlertDialog.Builder(this)
                .setView(viewInflated)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        // impede fechar tocando fora
        dialog.setCanceledOnTouchOutside(false);
        // impede fechar pelo botão "voltar"
        dialog.setCancelable(false);

        btnCancelar.setOnClickListener(v -> {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
        });

        btnAdicionar.setOnClickListener(v -> {
            String nome = inputNome.getText().toString().trim();
            String placa_melosa = inputPlacaMelosa.getText().toString().trim();
            if (nome.isEmpty()) {
                inputNome.setError("Informe o nome");
                return;
            }
            if (placa_melosa.isEmpty()) {
                inputPlacaMelosa.setError("informe a placa da melosa");
                return;
            }

            btnAdicionar.setEnabled(false);
            btnCancelar.setEnabled(false);

            io.execute(() -> {
                try {

                    MotoristaModel motorista = new MotoristaModel();
                    motorista.id = 1;
                    motorista.nome = nome;
                    motorista.placa_melosa = placa_melosa;
                    db.motoristaDAO().inserir(motorista);

                    runOnUiThread(() -> {
                        if (dialog != null && dialog.isShowing()) dialog.dismiss();
                        txtMotorista.setText(motorista.nome);
                        txtPlacaMelosa.setText(motorista.placa_melosa);
                        // se sua intenção é sair da Main após cadastrar, finalize AQUI (seguro):
                        // if (!isFinishing() && !isDestroyed()) finish();
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        btnAdicionar.setEnabled(true);
                        btnCancelar.setEnabled(true);
                        showToastErro(ex.getMessage());
                    });
                }
            });
        });

    }


    @Nullable
    public static Integer extrairIdVeiculo(String url) {
        if (url == null) return null;
        Pattern p = Pattern.compile("/home/(\\d+)(?:/|\\?|#|$)");
        Matcher m = p.matcher(url.trim());
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    public void fazer_novo_abastecimento(Integer cod_maquina) {

        if (isFinishing() || isDestroyed()) return;

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_novo_abastecimento, null);

        EditText inputQtdLitros = viewInflated.findViewById(R.id.inputQtdLitros);
        EditText inputKmHorimetro = viewInflated.findViewById(R.id.inputKmHorimetro);

        Button btnAdicionar = viewInflated.findViewById(R.id.btnGravaAbastecimento);
        //Button btnCancelar = viewInflated.findViewById(R.id.btnCancelar);


        dialog = new AlertDialog.Builder(this)
                .setView(viewInflated)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnAdicionar.setOnClickListener(v -> {
            String qtd_litros = inputQtdLitros.getText().toString().trim();
            String km_horimentro = inputKmHorimetro.getText().toString().trim();

            if (qtd_litros.isEmpty()) {
                inputQtdLitros.setError("Informe qtd de litros");
                return;
            }
            if (km_horimentro.isEmpty()) {
                inputKmHorimetro.setError("Informe km ou horímetro");
                return;
            }

            btnAdicionar.setEnabled(false);
            // btnCancelar.setEnabled(false);

            io.execute(() -> {
                try {

                    MotoristaModel motorista = pegar_motorista();

                    AbastecimentoModel a = new AbastecimentoModel();
                    a.cod_veiculo = cod_maquina;
                    a.data_abastecimento = DataHora.data_atual();
                    a.hora_abastecimento = DataHora.pegar_hora();
                    a.motorista_melosa = motorista.nome;
                    a.sit = "realizado";
                    try {
                        String valorStr = km_horimentro.trim().replace(",", ".");
                        String litros = qtd_litros.trim().replace(",", ".");
                        a.km_horimetro = valorStr.isEmpty() ? 0.0 : Double.parseDouble(valorStr);
                        a.qtd_litros = litros.isEmpty() ? 0.0 : Double.parseDouble(valorStr);
                    } catch (NumberFormatException e) {
                        a.km_horimetro = 0.0;
                        a.qtd_litros = 0.0;
                    }
                    db.abastecimentoDAO().inserir(a);

                    runOnUiThread(() -> {
                        dialog.dismiss();
                        showToast("Abastecimento realizado!");
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        btnAdicionar.setEnabled(true);
                        showToastErro(ex.getMessage());
                    });
                }
            });
        });

    }

    private MotoristaModel pegar_motorista() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<MotoristaModel> future = executor.submit(() ->
                db.motoristaDAO().pegar_motorista()
        );

        try {
            return future.get(); // aguarda até ter o resultado
        } catch (Exception e) {
            showToastErro("Erro ao buscar motorista" + e);
            return null;
        } finally {
            executor.shutdown();
        }
    }

    private void showToast(String msge) {
        android.app.AlertDialog.Builder msg = new android.app.AlertDialog.Builder(this);
        msg.setTitle("Sucesso");
        msg.setIcon(R.drawable.ic_success);
        msg.setMessage(msge);
        msg.setPositiveButton("ok", null);
        msg.create().show();
    }

    private void showToastErro(String msge) {
        android.app.AlertDialog.Builder msg = new android.app.AlertDialog.Builder(this);
        msg.setTitle("ERRO");
        msg.setIcon(R.drawable.ic_erro);
        msg.setMessage(msge);
        msg.setPositiveButton("entendi", null);
        msg.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_option_1) {
            abrir_cadastro_motorista();
            return true;
        }

        if (id == R.id.menu_option_2) {
            new AlertDialog.Builder(this)
                    .setTitle("Sincronizar")
                    .setMessage("Deseja sincronizar os dados?")
                    .setPositiveButton("Sim", (d, which) -> {

                    })
                    .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                    .show();
            return true;
        }

        if (id == R.id.menu_option_3) {

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}