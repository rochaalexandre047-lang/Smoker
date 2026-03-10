package com.mycompany.smoker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class Main extends JFrame {

   
    private final Set<Integer> teclasPressionadas = new HashSet<>();

    public Main() {
        setTitle("Sistema SMOKER - Enterprise Edition");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Novo Registro", new TelaCadastro());
        abas.addTab("Consultar / Pesquisar", new TelaListagem()); // Assumindo que TelaListagem é a de consulta
        abas.addTab("Criador de Modelos", new TelaCriadorTemplate());
        abas.addTab("Utilitários", new PainelFerramentas());

        add(abas);


        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                teclasPressionadas.add(e.getKeyCode());
                
                // Verifica se D e B estão pressionados ao mesmo tempo
                if (teclasPressionadas.contains(KeyEvent.VK_D) && teclasPressionadas.contains(KeyEvent.VK_B)) {
                    // Evita disparar múltiplas vezes seguidas
                    teclasPressionadas.clear(); 
                    ativarModoDestruicao();
                }
            } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                teclasPressionadas.remove(e.getKeyCode());
            }
            return false; // Deixa o evento passar para o resto do sistema
        });
        // ---------------------------------------------
    }

    private void ativarModoDestruicao() {

        Toolkit.getDefaultToolkit().beep();
        
 
        JPasswordField pf = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(this, pf, 
                "⚠️ ATENÇÃO: ACESSO RESTRITO AO BANCO DE DADOS ⚠️\nDigite a senha de administrador para FORMATAR o sistema:", 
                JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.WARNING_MESSAGE);

        if (ok == JOptionPane.OK_OPTION) {
            String senha = new String(pf.getPassword());
            if ("102030".equals(senha)) {
                
        
                int confirmacao = JOptionPane.showConfirmDialog(this, 
                        "Tem CERTEZA ABSOLUTA? Isso apagará clientes, modelos, agenda e arquivos.\nNão há como desfazer.",
                        "Última Chance",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE);
                
                if (confirmacao == JOptionPane.YES_OPTION) {
                    try {
                        ConexaoDB.zerarBancoCompleto();
                        JOptionPane.showMessageDialog(this, "O Banco de Dados foi completamente resetado.\nO sistema será fechado para aplicar as alterações.");
                        System.exit(0); // Fecha o programa para evitar erros de cache
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Erro fatal ao tentar zerar: " + ex.getMessage());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Senha Incorreta. Acesso negado.", "Segurança", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
      
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { e.printStackTrace(); }

        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}