package org.puerta.sslclient;

import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;

/**
 *
 * @author 777Styx
 */
public class SSLClient {

    // Variable estática para almacenar nuestro token de autenticación
    private static String authToken = null;

    public static void main(String[] args) throws Exception {
        int port = 65432;
        String host = "127.0.0.1";

        System.setProperty("javax.net.ssl.trustStore", "client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) ssf.createSocket(host, port);

        System.out.println("Conectado a SSL Server.");

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        Scanner scanner = new Scanner(System.in);

        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {

                    //Capturar el token
                    if (serverMessage.startsWith("LOGIN_SUCCESS ")) {
                        authToken = serverMessage.substring("LOGIN_SUCCESS ".length());
                        System.out.println("Autenticacion exitosa Token almacenado.");
                    } else {
                        // Imprimir mensajes
                        System.out.println("Server: " + serverMessage);
                    }
                }
            } catch (IOException e) {
                System.err.println("Conexion perdida con el servidor.");
                System.exit(1);
            }
        }).start();

        System.out.println("Listo para enviar comandos.");

        // Hilo para leer la entrada del usuario
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();

            if (input.startsWith("/login ")) {
                // El login se envía sin token
                out.println(input);
            } else if (input.equals("/logout")) {
                // El logout se envía sin token
                out.println(input);
                authToken = null; // El cliente borra su token
                System.out.println("Sesion cerrada localmente. Token eliminado.");
            } else {
                // Es otro comando, debemos adjuntar el token
                if (authToken != null) {
                    // Formato: [token] [comando]
                    out.println(authToken + " " + input);
                } else {
                    // Enviar de todos modos, pero esperar un error del servidor
                    // Opcionalmente, puedes bloquearlo aqui:
                    System.out.println("Necesitas hacer /login primero.");

                    // Lo enviamos para que el servidor muestre el error de "ACCESO DENEGADO"
                    out.println(input);
                }
            }
        }

        socket.close();
    }
}
