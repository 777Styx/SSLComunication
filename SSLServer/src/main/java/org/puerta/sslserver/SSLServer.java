package org.puerta.sslserver;

import javax.net.ssl.*;
import java.io.*;

/**
 *
 * @author julli
 */
public class SSLServer {

    public static void main(String[] args) throws Exception {
        int port = 65432;

        System.setProperty("javax.net.ssl.keyStore", "server-keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");

        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port);

   
        serverSocket.setNeedClientAuth(false);

        System.out.println("SSL Server started mai boi");
        SSLSocket socket = (SSLSocket) serverSocket.accept();
        System.out.println("Client in baby");

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        String message;
        while ((message = in.readLine()) != null) {
            System.out.println("Message recived: " + message);
            out.println("Message recived: " + message);
        }

        socket.close();
        serverSocket.close();
    }
}
