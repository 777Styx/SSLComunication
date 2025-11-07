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

    private static final Map<String, String> USER_DATABASE = new HashMap<>();
    private static final Map<String, Boolean> ACTIVE_SESSIONS = new HashMap<>();

    static {
        USER_DATABASE.put("admin", "admin123");
        USER_DATABASE.put("Styx", "password");
        USER_DATABASE.put("estudiante", "java4ever");
    }

    public static void main(String[] args) throws Exception {
        int port = 65432;

        // Configuracion de SSL/TLS 
        System.setProperty("javax.net.ssl.keyStore", "server-keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");

        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port);

        // No requerimos autenticacion del certificado del cliente 
        serverSocket.setNeedClientAuth(false);

        System.out.println("SSL Server iniciado en el puerto " + port + " y listo para recibir clientes.");

        while (true) {
            try {
                // Espera por una conexion de cliente
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                System.out.println("Client conectado: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

                // Crea y arranca un nuevo hilo para manejar la sesion del cliente
                ClientHandler clientHandler = new ClientHandler(socket, USER_DATABASE, ACTIVE_SESSIONS);
                clientHandler.start();

            } catch (IOException e) {
                System.err.println("Error al aceptar conexion de cliente: " + e.getMessage());
            }
        }
    }
}

