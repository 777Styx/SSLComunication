package org.puerta.keygen;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 *
 * @author julli
 */
public class KeyGen {

    public static void main(String[] args) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("secret.key"))) {
                oos.writeObject(secretKey);
                System.out.println(" AES key generated and saved in 'secret.key'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
