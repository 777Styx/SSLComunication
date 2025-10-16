package org.puerta.sslclient;

import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;

/**
 *
 * @author julli
 */
public class SSLClient {

    public static void main(String[] args) throws Exception {
        int port = 65432;
        String host = "127.0.0.1";

        System.setProperty("javax.net.ssl.trustStore", "client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) ssf.createSocket(host, port);

        System.out.println("Conected to SSL Server .");

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        Scanner scanner = new Scanner(System.in);

        System.out.println("Start Writting");

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
