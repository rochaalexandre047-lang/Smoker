package com.mycompany.smoker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
        
        JLabel lblLegenda = new JLabel("  (🔴 Vencido/Hoje   🟡 Vence em 7 dias)");
        lblLegenda.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        painelTopo.add(lblLegenda);
        
        add(painelTopo, BorderLayout.NORTH);

        String[] colunas = {"ID", "Tipo", "Título / Descrição", "Vencimento", "JSON Oculto", "Caminho Oculto"};
        
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
        JMenuItem itemExcluir = new JMenuItem("Excluir este registro");
        itemExcluir.addActionListener(e -> excluirRegistroSelecionado());
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
        
        JButton btnExcluir = criarBotao("Excluir", new Color(255, 200, 200));
        btnExcluir.addActionListener(e -> excluirRegistroSelecionado());

        JButton btnAtualizar = criarBotao("Atualizar", new Color(240, 240, 240));
        btnAtualizar.addActionListener(e -> carregarDados(""));
        
        JButton btnAbrir = criarBotao("Ver Arquivos", new Color(200, 230, 255));
        btnAbrir.addActionListener(e -> abrirArquivoSelecionado());
        
       
        JButton btnImprimir = criarBotao("Imprimir / Gerar PDF", new Color(150, 150, 255));
        btnImprimir.setForeground(Color.WHITE);
        btnImprimir.addActionListener(e -> gerarImpressaoHtml());
       

        painelBotoes.add(btnExcluir);
        painelBotoes.add(btnAtualizar);
        painelBotoes.add(btnAbrir);
        painelBotoes.add(btnImprimir); // Adiciona na tela
        
        add(painelBotoes, BorderLayout.SOUTH);

        carregarDados("");
    }

    //HTML
    private void gerarImpressaoHtml() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um registro para imprimir!");
            return;
        }

        try {
            // Pega dados da tabela
            String id = modelo.getValueAt(linha, 0).toString();
            String tipo = modelo.getValueAt(linha, 1).toString();
            String titulo = modelo.getValueAt(linha, 2).toString();
            String jsonBruto = (String) modelo.getValueAt(linha, 4);

            // Monta o HTML
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Documento #").append(id).append("</title>");
            
            // CSS para ficar bonito (Estilo Profissional)
            html.append("<style>");
            html.append("body { font-family: sans-serif; padding: 40px; color: #333; }");
            html.append(".header { text-align: center; border-bottom: 2px solid #444; padding-bottom: 20px; margin-bottom: 30px; }");
            html.append(".titulo-doc { font-size: 24px; font-weight: bold; color: #444; text-transform: uppercase; }");
            html.append(".meta-info { font-size: 14px; color: #666; margin-bottom: 20px; }");
            html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }");
            html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
            html.append("th { background-color: #f2f2f2; font-weight: bold; width: 30%; }");
            html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
            html.append(".footer { margin-top: 50px; text-align: center; font-size: 12px; color: #999; border-top: 1px solid #ddd; padding-top: 20px; }");
            html.append(".assinatura { margin-top: 80px; display: flex; justify-content: space-between; }");
            html.append(".linha-assinatura { width: 40%; border-top: 1px solid #000; text-align: center; padding-top: 10px; }");
            html.append("</style></head><body>");

            // Corpo
            html.append("<div class='header'>");
            html.append("<h1>SMOKER SYSTEMS</h1>"); // Aqui seria o nome da empresa do usuário
            html.append("<div class='titulo-doc'>").append(tipo).append("</div>");
            html.append("</div>");

            html.append("<div class='meta-info'>");
            html.append("<b>Documento Nº:</b> ").append(id).append("<br>");
            html.append("<b>Referência:</b> ").append(titulo).append("<br>");
            html.append("<b>Data de Emissão:</b> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("<br>");
            html.append("</div>");

          
            html.append("<table>");
            Gson gson = new Gson();
            JsonObject dados = gson.fromJson(jsonBruto, JsonObject.class);
            
            for (Map.Entry<String, JsonElement> entry : dados.entrySet()) {
                String chave = entry.getKey();
                String valor = entry.getValue().getAsString();
            
                if(!valor.isEmpty()) {
                    html.append("<tr>");
                    html.append("<th>").append(chave).append("</th>");
                    html.append("<td>").append(valor).append("</td>");
                    html.append("</tr>");
                }
            }
            html.append("</table>");

            // Área de Assinatura (para parecer documento sério)
            html.append("<div class='assinatura'>");
            html.append("<div class='linha-assinatura'>Assinatura do Responsável</div>");
            html.append("<div class='linha-assinatura'>Assinatura do Cliente</div>");
            html.append("</div>");

            html.append("<div class='footer'>");
            html.append("Gerado eletronicamente pelo Sistema SMOKER Enterprise.");
            html.append("</div>");

            html.append("</body></html>");

            // Salva e Abre
            File arquivoTemp = new File("documento_temp.html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoTemp));
            writer.write(html.toString());
            writer.close();

            Desktop.getDesktop().open(arquivoTemp);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar documento: " + e.getMessage());
        }
    }
    // ------------------------------------

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

    private JButton criarBotao(String texto, Color corFundo) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(corFundo);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        return btn;
    }

    private void carregarDados(String termoBusca) {
        modelo.setRowCount(0);
        String sql = "SELECT r.id, t.nome_modulo, r.titulo_principal, r.data_vencimento, r.dados_json, r.caminho_arquivo " +
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
                    dataVisual, rs.getString("dados_json"), rs.getString("caminho_arquivo")
                });
            }
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage()); }
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
        JButton btnFechar = new JButton("Fechar");
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