package com.mycompany.smoker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.sql.*;

class CampoDefinicao {
    String nome;
    String tipo; 
    String campoA;
    String operador;
    String campoB;

    public CampoDefinicao(String nome, String tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }
    
    public CampoDefinicao(String nome, String tipo, String campoA, String operador, String campoB) {
        this.nome = nome;
        this.tipo = tipo;
        this.campoA = campoA;
        this.operador = operador;
        this.campoB = campoB;
    }

    @Override
    public String toString() {
        if(tipo.equals("CALCULO")) {
            return nome + "  [= " + campoA + " " + operador + " " + campoB + "]";
        }
        // Tradução visual para ficar bonito na lista
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
    private JTextField txtOrigemA;
    private JComboBox<String> cmbOperador;
    private JTextField txtOrigemB;
    private DefaultListModel<CampoDefinicao> modeloListaCampos;
    private JList<CampoDefinicao> listaCamposVisual;
    private JTable tabelaTemplates;
    private DefaultTableModel modeloTabelaTemplates;

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
        painel.setBorder(BorderFactory.createTitledBorder("Criar Novo Modelo"));

        JPanel pnlTopo = new JPanel(new GridLayout(0, 1));
        txtNomeModulo = new JTextField();
        pnlTopo.add(new JLabel("Nome do Novo Módulo (Ex: Boleto):"));
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
            "DATA (Simples/Registro)", // Data normal
            "DATA (Vencimento/Prazo)", // Data que ativa o alerta vermelho
            "MOEDA (R$)", 
            "NUMERO (Inteiro)", 
            "CALCULO (Fórmula)"
        };
        cmbTipoCampo = new JComboBox<>(tipos);
        JButton btnAdd = new JButton("➕ Add");

        pnlFormula = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlFormula.setBorder(BorderFactory.createTitledBorder("Definir Fórmula"));
        pnlFormula.setVisible(false);
        
        txtOrigemA = new JTextField(8);
        String[] ops = {"-", "+", "*", "/"};
        cmbOperador = new JComboBox<>(ops);
        txtOrigemB = new JTextField(8);

        pnlFormula.add(new JLabel("Campo A:"));
        pnlFormula.add(txtOrigemA);
        pnlFormula.add(cmbOperador);
        pnlFormula.add(new JLabel("Campo B:"));
        pnlFormula.add(txtOrigemB);

        cmbTipoCampo.addActionListener(e -> {
            String selecionado = (String) cmbTipoCampo.getSelectedItem();
            if (selecionado.startsWith("CALCULO")) {
                pnlFormula.setVisible(true);
            } else {
                pnlFormula.setVisible(false);
            }
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

        pnlCentro.add(containerCampos, BorderLayout.NORTH);
        pnlCentro.add(new JScrollPane(listaCamposVisual), BorderLayout.CENTER);
        
        JButton btnRemoverCampo = new JButton("Remover Campo Selecionado");
        pnlCentro.add(btnRemoverCampo, BorderLayout.SOUTH);
        
        painel.add(pnlCentro, BorderLayout.CENTER);

        JButton btnSalvar = new JButton("SALVAR NOVO MODELO");
        btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSalvar.setBackground(new Color(200, 255, 200));
        btnSalvar.setContentAreaFilled(false);
        btnSalvar.setOpaque(true);
        painel.add(btnSalvar, BorderLayout.SOUTH);

        // AÇÕES
        Runnable acaoAdicionar = () -> {
            String nome = txtNomeCampo.getText().trim();
            if(!nome.isEmpty()){
                String tipoBruto = (String) cmbTipoCampo.getSelectedItem();
                
                // Lógica para definir a "Tag" interna correta
                String tipoSalvar = "TEXTO";
                if(tipoBruto.contains("Simples")) tipoSalvar = "DATA";
                else if(tipoBruto.contains("Vencimento")) tipoSalvar = "DATA_PRAZO"; // O novo tipo
                else if(tipoBruto.contains("MOEDA")) tipoSalvar = "MOEDA";
                else if(tipoBruto.contains("NUMERO")) tipoSalvar = "NUMERO";
                else if(tipoBruto.contains("CALCULO")) tipoSalvar = "CALCULO";
                
                if (tipoSalvar.equals("CALCULO")) {
                    String cA = txtOrigemA.getText().trim();
                    String op = (String) cmbOperador.getSelectedItem();
                    String cB = txtOrigemB.getText().trim();
                    if(cA.isEmpty() || cB.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Preencha A e B!"); return;
                    }
                    modeloListaCampos.addElement(new CampoDefinicao(nome, tipoSalvar, cA, op, cB));
                } else {
                    modeloListaCampos.addElement(new CampoDefinicao(nome, tipoSalvar));
                }
                
                txtNomeCampo.setText("");
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
            @Override public void actionPerformed(ActionEvent e) { usarModeloSelecionado(); }
        });

        painel.add(new JScrollPane(tabelaTemplates), BorderLayout.CENTER);
        JPanel pnlBotoes = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton btnUsar = new JButton("Disponibilizar");
        btnUsar.addActionListener(e -> usarModeloSelecionado());
        JButton btnExcluir = new JButton("Excluir");
        btnExcluir.setBackground(new Color(255, 200, 200));
        btnExcluir.setContentAreaFilled(false); 
        btnExcluir.setOpaque(true);
        btnExcluir.addActionListener(e -> excluirTemplate());
        pnlBotoes.add(btnUsar); pnlBotoes.add(btnExcluir);
        painel.add(pnlBotoes, BorderLayout.SOUTH);
        return painel;
    }

    private void salvarNoBanco() {
        String nome = txtNomeModulo.getText().trim();
        if (nome.isEmpty() || modeloListaCampos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha tudo!"); return;
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
                    objCampo.addProperty("campoA", campo.campoA);
                    objCampo.addProperty("operador", campo.operador);
                    objCampo.addProperty("campoB", campo.campoB);
                }
                arr.add(objCampo);
            }
            json.add("campos", arr);
            String sql = "INSERT INTO templates (nome_modulo, estrutura_json) VALUES (?, ?)";
            try(Connection conn = ConexaoDB.conectar();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, nome);
                stmt.setString(2, json.toString());
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Modelo Salvo!");
                txtNomeModulo.setText("");
                modeloListaCampos.clear();
                atualizarListaTemplates();
            }
        } catch(Exception e) { JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage()); }
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
        try(Connection conn = ConexaoDB.conectar();
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM templates WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            atualizarListaTemplates();
        } catch(Exception e) {}
    }

    private void usarModeloSelecionado() { JOptionPane.showMessageDialog(this, "Modelo disponível!"); }
}