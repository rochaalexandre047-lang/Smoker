package com.mycompany.smoker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class Main extends JFrame {

    private final Set<Integer> teclasPressionadas = new HashSet<>();

    public Main() {
        setTitle("SMOKER Enterprise - Usuário: " + SessaoUsuario.loginLogado + " [" + SessaoUsuario.perfilLogado + "]");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Novo Registro", new TelaCadastro());
        abas.addTab("Consultar / Pesquisar", new TelaListagem());
        
        // CONTROLE DE ACESSO: Só admin vê o criador de modelos
        if ("ADMIN".equals(SessaoUsuario.perfilLogado)) {
            abas.addTab("Criador de Modelos", new TelaCriadorTemplate());
        }
        
        abas.addTab("Utilitários", new PainelFerramentas());
        
        // TODOS VEEM A ABA DE SESSÃO (Para poderem fazer Logoff)
        abas.addTab("Sessão / Usuários", new PainelUsuarios());

        add(abas);

        // SISTEMA DE ATALHO GLOBAL ( D+B)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                teclasPressionadas.add(e.getKeyCode());
                
                if (teclasPressionadas.contains(KeyEvent.VK_D) && teclasPressionadas.contains(KeyEvent.VK_B)) {
                    teclasPressionadas.clear(); 
                    if ("ADMIN".equals(SessaoUsuario.perfilLogado)) {
                        ativarModoDestruicao();
                    } else {
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(this, "Acesso Negado. Apenas Administradores podem usar ferramentas de desenvolvedor.", "Segurança", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                teclasPressionadas.remove(e.getKeyCode());
            }
            return false; 
        });
    }

    private void ativarModoDestruicao() {
        Toolkit.getDefaultToolkit().beep();
        JPasswordField pf = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(this, pf, 
                "ATENÇÃO: ACESSO RESTRITO\nDigite a senha de administrador para FORMATAR os registros:", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

        if (ok == JOptionPane.OK_OPTION) {
            String senha = new String(pf.getPassword());
            if ("102030".equals(senha)) {
                int confirmacao = JOptionPane.showConfirmDialog(this, 
                        "Tem CERTEZA ABSOLUTA? Apagará clientes e arquivos.\nNão há como desfazer.",
                        "Última Chance", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                
                if (confirmacao == JOptionPane.YES_OPTION) {
                    try {
                        ConexaoDB.zerarBancoCompleto();
                        JOptionPane.showMessageDialog(this, "O Banco de Dados foi resetado.\nO sistema será fechado.");
                        System.exit(0); 
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Erro fatal: " + ex.getMessage());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Senha Incorreta.", "Segurança", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { e.printStackTrace(); }

        SwingUtilities.invokeLater(() -> {
            new TelaLogin().setVisible(true);
        });
    }
}