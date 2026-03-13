package com.mycompany.smoker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TelaLogin extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtSenha;
    private Point pontoInicialClick;

    public TelaLogin() {
        setUndecorated(true);
        setSize(700, 450);
        setLocationRelativeTo(null); 
        setLayout(new GridLayout(1, 2)); 

        // --- LADO ESQUERDO ---
        JPanel pnlEsquerda = new JPanel();
        pnlEsquerda.setBackground(new Color(30, 40, 55));
        pnlEsquerda.setLayout(new GridBagLayout()); 
        
        JLabel lblLogo = new JLabel("SMOKER");
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 40));
        
        JLabel lblSub = new JLabel("Gestão Inteligente & ERP");
        lblSub.setForeground(new Color(150, 180, 255));
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        JPanel pnlTitulos = new JPanel(new GridLayout(2, 1));
        pnlTitulos.setOpaque(false);
        pnlTitulos.add(lblLogo);
        pnlTitulos.add(lblSub);
        pnlEsquerda.add(pnlTitulos);

        // --- LADO DIREITO ---
        JPanel pnlDireita = new JPanel();
        pnlDireita.setBackground(Color.WHITE);
        pnlDireita.setLayout(new BoxLayout(pnlDireita, BoxLayout.Y_AXIS));
        pnlDireita.setBorder(new EmptyBorder(40, 50, 40, 50));

        JPanel pnlFechar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlFechar.setBackground(Color.WHITE);
        pnlFechar.setMaximumSize(new Dimension(400, 30));
        pnlFechar.setAlignmentX(Component.LEFT_ALIGNMENT); // Mantém alinhado na estrutura
        
        JLabel lblFechar = new JLabel("X");
        lblFechar.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblFechar.setForeground(Color.GRAY);
        lblFechar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblFechar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { lblFechar.setForeground(Color.RED); }
            public void mouseExited(MouseEvent e) { lblFechar.setForeground(Color.GRAY); }
        });
        pnlFechar.add(lblFechar);
        pnlDireita.add(pnlFechar);

        // Título perfeitamente alinhado à esquerda
        JLabel lblLoginTitulo = new JLabel("Acesse sua conta");
        lblLoginTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblLoginTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlDireita.add(lblLoginTitulo);
        pnlDireita.add(Box.createRigidArea(new Dimension(0, 40)));

        txtUsuario = new JTextField();
        txtUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        txtUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsuario.setAlignmentX(Component.LEFT_ALIGNMENT); // Correção de Alinhamento
        
        txtSenha = new JPasswordField();
        txtSenha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        txtSenha.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSenha.setAlignmentX(Component.LEFT_ALIGNMENT); // Correção de Alinhamento

        pnlDireita.add(criarLabel("Usuário"));
        pnlDireita.add(txtUsuario);
        pnlDireita.add(Box.createRigidArea(new Dimension(0, 15)));
        pnlDireita.add(criarLabel("Senha"));
        pnlDireita.add(txtSenha);
        
        JCheckBox chkMostrar = new JCheckBox("Mostrar Senha");
        chkMostrar.setBackground(Color.WHITE);
        chkMostrar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkMostrar.setAlignmentX(Component.LEFT_ALIGNMENT); // Correção de Alinhamento
        chkMostrar.addActionListener(e -> {
            txtSenha.setEchoChar(chkMostrar.isSelected() ? '\u0000' : '•');
        });
        pnlDireita.add(chkMostrar);
        pnlDireita.add(Box.createRigidArea(new Dimension(0, 30)));

        JButton btnEntrar = new JButton("ENTRAR");
        btnEntrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnEntrar.setBackground(new Color(40, 100, 255));
        btnEntrar.setForeground(Color.WHITE);
        btnEntrar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnEntrar.setFocusPainted(false);
        btnEntrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEntrar.setAlignmentX(Component.LEFT_ALIGNMENT); // Correção de Alinhamento
        
        btnEntrar.addActionListener(e -> tentarLogin());
        pnlDireita.add(btnEntrar);

        KeyListener enterListener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) tentarLogin();
            }
        };
        txtUsuario.addKeyListener(enterListener);
        txtSenha.addKeyListener(enterListener);

        add(pnlEsquerda);
        add(pnlDireita);

        MouseAdapter dragListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) { pontoInicialClick = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                Point pontoAtual = e.getLocationOnScreen();
                setLocation(pontoAtual.x - pontoInicialClick.x, pontoAtual.y - pontoInicialClick.y);
            }
        };
        pnlEsquerda.addMouseListener(dragListener);
        pnlEsquerda.addMouseMotionListener(dragListener);
    }

    private JLabel criarLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(Color.DARK_GRAY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void tentarLogin() {
        String user = txtUsuario.getText().trim();
        String pass = new String(txtSenha.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha usuário e senha!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT perfil FROM usuarios WHERE login = ? AND senha = ?")) {
            
            stmt.setString(1, user);
            stmt.setString(2, pass);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                SessaoUsuario.loginLogado = user;
                SessaoUsuario.perfilLogado = rs.getString("perfil");
                this.dispose(); 
                SwingUtilities.invokeLater(() -> new Main().setVisible(true));
            } else {
                JOptionPane.showMessageDialog(this, "Usuário ou senha incorretos.", "Erro de Login", JOptionPane.ERROR_MESSAGE);
                txtSenha.setText("");
                txtSenha.requestFocus();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro no banco: " + ex.getMessage());
        }
    }
}