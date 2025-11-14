package org.puerta.sslserver;

import javax.net.ssl.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author 777Styx
 */
public class SSLServer {

    //Simula la BD
    private static final Map<String, String> USER_DATABASE = new HashMap<>();

    // Usa un secreto fuerte y no lo hardcodees en produccion, pero aqui no es produccion.
    // Léelo de variables de entorno o un archivo de configuración, .env normalmente.
    private static final String JWT_SECRET = "mi-clave-secreta-para-firmar-tokens-jwt-muy-larga-y-segura-y-puro-ct";

    //Son los usuarios registrados en la BD
    static {
        USER_DATABASE.put("admin", "admin123");
        USER_DATABASE.put("Styx", "password");
        USER_DATABASE.put("estudiante", "java4ever");
    }

    public static void main(String[] args) throws Exception {
        //Puerto en donde se inciara
        int port = 65432;

        // Configuracion de SSL/TLS 
        System.setProperty("javax.net.ssl.keyStore", "server-keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");

        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port);

        // No requerimos autenticacion del certificado del cliente 
        serverSocket.setNeedClientAuth(false);

        System.out.println("SSL Server (JWT) iniciado en el puerto " + port);

        //Bucle infinito esperando por concexiones
        while (true) {
            try {
                // Espera por una conexion de cliente
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                System.out.println("Client conectado: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

                // Crea y arranca un nuevo hilo para manejar la sesion del cliente
                // Ya no pasamos el mapa de sesiones, pasamos el secreto JWT
                ClientHandler clientHandler = new ClientHandler(socket, USER_DATABASE, JWT_SECRET);
                clientHandler.start();

            } catch (IOException e) {
                System.err.println("Error al aceptar conexion de cliente: " + e.getMessage());
            }
        }
    }
}
