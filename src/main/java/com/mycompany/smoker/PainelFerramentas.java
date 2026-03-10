package com.mycompany.smoker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class PainelFerramentas extends JPanel {


    private YearMonth mesAtual;
    private JLabel lblMesAno;
    private JPanel painelDias;
    

    private DefaultListModel<String> modeloListaEventos;
    private JList<String> listaEventos;

    public PainelFerramentas() {
        setLayout(new BorderLayout());
        mesAtual = YearMonth.now(); 
        

        JPanel pnlBackup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        pnlBackup.setBorder(BorderFactory.createTitledBorder("️ Segurança & Backup"));
        pnlBackup.setBackground(new Color(245, 245, 255));
        
        JButton btnFazerBackup = new JButton(" Criar Backup Agora");
        btnFazerBackup.setBackground(new Color(100, 100, 200));
        btnFazerBackup.setForeground(Color.WHITE);
        btnFazerBackup.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JButton btnRestaurar = new JButton(" Ler/Restaurar Backup");
        btnRestaurar.setBackground(new Color(255, 150, 100));
        btnRestaurar.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnFazerBackup.addActionListener(e -> realizarBackupCompleto());
        btnRestaurar.addActionListener(e -> restaurarBackupCompleto());
        
        pnlBackup.add(btnFazerBackup);
        pnlBackup.add(btnRestaurar);
        pnlBackup.add(new JLabel("<html><font size='2' color='gray'>O backup salva seus clientes, agenda e arquivos anexos.</font></html>"));
        
        add(pnlBackup, BorderLayout.NORTH);


        JPanel pnlPrincipal = new JPanel(new GridLayout(2, 1)); 
        

        JPanel pnlCalendario = new JPanel(new BorderLayout());
        pnlCalendario.setBorder(BorderFactory.createTitledBorder(" Visão Geral (Agenda + Prazos)"));
        JPanel pnlNav = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnAnt = new JButton("◀");
        JButton btnProx = new JButton("▶");
        lblMesAno = new JLabel();
        lblMesAno.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnAnt.addActionListener(e -> { mesAtual = mesAtual.minusMonths(1); montarCalendario(); });
        btnProx.addActionListener(e -> { mesAtual = mesAtual.plusMonths(1); montarCalendario(); });
        pnlNav.add(btnAnt); pnlNav.add(lblMesAno); pnlNav.add(btnProx);
        pnlCalendario.add(pnlNav, BorderLayout.NORTH);
        painelDias = new JPanel(new GridLayout(0, 7, 5, 5)); 
        painelDias.setBorder(new EmptyBorder(10, 10, 10, 10));
        pnlCalendario.add(painelDias, BorderLayout.CENTER);
        pnlPrincipal.add(pnlCalendario);


        JPanel pnlProximos = new JPanel(new BorderLayout());
        pnlProximos.setBorder(BorderFactory.createTitledBorder("🔔 Próximas Atividades"));
        JPanel pnlFerramentasLista = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAtualizar = new JButton("🔄 Atualizar Lista");
        btnAtualizar.addActionListener(e -> { montarCalendario(); atualizarListaProximos(); });
        pnlFerramentasLista.add(btnAtualizar);
        pnlProximos.add(pnlFerramentasLista, BorderLayout.NORTH);
        modeloListaEventos = new DefaultListModel<>();
        listaEventos = new JList<>(modeloListaEventos);
        listaEventos.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        listaEventos.setCellRenderer(new RenderizadorEventos()); 
        listaEventos.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) excluirEventoDaLista();
            }
        });
        pnlProximos.add(new JScrollPane(listaEventos), BorderLayout.CENTER);
        pnlPrincipal.add(pnlProximos);

        add(pnlPrincipal, BorderLayout.CENTER);
        montarCalendario();
        atualizarListaProximos();
    }
    

    private void realizarBackupCompleto() {
        try {
            File pastaBackups = new File("backups");
            if (!pastaBackups.exists()) pastaBackups.mkdir();
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File pastaDestino = new File(pastaBackups, "backup_" + timestamp);
            if (!pastaDestino.mkdir()) {
                JOptionPane.showMessageDialog(this, "Erro ao criar pasta de destino.");
                return;
            }

            File dbOriginal = new File("sistema_arquivos.db");
            if (dbOriginal.exists()) {
                Files.copy(dbOriginal.toPath(), new File(pastaDestino, "sistema_arquivos.db").toPath());
            }

            File pastaDocs = new File("docs");
            File docsDestino = new File(pastaDestino, "docs");
            if (pastaDocs.exists()) {
                copiarDiretorio(pastaDocs, docsDestino);
            }

            JOptionPane.showMessageDialog(this, " Backup criado com sucesso!\nLocal: " + pastaDestino.getAbsolutePath());
            Desktop.getDesktop().open(pastaDestino);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro no backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void restaurarBackupCompleto() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            " ATENÇÃO \nAo restaurar um backup, TODOS os dados e arquivos atuais serão SUBSTITUÍDOS.\nO sistema precisará ser fechado.\n\nDeseja continuar?", 
            "Restaurar Backup", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) return;

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Selecione a PASTA do Backup");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File pastaPadrao = new File("backups");
        if(pastaPadrao.exists()) fc.setCurrentDirectory(pastaPadrao);

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File pastaBackupSelecionada = fc.getSelectedFile();
            File dbBackup = new File(pastaBackupSelecionada, "sistema_arquivos.db");
            if (!dbBackup.exists()) {
                JOptionPane.showMessageDialog(this, " Backup inválido (falta o arquivo .db).");
                return;
            }

            try {
  
                File dbAtual = new File("sistema_arquivos.db");
                Files.copy(dbBackup.toPath(), dbAtual.toPath(), StandardCopyOption.REPLACE_EXISTING);

      
                File pastaDocsBackup = new File(pastaBackupSelecionada, "docs");
                if (pastaDocsBackup.exists()) {
                    File pastaDocsAtual = new File("docs");
                    if (!pastaDocsAtual.exists()) {
                        pastaDocsAtual.mkdir();
                    } else {
     
                        limparDiretorio(pastaDocsAtual);
                    }
                    copiarDiretorio(pastaDocsBackup, pastaDocsAtual);
                }

                JOptionPane.showMessageDialog(this, " Restauração Concluída!\nO sistema será fechado.");
                System.exit(0);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao restaurar (Talvez o banco esteja travado).\nDetalhe: " + e.getMessage());
            }
        }
    }

    private void copiarDiretorio(File origem, File destino) throws IOException {
        if (origem.isDirectory()) {
            if (!destino.exists()) { destino.mkdir(); }
            String[] filhos = origem.list();
            if (filhos != null) {
                for (String filho : filhos) {
                    copiarDiretorio(new File(origem, filho), new File(destino, filho));
                }
            }
        } else {
            Files.copy(origem.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    

    private void limparDiretorio(File diretorio) {
        if (!diretorio.exists()) return;
        File[] files = diretorio.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    limparDiretorio(file);
                }
                file.delete();
            }
        }
    }

    
    private void montarCalendario() {
        painelDias.removeAll();
        String[] mesesPT = {"JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO", "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"};
        String titulo = mesesPT[mesAtual.getMonthValue()-1] + " " + mesAtual.getYear();
        lblMesAno.setText(titulo);
        String[] sem = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab"};
        for (String s : sem) {
            JLabel lbl = new JLabel(s, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            painelDias.add(lbl);
        }
        LocalDate primeiroDia = mesAtual.atDay(1);
        int diaSemanaInicio = primeiroDia.getDayOfWeek().getValue(); 
        if (diaSemanaInicio == 7) diaSemanaInicio = 0; 
        for (int i = 0; i < diaSemanaInicio; i++) { painelDias.add(new JLabel("")); }
        int diasNoMes = mesAtual.lengthOfMonth();
        LocalDate hoje = LocalDate.now();
        for (int dia = 1; dia <= diasNoMes; dia++) {
            LocalDate dataDoBotao = mesAtual.atDay(dia);
            boolean temEvento = verificarSeTemAgenda(dataDoBotao);
            boolean temVencimento = verificarSeTemVencimento(dataDoBotao);
            StringBuilder html = new StringBuilder("<html><b>" + dia + "</b>");
            if (temEvento || temVencimento) {
                html.append("<sup>"); 
                if (temEvento) html.append(" <font color='blue'>★</font>");
                if (temVencimento) html.append(" <font color='red'><b>!</b></font>");
                html.append("</sup>");
            }
            html.append("</html>");
            JButton btnDia = new JButton(html.toString());
            if (dataDoBotao.equals(hoje)) { btnDia.setBackground(new Color(200, 230, 255)); } 
            else if (temVencimento) { btnDia.setBackground(new Color(255, 230, 230)); } 
            else if (temEvento) { btnDia.setBackground(new Color(255, 255, 220)); } 
            else { btnDia.setBackground(Color.WHITE); }
            btnDia.addActionListener(e -> abrirDialogoEvento(dataDoBotao));
            painelDias.add(btnDia);
        }
        painelDias.revalidate(); painelDias.repaint();
    }
    private boolean verificarSeTemAgenda(LocalDate data) {
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM eventos WHERE data_evento = ? LIMIT 1")) {
            stmt.setString(1, data.toString());
            return stmt.executeQuery().next();
        } catch (Exception e) { return false; }
    }
    private boolean verificarSeTemVencimento(LocalDate data) {
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM registros WHERE data_vencimento = ? LIMIT 1")) {
            stmt.setString(1, data.toString());
            return stmt.executeQuery().next();
        } catch (Exception e) { return false; }
    }
    private void abrirDialogoEvento(LocalDate data) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Agenda: " + data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), true);
        d.setSize(400, 350);
        d.setLayout(new GridLayout(5, 1, 10, 10));
        JTextField txtTitulo = new JTextField();
        JTextField txtInicio = new JTextField("08:00");
        JTextField txtFim = new JTextField("09:00");
        JTextArea txtDesc = new JTextArea();
        d.add(criarPainelInput("Título do Compromisso:", txtTitulo));
        JPanel pnlHoras = new JPanel(new GridLayout(1, 2));
        pnlHoras.add(criarPainelInput("Início:", txtInicio));
        pnlHoras.add(criarPainelInput("Fim:", txtFim));
        d.add(pnlHoras);
        d.add(new JScrollPane(txtDesc));
        txtDesc.setBorder(BorderFactory.createTitledBorder("Descrição"));
        JButton btnSalvar = new JButton(" Agendar Compromisso");
        btnSalvar.setBackground(new Color(100, 200, 100));
        btnSalvar.setForeground(Color.WHITE);
        btnSalvar.addActionListener(e -> {
            salvarEvento(data, txtTitulo.getText(), txtInicio.getText(), txtFim.getText(), txtDesc.getText());
            d.dispose();
            montarCalendario(); 
            atualizarListaProximos();
        });
        d.add(btnSalvar);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }
    private JPanel criarPainelInput(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(label), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        p.setBorder(new EmptyBorder(0,5,0,5));
        return p;
    }
    private void salvarEvento(LocalDate data, String titulo, String inicio, String fim, String desc) {
        if(titulo.isEmpty()) return;
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO eventos (titulo, data_evento, hora_inicio, hora_fim, descricao) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, titulo);
            stmt.setString(2, data.toString());
            stmt.setString(3, inicio);
            stmt.setString(4, fim);
            stmt.setString(5, desc);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Agendado!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage());
        }
    }
    private void atualizarListaProximos() {
        modeloListaEventos.clear();
        String sql = 
            "SELECT id, titulo, data_evento as data, hora_inicio as hora, 'AGENDA' as tipo " +
            "FROM eventos WHERE data_evento >= date('now', 'localtime') " +
            "UNION ALL " +
            "SELECT id, titulo_principal as titulo, data_vencimento as data, '23:59' as hora, 'PRAZO' as tipo " +
            "FROM registros WHERE data_vencimento >= date('now', 'localtime') AND data_vencimento IS NOT NULL " +
            "ORDER BY data ASC, hora ASC LIMIT 10";
        try (Connection conn = ConexaoDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                String titulo = rs.getString("titulo");
                String data = rs.getString("data");
                String hora = rs.getString("hora");
                String tipo = rs.getString("tipo");
                int id = rs.getInt("id");
                String[] partesData = data.split("-");
                String dataBonita = partesData[2] + "/" + partesData[1];
                String html;
                if (tipo.equals("AGENDA")) {
                    html = "<html><font color='#0066cc'>★ " + dataBonita + " [" + hora + "]</font> - <b>" + titulo + "</b></html>";
                } else {
                    html = "<html><font color='#cc0000'><b>! " + dataBonita + " [VENCIMENTO]</b></font> - " + titulo + "</html>";
                }
                String item = tipo + "||" + id + "||" + html; 
                modeloListaEventos.addElement(item);
            }
            if (modeloListaEventos.isEmpty()) {
                modeloListaEventos.addElement("NADA||0||<html><i>Nenhuma atividade próxima.</i></html>");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    private void excluirEventoDaLista() {
        String item = listaEventos.getSelectedValue();
        if(item == null || item.startsWith("NADA")) return;
        String[] parts = item.split("\\|\\|"); 
        String tipo = parts[0];
        int id = Integer.parseInt(parts[1]);
        if (tipo.equals("PRAZO")) {
            JOptionPane.showMessageDialog(this, "Para excluir este Vencimento, vá na aba 'Consultar'."); return;
        }
        int op = JOptionPane.showConfirmDialog(this, "Excluir?", "Agenda", JOptionPane.YES_NO_OPTION);
        if(op == JOptionPane.YES_OPTION) {
             try (Connection conn = ConexaoDB.conectar();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM eventos WHERE id = ?")) {
                stmt.setInt(1, id); stmt.executeUpdate();
                atualizarListaProximos(); montarCalendario();
            } catch (Exception e) {}
        }
    }
    class RenderizadorEventos extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String texto = (String) value;
            if(texto.contains("||")) {
                String[] parts = texto.split("\\|\\|", 3);
                if(parts.length >= 3) setText(parts[2]); 
            }
            return this;
        }
    }
}
