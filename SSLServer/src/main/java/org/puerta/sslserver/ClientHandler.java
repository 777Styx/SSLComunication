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
    private final String jwtSecret; //Secreto utilizado para firmar y validar tokens JWT.
    private final String clientIdentifier;
    private BufferedReader in;
    private PrintWriter out;
    private String username = "Invitado"; // Solo para logging

    //  recibe el secreto JWT, no el mapa de sesiones
    //Genera el identificador del cliente usando su IP y puerto.
    public ClientHandler(Socket socket, Map<String, String> db, String secret) {
        this.clientSocket = socket;
        this.userDatabase = db;
        this.jwtSecret = secret;
        this.clientIdentifier = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    /**
     * Configura flujos de entrada/salida Envía mensaje de bienvenida e
     * instrucciones Espera comandos del cliente en un bucle
     *
     * Clasifica los comandos: /login /logout Otros: estos requieren token
     */
    @Override
    public void run() {
        try {
            //para lectura y escritura
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            out.println("Bienvenido. Escribe /login [usuario] [contraseña] para autenticarte.");
            out.println("Para otros comandos, usa el formato: [token] [comando]");

            String clientMessage;
            //Flujo general
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[" + username + " - " + clientIdentifier + "] Mensaje recibido: " + clientMessage);

                if (clientMessage.startsWith("/login ")) {
                    //Aqui se maneja la autenticacion
                    handleLogin(clientMessage);
                } else if (clientMessage.equals("/logout")) {
                    handleLogout();
                    break; // El cliente inicia el cierre
                } else {
                    // Todos los demás comandos deben ser validados
                    handleCommand(clientMessage);
                }
            }
        } catch (IOException e) {
            System.out.println("[" + username + "] Conexion perdida: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Sesion de [" + username + "] terminada.");
            } catch (IOException e) {

            }
        }
    }

    /**
     * Logica para manejar el comando /login, 
     * Valida que el comando tenga exactamente 3 partes 
     * Busca el usuario en la base de datos
     * Genera un JWT usando TokenManager.generateToken
     * 
     */
    private void handleLogin(String command) {
        String[] parts = command.split(" ");
        if (parts.length != 3) {
            out.println("Uso: /login [usuario] [contraseña]");
            return;
        }

        String user = parts[1];
        String pass = parts[2];

        // Autenticacion
        if (userDatabase.containsKey(user) && userDatabase.get(user).equals(pass)) {
            //Genera y envía el token
            String token = TokenManager.generateToken(user, jwtSecret);
            if (token != null) {
                // Actualiza el nombre de usuario para los logs del servidor
                this.username = user;
                // Envía el token al cliente
                out.println("LOGIN_SUCCESS " + token);
                System.out.println("Token generado para " + user);
            } else {
                out.println("Error interno al generar el token.");
            }
        } else {
            out.println("Error de autenticacion. Usuario o contraseña incorrectos.");
        }
    }

    // Este servidor es stateless, por lo que el token no se invalida en servidor,
    //solo se indica al cliente que lo borre
    private void handleLogout() {
        // En un sistema stateless, el "logout" real ocurre en el cliente
        // (el cliente simplemente borra su token).
        // El servidor solo confirma y resetea su log.
        out.println("Cerrando sesion. El cliente debe desechar el token.");
        this.username = "Invitado";
    }

    // CONTROL DE ACCESO, si el mensaje no cumple el formato, rechazo inmediato
    private void handleCommand(String clientMessage) {
        // El formato esperado es: "[token] [comando]"
        String[] parts = clientMessage.split(" ", 2);

        if (parts.length != 2) {
            out.println("ACCESO DENEGADO. Formato inválido. Se esperaba: [token] [comando]");
            return;
        }

        String token = parts[0];
        String actualCommand = parts[1];

        // Si es válido, devuelve el nombre de usuario contenido en el token.
        //Si es invalido, retorna null, acceso denegado.
        String userFromToken = TokenManager.validateToken(token, jwtSecret);

        if (userFromToken != null) {

            // (Opcional) Verificamos que el usuario del token coincida con el usuario que se logueó
            // en este handler. En un modelo puramente stateless, esto no es necesario,
            // pero lo mantenemos para la consistencia del log.
            if (userFromToken.equals(this.username)) {
                out.println("Mensaje (" + userFromToken + ") procesado: " + actualCommand);
            } else {
                // Esto podría pasar si el cliente se loguea como 'userA', luego como 'userB'
                // y trata de usar el token de 'userA' en la conexión de 'userB'.
                out.println("TOKEN_INVALIDO. El token no corresponde a la sesión activa.");
                this.username = "Invitado"; // Forzar deslogueo
            }
        } else {
            // Token inválido (expirado, firma incorrecta, etc.)
            out.println("ACCESO DENEGADO. Token inválido o expirado.");
        }
    }
}
