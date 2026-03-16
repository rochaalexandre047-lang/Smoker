package com.mycompany.smoker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelaCadastro extends JPanel {

    private JComboBox<TemplateItem> cmbTemplates;
    private JPanel painelCamposDinamicos;
    private Map<String, JComponent> camposGerados;
    private List<File> arquivosAnexados;
    private JLabel lblContadorAnexos;

    public TelaCadastro() {
        setLayout(new BorderLayout());
        camposGerados = new HashMap<>();
        arquivosAnexados = new ArrayList<>();

        JPanel pnlTopo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTopo.setBorder(new EmptyBorder(10, 10, 10, 10));
        pnlTopo.add(new JLabel("Selecione o Modelo: "));

        cmbTemplates = new JComboBox<>();
        carregarTemplates();
        cmbTemplates.addActionListener(e -> renderizarCampos());
        pnlTopo.add(cmbTemplates);

        JButton btnAtualizarModelos = criarBotao("Atualizar Lista", new Color(240, 240, 240), Color.BLACK);
        btnAtualizarModelos.addActionListener(e -> carregarTemplates());
        pnlTopo.add(btnAtualizarModelos);

        add(pnlTopo, BorderLayout.NORTH);

        painelCamposDinamicos = new JPanel();
        painelCamposDinamicos.setLayout(new BoxLayout(painelCamposDinamicos, BoxLayout.Y_AXIS));
        painelCamposDinamicos.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JScrollPane scroll = new JScrollPane(painelCamposDinamicos);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        JPanel pnlInferior = new JPanel(new BorderLayout());
        pnlInferior.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel pnlAnexos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAnexar = criarBotao("Anexar Arquivo(s)", new Color(200, 230, 255), Color.BLACK);
        lblContadorAnexos = new JLabel("Nenhum arquivo anexado.");
        btnAnexar.addActionListener(e -> anexarArquivo());
        
        JButton btnLimparAnexos = criarBotao("Limpar Anexos", new Color(255, 200, 200), Color.BLACK);
        btnLimparAnexos.addActionListener(e -> {
            arquivosAnexados.clear();
            lblContadorAnexos.setText("Nenhum arquivo anexado.");
        });

        pnlAnexos.add(btnAnexar);
        pnlAnexos.add(lblContadorAnexos);
        pnlAnexos.add(btnLimparAnexos);

        JPanel pnlSalvar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvar = criarBotao("Salvar Registro", new Color(100, 200, 100), Color.WHITE);
        btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSalvar.addActionListener(e -> salvarRegistro());
        pnlSalvar.add(btnSalvar);

        pnlInferior.add(pnlAnexos, BorderLayout.WEST);
        pnlInferior.add(pnlSalvar, BorderLayout.EAST);

        add(pnlInferior, BorderLayout.SOUTH);

        renderizarCampos();
    }

    private JButton criarBotao(String texto, Color corFundo, Color corTexto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(corFundo);
        btn.setForeground(corTexto);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        return btn;
    }

    private void carregarTemplates() {
        cmbTemplates.removeAllItems();
        try (Connection conn = ConexaoDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, nome_modulo FROM templates")) {
            while (rs.next()) {
                cmbTemplates.addItem(new TemplateItem(rs.getInt("id"), rs.getString("nome_modulo")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar modelos: " + e.getMessage());
        }
    }

    private void renderizarCampos() {
        painelCamposDinamicos.removeAll();
        camposGerados.clear();

        TemplateItem selecionado = (TemplateItem) cmbTemplates.getSelectedItem();
        if (selecionado == null) {
            painelCamposDinamicos.revalidate();
            painelCamposDinamicos.repaint();
            return;
        }

        String json = "";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT estrutura_json FROM templates WHERE id = ?")) {
            stmt.setInt(1, selecionado.id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                json = rs.getString("estrutura_json");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage());
            return;
        }

        if (json.isEmpty()) return;

        List<JsonObject> definicoesCalculo = new ArrayList<>();

        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray campos = obj.getAsJsonArray("campos");

            for (JsonElement e : campos) {
                String nome = "";
                String tipo = "TEXTO";
                JsonObject configCampo = null;

                if (e.isJsonObject()) {
                    configCampo = e.getAsJsonObject();
                    nome = configCampo.get("nome").getAsString();
                    if(configCampo.has("tipo")) tipo = configCampo.get("tipo").getAsString();
                } else {
                    nome = e.getAsString();
                }

                JPanel pnlLinha = new JPanel(new FlowLayout(FlowLayout.LEFT));
                pnlLinha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

                JLabel lbl = new JLabel(nome + ": ");
                lbl.setPreferredSize(new Dimension(180, 20));
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
                pnlLinha.add(lbl);

                JComponent comp;

                if (tipo.equals("DATA") || tipo.equals("DATA_PRAZO")) {
                    try {
                        MaskFormatter mask = new MaskFormatter("##/##/####");
                        mask.setPlaceholderCharacter('_');
                        JFormattedTextField txtData = new JFormattedTextField(mask);
                        txtData.setColumns(10);
                        txtData.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                        if(tipo.equals("DATA_PRAZO")) txtData.setForeground(Color.RED);
                        comp = txtData;
                    } catch (Exception ex) { comp = new JTextField(10); }
                } 
                else if (tipo.equals("NUMERO")) {
                    JTextField txtNum = new JTextField(15);
                    txtNum.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    comp = txtNum;
                } 
                else if (tipo.equals("MOEDA")) {
                    JTextField txtMoeda = new JTextField(15);
                    txtMoeda.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    txtMoeda.setForeground(new Color(0, 100, 0));
                    txtMoeda.setText("0,00");
                    txtMoeda.addFocusListener(new java.awt.event.FocusAdapter() {
                        public void focusGained(java.awt.event.FocusEvent evt) {
                            SwingUtilities.invokeLater(() -> txtMoeda.selectAll());
                        }
                        public void focusLost(java.awt.event.FocusEvent evt) {
                            try {
                                String t = txtMoeda.getText().replace(".", "").replace(",", ".");
                                if(t.isEmpty()) t = "0";
                                double v = Double.parseDouble(t);
                                txtMoeda.setText(new DecimalFormat("#,##0.00").format(v));
                            } catch (Exception ex) { txtMoeda.setText("0,00"); }
                        }
                    });
                    pnlLinha.add(new JLabel("R$ "));
                    comp = txtMoeda;
                } 
                else if (tipo.equals("CALCULO")) {
                    JTextField txtCalc = new JTextField(15);
                    txtCalc.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    txtCalc.setEditable(false);
                    txtCalc.setBackground(new Color(240, 240, 240));
                    comp = txtCalc;
                    definicoesCalculo.add(configCampo);
                } 
                else {
                    JTextField txt = new JTextField(25);
                    txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    comp = txt;
                }

                comp.putClientProperty("tipoSMOKER", tipo);
                pnlLinha.add(comp);
                painelCamposDinamicos.add(pnlLinha);
                camposGerados.put(nome, comp);
            }

            // --- O MOTOR MATEMÁTICO INJETADO AQUI ---
            for (JsonObject config : definicoesCalculo) {
                try {
                    String nomeDestino = config.get("nome").getAsString();
                    String formulaOriginal = config.get("formula").getAsString(); 
                    
                    JComponent compDestino = camposGerados.get(nomeDestino);
                    
                    if (compDestino instanceof JTextField) {
                        JTextField txtRes = (JTextField) compDestino;
                        
                        DocumentListener ouvinteUniversal = new DocumentListener() {
                            public void insertUpdate(DocumentEvent e) { calcular(); }
                            public void removeUpdate(DocumentEvent e) { calcular(); }
                            public void changedUpdate(DocumentEvent e) { calcular(); }
                            
                            void calcular() {
                                try {
                                    String expressao = formulaOriginal;
                                    
                                    List<String> nomesDosCampos = new ArrayList<>(camposGerados.keySet());
                                    nomesDosCampos.sort((a, b) -> Integer.compare(b.length(), a.length()));
                                    
                                    for(String nome : nomesDosCampos) {
                                        if(expressao.contains(nome)) {
                                            JComponent c = camposGerados.get(nome);
                                            double val = 0;
                                            if(c instanceof JTextField) val = parseValor(((JTextField)c).getText());
                                            expressao = expressao.replace(nome, String.valueOf(val));
                                        }
                                    }
                                    
                                    double resultado = avaliarExpressaoMatematica(expressao);
                                    DecimalFormat df = new DecimalFormat("#,##0.00");
                                    txtRes.setText("R$ " + df.format(resultado));
                                    
                                } catch (Exception ex) { 
                                    txtRes.setText("R$ 0,00"); 
                                }
                            }
                            
                            double parseValor(String texto) {
                                if(texto == null || texto.isEmpty()) return 0;
                                String limpo = texto.replace("R$", "").replace(" ", "").replace(".", "").replace(",", ".");
                                try { return Double.parseDouble(limpo); } catch(Exception e) { return 0; }
                            }
                        };
                        
                        for(JComponent c : camposGerados.values()) {
                            if(c instanceof JTextField && c != txtRes) {
                                ((JTextField)c).getDocument().addDocumentListener(ouvinteUniversal);
                            }
                        }
                    }
                } catch (Exception e) {}
            }

        } catch (Exception e) {
            painelCamposDinamicos.add(new JLabel("Erro ao carregar campos."));
        }

        painelCamposDinamicos.revalidate();
        painelCamposDinamicos.repaint();
    }

    public static double avaliarExpressaoMatematica(final String str) {
        return new Object() {
            int pos = -1, ch;
            void nextChar() { ch = (++pos < str.length()) ? str.charAt(pos) : -1; }
            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) { nextChar(); return true; }
                return false;
            }
            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Inesperado: " + (char)ch);
                return x;
            }
            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); 
                    else if (eat('-')) x -= parseTerm(); 
                    else return x;
                }
            }
            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); 
                    else if (eat('/')) x /= parseFactor(); 
                    else return x;
                }
            }
            double parseFactor() {
                if (eat('+')) return parseFactor(); 
                if (eat('-')) return -parseFactor(); 
                double x;
                int startPos = pos;
                if (eat('(')) { 
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { 
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, pos));
                } else {
                    throw new RuntimeException("Inesperado: " + (char)ch);
                }
                return x;
            }
        }.parse();
    }

    private void anexarArquivo() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selecionados = fc.getSelectedFiles();
            for(File f : selecionados) arquivosAnexados.add(f);
            lblContadorAnexos.setText(arquivosAnexados.size() + " arquivo(s) anexado(s).");
        }
    }

    private void salvarRegistro() {
        TemplateItem selecionado = (TemplateItem) cmbTemplates.getSelectedItem();
        if (selecionado == null) return;

        JsonObject dados = new JsonObject();
        String tituloPrincipal = "Sem Título";
        String dataVencimento = null;
        boolean tituloDefinido = false;

        for (Map.Entry<String, JComponent> entry : camposGerados.entrySet()) {
            String chave = entry.getKey();
            JComponent comp = entry.getValue();
            String valor = "";

            if (comp instanceof JTextField) {
                valor = ((JTextField) comp).getText();
            } else if (comp instanceof JFormattedTextField) {
                valor = ((JFormattedTextField) comp).getText();
            }

            dados.addProperty(chave, valor);

            if (!tituloDefinido && !valor.isEmpty() && !valor.equals("__/__/____") && !valor.equals("0,00")) {
                tituloPrincipal = valor;
                tituloDefinido = true;
            }
            
            String tipoCampo = (String) comp.getClientProperty("tipoSMOKER");
            if(tipoCampo != null && tipoCampo.equals("DATA_PRAZO")) {
                if(!valor.isEmpty() && !valor.equals("__/__/____")) {
                    try {
                        String[] partes = valor.split("/");
                        dataVencimento = partes[2] + "-" + partes[1] + "-" + partes[0];
                    } catch(Exception e){}
                }
            }
        }

        JsonArray arrayCaminhos = new JsonArray();
        if (!arquivosAnexados.isEmpty()) {
            File pastaDocs = new File("docs");
            if (!pastaDocs.exists()) pastaDocs.mkdir();

            for(File arquivoOriginal : arquivosAnexados) {
                String nomeArquivoUnico = System.currentTimeMillis() + "_" + arquivoOriginal.getName();
                File arquivoDestino = new File(pastaDocs, nomeArquivoUnico);
                try {
                    Files.copy(arquivoOriginal.toPath(), arquivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    arrayCaminhos.add(arquivoDestino.getPath());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao copiar arquivo: " + arquivoOriginal.getName());
                }
            }
        }
        
        String strCaminhosSalvar = arrayCaminhos.size() > 0 ? arrayCaminhos.toString() : "Sem Anexo";

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO registros (template_id, titulo_principal, caminho_arquivo, dados_json, data_vencimento) VALUES (?, ?, ?, ?, ?)")) {
            
            stmt.setInt(1, selecionado.id);
            stmt.setString(2, tituloPrincipal);
            stmt.setString(3, strCaminhosSalvar);
            stmt.setString(4, dados.toString());
            stmt.setString(5, dataVencimento);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Registro salvo com sucesso!");
            
            arquivosAnexados.clear();
            lblContadorAnexos.setText("Nenhum arquivo anexado.");
            renderizarCampos(); 
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + e.getMessage());
        }
    }

    class TemplateItem {
        int id;
        String nome;
        public TemplateItem(int id, String nome) { this.id = id; this.nome = nome; }
        @Override public String toString() { return nome; }
    }
}