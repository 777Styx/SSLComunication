package org.puerta.sslclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;

/**
 *
 * @author 777Styx
 */
public class SSLClient {

    private static final Logger logger = LogManager.getLogger(SSLClient.class);

    public static void main(String[] args) throws Exception {
        int port = 65432;
        String host = "127.0.0.1";

        //trustStore; archivo JKS que contiene certificados del servidor en los que el cliente confía.
        System.setProperty("javax.net.ssl.trustStore", "client-truststore.jks");
        //trustStorePassword; contraseña del almacén.
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        try {
            //Se obtiene la fabrica de sockets SSL.
            SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            //Se crea un SSLSocket que inicia la conexion
            SSLSocket socket = (SSLSocket) ssf.createSocket(host, port);

            //LOG: Conexion  
            logger.info("Conectado exitosamente al servidor SSL en {}:{}", host, port);

            //Lectura y escritura hacia el servidor
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            //permite leer desde la consola
            Scanner scanner = new Scanner(System.in);

            System.out.println("Escribe tus comandos (/login usuario pass):");

            /**
             * Crea un hilo que lee las líneas enviadas por el servidor, Las
             * imprime en pantalla con el prefijo "Server:" Si ocurre un error
             * (servidor desconectado), muestra "Lost conexion"
             */
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        // Usamos System.out para que el usuario lo vea limpio,
                        // pero podríamos usar logger.info si queremos guardar registro local.
                        System.out.println("Server: " + serverMessage);
                    }
                } catch (IOException e) {
                    logger.error("Conexión perdida con el servidor.");
                }
            }).start();

            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                out.println(input);
                // Logueamos localmente lo que enviamos (para debug)
                logger.debug("Enviado: {}", input);
            }

            socket.close();
        } catch (Exception e) {
            logger.fatal("No se pudo conectar al servidor o error fatal: {}", e.getMessage());
        }
    }
}
