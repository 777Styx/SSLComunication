package org.puerta.sslserver;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * Hilo encargado de gestionar la comunicación con un único cliente conectado al
 * servidor SSL. Maneja autenticación, sesiones y validación de comandos.
 *
 * @author 777Styx
 */
public class ClientHandler extends Thread {

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

            // flujos de comunicación del socket
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
            System.out.println("[" + username + "] Conexion perdida: " + e.getMessage());
        } finally {
            try {
                // Limpieza al cerrar la conexion, se elimina el mapa 
                activeSessions.remove(clientIdentifier);
                clientSocket.close();
                System.out.println("Sesion de [" + username + "] terminada.");
            } catch (IOException e) {
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
            return;
        }

        //Separa el comando usando espacios: "/login user pass"
        String[] parts = command.split(" ");
        if (parts.length != 3) {
            out.println("Uso: /login [usuario] [contraseña]");
            return;
        }

        String user = parts[1];
        String pass = parts[2];

        // Autenticacion simple (en un proyecto real usarías hashing o usa el que se ha visto previamente)
        if (userDatabase.containsKey(user) && userDatabase.get(user).equals(pass)) {

            isAuthenticated = true; // Marcamos la sesion como autenticada
            username = user;// Actualizamos el nombre de usuario
            activeSessions.put(clientIdentifier, true);   // Actualizamos el estado de sesión en el mapa
            out.println("Autenticacion exitosa." + username + "!");
        } else {
            out.println("Error de autenticacion. Usuario o contraseña incorrectos.");
        }
    }

    // Cierra la sesión y restablece el estado.
    private void handleLogout() {
        if (isAuthenticated) {
            out.println("Cerrando sesion de " + username + ".");
        } else {
            out.println("No estabas autenticado.");
        }
        // Restablece la sesion a estado no autenticado
        isAuthenticated = false;
        username = "Invitado";

        // Actualiza el mapa de sesiones
        activeSessions.put(clientIdentifier, false);
    }

    // Lógica de CONTROL DE ACCESO (Autorizacion)
    /**
     *
     * @param command
     */
    private void handleCommand(String command) {
        if (isAuthenticated) {
            // Lógica si el usuario está autenticado
            out.println("Mensaje recibido de " + username + ": " + command);

        } else {
            // Lógica si el usuario NO está autenticado
            out.println("ACCESO DENEGADO. Debes autenticarte para realizar acciones. Usa /login [usuario] [contraseña]");
        }
    }
}
