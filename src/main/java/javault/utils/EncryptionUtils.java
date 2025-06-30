package javault.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Gestisce la cifratura e decifratura con AES-GCM,
 * e la derivazione sicura della chiave AES da master password tramite PBKDF2.
 */
public class EncryptionUtils {

    private static final int KEY_SIZE = 256;
    private static final int ITERATIONS = 65536;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String SALT_PATH = "vault/salt.bin";

    /**
     * Deriva una chiave AES dalla master password usando PBKDF2 con salt persistente.
     * Se il salt non esiste, ne genera uno nuovo e lo salva su disco.
     *
     * @param password la master password (char array) da cui derivare la chiave
     * @return la chiave AES derivata, pronta per cifratura/decifratura
     * @throws RuntimeException se la generazione della chiave fallisce per problemi crittografici
     */
    public static SecretKey getKeyFromPassword(char[] password) {
        byte[] salt = null;

        Path path = Path.of(SALT_PATH);
        if (Files.exists(path)) {
            try {
                salt = Files.readAllBytes(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            salt = generateSalt();
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, salt);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE);
        
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Errore nella generazione della chiave", e);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(salt, (byte) 0);
        }
    }

    /**
     * Cifra un array di byte con AES in modalità GCM (Galois/Counter Mode).
     * Usa un IV casuale di 12 byte, che viene pre-posto al ciphertext nel risultato.
     *
     * @param key la chiave AES da usare per cifrare
     * @param plaintext dati in chiaro da cifrare
     * @return dati cifrati con IV pre-posto
     */
    public static byte[] encrypt(SecretKey key, byte[] plaintext)  {
        Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES/GCM/NoPadding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			System.err.println("Errore del cifrario. Algoritmo non esistente? Padding non esistente?");
		    throw new IllegalStateException("Errore del cifrario. Algoritmo non esistente? Padding non esistente?");
		    }
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);

        try {
			cipher.init(Cipher.ENCRYPT_MODE, key, spec);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			System.err.println("Errore del cifrario. Chiave invalida? Parametro invalido per l'algoritmo scelto?");
		    throw new IllegalStateException("Errore del cifrario. Chiave invalida? Parametro invalido per l'algoritmo scelto?");
		}
        byte[] ciphertext = null;
		try {
			ciphertext = cipher.doFinal(plaintext);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			System.err.println("Errore del cifrario. Padding errato? Dimensione del blocco errata?");
			throw new IllegalStateException("Errore del cifrario. Padding errato? Dimensione del blocco errata?");
		}

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
			outputStream.write(iv);
			outputStream.write(ciphertext);
		} catch (IOException e) {
			System.err.println("Errore nella cifratura");
			e.printStackTrace();
		}

        return outputStream.toByteArray();
    }

    /**
     * Decifra dati cifrati con AES-GCM e IV pre-posto (primi 12 byte).
     *
     * @param key la chiave AES da usare per decifrare
     * @param cipherMessage dati cifrati, con IV nei primi 12 byte
     * @return dati decifrati in chiaro, o null se la password è errata o dati corrotti
     */
    public static byte[] decrypt(SecretKey key, byte[] cipherMessage) {
        byte[] iv = new byte[12];
        System.arraycopy(cipherMessage, 0, iv, 0, iv.length);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.err.println("Errore critico: algoritmo o padding non supportati.");
            e.printStackTrace();
            return null;
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.err.println("Errore di inizializzazione del cifrario. Chiave o IV non validi.");
            e.printStackTrace();
            return null;
        }

        try {
            return cipher.doFinal(cipherMessage, iv.length, cipherMessage.length - iv.length);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            
            System.err.println("Master password errata o dati corrotti.");
            return null;
        }
    }


    /**
     * Genera un salt crittograficamente sicuro di 16 byte,
     * usato per la derivazione della chiave e prevenzione rainbow tables.
     *
     * @return un array di byte con il salt generato
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }
}