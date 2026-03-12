package com.mycompany.smoker;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TelaListagem extends JPanel {

    private JTable tabela;
    private DefaultTableModel modelo;
    private JTextField txtPesquisa;

    public TelaListagem() {
        setLayout(new BorderLayout());

        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelTopo.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel lblPesquisa = new JLabel("Pesquisar:");
        lblPesquisa.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        txtPesquisa = new JTextField(30);
        txtPesquisa.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        txtPesquisa.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                carregarDados(txtPesquisa.getText());
            }
        });

        painelTopo.add(lblPesquisa);
        painelTopo.add(txtPesquisa);
        
        JLabel lblLegenda = new JLabel("  (Vermelho: Vencido/Hoje | Amarelo: Vence em 7 dias)");
        lblLegenda.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        painelTopo.add(lblLegenda);
        
        add(painelTopo, BorderLayout.NORTH);

        String[] colunas = {"ID", "Tipo", "Título / Descrição", "Vencimento", "JSON Oculto", "Caminho Oculto", "Template ID"};
        
        modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        tabela = new JTable(modelo);
        tabela.setRowHeight(25);
        tabela.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        tabela.getColumnModel().getColumn(4).setMinWidth(0); tabela.getColumnModel().getColumn(4).setMaxWidth(0);
        tabela.getColumnModel().getColumn(5).setMinWidth(0); tabela.getColumnModel().getColumn(5).setMaxWidth(0);
        tabela.getColumnModel().getColumn(6).setMinWidth(0); tabela.getColumnModel().getColumn(6).setMaxWidth(0);

        tabela.setDefaultRenderer(Object.class, new RenderizadorDePrazos());

        InputMap im = tabela.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = tabela.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "EnterAction");
        am.put("EnterAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarDetalhesBonitos();
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem itemEditar = new JMenuItem("Editar Registro");
        itemEditar.addActionListener(e -> editarRegistroSelecionado());
        JMenuItem itemExcluir = new JMenuItem("Excluir Registro");
        itemExcluir.addActionListener(e -> excluirRegistroSelecionado());
        popupMenu.add(itemEditar);
        popupMenu.addSeparator();
        popupMenu.add(itemExcluir);
        tabela.setComponentPopupMenu(popupMenu);

        tabela.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) mostrarDetalhesBonitos();
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = tabela.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < tabela.getRowCount()) tabela.setRowSelectionInterval(r, r);
                }
            }
        });

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelBotoes.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JButton btnExcluir = criarBotao("Excluir", new Color(255, 200, 200), Color.BLACK);
        btnExcluir.addActionListener(e -> excluirRegistroSelecionado());

        JButton btnAtualizar = criarBotao("Atualizar", new Color(240, 240, 240), Color.BLACK);
        btnAtualizar.addActionListener(e -> carregarDados(""));
        
        JButton btnAbrir = criarBotao("Ver Arquivos", new Color(200, 230, 255), Color.BLACK);
        btnAbrir.addActionListener(e -> abrirArquivoSelecionado());
        
        JButton btnImprimir = criarBotao("Imprimir / PDF", new Color(150, 150, 255), Color.WHITE);
        btnImprimir.addActionListener(e -> gerarImpressaoHtml());

        JButton btnEditar = criarBotao("Editar", new Color(255, 220, 100), Color.BLACK);
        btnEditar.addActionListener(e -> editarRegistroSelecionado());

        painelBotoes.add(btnExcluir);
        painelBotoes.add(btnEditar);
        painelBotoes.add(btnAtualizar);
        painelBotoes.add(btnAbrir);
        painelBotoes.add(btnImprimir); 
        
        add(painelBotoes, BorderLayout.SOUTH);

        carregarDados("");
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

    private void carregarDados(String termoBusca) {
        modelo.setRowCount(0);
        String sql = "SELECT r.id, t.nome_modulo, r.titulo_principal, r.data_vencimento, r.dados_json, r.caminho_arquivo, r.template_id " +
                     "FROM registros r " +
                     "JOIN templates t ON r.template_id = t.id ";
        if (!termoBusca.isEmpty()) { sql += "WHERE r.titulo_principal LIKE ? OR t.nome_modulo LIKE ? "; }
        sql += "ORDER BY r.data_vencimento ASC, r.id DESC";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (!termoBusca.isEmpty()) {
                String buscaLike = "%" + termoBusca + "%";
                stmt.setString(1, buscaLike); stmt.setString(2, buscaLike);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String dataBanco = rs.getString("data_vencimento");
                String dataVisual = "-";
                if(dataBanco != null && dataBanco.contains("-")) {
                    String[] partes = dataBanco.split("-");
                    dataVisual = partes[2] + "/" + partes[1] + "/" + partes[0];
                }
                modelo.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("nome_modulo"), rs.getString("titulo_principal"),
                    dataVisual, rs.getString("dados_json"), rs.getString("caminho_arquivo"), rs.getInt("template_id")
                });
            }
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage()); }
    }

    private void editarRegistroSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um registro para editar.");
            return;
        }

        int idRegistro = (int) modelo.getValueAt(linha, 0);
        int idTemplate = (int) modelo.getValueAt(linha, 6);
        String jsonDados = (String) modelo.getValueAt(linha, 4);

        String estruturaJson = "";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT estrutura_json FROM templates WHERE id = ?")) {
            stmt.setInt(1, idTemplate);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) estruturaJson = rs.getString("estrutura_json");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar modelo: " + e.getMessage());
            return;
        }

        JsonObject dadosAtuais = new JsonObject();
        try { dadosAtuais = JsonParser.parseString(jsonDados).getAsJsonObject(); } catch (Exception e) {}

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialogEdit = new JDialog(parentWindow, "Editar Registro #" + idRegistro, Dialog.ModalityType.APPLICATION_MODAL);
        dialogEdit.setSize(600, 500);
        dialogEdit.setLayout(new BorderLayout());

        JPanel painelCampos = new JPanel();
        painelCampos.setLayout(new BoxLayout(painelCampos, BoxLayout.Y_AXIS));
        painelCampos.setBorder(new EmptyBorder(10, 10, 10, 10));

        Map<String, JComponent> camposGerados = new HashMap<>();
        List<JsonObject> definicoesCalculo = new ArrayList<>();

        try {
            JsonObject objetoEstrutura = JsonParser.parseString(estruturaJson).getAsJsonObject();
            JsonArray listaCampos = objetoEstrutura.getAsJsonArray("campos");

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

                String valorSalvo = dadosAtuais.has(nomeCampo) ? dadosAtuais.get(nomeCampo).getAsString() : "";

                JPanel pnlLinha = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JLabel lbl = new JLabel(nomeCampo + ": ");
                lbl.setPreferredSize(new Dimension(150, 20)); 
                
                JComponent componenteCampo;
                
                if (tipoCampo.equals("MOEDA")) {
                    JTextField txtMoeda = new JTextField(15);
                    txtMoeda.setText(valorSalvo.isEmpty() ? "0,00" : valorSalvo);
                    txtMoeda.setForeground(new Color(0, 100, 0));
                    txtMoeda.addActionListener(evt -> txtMoeda.transferFocus());
                    txtMoeda.addKeyListener(new KeyAdapter() {
                        public void keyTyped(KeyEvent e) {
                            char c = e.getKeyChar();
                            if (!((c >= '0') && (c <= '9') || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE) || (c == ','))) e.consume();
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
                    pnlLinha.add(new JLabel("R$ ")); 
                    
                } else if (tipoCampo.equals("CALCULO")) {
                    JTextField txtCalc = new JTextField(15);
                    txtCalc.setEditable(false);
                    txtCalc.setBackground(new Color(240, 240, 240));
                    txtCalc.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    txtCalc.setText(valorSalvo);
                    componenteCampo = txtCalc;
                    definicoesCalculo.add(objDefinicao);
                    
                } else if (tipoCampo.equals("DATA") || tipoCampo.equals("DATA_PRAZO")) {
                    try {
                        MaskFormatter maskData = new MaskFormatter("##/##/####");
                        maskData.setPlaceholderCharacter('_');
                        JFormattedTextField txtData = new JFormattedTextField(maskData);
                        txtData.setColumns(10);
                        if (!valorSalvo.isEmpty() && !valorSalvo.equals("__/__/____")) txtData.setText(valorSalvo);
                        if(tipoCampo.equals("DATA_PRAZO")) txtData.setForeground(Color.RED);
                        txtData.addFocusListener(new FocusAdapter() {
                            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> txtData.selectAll()); }
                        });
                        componenteCampo = txtData;
                    } catch(Exception ex) { componenteCampo = new JTextField(valorSalvo, 10); }

                } else if (tipoCampo.equals("NUMERO")) {
                    JTextField txtNum = new JTextField(valorSalvo, 15);
                    txtNum.addKeyListener(new KeyAdapter() {
                        public void keyTyped(KeyEvent e) {
                            char c = e.getKeyChar();
                            if (!((c >= '0') && (c <= '9') || (c == KeyEvent.VK_BACK_SPACE))) e.consume();
                        }
                    });
                    txtNum.addActionListener(evt -> txtNum.transferFocus());
                    componenteCampo = txtNum;

                } else {
                    JTextField txt = new JTextField(valorSalvo, 25);
                    txt.addActionListener(evt -> txt.transferFocus());
                    componenteCampo = txt;
                }
                
                componenteCampo.putClientProperty("tipoSMOKER", tipoCampo);
                pnlLinha.add(lbl);
                pnlLinha.add(componenteCampo);
                painelCampos.add(pnlLinha);
                camposGerados.put(nomeCampo, componenteCampo);
            }

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
                    }
                } catch (Exception e) {}
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao processar formulário: " + e.getMessage());
        }

        dialogEdit.add(new JScrollPane(painelCampos), BorderLayout.CENTER);

        JPanel pnlSalvarEdit = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvarEdicao = criarBotao("Salvar Alterações", new Color(100, 200, 100), Color.WHITE);
        JButton btnCancelar = criarBotao("Cancelar", new Color(200, 200, 200), Color.BLACK);

        btnCancelar.addActionListener(e -> dialogEdit.dispose());
        
        btnSalvarEdicao.addActionListener(e -> {
            JsonObject novosDados = new JsonObject();
            String novoTitulo = "Registro Editado";
            String novaDataPrazo = null;
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
                novosDados.addProperty(chave, valor);

                if (isPrimeiro && !valor.isEmpty() && !valor.equals("__/__/____") && !valor.equals("0,00")) {
                    novoTitulo = valor;
                    isPrimeiro = false;
                }
                
                String tipoDoCampo = (String) comp.getClientProperty("tipoSMOKER");
                if (tipoDoCampo != null && tipoDoCampo.equals("DATA_PRAZO")) {
                    if(valor != null && !valor.equals("__/__/____") && !valor.trim().isEmpty()) {
                        try {
                            String[] partes = valor.split("/");
                            if(partes.length == 3) novaDataPrazo = partes[2] + "-" + partes[1] + "-" + partes[0];
                        } catch (Exception ex) {}
                    }
                }
            }

            String sqlUpdate = "UPDATE registros SET titulo_principal = ?, dados_json = ?, data_vencimento = ? WHERE id = ?";
            try (Connection conn = ConexaoDB.conectar();
                 PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {
                stmtUpdate.setString(1, novoTitulo);
                stmtUpdate.setString(2, novosDados.toString());
                stmtUpdate.setString(3, novaDataPrazo);
                stmtUpdate.setInt(4, idRegistro);
                stmtUpdate.executeUpdate();
                
                JOptionPane.showMessageDialog(dialogEdit, "Registro atualizado com sucesso!");
                dialogEdit.dispose();
                carregarDados(txtPesquisa.getText()); 
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialogEdit, "Erro ao salvar edição: " + ex.getMessage());
            }
        });

        pnlSalvarEdit.add(btnCancelar);
        pnlSalvarEdit.add(btnSalvarEdicao);
        dialogEdit.add(pnlSalvarEdit, BorderLayout.SOUTH);
        dialogEdit.setLocationRelativeTo(this);
        dialogEdit.setVisible(true);
    }

    // --- LÓGICA DE IMPRESSÃO COM SELEÇÃO DE CAMPOS VISÍVEIS ---
    private void gerarImpressaoHtml() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um registro para imprimir.");
            return;
        }

        //Pergunta o formato
        String[] opcoes = {
            "1. Documento Simples (Padrão)", 
            "2. Termo de Garantia", 
            "3. Recibo de Venda / Saída", 
            "4. Comprovante de Entrada / Compra"
        };
        
        String escolhaStr = (String) JOptionPane.showInputDialog(
            this,
            "Selecione o formato do documento:",
            "Opções de Impressão",
            JOptionPane.QUESTION_MESSAGE,
            null,
            opcoes,
            opcoes[0]
        );

        if (escolhaStr == null) return; // Cancelou

        // Extrai os dados
        String id = modelo.getValueAt(linha, 0).toString();
        String tipoBanco = modelo.getValueAt(linha, 1).toString();
        String titulo = modelo.getValueAt(linha, 2).toString();
        String jsonBruto = (String) modelo.getValueAt(linha, 4);

        Gson gson = new Gson();
        JsonObject dados = gson.fromJson(jsonBruto, JsonObject.class);

        // 2. TELA INTERMEDIÁRIA: QUAIS CAMPOS APARECEM?
        JPanel pnlEscolhaCampos = new JPanel(new BorderLayout());
        pnlEscolhaCampos.add(new JLabel("Desmarque os campos que devem ficar OCULTOS no documento:"), BorderLayout.NORTH);
        
        JPanel pnlCheckboxes = new JPanel();
        pnlCheckboxes.setLayout(new BoxLayout(pnlCheckboxes, BoxLayout.Y_AXIS));
        
        // Usamos LinkedHashMap para manter a ordem original que o usuário criou os campos
        Map<String, JCheckBox> mapaCheckboxes = new LinkedHashMap<>();
        
        for (Map.Entry<String, JsonElement> entry : dados.entrySet()) {
            String chave = entry.getKey();
            String valor = entry.getValue().getAsString();
            
            if(!valor.isEmpty() && !valor.equals("0,00") && !valor.equals("__/__/____")) {
                JCheckBox chk = new JCheckBox(chave + "  (Atual: " + valor + ")", true);
                mapaCheckboxes.put(chave, chk);
                pnlCheckboxes.add(chk);
            }
        }
        
        JScrollPane scrollChecks = new JScrollPane(pnlCheckboxes);
        scrollChecks.setPreferredSize(new Dimension(350, 250));
        pnlEscolhaCampos.add(scrollChecks, BorderLayout.CENTER);

        int resultChecks = JOptionPane.showConfirmDialog(this, pnlEscolhaCampos, "Campos Visíveis", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (resultChecks != JOptionPane.OK_OPTION) return; // Cancelou a seleção de campos

        try {
            // Configurações baseadas na escolha do documento
            String tituloDoc = "DOCUMENTO DE REGISTRO";
            String textoSuperior = "";
            String textoInferior = "";

            if (escolhaStr.startsWith("1")) {
                tituloDoc = "Registro: " + tipoBanco;
                textoInferior = "<div class='footer'>Gerado eletronicamente pelo Sistema SMOKER Enterprise.</div>";
            } 
            else if (escolhaStr.startsWith("2")) {
                tituloDoc = "TERMO DE GARANTIA";
                textoInferior = "<div style='border: 1px solid #000; padding: 15px; margin-top: 20px; font-size: 12px; text-align: justify;'>"
                              + "<b>CONDIÇÕES DE GARANTIA:</b><br><br>"
                              + "1. O prazo de garantia legal é de 90 (noventa) dias a contar da data de emissão deste documento, cobrindo exclusivamente defeitos de fabricação referentes aos itens/serviços descritos acima.<br>"
                              + "2. A garantia perderá sua validade em casos de mau uso, quedas, contato com líquidos, instalações indevidas, picos de energia ou violação de lacres de segurança.<br>"
                              + "3. Para acionamento da garantia, é indispensável a apresentação deste termo.</div>";
            }
            else if (escolhaStr.startsWith("3")) {
                tituloDoc = "RECIBO DE VENDA";
                textoSuperior = "<div style='margin-bottom: 20px; font-size: 14px;'>Declaramos o recebimento dos valores referentes à venda/prestação de serviços dos itens discriminados abaixo.</div>";
                textoInferior = "<div class='footer'>Agradecemos a preferência!</div>";
            }
            else if (escolhaStr.startsWith("4")) {
                tituloDoc = "COMPROVANTE DE ENTRADA";
                textoSuperior = "<div style='margin-bottom: 20px; font-size: 14px;'>Declaramos o recebimento / entrada em estoque dos itens discriminados abaixo para os devidos fins.</div>";
                textoInferior = "<div class='footer'>Documento de uso e controle interno.</div>";
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Doc #").append(id).append("</title>");
            html.append("<style>");
            html.append("body { font-family: sans-serif; padding: 40px; color: #333; }");
            html.append(".header { text-align: center; border-bottom: 2px solid #444; padding-bottom: 20px; margin-bottom: 30px; }");
            html.append(".titulo-doc { font-size: 24px; font-weight: bold; color: #444; text-transform: uppercase; margin-top: 10px; }");
            html.append(".meta-info { font-size: 14px; color: #666; margin-bottom: 20px; display: flex; justify-content: space-between; }");
            html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }");
            html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
            html.append("th { background-color: #f2f2f2; font-weight: bold; width: 35%; }");
            html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
            html.append(".footer { margin-top: 50px; text-align: center; font-size: 12px; color: #999; border-top: 1px solid #ddd; padding-top: 20px; }");
            html.append(".assinatura { margin-top: 80px; display: flex; justify-content: space-between; }");
            html.append(".linha-assinatura { width: 45%; border-top: 1px solid #000; text-align: center; padding-top: 10px; font-size: 14px;}");
            html.append("</style></head><body>");

            // Cabeçalho
            html.append("<div class='header'>");
            html.append("<h1>SMOKER SYSTEMS</h1>"); 
            html.append("<div class='titulo-doc'>").append(tituloDoc).append("</div>");
            html.append("</div>");

            // Info Superior
            html.append("<div class='meta-info'>");
            html.append("<div><b>Documento Nº:</b> ").append(id).append("<br><b>Referência:</b> ").append(titulo).append("</div>");
            html.append("<div><b>Emissão:</b> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</div>");
            html.append("</div>");

            if (!textoSuperior.isEmpty()) {
                html.append(textoSuperior);
            }

            // Tabela FILTRANDO PELAS CAIXINHAS MARCADAS
            html.append("<table>");
            
            for (Map.Entry<String, JsonElement> entry : dados.entrySet()) {
                String chave = entry.getKey();
                // Verifica se a chave existe no nosso mapa de Checkboxes e se o usuário deixou ela marcada
                if (mapaCheckboxes.containsKey(chave) && mapaCheckboxes.get(chave).isSelected()) {
                    String valor = entry.getValue().getAsString();
                    html.append("<tr>");
                    html.append("<th>").append(chave).append("</th>");
                    html.append("<td>").append(valor).append("</td>");
                    html.append("</tr>");
                }
            }
            html.append("</table>");

            html.append(textoInferior);

            // Assinaturas
            html.append("<div class='assinatura'>");
            html.append("<div class='linha-assinatura'>Responsável da Empresa</div>");
            html.append("<div class='linha-assinatura'>Assinatura do Cliente</div>");
            html.append("</div>");

            html.append("</body></html>");

            File arquivoTemp = new File("documento_temp.html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoTemp));
            writer.write(html.toString());
            writer.close();

            Desktop.getDesktop().open(arquivoTemp);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar documento: " + e.getMessage());
        }
    }

    class RenderizadorDePrazos extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
                return c;
            }
            String dataString = (String) table.getModel().getValueAt(row, 3);
            c.setBackground(Color.WHITE); 
            c.setForeground(Color.BLACK);
            if (dataString != null && !dataString.isEmpty() && !dataString.equals("-")) {
                try {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate dataVenc = LocalDate.parse(dataString, fmt);
                    LocalDate hoje = LocalDate.now();
                    long dias = ChronoUnit.DAYS.between(hoje, dataVenc);
                    if (dias < 0) { 
                        c.setBackground(new Color(255, 200, 200)); 
                    } else if (dias == 0) { 
                         c.setBackground(new Color(255, 150, 150)); 
                    } else if (dias <= 7) { 
                        c.setBackground(new Color(255, 255, 200)); 
                    }
                } catch (Exception e) {}
            }
            return c;
        }
    }

    private void mostrarDetalhesBonitos() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) return;
        String titulo = (String) modelo.getValueAt(linha, 2);
        String jsonBruto = (String) modelo.getValueAt(linha, 4);
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Detalhes: " + titulo, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(500, 400);
        dialog.setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        contentPanel.setBackground(Color.WHITE);
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonBruto, JsonObject.class);
            JLabel lblTitulo = new JLabel(titulo);
            lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
            lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(lblTitulo);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String chave = entry.getKey();
                String valor = entry.getValue().getAsString();
                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(Color.WHITE);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                JLabel lblChave = new JLabel(chave + ": ");
                lblChave.setFont(new Font("Segoe UI", Font.BOLD, 14));
                lblChave.setForeground(new Color(100, 100, 100));
                JLabel lblValor = new JLabel(valor);
                lblValor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                lblValor.setForeground(Color.BLACK);
                row.add(lblChave, BorderLayout.WEST);
                row.add(lblValor, BorderLayout.CENTER);
                contentPanel.add(row);
                contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        } catch (Exception e) { contentPanel.add(new JLabel("Erro: " + jsonBruto)); }
        dialog.add(new JScrollPane(contentPanel), BorderLayout.CENTER);
        JButton btnFechar = criarBotao("Fechar", new Color(240, 240, 240), Color.BLACK);
        btnFechar.addActionListener(e -> dialog.dispose());
        JPanel pnlSul = new JPanel(); pnlSul.add(btnFechar);
        dialog.add(pnlSul, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void excluirRegistroSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) { JOptionPane.showMessageDialog(this, "Selecione para excluir."); return; }
        int id = (int) modelo.getValueAt(linha, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Excluir permanentemente?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = ConexaoDB.conectar();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM registros WHERE id = ?")) {
                stmt.setInt(1, id); stmt.executeUpdate();
                modelo.removeRow(linha);
                JOptionPane.showMessageDialog(this, "Registro excluído!");
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage()); }
        }
    }

    private void abrirArquivoSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) { JOptionPane.showMessageDialog(this, "Selecione uma linha!"); return; }
        String conteudo = (String) modelo.getValueAt(linha, 5); 
        if (conteudo == null || conteudo.isEmpty() || conteudo.equals("Sem Anexo")) {
            JOptionPane.showMessageDialog(this, "Não tem anexos."); return;
        }
        try {
             if (conteudo.trim().startsWith("[")) {
                 com.google.gson.JsonArray caminhos = com.google.gson.JsonParser.parseString(conteudo).getAsJsonArray();
                 if(caminhos.size()==1) { abrirArquivoFisico(caminhos.get(0).getAsString()); return; }
                 String[] opcoes = new String[caminhos.size()];
                 for(int i=0; i<caminhos.size(); i++) opcoes[i] = caminhos.get(i).getAsString().replace("docs\\", "");
                 String escolhido = (String) JOptionPane.showInputDialog(this, "Qual anexo?", "Abrir", JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[0]);
                 if(escolhido!=null) abrirArquivoFisico("docs" + File.separator + escolhido.replace("docs/", ""));
             } else { abrirArquivoFisico(conteudo); }
        } catch(Exception e) { abrirArquivoFisico(conteudo); }
    }

    private void abrirArquivoFisico(String caminho) {
        try {
            File arquivo = new File(caminho);
            if (!arquivo.exists()) arquivo = new File("docs", new File(caminho).getName());
            if (arquivo.exists()) Desktop.getDesktop().open(arquivo);
            else JOptionPane.showMessageDialog(this, "Arquivo não encontrado: " + caminho);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage()); }
    }
}