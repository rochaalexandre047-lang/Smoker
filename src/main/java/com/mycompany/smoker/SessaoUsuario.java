package com.mycompany.smoker;

public class SessaoUsuario {
    public static String loginLogado = "";
    public static String perfilLogado = ""; // Vai ser "ADMIN" ou "OPERADOR"
    
    public static void limparSessao() {
        loginLogado = "";
        perfilLogado = "";
    }
}