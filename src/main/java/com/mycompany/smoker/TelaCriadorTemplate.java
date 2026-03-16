package com.mycompany.smoker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

class CampoDefinicao {
    String nome;
    String tipo; 
    String formula;

    public CampoDefinicao(String nome, String tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }
    
    public CampoDefinicao(String nome, String tipo, String formula) {
        this.nome = nome;
        this.tipo = tipo;
        this.formula = formula;
    }

    @Override
    public String toString() {
        if(tipo.equals("CALCULO")) {
            return nome + "  [= " + formula + "]";
        }
        String tipoVisual = tipo;
        if(tipo.equals("DATA_PRAZO")) tipoVisual = "DATA (Vencimento)";
        if(tipo.equals("DATA")) tipoVisual = "DATA (Simples)";
        
        return nome + "  [" + tipoVisual + "]";
    }
}

public class TelaCriadorTemplate extends JPanel {

    private JTextField txtNomeModulo;
    private JTextField txtNomeCampo;
    private JComboBox<String> cmbTipoCampo; 
    private JPanel pnlFormula;
    private JTextField txtFormulaLivre; 
    private DefaultListModel<CampoDefinicao> modeloListaCampos;
    private JList<CampoDefinicao> listaCamposVisual;
    private JTable tabelaTemplates;
    private DefaultTableModel modeloTabelaTemplates;

    private int idTemplateEmEdicao = -1;
    private JButton btnSalvar;
    private JButton btnCancelarEdicao;

    public TelaCriadorTemplate() {
        setLayout(new GridLayout(1, 2, 10, 0)); 
        JPanel painelEsquerdo = montarPainelCriacao();
        add(painelEsquerdo);
        JPanel painelDireito = montarPainelListagem();
        add(painelDireito);
        atualizarListaTemplates();
    }

    private JPanel montarPainelCriacao() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Criar / Editar Modelo"));

        JPanel pnlTopo = new JPanel(new GridLayout(0, 1));
        txtNomeModulo = new JTextField();
        pnlTopo.add(new JLabel("Nome do Módulo (Ex: Venda, Ordem de Serviço):"));
        pnlTopo.add(txtNomeModulo);
        painel.add(pnlTopo, BorderLayout.NORTH);

        JPanel pnlCentro = new JPanel(new BorderLayout());
        
        JPanel pnlAdd = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        txtNomeCampo = new JTextField();
        
        String[] tipos = {
            "TEXTO (Livre)", 
            "DATA (Simples/Registro)", 
            "DATA (Vencimento/Prazo)", 
            "MOEDA (R$)", 
            "NUMERO (Inteiro)", 
            "CALCULO (Fórmula Livre)"
        };
        cmbTipoCampo = new JComboBox<>(tipos);
        JButton btnAdd = new JButton("Adicionar Campo");

        // --- PAINEL DE FÓRMULA COM TECLADO "ANTI-BURRO" ---
        pnlFormula = new JPanel(new BorderLayout());
        pnlFormula.setBorder(BorderFactory.createTitledBorder("Fórmula Matemática"));
        pnlFormula.setVisible(false);
        
        // Teclado de Operadores
        JPanel pnlTeclado = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        pnlTeclado.add(new JLabel("Operadores: "));
        String[] operadores = {" + ", " - ", " * ", " / ", " ( ", " ) "};
        for(String op : operadores) {
            JButton btnOp = new JButton(op.trim());
            btnOp.setMargin(new Insets(2, 6, 2, 6));
            btnOp.setBackground(new Color(230, 230, 240));
            btnOp.setFocusPainted(false);
            btnOp.addActionListener(e -> {
                txtFormulaLivre.setText(txtFormulaLivre.getText() + op);
                txtFormulaLivre.requestFocus();
            });
            pnlTeclado.add(btnOp);
        }
        
        txtFormulaLivre = new JTextField();
        txtFormulaLivre.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JPanel pnlInstrucao = new JPanel(new GridLayout(2, 1));
        JLabel lblInstrucao1 = new JLabel("Clique nos campos da lista abaixo para inseri-los na fórmula:");
        lblInstrucao1.setForeground(new Color(0, 100, 200));
        pnlInstrucao.add(pnlTeclado);
        pnlInstrucao.add(lblInstrucao1);

        pnlFormula.add(pnlInstrucao, BorderLayout.NORTH);
        pnlFormula.add(txtFormulaLivre, BorderLayout.CENTER);

        cmbTipoCampo.addActionListener(e -> {
            String selecionado = (String) cmbTipoCampo.getSelectedItem();
            pnlFormula.setVisible(selecionado.startsWith("CALCULO"));
            painel.revalidate();
        });

        gbc.gridx = 0; gbc.weightx = 0.5; pnlAdd.add(txtNomeCampo, gbc);
        gbc.gridx = 1; gbc.weightx = 0.4; pnlAdd.add(cmbTipoCampo, gbc);
        gbc.gridx = 2; gbc.weightx = 0.1; pnlAdd.add(btnAdd, gbc);
        
