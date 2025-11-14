package org.puerta.sslclient;

import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;

/**
 *
 * @author 777Styx
 */
public class SSLClient {

    public static void main(String[] args) throws Exception {
        int port = 65432;
        String host = "127.0.0.1";

        //trustStore; archivo JKS que contiene certificados del servidor en los que el cliente confía.
        System.setProperty("javax.net.ssl.trustStore", "client-truststore.jks");
        //trustStorePassword; contraseña del almacén.
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        //Se obtiene la fabrica de sockets SSL.
        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        //Se crea un SSLSocket que inicia la conexión
        SSLSocket socket = (SSLSocket) ssf.createSocket(host, port);

        System.out.println("Conected to SSL Server .");

        //Lectura y escritura hacia el servidor
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        //permite leer desde la consola
        Scanner scanner = new Scanner(System.in);

        System.out.println("Start Writting");

      
        /**
         * Lee las líneas enviadas por el servidor,
           Las imprime en pantalla con el prefijo "Server:"
           Si ocurre un error (servidor desconectado), muestra "Lost conexion"
         */
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println("Server: " + serverMessage);
                }
            } catch (IOException e) {
                System.err.println("Lost conexion");
            }
        }).start();

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();
            out.println(input);
        }

        socket.close();
    }
}
