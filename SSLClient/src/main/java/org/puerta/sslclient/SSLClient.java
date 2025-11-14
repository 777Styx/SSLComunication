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

        //Establecemos a donde conectarnos 
        int port = 65432;
        String host = "127.0.0.1";

        //TrustStore: Configura la ubicación del "truststore" y su contraseña. 
        //El truststore contiene las claves y certificados de confianza para las conexiones SSL.
        System.setProperty("javax.net.ssl.trustStore", "client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        //Crea un socket SSL que se conecta al servidor en el host y puerto especificados.
        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) ssf.createSocket(host, port);

        System.out.println("Conectado a SSL Server.");

        //Lectura y escritura
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        Scanner scanner = new Scanner(System.in);

        //Hilo para escuchar (recibir) mensajes del servidor
        //Si el servidor envía un mensaje de autenticación exitosa 
        //(que empieza con "LOGIN_SUCCESS "), el token de autenticación se extrae y se almacena en la variable authToken.
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {

                    if (serverMessage.startsWith("LOGIN_SUCCESS ")) {
                        authToken = serverMessage.substring("LOGIN_SUCCESS ".length());  //Capturar el token
                        System.out.println("Autenticacion exitosa Token almacenado.");
                    } else {
                        System.out.println("Server: " + serverMessage);
                    }
                }
            } catch (IOException e) {
                System.err.println("Conexion perdida con el servidor.");
                System.exit(1);
            }
        }).start();

        System.out.println("Listo para enviar comandos.");

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();

            // El login se envía sin token
            if (input.startsWith("/login ")) {
                out.println(input);
            } // El logout se envía sin token
            else if (input.equals("/logout")) {
                out.println(input);
                authToken = null; // El cliente borra su token
                System.out.println("Sesion cerrada localmente. Token eliminado.");
            } else {
                // Es otro comando, debemos adjuntar el token (esto para que el servidor pueda verificar )
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
