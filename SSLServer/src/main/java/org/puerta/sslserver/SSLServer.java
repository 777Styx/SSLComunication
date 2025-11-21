package org.puerta.sslserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author 777Styx
 */
public class SSLServer {

    // Inicializamos el Logger
    private static final Logger logger = LogManager.getLogger(SSLServer.class);

    private static final Map<String, String> USER_DATABASE = new HashMap<>(); //Base de datos de usuarios
    private static final Map<String, Boolean> ACTIVE_SESSIONS = new HashMap<>(); //mantiene las sesiones activas

    //inicializar la base de datos de usuarios
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

        // LOG: Inicio del servicio
        logger.info("SSL Server INICIADO en el puerto {}", port);
        logger.info("Esperando clientes...");

        while (true) {
            try {
                // Espera por una conexion de cliente
                SSLSocket socket = (SSLSocket) serverSocket.accept();

                // LOG: Auditoría de conexion (Importante registrar la IP)
                logger.info("NUEVA CONEXION entrante desde: {}:{}",
                        socket.getInetAddress().getHostAddress(), socket.getPort());

                // Crea y arranca un nuevo hilo para manejar la sesion del cliente
                ClientHandler clientHandler = new ClientHandler(socket, USER_DATABASE, ACTIVE_SESSIONS);
                clientHandler.start();

            } catch (IOException e) {
                // LOG: Error critico
                logger.error("Error al aceptar conexion de cliente: {}", e.getMessage());
            }
        }
    }
}
