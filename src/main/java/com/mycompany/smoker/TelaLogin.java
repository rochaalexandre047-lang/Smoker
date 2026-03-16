package com.mycompany.smoker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

class UsuarioComboItem {
    String login;
    String perfil;

    public UsuarioComboItem(String login, String perfil) {
        this.login = login;
        this.perfil = perfil;
    }

    @Override
    public String toString() {
        return login; 
    }
}

public class TelaLogin extends JFrame {

    private JComboBox<Object> cmbUsuario;
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

        // --- LADO DIREITO BASE (Controla o Topo e o Formulário) ---
        JPanel pnlDireitaBase = new JPanel(new BorderLayout());
        pnlDireitaBase.setBackground(Color.WHITE);

        // --- BARRA DE CONTROLE DA JANELA (Minimizar, Maximizar, Fechar) ---
        JPanel pnlBarraJanela = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlBarraJanela.setBackground(Color.WHITE);
        
        JButton btnMinimizar = criarBotaoJanela("—", false);
        JButton btnMaximizar = criarBotaoJanela("□", false);
        JButton btnFechar = criarBotaoJanela("X", true);

        // Ações dos botões da janela
        btnMinimizar.addActionListener(e -> setExtendedState(JFrame.ICONIFIED)); // Esconde na barra de tarefas
        
        btnMaximizar.addActionListener(e -> {
            // Alterna entre tela cheia e tamanho normal
            if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                setExtendedState(JFrame.NORMAL);
            } else {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
        
        btnFechar.addActionListener(e -> System.exit(0)); // Fecha tudo

        pnlBarraJanela.add(btnMinimizar);
        pnlBarraJanela.add(btnMaximizar);
        pnlBarraJanela.add(btnFechar);
        
        pnlDireitaBase.add(pnlBarraJanela, BorderLayout.NORTH);

        // --- ÁREA DO FORMULÁRIO ---
        JPanel pnlDireita = new JPanel();
        pnlDireita.setBackground(Color.WHITE);
        pnlDireita.setLayout(new BoxLayout(pnlDireita, BoxLayout.Y_AXIS));
        pnlDireita.setBorder(new EmptyBorder(10, 50, 40, 50)); 

        JLabel lblLoginTitulo = new JLabel("Acesse sua conta");
        lblLoginTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblLoginTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlDireita.add(lblLoginTitulo);
        pnlDireita.add(Box.createRigidArea(new Dimension(0, 40)));

        cmbUsuario = new JComboBox<>();
        cmbUsuario.setEditable(true);
        cmbUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        cmbUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbUsuario.setBackground(Color.WHITE);
        
        cmbUsuario.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UsuarioComboItem) {
                    UsuarioComboItem item = (UsuarioComboItem) value;
                    setText(item.login + " (" + item.perfil + ")"); 
                }
                return this;
            }
        });
        
        carregarUsuariosNoCombo();
        
        txtSenha = new JPasswordField();
        txtSenha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        txtSenha.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSenha.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlDireita.add(criarLabel("Usuário"));
        pnlDireita.add(cmbUsuario);
        pnlDireita.add(Box.createRigidArea(new Dimension(0, 15)));
        pnlDireita.add(criarLabel("Senha"));
        pnlDireita.add(txtSenha);
        
        JCheckBox chkMostrar = new JCheckBox("Mostrar Senha");
        chkMostrar.setBackground(Color.WHITE);
        chkMostrar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkMostrar.setAlignmentX(Component.LEFT_ALIGNMENT); 
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
        btnEntrar.setAlignmentX(Component.LEFT_ALIGNMENT); 
        
        btnEntrar.addActionListener(e -> tentarLogin());
        pnlDireita.add(btnEntrar);

        KeyListener enterListener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) tentarLogin();
            }
        };
        cmbUsuario.getEditor().getEditorComponent().addKeyListener(enterListener);
        txtSenha.addKeyListener(enterListener);

        pnlDireitaBase.add(pnlDireita, BorderLayout.CENTER);

        add(pnlEsquerda);
        add(pnlDireitaBase);

        // Movimentação da janela clicando e arrastando o lado azul
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

    // --- FUNÇÃO PARA CRIAR BOTÕES DE JANELA NO ESTILO WINDOWS ---
    private JButton criarBotaoJanela(String texto, boolean isBotaoFechar) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.GRAY);
        btn.setPreferredSize(new Dimension(45, 30));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (isBotaoFechar) {
                    btn.setBackground(new Color(232, 17, 35)); // Vermelho 
                    btn.setForeground(Color.WHITE);
                } else {
                    btn.setBackground(new Color(229, 229, 229)); // Cinza claro hover
                    btn.setForeground(Color.BLACK);
                }
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(Color.WHITE);
                btn.setForeground(Color.GRAY);
            }
        });
        
        return btn;
    }

    private JLabel criarLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(Color.DARK_GRAY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void carregarUsuariosNoCombo() {
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT login, perfil FROM usuarios ORDER BY login");
             ResultSet rs = stmt.executeQuery()) {
            
            cmbUsuario.addItem(""); 
            
            while (rs.next()) {
                String login = rs.getString("login");
                String perfil = rs.getString("perfil");
                cmbUsuario.addItem(new UsuarioComboItem(login, perfil));
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar lista de usuários: " + e.getMessage());
        }
    }

    private void tentarLogin() {
        Object itemSelecionado = cmbUsuario.getSelectedItem();
        String pass = new String(txtSenha.getPassword());

        if (itemSelecionado == null || itemSelecionado.toString().trim().isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha usuário e senha!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userFinal = "";
        
        if (itemSelecionado instanceof UsuarioComboItem) {
            userFinal = ((UsuarioComboItem) itemSelecionado).login;
        } else {
            userFinal = itemSelecionado.toString().trim();
        }

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT perfil FROM usuarios WHERE login = ? AND senha = ?")) {
            
            stmt.setString(1, userFinal);
            stmt.setString(2, pass);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                SessaoUsuario.loginLogado = userFinal;
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