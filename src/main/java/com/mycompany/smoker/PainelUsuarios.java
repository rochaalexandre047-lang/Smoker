package com.mycompany.smoker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PainelUsuarios extends JPanel {

    private JTable tabela;
    private DefaultTableModel modelo;

    public PainelUsuarios() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        boolean isAdmin = "ADMIN".equals(SessaoUsuario.perfilLogado);

        // --- TOPO COM BOTÃO DE LOGOFF ---
        JPanel pnlTopo = new JPanel(new BorderLayout());
        JLabel lblTitulo = new JLabel(isAdmin ? "Controle de Acesso e Usuários" : "Minha Sessão");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        pnlTopo.add(lblTitulo, BorderLayout.WEST);

        JButton btnLogoff = criarBotao("Fazer Logoff", new Color(255, 100, 100), Color.WHITE);
        btnLogoff.addActionListener(e -> acaoLogoff());
        
        // Posição do botão muda para ficar bonito
        JPanel pnlLogoffBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlLogoffBox.add(btnLogoff);
        pnlTopo.add(pnlLogoffBox, BorderLayout.EAST);
        
        add(pnlTopo, BorderLayout.NORTH);

        // --- CONTEÚDO BASEADO NO PERFIL ---
        if (isAdmin) {
            montarPainelAdmin();
        } else {
            montarPainelOperador();
        }
    }
    
    // Método que encerra a sessão e volta pra tela de Login
    private void acaoLogoff() {
        int confirm = JOptionPane.showConfirmDialog(this, "Deseja realmente sair do sistema?", "Fazer Logoff", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            SessaoUsuario.limparSessao();
            
            // Fecha a janela principal (Main)
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            if (parentWindow != null) {
                parentWindow.dispose();
            }
            
            // Abre a tela de login de novo
            SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
        }
    }

    private void montarPainelOperador() {
        // Tela simples só para operadores
        JPanel pnlCentro = new JPanel(new GridBagLayout());
        
        JLabel lblMsg = new JLabel("Você está logado como OPERADOR (" + SessaoUsuario.loginLogado + ").");
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblMsg.setForeground(Color.DARK_GRAY);
        
        pnlCentro.add(lblMsg);
        add(pnlCentro, BorderLayout.CENTER);
    }

    private void montarPainelAdmin() {
        // Tela completa para Administradores
        String[] colunas = {"ID", "Login / Usuário", "Perfil de Acesso"};
        modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setRowHeight(25);
        tabela.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        // Botões inferiores do Admin
        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnNovo = criarBotao("Novo Usuário", new Color(100, 200, 100), Color.WHITE);
        btnNovo.addActionListener(e -> abrirDialogoUsuario(-1, "", ""));

        JButton btnEditar = criarBotao("Editar / Trocar Senha", new Color(255, 220, 100), Color.BLACK);
        btnEditar.addActionListener(e -> acaoEditar());

        JButton btnExcluir = criarBotao("Excluir Usuário", new Color(255, 100, 100), Color.WHITE);
        btnExcluir.addActionListener(e -> acaoExcluir());
        
        JButton btnAtualizar = criarBotao("Atualizar", new Color(240, 240, 240), Color.BLACK);
        btnAtualizar.addActionListener(e -> carregarUsuarios());

        pnlBotoes.add(btnNovo);
        pnlBotoes.add(btnEditar);
        pnlBotoes.add(btnExcluir);
        pnlBotoes.add(btnAtualizar);

        add(pnlBotoes, BorderLayout.SOUTH);

        carregarUsuarios();
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

    private void carregarUsuarios() {
        modelo.setRowCount(0);
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, login, perfil FROM usuarios ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("login"),
                    rs.getString("perfil")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar usuários: " + e.getMessage());
        }
    }

    private void acaoEditar() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um usuário para editar.");
            return;
        }
        int id = (int) modelo.getValueAt(linha, 0);
        String login = (String) modelo.getValueAt(linha, 1);
        String perfil = (String) modelo.getValueAt(linha, 2);
        
        abrirDialogoUsuario(id, login, perfil);
    }

    private void acaoExcluir() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um usuário para excluir.");
            return;
        }
        
        int id = (int) modelo.getValueAt(linha, 0);
        String login = (String) modelo.getValueAt(linha, 1);

        if (login.equals(SessaoUsuario.loginLogado)) {
            JOptionPane.showMessageDialog(this, "Você não pode excluir seu próprio usuário enquanto está logado!", "Segurança", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int op = JOptionPane.showConfirmDialog(this, "Excluir o acesso do usuário '" + login + "'?", "Atenção", JOptionPane.YES_NO_OPTION);
        if (op == JOptionPane.YES_OPTION) {
            try (Connection conn = ConexaoDB.conectar();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
                carregarUsuarios();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir: " + e.getMessage());
            }
        }
    }

    private void abrirDialogoUsuario(int idRegistro, String loginAtual, String perfilAtual) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, idRegistro == -1 ? "Novo Usuário" : "Editar Usuário", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(350, 300);
        dialog.setLayout(new GridLayout(4, 1, 10, 10));
        
        JPanel pnl1 = new JPanel(new BorderLayout());
        pnl1.add(new JLabel("Login / Nome de Usuário:"), BorderLayout.NORTH);
        JTextField txtLogin = new JTextField(loginAtual);
        if (idRegistro != -1) txtLogin.setEditable(false); 
        pnl1.add(txtLogin, BorderLayout.CENTER);

        JPanel pnl2 = new JPanel(new BorderLayout());
        pnl2.add(new JLabel(idRegistro == -1 ? "Senha:" : "Nova Senha (deixe em branco para não alterar):"), BorderLayout.NORTH);
        JPasswordField txtSenha = new JPasswordField();
        pnl2.add(txtSenha, BorderLayout.CENTER);

        JPanel pnl3 = new JPanel(new BorderLayout());
        pnl3.add(new JLabel("Nível de Acesso:"), BorderLayout.NORTH);
        JComboBox<String> cmbPerfil = new JComboBox<>(new String[]{"OPERADOR", "ADMIN"});
        if (!perfilAtual.isEmpty()) cmbPerfil.setSelectedItem(perfilAtual);
        pnl3.add(cmbPerfil, BorderLayout.CENTER);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.setBackground(new Color(40, 150, 40));
        btnSalvar.setForeground(Color.WHITE);
        
        btnSalvar.addActionListener(e -> {
            String u = txtLogin.getText().trim();
            String s = new String(txtSenha.getPassword());
            String p = (String) cmbPerfil.getSelectedItem();

            if (u.isEmpty()) { JOptionPane.showMessageDialog(dialog, "O login não pode ser vazio!"); return; }

            try (Connection conn = ConexaoDB.conectar()) {
                if (idRegistro == -1) {
                    if (s.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Digite uma senha!"); return; }
                    
                    try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO usuarios (login, senha, perfil) VALUES (?, ?, ?)")) {
                        stmt.setString(1, u);
                        stmt.setString(2, s);
                        stmt.setString(3, p);
                        stmt.executeUpdate();
                    }
                } else {
                    if (s.isEmpty()) {
                        try (PreparedStatement stmt = conn.prepareStatement("UPDATE usuarios SET perfil = ? WHERE id = ?")) {
                            stmt.setString(1, p);
                            stmt.setInt(2, idRegistro);
                            stmt.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement stmt = conn.prepareStatement("UPDATE usuarios SET senha = ?, perfil = ? WHERE id = ?")) {
                            stmt.setString(1, s);
                            stmt.setString(2, p);
                            stmt.setInt(3, idRegistro);
                            stmt.executeUpdate();
                        }
                    }
                }
                dialog.dispose();
                carregarUsuarios();
            } catch (Exception ex) {
                if(ex.getMessage().contains("UNIQUE")) {
                    JOptionPane.showMessageDialog(dialog, "Este login já existe!");
                } else {
                    JOptionPane.showMessageDialog(dialog, "Erro: " + ex.getMessage());
                }
            }
        });

        pnl1.setBorder(new EmptyBorder(5, 15, 0, 15));
        pnl2.setBorder(new EmptyBorder(5, 15, 0, 15));
        pnl3.setBorder(new EmptyBorder(5, 15, 0, 15));
        
        dialog.add(pnl1); dialog.add(pnl2); dialog.add(pnl3); 
        
        JPanel pnlSalvar = new JPanel();
        pnlSalvar.add(btnSalvar);
        dialog.add(pnlSalvar);
        
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}