        JPanel containerCampos = new JPanel(new BorderLayout());
        containerCampos.add(pnlAdd, BorderLayout.NORTH);
        containerCampos.add(pnlFormula, BorderLayout.SOUTH);

        modeloListaCampos = new DefaultListModel<>();
        listaCamposVisual = new JList<>(modeloListaCampos);
        listaCamposVisual.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // --- MÁGICA DO CLIQUE NA LISTA PARA INJETAR O NOME ---
        listaCamposVisual.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Só injeta se o painel de fórmula estiver visível e ativo
                if (pnlFormula.isVisible()) {
                    int index = listaCamposVisual.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        CampoDefinicao campoClicado = modeloListaCampos.getElementAt(index);
                        
                        // Impede de adicionar um campo de cálculo dentro de outro cálculo para não dar loop infinito
                        if (!campoClicado.tipo.equals("CALCULO")) {
                            String textoAtual = txtFormulaLivre.getText();
                            if (!textoAtual.isEmpty() && !textoAtual.endsWith(" ")) {
                                textoAtual += " "; // Dá um espaço automático para ficar bonito
                            }
                            txtFormulaLivre.setText(textoAtual + campoClicado.nome + " ");
                            txtFormulaLivre.requestFocus();
                        } else {
                            JOptionPane.showMessageDialog(painel, "Você não pode usar um campo de cálculo dentro de outro cálculo.", "Aviso", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            }
        });

        pnlCentro.add(containerCampos, BorderLayout.NORTH);
        pnlCentro.add(new JScrollPane(listaCamposVisual), BorderLayout.CENTER);
        
        JButton btnRemoverCampo = new JButton("Remover Campo Selecionado");
        pnlCentro.add(btnRemoverCampo, BorderLayout.SOUTH);
        
        painel.add(pnlCentro, BorderLayout.CENTER);

        JPanel pnlAcoes = new JPanel(new GridLayout(2, 1, 5, 5));
        
        btnSalvar = new JButton("Salvar Novo Modelo");
        btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSalvar.setBackground(new Color(200, 255, 200));
        btnSalvar.setContentAreaFilled(false);
        btnSalvar.setOpaque(true);
        
        btnCancelarEdicao = new JButton("Cancelar Edição");
        btnCancelarEdicao.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancelarEdicao.setBackground(new Color(255, 200, 200));
        btnCancelarEdicao.setContentAreaFilled(false);
        btnCancelarEdicao.setOpaque(true);
        btnCancelarEdicao.setVisible(false); 

        pnlAcoes.add(btnSalvar);
        pnlAcoes.add(btnCancelarEdicao);
        painel.add(pnlAcoes, BorderLayout.SOUTH);

