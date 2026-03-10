package com.mycompany.smoker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexaoDB {

    private static final String URL_BANCO = "jdbc:sqlite:sistema_arquivos.db";

    public static Connection conectar() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL_BANCO);
            criarTabelasIniciais(conn);
            verificarAtualizacoes(conn);
        } catch (SQLException e) {
            System.out.println("Erro ao conectar: " + e.getMessage());
        }
        return conn;
    }

    private static void criarTabelasIniciais(Connection conn) {
        String sqlTemplates = "CREATE TABLE IF NOT EXISTS templates ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " nome_modulo VARCHAR(100),"
                + " estrutura_json TEXT"
                + ");";

        String sqlRegistros = "CREATE TABLE IF NOT EXISTS registros ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " template_id INTEGER,"
                + " titulo_principal VARCHAR(200),"
                + " caminho_arquivo VARCHAR(500),"
                + " dados_json TEXT,"
                + " data_vencimento VARCHAR(20),"  
                + " data_criacao DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + " FOREIGN KEY(template_id) REFERENCES templates(id)"
                + ");";
        
        String sqlEventos = "CREATE TABLE IF NOT EXISTS eventos ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " titulo VARCHAR(200),"
                + " data_evento DATE," 
                + " hora_inicio VARCHAR(10),"
                + " hora_fim VARCHAR(10),"
                + " descricao TEXT"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sqlTemplates);
            stmt.execute(sqlRegistros);
            stmt.execute(sqlEventos); 
        } catch (SQLException e) {
            System.out.println("Erro ao criar tabelas: " + e.getMessage());
        }
    }
    
    private static void verificarAtualizacoes(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE registros ADD COLUMN data_vencimento VARCHAR(20);");
        } catch (SQLException e) { }
    }
    

    public static void zerarBancoCompleto() throws SQLException {
        try (Connection conn = conectar();
             Statement stmt = conn.createStatement()) {
            

            stmt.executeUpdate("DELETE FROM registros");
            stmt.executeUpdate("DELETE FROM templates");
            stmt.executeUpdate("DELETE FROM eventos");

            stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='registros'");
            stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='templates'");
            stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='eventos'");
            
 
            stmt.executeUpdate("VACUUM");
        }
    }
}