package com.mycompany.smoker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelaCadastro extends JPanel {

    private JComboBox<String> comboTemplates;
    private JPanel painelCampos;
    private JButton btnSalvar;
  
    
    private Map<String, Integer> mapaTemplates = new HashMap<>();
    private Map<String, String> mapaEstruturas = new HashMap<>();
    
    private Map<String, JComponent> camposGerados = new HashMap<>();
    private List<JsonObject> definicoesCalculo = new ArrayList<>(); 
    
    private List<File> arquivosSelecionados = new ArrayList<>();
    private DefaultListModel<String> modeloListaArquivos;
    private JList<String> listaVisualArquivos;

    public TelaCadastro() {
        setLayout(new BorderLayout());
        this.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { carregarTemplatesDoBanco(); }
        });

        // Topo
        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboTemplates = new JComboBox<>();
        comboTemplates.addActionListener(e -> gerarFormulario()); 
        painelTopo.add(new JLabel("Tipo de Documento:"));
        painelTopo.add(comboTemplates);
        add(painelTopo, BorderLayout.NORTH);

        // Centro
        painelCampos = new JPanel();
        painelCampos.setLayout(new BoxLayout(painelCampos, BoxLayout.Y_AXIS));
        add(new JScrollPane(painelCampos), BorderLayout.CENTER);

        // Rodapé
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT)); 
        btnSalvar = new JButton("Salvar Registro");
        btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSalvar.setBackground(new Color(100, 200, 100));
        btnSalvar.setForeground(Color.WHITE);
        btnSalvar.setContentAreaFilled(false);
        btnSalvar.setOpaque(true);
        btnSalvar.addActionListener(e -> salvarDados());
        painelBotoes.add(btnSalvar);
        add(painelBotoes, BorderLayout.SOUTH);

        carregarTemplatesDoBanco();
    }

    private void carregarTemplatesDoBanco() {
        comboTemplates.removeAllItems();
        mapaTemplates.clear();
        mapaEstruturas.clear();
        try (Connection conn = ConexaoDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM templates")) {
            while (rs.next()) {
                String nome = rs.getString("nome_modulo");
                int id = rs.getInt("id");
                String json = rs.getString("estrutura_json");
                mapaTemplates.put(nome, id);
                mapaEstruturas.put(nome, json);
                comboTemplates.addItem(nome);
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage()); }
    }

    private void gerarFormulario() {
        painelCampos.removeAll(); 
        camposGerados.clear();
        definicoesCalculo.clear();
        arquivosSelecionados.clear();
        
        String selecionado = (String) comboTemplates.getSelectedItem();
        if (selecionado == null) {
            painelCampos.revalidate(); painelCampos.repaint(); return;
        }
        
   

        String jsonEstrutura = mapaEstruturas.get(selecionado);

        try {
            Gson gson = new Gson();
            JsonObject objetoJson = gson.fromJson(jsonEstrutura, JsonObject.class);
            JsonArray listaCampos = objetoJson.getAsJsonArray("campos");

            for (JsonElement elemento : listaCampos) {
                String nomeCampo = "";
                String tipoCampo = "TEXTO"; 
                JsonObject objDefinicao = null;

                if (elemento.isJsonObject()) {
                    objDefinicao = elemento.getAsJsonObject();
                    nomeCampo = objDefinicao.get("nome").getAsString();
                    if(objDefinicao.has("tipo")) tipoCampo = objDefinicao.get("tipo").getAsString();
                } else {
                    nomeCampo = elemento.getAsString();
                }

                JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JLabel lbl = new JLabel(nomeCampo + ": ");
                lbl.setPreferredSize(new Dimension(150, 20)); 
                
                JComponent componenteCampo;
                
                if (tipoCampo.equals("MOEDA")) {
                    JTextField txtMoeda = new JTextField();
                    txtMoeda.setColumns(15);
                    txtMoeda.setText("0,00");
                    txtMoeda.setForeground(new Color(0, 100, 0));
                    txtMoeda.addActionListener(evt -> txtMoeda.transferFocus());
                    txtMoeda.addKeyListener(new KeyAdapter() {
                        public void keyTyped(KeyEvent e) {
                            char c = e.getKeyChar();
                            if (!((c >= '0') && (c <= '9') || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE) || (c == ','))) { e.consume(); }
                        }
                    });
                    txtMoeda.addFocusListener(new FocusAdapter() {
                        @Override public void focusGained(FocusEvent e) {
                            String limpo = txtMoeda.getText().replace(".", "");
                            if(limpo.equals("0,00")) limpo = ""; 
                            txtMoeda.setText(limpo);
                            SwingUtilities.invokeLater(() -> txtMoeda.selectAll());
                        }
                        @Override public void focusLost(FocusEvent e) {
                            String texto = txtMoeda.getText().replace(".", "").replace(",", ".");
                            try {
                                if(texto.isEmpty()) texto = "0";
                                double valor = Double.parseDouble(texto);
                                DecimalFormat df = new DecimalFormat("#,##0.00");
                                txtMoeda.setText(df.format(valor));
                            } catch (Exception ex) { txtMoeda.setText("0,00"); }
                        }
                    });
                    componenteCampo = txtMoeda;
                    linha.add(new JLabel("R$ ")); 
                    
                } else if (tipoCampo.equals("CALCULO")) {
                    JTextField txtCalc = new JTextField(15);
                    txtCalc.setEditable(false);
                    txtCalc.setBackground(new Color(240, 240, 240));
                    txtCalc.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    txtCalc.setText("Calculando...");
                    componenteCampo = txtCalc;
                    definicoesCalculo.add(objDefinicao);
                    
                } else if (tipoCampo.equals("DATA") || tipoCampo.equals("DATA_PRAZO")) {
                    // AMBOS CRIAM O MESMO VISUAL, MAS O "DATA_PRAZO" TEM UM TRATAMENTO ESPECIAL AO SALVAR
                    try {
                        MaskFormatter maskData = new MaskFormatter("##/##/####");
                        maskData.setPlaceholderCharacter('_');
                        JFormattedTextField txtData = new JFormattedTextField(maskData);
                        txtData.setColumns(10);
                        
                     
                        if(tipoCampo.equals("DATA_PRAZO")) {
                            txtData.setForeground(Color.RED);
                            txtData.setToolTipText("Este campo define o vencimento na lista!");
                        }
                        
                        txtData.addFocusListener(new FocusAdapter() {
                            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> txtData.selectAll()); }
                        });
                        componenteCampo = txtData;
                    } catch(Exception ex) { componenteCampo = new JTextField(10); }

                } else if (tipoCampo.equals("NUMERO")) {
                    JTextField txtNum = new JTextField(15);
                    txtNum.addKeyListener(new KeyAdapter() {
                        public void keyTyped(KeyEvent e) {
                            char c = e.getKeyChar();
                            if (!((c >= '0') && (c <= '9') || (c == KeyEvent.VK_BACK_SPACE))) { e.consume(); }
                        }
                    });
                    txtNum.addActionListener(evt -> txtNum.transferFocus());
                    componenteCampo = txtNum;

                } else {
                    JTextField txt = new JTextField(25);
                    txt.addActionListener(evt -> txt.transferFocus());
                    componenteCampo = txt;
                }
                
               
                componenteCampo.putClientProperty("tipoSMOKER", tipoCampo);
                
                linha.add(lbl);
                linha.add(componenteCampo);
                painelCampos.add(linha);

                camposGerados.put(nomeCampo, componenteCampo);
            }
            
            configurarCalculosAutomaticos();

            //ANEXOS 
            JPanel painelArquivos = new JPanel(new BorderLayout());
            painelArquivos.setBorder(BorderFactory.createTitledBorder("Anexos"));
            painelArquivos.setMaximumSize(new Dimension(500, 150)); 
            JPanel pnlBotoesArq = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton btnAddArq = new JButton(" Adicionar");
            JButton btnRemArq = new JButton(" Remover");
            pnlBotoesArq.add(btnAddArq); pnlBotoesArq.add(btnRemArq);
            modeloListaArquivos = new DefaultListModel<>();
            listaVisualArquivos = new JList<>(modeloListaArquivos);
            JScrollPane scrollLista = new JScrollPane(listaVisualArquivos);
            scrollLista.setPreferredSize(new Dimension(400, 80));
            painelArquivos.add(pnlBotoesArq, BorderLayout.NORTH);
            painelArquivos.add(scrollLista, BorderLayout.CENTER);
            
            btnAddArq.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                fc.setMultiSelectionEnabled(true); 
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    for (File f : fc.getSelectedFiles()) {
                        arquivosSelecionados.add(f);
                        modeloListaArquivos.addElement(f.getName());
                    }
                }
            });
            btnRemArq.addActionListener(e -> {
                int idx = listaVisualArquivos.getSelectedIndex();
                if (idx != -1) { arquivosSelecionados.remove(idx); modeloListaArquivos.remove(idx); }
            });

            painelCampos.add(Box.createRigidArea(new Dimension(0, 10))); 
            painelCampos.add(painelArquivos);

        } catch (Exception e) { e.printStackTrace(); }

        painelCampos.revalidate();
        painelCampos.repaint();
    }
    
    private void configurarCalculosAutomaticos() {
        for (JsonObject config : definicoesCalculo) {
            try {
                String nomeDestino = config.get("nome").getAsString();
                String nomeA = config.get("campoA").getAsString();
                String op = config.get("operador").getAsString();
                String nomeB = config.get("campoB").getAsString();
                JComponent compDestino = camposGerados.get(nomeDestino);
                JComponent compA = camposGerados.get(nomeA);
                JComponent compB = camposGerados.get(nomeB);
                
                if (compDestino instanceof JTextField && compA instanceof JTextField && compB instanceof JTextField) {
                    JTextField txtRes = (JTextField) compDestino;
                    JTextField txtA = (JTextField) compA;
                    JTextField txtB = (JTextField) compB;
                    DocumentListener ouvinte = new DocumentListener() {
                        public void insertUpdate(DocumentEvent e) { calcular(); }
                        public void removeUpdate(DocumentEvent e) { calcular(); }
                        public void changedUpdate(DocumentEvent e) { calcular(); }
                        void calcular() {
                            try {
                                double valA = parseValor(txtA.getText());
                                double valB = parseValor(txtB.getText());
                                double resultado = 0;
                                switch (op) {
                                    case "+": resultado = valA + valB; break;
                                    case "-": resultado = valA - valB; break;
                                    case "*": resultado = valA * valB; break;
                                    case "/": resultado = (valB != 0) ? valA / valB : 0; break;
                                }
                                DecimalFormat df = new DecimalFormat("#,##0.00");
                                txtRes.setText("R$ " + df.format(resultado));
                            } catch (Exception ex) { txtRes.setText("Erro"); }
                        }
                        double parseValor(String texto) {
                            if(texto == null || texto.isEmpty()) return 0;
                            String limpo = texto.replace("R$", "").replace(" ", "").replace(".", "").replace(",", ".");
                            try { return Double.parseDouble(limpo); } catch(Exception e) { return 0; }
                        }
                    };
                    txtA.getDocument().addDocumentListener(ouvinte);
                    txtB.getDocument().addDocumentListener(ouvinte);
                    ouvinte.insertUpdate(null);
                }
            } catch (Exception e) { }
        }
    }

    private void salvarDados() {
        if (comboTemplates.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Selecione um tipo de documento!"); return;
        }

        try {
            String nomeTemplate = (String) comboTemplates.getSelectedItem();
            int templateId = mapaTemplates.get(nomeTemplate);
            JsonObject jsonDados = new JsonObject();
            String tituloPrincipal = "Novo Registro"; 
            
            // VARIAVEL QUE VAI SEGURAR A DATA DE VENCIMENTO DO BANCO
            String dataPrazoParaBanco = null;

            boolean isPrimeiro = true;
            for (Map.Entry<String, JComponent> entry : camposGerados.entrySet()) {
                String chave = entry.getKey();
                JComponent comp = entry.getValue();
                String valor = "";

                if (comp instanceof JTextField) {
                    valor = ((JTextField) comp).getText();
                } else if (comp instanceof JFormattedTextField) {
                     valor = ((JFormattedTextField) comp).getText();
                }

                jsonDados.addProperty(chave, valor);

               
                if (isPrimeiro && !valor.isEmpty() && !valor.equals("__/__/____") && !valor.equals("0,00") && !valor.trim().isEmpty()) {
                    tituloPrincipal = valor;
                    isPrimeiro = false;
                }
                
              
                String tipoDoCampo = (String) comp.getClientProperty("tipoSMOKER");
                if (tipoDoCampo != null && tipoDoCampo.equals("DATA_PRAZO")) {
                    if(valor != null && !valor.equals("__/__/____") && !valor.trim().isEmpty()) {
                        try {
                            String[] partes = valor.split("/");
                            if(partes.length == 3) dataPrazoParaBanco = partes[2] + "-" + partes[1] + "-" + partes[0];
                        } catch (Exception ex) {}
                    }
                }
                // -------------------------
            }
            
            String caminhoSalvoNoBanco;
            if (arquivosSelecionados.isEmpty()) {
                caminhoSalvoNoBanco = "Sem Anexo";
            } else {
                JsonArray listaCaminhos = new JsonArray();
                for (File f : arquivosSelecionados) {
                    listaCaminhos.add(salvarArquivoNoDisco(f));
                }
                caminhoSalvoNoBanco = listaCaminhos.toString(); 
            }

            String sql = "INSERT INTO registros (template_id, titulo_principal, dados_json, caminho_arquivo, data_vencimento) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = ConexaoDB.conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, templateId);
                stmt.setString(2, tituloPrincipal);
                stmt.setString(3, jsonDados.toString()); 
                stmt.setString(4, caminhoSalvoNoBanco);
                stmt.setString(5, dataPrazoParaBanco); // Salva a data capturada do campo DATA_PRAZO
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "✅ Salvo!");
                
                arquivosSelecionados.clear();
                if(modeloListaArquivos != null) modeloListaArquivos.clear();
                gerarFormulario();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String salvarArquivoNoDisco(File arquivoOriginal) throws java.io.IOException {
        java.nio.file.Path pastaDestino = java.nio.file.Paths.get("docs");
        if (!java.nio.file.Files.exists(pastaDestino)) java.nio.file.Files.createDirectories(pastaDestino);
        String nomeOriginal = arquivoOriginal.getName();
        String nomeFinal = System.currentTimeMillis() + "_" + (int)(Math.random() * 1000) + "_" + nomeOriginal;
        java.nio.file.Path destinoFinal = pastaDestino.resolve(nomeFinal);
        java.nio.file.Files.copy(arquivoOriginal.toPath(), destinoFinal);
        return destinoFinal.toString(); 
    }
}