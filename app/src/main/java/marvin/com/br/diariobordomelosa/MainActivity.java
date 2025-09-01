package marvin.com.br.diariobordomelosa;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import marvin.com.br.diariobordomelosa.Adapter.ItemLctoAdapter;
import marvin.com.br.diariobordomelosa.DAO.ApiClient;
import marvin.com.br.diariobordomelosa.DAO.AppDatabase;
import marvin.com.br.diariobordomelosa.model.AbastecimentoModel;
import marvin.com.br.diariobordomelosa.model.ManutencaoItem;
import marvin.com.br.diariobordomelosa.model.ManutencaoModel;
import marvin.com.br.diariobordomelosa.model.MotoristaModel;
import marvin.com.br.diariobordomelosa.model.SincronizacaoRequest;
import marvin.com.br.diariobordomelosa.repository.RetroServiceInterface;
import marvin.com.br.diariobordomelosa.util.DataHora;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
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

                        View viewInflated = LayoutInflater.from(this)
                                .inflate(R.layout.dialog_abastece_manutencao, null);

                        Button btnAbastecer = viewInflated.findViewById(R.id.btnNovoAbastecimento);
                        Button btnManutencao = viewInflated.findViewById(R.id.btnNovaManutencao);
                        ImageButton btnFecha = viewInflated.findViewById(R.id.btnFecharLcto);


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
                        //dialog.setCancelable(false);

                        btnAbastecer.setOnClickListener(v -> {
                            fazer_novo_abastecimento(veiculoId);
                        });

                        btnManutencao.setOnClickListener(v -> {
                            abrir_nova_manutencao(veiculoId);
                        });
                        btnFecha.setOnClickListener(v -> {
                            dialog.dismiss();
                        });

                    } else {
                        Toast.makeText(this, "QR inválido", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    public void cancelar_manutencao() {
        io.execute(() -> {
            try {
                ManutencaoModel manutencao = db.manutencaoDAO().pegar_ultima_manutencao();
                if (manutencao == null) {
                    runOnUiThread(() -> showToast("Nenhuma manutenção para cancelar"));
                    return;
                }

                List<ManutencaoItem> itens = db.manutencaoItemDAO().pegar_itens_manutencao(manutencao.id);
                if (itens != null && !itens.isEmpty()) {
                    List<Integer> ids = itens.stream()
                            .map(item -> item.id)
                            .collect(Collectors.toList());

                    db.manutencaoItemDAO().apagar_itens_manutencao(ids);
                }

                db.manutencaoDAO().cancelar_manutencao(manutencao.id);

                runOnUiThread(() -> showToast("Manutenção cancelada com sucesso"));

            } catch (Exception ex) {
                runOnUiThread(() -> showToast("Erro ao cancelar manutenção: " + ex.getMessage()));
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        retrofit = ApiClient.getClient(this);
        service = retrofit.create(RetroServiceInterface.class);

        txtVersao = findViewById(R.id.txtVersao);
        txtMotorista = findViewById(R.id.txt_nome_motorista);
        txtPlacaMelosa = findViewById(R.id.txt_placa_melosa);

        ImageView btn_bomba = findViewById(R.id.btn_brasao);
        btn_bomba.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            MediaPlayer mp = MediaPlayer.create(this, R.raw.button_click);
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.start();
            Intent it = new Intent(this, QrScannerActivity.class);
            qrLauncher.launch(it);
        });

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

    private void abrir_nova_manutencao(Integer cod_veiculo) {

        ManutencaoModel m = new ManutencaoModel();

        m.data_manutencao = DataHora.data_atual();
        m.hora_manutencao = DataHora.pegar_hora();
        m.cod_veiculo = cod_veiculo;
        m.melosa = txtPlacaMelosa.getText().toString().trim();
        m.motorista_melosa = txtMotorista.getText().toString().trim();
        m.sit = "realizado";


        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                long idGerado = db.manutencaoDAO().inserir(m);
                m.id = Integer.parseInt(String.valueOf(idGerado));
                runOnUiThread(() -> {
                    lancar_itens_manutencao(m);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                            .setTitle("ERRO")
                            .setIcon(R.drawable.ic_atencao)
                            .setMessage("Erro ao gravar nova manuntenção: " + ex.getMessage())
                            .setPositiveButton("ENTENDI", null)
                            .create()
                            .show();
                });
            }
        });

    }

    private void lancar_itens_manutencao(ManutencaoModel m) {
        if (isFinishing() || isDestroyed()) return;

        View viewInflated = LayoutInflater.from(this)
                .inflate(R.layout.dialog_lancar_manutencao, null);

        EditText desc_item = viewInflated.findViewById(R.id.inputDescricaoItemManutencao);
        EditText qtd_item = viewInflated.findViewById(R.id.inputQtdItem);
        TextView tvErroUnidade = viewInflated.findViewById(R.id.tvErroUnidade);
        ImageButton btn_incluir_item = viewInflated.findViewById(R.id.btnGravaItem);
        ImageButton btn_fechar = viewInflated.findViewById(R.id.btnFecharDialogSeletorAbasteceManutencao);
        Spinner spUnidade = viewInflated.findViewById(R.id.sp_un_medida_i);
        RecyclerView recyclerView = viewInflated.findViewById(R.id.recyclerItensManutencao);
        Button btnConcluirManutencao = viewInflated.findViewById(R.id.btnConfirmarManutencao);

        // popula spinner
        ArrayAdapter<CharSequence> adapterSp = ArrayAdapter.createFromResource(
                this,
                R.array.un_medida_item,
                android.R.layout.simple_spinner_item
        );
        adapterSp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnidade.setAdapter(adapterSp);

        // prepara lista e adapter
        List<ManutencaoItem> itensExistentes = new ArrayList<>();
        ItemLctoAdapter adapter = new ItemLctoAdapter(itensExistentes, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // carrega itens existentes dessa manutenção em background
        io.execute(() -> {
            List<ManutencaoItem> carregados = db.manutencaoItemDAO().pegar_itens_manutencao(m.id);
            runOnUiThread(() -> {
                itensExistentes.clear();
                itensExistentes.addAll(carregados);
                adapter.notifyDataSetChanged();
            });
        });


        // cria e exibe o dialog
        dialog = new AlertDialog.Builder(this)
                .setView(viewInflated)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        btnConcluirManutencao.setOnClickListener(v -> {

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Concluir")
                    .setMessage("todos os itens foram lançados?")
                    .setIcon(R.drawable.ic_question)
                    .setPositiveButton("Sim", (dialog, which) -> {
                        // Reinicia a activity atual
                        Intent intent = getIntent();
                        finish(); // encerra a atual
                        startActivity(intent); // recria a mesma
                    })

                    .setNegativeButton("Não", null)
                    .show();
        });

        btn_fechar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sair do lançamento")
                    .setIcon(R.drawable.ic_atencao)
                    .setMessage("Cancelar a manutenção?")
                    .setPositiveButton("Sim", (d, which) -> {
                        cancelar_manutencao();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Não", (d, which) -> d.dismiss())
                    .show();
        });

        // botão incluir item
        btn_incluir_item.setOnClickListener(v -> {
            String descricao = desc_item.getText().toString().trim();
            String qtd = qtd_item.getText().toString().trim();
            String un = spUnidade.getSelectedItem().toString();

            if (descricao.isEmpty()) {
                desc_item.setError("Informe a descrição");
                return;
            }
            if (qtd.isEmpty()) {
                qtd_item.setError("Informe a qtd");
                return;
            }
            if (un.isEmpty() || un.equals("Un medida")) {
                tvErroUnidade.setText("Informe a unidade");
                tvErroUnidade.setVisibility(View.VISIBLE);
                return;
            } else {
                tvErroUnidade.setVisibility(View.GONE);
            }

            io.execute(() -> {
                try {
                    ManutencaoItem item = new ManutencaoItem();
                    item.descricao = descricao;
                    item.manutencaoId = m.id;
                    item.quantidade = Double.parseDouble(qtd.replace(",", "."));
                    item.unidade = un;
                    item.sit = "lancado";

                    long idGerado = db.manutencaoItemDAO().inserir(item);
                    item.id = (int) idGerado;

                    runOnUiThread(() -> {
                        itensExistentes.add(item);
                        adapter.notifyItemInserted(itensExistentes.size() - 1);
                        recyclerView.scrollToPosition(itensExistentes.size() - 1);

                        // limpa campos
                        desc_item.setText("");
                        qtd_item.setText("");
                        spUnidade.setSelection(0);
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        showToastErro(ex.getMessage());
                    });
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
                    a.melosa = motorista.placa_melosa;
                    a.sit = "realizado";
                    try {
                        String valorStr = km_horimentro.trim().replace(",", ".");
                        String litros = qtd_litros.trim().replace(",", ".");
                        a.km_horimetro = valorStr.isEmpty() ? 0.0 : Double.parseDouble(valorStr);
                        a.qtd_litros = litros.isEmpty() ? 0.0 : Double.parseDouble(litros);
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

    private void ver_abastecimentos() {

        new Thread(() -> {
            List<AbastecimentoModel> lista = db.abastecimentoDAO().pegar_abastecimentos_realizados();

            runOnUiThread(() -> {
                LayoutInflater inflater = LayoutInflater.from(this);
                View view = inflater.inflate(R.layout.dialog_lista_abastecimentos, null);
                LinearLayout layout = view.findViewById(R.id.layout_lista_abastecimentos);

                // Cabeçalho
                LinearLayout linhaCabecalho = new LinearLayout(this);
                linhaCabecalho.setOrientation(LinearLayout.HORIZONTAL);

                TextView cabecalho1 = new TextView(this);
                cabecalho1.setText("Máquina");
                cabecalho1.setPadding(8, 8, 8, 8);
                cabecalho1.setTypeface(Typeface.DEFAULT_BOLD);
                cabecalho1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                TextView cabecalho2 = new TextView(this);
                cabecalho2.setText("Km/Horímetro");
                cabecalho2.setPadding(8, 8, 8, 8);
                cabecalho2.setTypeface(Typeface.DEFAULT_BOLD);
                cabecalho2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                TextView cabecalho3 = new TextView(this);
                cabecalho3.setText("Litros");
                cabecalho3.setPadding(8, 8, 8, 8);
                cabecalho3.setTypeface(Typeface.DEFAULT_BOLD);
                cabecalho3.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                linhaCabecalho.addView(cabecalho1);
                linhaCabecalho.addView(cabecalho2);
                linhaCabecalho.addView(cabecalho3);
                layout.addView(linhaCabecalho);

                // Linhas de dados
                for (AbastecimentoModel prop : lista) {
                    LinearLayout linha = new LinearLayout(this);
                    linha.setOrientation(LinearLayout.HORIZONTAL);

                    TextView col1 = new TextView(this);
                    col1.setText(String.valueOf(prop.cod_veiculo));
                    col1.setPadding(8, 8, 8, 8);
                    col1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    TextView col2 = new TextView(this);
                    col2.setText(prop.km_horimetro != null ? String.valueOf(prop.km_horimetro) : "");
                    col2.setPadding(8, 8, 8, 8);
                    col2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    TextView col3 = new TextView(this);
                    col3.setText(prop.qtd_litros != null ? String.valueOf(prop.qtd_litros) : "");
                    col3.setPadding(8, 8, 8, 8);
                    col3.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    linha.addView(col1);
                    linha.addView(col2);
                    linha.addView(col3);

                    layout.addView(linha);
                }

                new android.app.AlertDialog.Builder(this)
                        .setTitle("Abastecimentos")
                        .setView(view)
                        .setPositiveButton("Fechar", null)
                        .show();
            });
        }).start();

    }

    private void ver_manutencoes() {
        new Thread(() -> {
            List<ManutencaoItem> lista = db.manutencaoItemDAO().pegar_todas_realizadas();

            runOnUiThread(() -> {
                LayoutInflater inflater = LayoutInflater.from(this);
                View view = inflater.inflate(R.layout.dialog_lista_abastecimentos, null);
                LinearLayout layout = view.findViewById(R.id.layout_lista_abastecimentos);

                // Cabeçalho
                LinearLayout linhaCabecalho = new LinearLayout(this);
                linhaCabecalho.setOrientation(LinearLayout.HORIZONTAL);

                TextView cabecalho1 = new TextView(this);
                cabecalho1.setText("Descrição");
                cabecalho1.setPadding(8, 8, 8, 8);
                cabecalho1.setTypeface(Typeface.DEFAULT_BOLD);
                cabecalho1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                TextView cabecalho2 = new TextView(this);
                cabecalho2.setText("Qtd");
                cabecalho2.setPadding(8, 8, 8, 8);
                cabecalho2.setTypeface(Typeface.DEFAULT_BOLD);
                cabecalho2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                TextView cabecalho3 = new TextView(this);
                cabecalho3.setText("Un");
                cabecalho3.setPadding(8, 8, 8, 8);
                cabecalho3.setTypeface(Typeface.DEFAULT_BOLD);
                cabecalho3.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                linhaCabecalho.addView(cabecalho1);
                linhaCabecalho.addView(cabecalho2);
                linhaCabecalho.addView(cabecalho3);
                layout.addView(linhaCabecalho);

                // Linhas de dados
                for (ManutencaoItem prop : lista) {
                    LinearLayout linha = new LinearLayout(this);
                    linha.setOrientation(LinearLayout.HORIZONTAL);

                    TextView col1 = new TextView(this);
                    col1.setText(String.valueOf(prop.descricao));
                    col1.setPadding(8, 8, 8, 8);
                    col1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    TextView col2 = new TextView(this);
                    col2.setText(prop.quantidade != null ? String.valueOf(prop.quantidade) : "");
                    col2.setPadding(8, 8, 8, 8);
                    col2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    TextView col3 = new TextView(this);
                    col3.setText(prop.unidade != null ? String.valueOf(prop.unidade) : "");
                    col3.setPadding(8, 8, 8, 8);
                    col3.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    linha.addView(col1);
                    linha.addView(col2);
                    linha.addView(col3);

                    layout.addView(linha);
                }

                new android.app.AlertDialog.Builder(this)
                        .setTitle("Manutenções")
                        .setView(view)
                        .setPositiveButton("Fechar", null)
                        .show();
            });
        }).start();
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

        if (id == R.id.menu_option_0) {
            ver_abastecimentos();
            return true;
        }
        if (id == R.id.menu_option_1) {
            ver_manutencoes();
            return true;
        }


        if (id == R.id.menu_option_2) {
            abrir_cadastro_motorista();
            return true;
        }

        if (id == R.id.menu_option_3) {
            new AlertDialog.Builder(this)
                    .setTitle("Sincronizar")
                    .setMessage("Deseja sincronizar os dados?")
                    .setPositiveButton("Sim", (d, which) -> {
                        sincronizar_cadastros();
                    })
                    .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                    .show();
            return true;
        }

        if (id == R.id.menu_option_4) {
            new AlertDialog.Builder(this)
                    .setTitle("*ATENÇÂO*")
                    .setMessage("este procedimento exclui os abastecimentos sincronizados! confirma?")
                    .setPositiveButton("Sim, Confirmo", (d, which) -> {
                        zerar_dados_sincronizados();
                    })
                    .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sincronizar_cadastros() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Enviando dados... Aguarde.");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            // 🔹 Busca no banco (thread secundária)
            List<AbastecimentoModel> cadastros = db.abastecimentoDAO().pegar_abastecimentos_realizados();
            List<ManutencaoModel> manutencoes = db.manutencaoDAO().pegar_manutencoes_realizadas();

            List<Integer> ids_manutencao = manutencoes.stream()
                    .map(item -> item.id)
                    .collect(Collectors.toList());

            final List<ManutencaoItem> itens_manutencao;
            if (ids_manutencao != null && !ids_manutencao.isEmpty()) {
                itens_manutencao = db.manutencaoItemDAO().pegar_itens_manutencoes_realizadas(ids_manutencao);
            } else {
                itens_manutencao = Collections.emptyList();
            }


            // ⚠️ Verifica se tem algo pra sincronizar
            if ((cadastros == null || cadastros.isEmpty()) &&
                    (manutencoes == null || manutencoes.isEmpty())) {

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("OPS")
                            .setMessage("Não há dados para sincronizar!")
                            .setIcon(R.drawable.ic_atencao)
                            .setPositiveButton("OK", null)
                            .create()
                            .show();
                });
                return;
            }

            // 🔹 Prepara request
            SincronizacaoRequest request = new SincronizacaoRequest(cadastros, manutencoes, itens_manutencao);

            // 🔹 Remove IDs antes de enviar
            cadastros.forEach(c -> c.id = null);
            manutencoes.forEach(m -> m.id = null);
            itens_manutencao.forEach(i -> i.id = null);

            int totalAbastecimentos = cadastros.size();
            int totalManutencoes = manutencoes.size();
            int totalItens = itens_manutencao.size();

            // 🔹 Chama API
            service.sincronizarTudo(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    runOnUiThread(() -> progressDialog.dismiss());

                    if (response.isSuccessful()) {
                        // 🔹 Atualiza dados localmente em thread separada
                        new Thread(() -> {
                            cadastros.forEach(c -> c.sit = "sincronizado");
                            manutencoes.forEach(m -> m.sit = "sincronizado");
                            itens_manutencao.forEach(i -> i.sit = "sincronizado");

                            db.abastecimentoDAO().updateAll(cadastros);
                            db.manutencaoDAO().updateAll(manutencoes);
                            db.manutencaoItemDAO().updateAll(itens_manutencao);

                            // 🔹 Feedback pro usuário na UI
                            runOnUiThread(() -> {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Sincronizados!")
                                        .setMessage("Abastecimentos: " + totalAbastecimentos + "\n"
                                                + "Manutenções: " + totalManutencoes + "\n"
                                                + "Itens: " + totalItens)
                                        .setIcon(R.drawable.ic_success)
                                        .setPositiveButton("OK", null)
                                        .create()
                                        .show();
                            });
                        }).start();

                    } else {
                        try {
                            String erroMsg = response.errorBody() != null
                                    ? response.errorBody().string()
                                    : "Erro desconhecido";

                            runOnUiThread(() -> {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("ERRO")
                                        .setMessage("Falha: " + erroMsg)
                                        .setIcon(R.drawable.ic_erro)
                                        .setPositiveButton("OK", null)
                                        .create()
                                        .show();
                            });

                        } catch (Exception e) {
                            runOnUiThread(() -> showToastErro("Erro ao ler resposta: " + e.getMessage()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showToastErro("Falha na comunicação: " + t.getMessage());
                    });
                }
            });

        }).start();
    }



    private void zerar_dados_sincronizados() {
        new Thread(() -> {
            try {
                db.abastecimentoDAO().apagar_sincronizados();
                runOnUiThread(() -> {
                    try {
                        showToast("Sincronizados apagados!");
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    } catch (Exception uiEx) {
                        Log.e("ZERAR_DADOS", "Erro ao atualizar a UI", uiEx);
                    }
                });

            } catch (Exception dbEx) {
                Log.e("ZERAR_DADOS", "Erro ao apagar sincronizados", dbEx);
                runOnUiThread(() ->
                        showToastErro(dbEx.getMessage())
                );
            }
        }).start();
    }

}