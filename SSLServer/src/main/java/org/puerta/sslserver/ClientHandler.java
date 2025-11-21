package org.puerta.sslserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import org.apache.logging.log4j.LogManager;

/**
 * Hilo encargado de gestionar la comunicación con un único cliente conectado al
 * servidor SSL. Maneja autenticación, sesiones y validación de comandos.
 *
 * @author 777Styx
 */
public class ClientHandler extends Thread {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final Socket clientSocket;
    private final Map<String, String> userDatabase; // {username: password}
    private final Map<String, Boolean> activeSessions; // {clientIdentifier: isAuthenticated}
    private final String clientIdentifier;  //ID unico basado en IP y puerto
    private BufferedReader in;
    private PrintWriter out;
    private boolean isAuthenticated = false; //Estado actual de autenticacion del cliente
    private String username = "Invitado";

    /**
     *
     * @param socket Socket SSL del cliente
     * @param db Base de datos de usuarios (mapa usuario→contraseña)
     * @param sessions Mapa que gestiona las sesiones activas
     */
    public ClientHandler(Socket socket, Map<String, String> db, Map<String, Boolean> sessions) {
        this.clientSocket = socket;
        this.userDatabase = db;
        this.activeSessions = sessions;
        // Usamos la IP:Puerto como identificador único de la sesión
        this.clientIdentifier = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        //Registra la sesion como no autenticada
        this.activeSessions.put(clientIdentifier, false);
    }

    //Se ejecuta un hilo por cliente
    //aqui se atienden los mensajes del cliente mientras la conexion este activa.
    @Override
    public void run() {
        try {

            // flujos de comunicacion del socket
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            out.println("Bienvenido. Escribe /login [usuario] [contraseña] para autenticarte.");

            String clientMessage;
            //procesa mensajes hasta que el cliente cierre la conexion
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[" + username + " - " + clientIdentifier + "] Mensaje recibido: " + clientMessage);
                if (clientMessage.startsWith("/login ")) {
                    handleLogin(clientMessage);
                } else if (clientMessage.equals("/logout")) {
                    handleLogout();
                    break;
                    //Cualquier otro mensaje es tratado como comando
                } else {
                    handleCommand(clientMessage);
                }
            }
        } catch (IOException e) {
            logger.warn("[{}] Conexion perdida : {}", username, e.getMessage());
        } finally {
            try {
                // Limpieza al cerrar la conexion, se elimina el mapa 
                activeSessions.remove(clientIdentifier);
                clientSocket.close();
                logger.info("[{}] Sesion finalizada y socket cerrado.", username);
            } catch (IOException e) {
                logger.error("Error cerrando socket", e);
            }
        }
    }

    /**
     * Procesa el login enviado por el cliente.
     *
     * @param command: texto completo recibido del cliente
     */
    private void handleLogin(String command) {
        //Evita que un usuario ya autenticado vuelva a intentar loguearse
        if (isAuthenticated) {
            out.println("Ya estas autenticado como " + username + ".");
            logger.warn("[{}] Intento de re-login detectado (sospechoso).", username);
            return;
        }

        //Separa el comando usando espacios: "/login user pass"
        String[] parts = command.split(" ");
        if (parts.length != 3) {
            out.println("Uso: /login [usuario] [contraseña]");
            logger.warn("[{}] Formato de login incorrecto recibido.", clientIdentifier);
            return;
        }

        String user = parts[1];
        String pass = parts[2];

        // SEGURIDAD: NUNCA loguear la contraseña ('pass') en texto plano.
        // Autenticacion simple (en un proyecto real usarías hashing o usa el que se ha visto previamente)
        if (userDatabase.containsKey(user) && userDatabase.get(user).equals(pass)) {
            isAuthenticated = true; // Marcamos la sesion como autenticada
            username = user;// Actualizamos el nombre de usuario
            activeSessions.put(clientIdentifier, true);   // Actualizamos el estado de sesión en el mapa
            out.println("Autenticacion exitosa." + username + "!");

            // LOG: Exito - critico
            logger.info("LOGIN EXITOSO: Usuario '{}' autenticado desde IP {}", user, clientIdentifier);
        } else {
            out.println("Error de autenticacion. Usuario o contraseña incorrectos.");

            // LOG: Fallo de seguridad (Importante para detectar Fuerza Bruta)
            logger.warn("LOGIN FALLIDO: Intento para usuario '{}' desde IP {}", user, clientIdentifier);
        }
    }

    // Cierra la sesion y restablece el estado.
    private void handleLogout() {
        if (isAuthenticated) {
            out.println("Cerrando sesion de " + username + ".");
            logger.info("LOGOUT: Usuario '{}' cerró sesión voluntariamente.", username);
        } else {
            out.println("No estabas autenticado.");
        }
        // Restablece la sesion a estado no autenticado
        isAuthenticated = false;
        username = "Invitado";

        // Actualiza el mapa de sesiones
        activeSessions.put(clientIdentifier, false);
    }

    // Logica de CONTROL DE ACCESO (Autorizacion)
    /**
     *
     * @param command
     */
    private void handleCommand(String command) {
        if (isAuthenticated) {
            // Logica si el usuario está autenticado
            out.println("Mensaje recibido de " + username + ": " + command);

            // LOG: Auditoría de acciones (Quién hizo qué)
            logger.info("ACCION: Usuario '{}' ejecutó comando/mensaje: '{}'", username, command);
        } else {
            // Lógica si el usuario NO está autenticado
            out.println("ACCESO DENEGADO. Debes autenticarte para realizar acciones. Usa /login [usuario] [contraseña]");
            // LOG: Intento de acceso no autorizado
            logger.warn("ACCESO DENEGADO: IP {} intentó ejecutar comandos sin autenticación.", clientIdentifier);
        }
    }
}