        Runnable acaoAdicionar = () -> {
            String nome = txtNomeCampo.getText().trim();
            if(!nome.isEmpty()){
                String tipoBruto = (String) cmbTipoCampo.getSelectedItem();
                String tipoSalvar = "TEXTO";
                if(tipoBruto.contains("Simples")) tipoSalvar = "DATA";
                else if(tipoBruto.contains("Vencimento")) tipoSalvar = "DATA_PRAZO"; 
                else if(tipoBruto.contains("MOEDA")) tipoSalvar = "MOEDA";
                else if(tipoBruto.contains("NUMERO")) tipoSalvar = "NUMERO";
                else if(tipoBruto.contains("CALCULO")) tipoSalvar = "CALCULO";
                
                if (tipoSalvar.equals("CALCULO")) {
                    String form = txtFormulaLivre.getText().trim();
                    if(form.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "A fórmula está vazia! Clique nos campos abaixo para montar a conta."); return;
                    }
                    modeloListaCampos.addElement(new CampoDefinicao(nome, tipoSalvar, form));
                } else {
                    modeloListaCampos.addElement(new CampoDefinicao(nome, tipoSalvar));
                }
                txtNomeCampo.setText("");
                txtFormulaLivre.setText(""); // Limpa a formula pro próximo
                txtNomeCampo.requestFocus();
            }
        };

        btnAdd.addActionListener(e -> acaoAdicionar.run());
        txtNomeCampo.addActionListener(e -> acaoAdicionar.run());

        btnRemoverCampo.addActionListener(e -> {
            int idx = listaCamposVisual.getSelectedIndex();
            if(idx != -1) modeloListaCampos.remove(idx);
        });

        btnSalvar.addActionListener(e -> salvarNoBanco());
        btnCancelarEdicao.addActionListener(e -> cancelarModoEdicao());

        return painel;
    }

    private JPanel montarPainelListagem() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Modelos Existentes"));
        String[] colunas = {"ID", "Nome do Módulo", "Resumo"};
        modeloTabelaTemplates = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tabelaTemplates = new JTable(modeloTabelaTemplates);
        tabelaTemplates.getColumnModel().getColumn(0).setMaxWidth(50); 
        
        InputMap im = tabelaTemplates.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = tabelaTemplates.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "EnterAction");
        am.put("EnterAction", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { carregarTemplateParaEdicao(); }
        });

        painel.add(new JScrollPane(tabelaTemplates), BorderLayout.CENTER);
        
        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnEditar = criarBotao("Editar", new Color(255, 220, 100));
        btnEditar.addActionListener(e -> carregarTemplateParaEdicao());

        JButton btnExcluir = criarBotao("Excluir", new Color(255, 200, 200));
        btnExcluir.addActionListener(e -> excluirTemplate());
        
        pnlBotoes.add(btnEditar); 
        pnlBotoes.add(btnExcluir);
        
        painel.add(pnlBotoes, BorderLayout.SOUTH);
        return painel;
    }

    private JButton criarBotao(String texto, Color corFundo) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(corFundo);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        return btn;
    }

    private void carregarTemplateParaEdicao() {
        int linha = tabelaTemplates.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um modelo na lista para editar.");
            return;
        }

        int id = (int) modeloTabelaTemplates.getValueAt(linha, 0);

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM templates WHERE id = ?")) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                txtNomeModulo.setText(rs.getString("nome_modulo"));
                String json = rs.getString("estrutura_json");

                modeloListaCampos.clear();
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                JsonArray campos = obj.getAsJsonArray("campos");
                
                for (JsonElement e : campos) {
                    JsonObject campo = e.getAsJsonObject();
                    String cNome = campo.get("nome").getAsString();
                    String cTipo = campo.get("tipo").getAsString();
                    
                    if (cTipo.equals("CALCULO")) {
                        String form = campo.has("formula") ? campo.get("formula").getAsString() : "";
                        modeloListaCampos.addElement(new CampoDefinicao(cNome, cTipo, form));
                    } else {
                        modeloListaCampos.addElement(new CampoDefinicao(cNome, cTipo));
                    }
                }

                idTemplateEmEdicao = id;
                btnSalvar.setText("Salvar Alterações do Modelo");
                btnSalvar.setBackground(new Color(255, 220, 100)); 
                btnCancelarEdicao.setVisible(true);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar modelo: " + e.getMessage());
        }
    }

    private void cancelarModoEdicao() {
        idTemplateEmEdicao = -1;
        txtNomeModulo.setText("");
        modeloListaCampos.clear();
        btnSalvar.setText("Salvar Novo Modelo");
        btnSalvar.setBackground(new Color(200, 255, 200)); 
        btnCancelarEdicao.setVisible(false);
    }

    private void salvarNoBanco() {
        String nome = txtNomeModulo.getText().trim();
        if (nome.isEmpty() || modeloListaCampos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha o nome do módulo e adicione pelo menos um campo!"); 
            return;
        }

        try {
            JsonObject json = new JsonObject();
            JsonArray arr = new JsonArray();
            for(int i=0; i<modeloListaCampos.size(); i++) {
                CampoDefinicao campo = modeloListaCampos.get(i);
                JsonObject objCampo = new JsonObject();
                objCampo.addProperty("nome", campo.nome);
                objCampo.addProperty("tipo", campo.tipo);
                if (campo.tipo.equals("CALCULO")) {
                    objCampo.addProperty("formula", campo.formula);
                }
                arr.add(objCampo);
            }
            json.add("campos", arr);

            String sql;
            if (idTemplateEmEdicao == -1) {
                sql = "INSERT INTO templates (nome_modulo, estrutura_json) VALUES (?, ?)";
            } else {
                sql = "UPDATE templates SET nome_modulo = ?, estrutura_json = ? WHERE id = ?";
            }

            try(Connection conn = ConexaoDB.conectar();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, nome);
                stmt.setString(2, json.toString());
                
                if (idTemplateEmEdicao != -1) stmt.setInt(3, idTemplateEmEdicao);
                
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Modelo salvo com sucesso!");
                cancelarModoEdicao(); 
                atualizarListaTemplates();
            }
        } catch(Exception e) { 
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + e.getMessage()); 
        }
    }

    private void atualizarListaTemplates() {
        modeloTabelaTemplates.setRowCount(0);
        try(Connection conn = ConexaoDB.conectar();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM templates")) {
            while(rs.next()) {
                modeloTabelaTemplates.addRow(new Object[]{rs.getInt("id"), rs.getString("nome_modulo"), "..."});
            }
        } catch(Exception e) {}
    }

    private void excluirTemplate() {
        int linha = tabelaTemplates.getSelectedRow();
        if(linha == -1) return;
        int id = (int) modeloTabelaTemplates.getValueAt(linha, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Excluir este modelo?", "Confirmar Exclusão", JOptionPane.YES_NO_OPTION);
        if(confirm == JOptionPane.YES_OPTION) {
            try(Connection conn = ConexaoDB.conectar();
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM templates WHERE id = ?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
                atualizarListaTemplates();
            } catch(Exception e) {}
        }
    }
}