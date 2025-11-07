package org.puerta.sslserver;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 *
 * @author 777Styx
 */
public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final Map<String, String> userDatabase; // {username: password}
    private final Map<String, Boolean> activeSessions; // {clientIdentifier: isAuthenticated}
    private final String clientIdentifier;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isAuthenticated = false;
    private String username = "Invitado";

    // Constructor que recibe el socket, la base de datos de usuarios y el mapa de sesiones
    public ClientHandler(Socket socket, Map<String, String> db, Map<String, Boolean> sessions) {
        this.clientSocket = socket;
        this.userDatabase = db;
        this.activeSessions = sessions;
        // Usamos la IP:Puerto como identificador único de la sesión
        this.clientIdentifier = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        this.activeSessions.put(clientIdentifier, false);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            out.println("Bienvenido. Escribe /login [usuario] [contraseña] para autenticarte.");

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[" + username + " - " + clientIdentifier + "] Mensaje recibido: " + clientMessage);
                if (clientMessage.startsWith("/login ")) {
                    handleLogin(clientMessage);
                } else if (clientMessage.equals("/logout")) {
                    handleLogout();
                    break;
                } else {
                    handleCommand(clientMessage);
                }
            }
        } catch (IOException e) {
            System.out.println("[" + username + "] Conexion perdida: " + e.getMessage());
        } finally {
            try {
                // Limpieza al cerrar la conexion
                activeSessions.remove(clientIdentifier);
                clientSocket.close();
                System.out.println("Sesion de [" + username + "] terminada.");
            } catch (IOException e) {
            }
        }
    }

    // Logica para manejar el comando /login
    private void handleLogin(String command) {
        if (isAuthenticated) {
            out.println("Ya estas autenticado como " + username + ".");
            return;
        }

        String[] parts = command.split(" ");
        if (parts.length != 3) {
            out.println("Uso: /login [usuario] [contraseña]");
            return;
        }

        String user = parts[1];
        String pass = parts[2];

        // Autenticacion simple (en un proyecto real usarías hashing o usa el que se ha visto previamente)
        if (userDatabase.containsKey(user) && userDatabase.get(user).equals(pass)) {
            isAuthenticated = true;
            username = user;
            activeSessions.put(clientIdentifier, true);
            out.println("Autenticacion exitosa." + username + "!");
        } else {
            out.println("Error de autenticacion. Usuario o contraseña incorrectos.");
        }
    }

    // Logica para manejar el comando /logout
    private void handleLogout() {
        if (isAuthenticated) {
            out.println("Cerrando sesion de " + username + ".");
        } else {
            out.println("No estabas autenticado.");
        }
        isAuthenticated = false;
        username = "Invitado";
        activeSessions.put(clientIdentifier, false);
    }

    // Lógica de CONTROL DE ACCESO (Autorizacion)
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
