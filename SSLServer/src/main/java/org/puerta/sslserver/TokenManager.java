package org.puerta.sslserver;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * Clase de utilidad para manejar la creación y validación de tokens JWT.
 *
 * @author 777Styx 
 */
public class TokenManager {

    /**
     * Genera un token JWT para un usuario.
     *
     * @param username El nombre de usuario (subject) del token.
     * @param secret La clave secreta para firmar el token.
     * @return El token JWT como un String.
     */
    public static String generateToken(String username, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String token = JWT.create()
                    .withSubject(username)
                    // Puedes añadir más "claims" si lo necesitas, ej: .withClaim("rol", "admin")
                    // Tambien puedes añadir una fecha de expiración
                    // .withExpiresAt(new Date(System.currentTimeMillis() + (60 * 60 * 1000))) // 1 hora
                    .sign(algorithm);
            return token;
        } catch (Exception e) {
            System.err.println("Error al generar el token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Valida un token JWT y devuelve el nombre de usuario (subject) si es
     * válido.
     *
     * @param token El token JWT a validar.
     * @param secret La clave secreta con la que se firmó el token.
     * @return El nombre de usuario si el token es válido, o null si es
     * inválido.
     */
    public static String validateToken(String token, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .build(); // No se especifica issuer o audience
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getSubject();
        } catch (JWTVerificationException exception) {
            // El token es inválido (firma incorrecta, expirado, etc.)
            System.err.println("Validación de token fallida: " + exception.getMessage());
            return null;
        }
    }
}
