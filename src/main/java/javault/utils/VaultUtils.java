package javault.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javault.exception.InvalidPasswordException;
import javault.exception.UnknownErrorException;
import javault.exception.VaultException;
import javault.model.VaultData;
import javax.crypto.SecretKey;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utility per la gestione sicura del file vault:
 * include salvataggio, caricamento e locking per prevenire accessi concorrenti.
 */
public class VaultUtils {

    private static final String VAULT_PATH = "vault/vault.bin";
    private static final String LOCK_PATH = "vault/vault.lock";

    private static final Gson gson = new GsonBuilder().create();
    private static FileChannel lockChannel;
    private static FileLock vaultLock;

    /**
     * Acquisisce un lock sul file del vault per evitare accessi concorrenti
     * da più istanze del programma (prevenzione race condition).
     *
     * @throws IOException se il lock è già stato acquisito da un altro processo
     */
    public static void acquireVaultLock() throws IOException {
        Files.createDirectories(Path.of("vault"));

        lockChannel = FileChannel.open(Path.of(LOCK_PATH),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);

        vaultLock = lockChannel.tryLock();
        if (vaultLock == null) {
            throw new IOException("Vault già aperto da un'altra istanza.");
        }
    }

    /**
     * Rilascia il lock precedentemente acquisito sul vault.
     * Cancella il file di lock se tutto va a buon fine.
     *
     * @throws VaultException se il lock non può essere rilasciato correttamente
     */
    public static void releaseVaultLock() throws VaultException{
        try {
            if (vaultLock != null && vaultLock.isValid()) {
                vaultLock.release();
            }
            if (lockChannel != null && lockChannel.isOpen()) {
                lockChannel.close();
            }
            Files.deleteIfExists(Path.of(LOCK_PATH));
        } catch (IOException e) {
            System.err.println("Errore durante il rilascio del lock: " + e.getMessage());
            throw new VaultException("Errore durante il rilascio del lock");
        }
    }

    /**
     * Salva il contenuto cifrato del vault su disco.
     *
     * @param vault i dati del vault da salvare
     * @param key la chiave AES utilizzata per cifrare il contenuto
     * @throws VaultException se si verifica un errore durante il salvataggio
     */
    public static void saveVault(VaultData vault, SecretKey key) throws VaultException {
        String json = gson.toJson(vault);
        if(json == null) throw new VaultException("Impossibile ottenere il file vault");
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedVault = EncryptionUtils.encrypt(key, jsonBytes);

        try {
			Files.createDirectories(Path.of("vault"));
			Files.write(Path.of(VAULT_PATH), encryptedVault);
		} catch (IOException e) {
			throw new VaultException("Impossibile salvare il vault");
		}
        
    }

    /**
     * Carica il vault da disco e ne decifra il contenuto.
     *
     * @param key la chiave AES utilizzata per decifrare il contenuto
     * @return un oggetto {@link VaultData} con i dati decifrati,
     *         oppure un nuovo {@link VaultData} se il file non esiste
     * @throws InvalidPasswordException se la master password è errata o il vault è corrotto
     * @throws UnknownErrorException se si verifica un errore sconosciuto durante il parsing
     */
    public static VaultData loadVault(SecretKey key) throws InvalidPasswordException, UnknownErrorException{
        File vaultFile = new File(VAULT_PATH);
        if (!vaultFile.exists()) {
            return new VaultData();
        }

        byte[] encryptedVault = null;
        try {
            encryptedVault = Files.readAllBytes(vaultFile.toPath());
        } catch (IOException e) {
            System.err.println("Errore durante la lettura del vault: " + e.getMessage());
        }

        byte[] decryptedJson = EncryptionUtils.decrypt(key, encryptedVault);
        if (decryptedJson == null) {
            throw new InvalidPasswordException("Master password errata, VaultUtils"); // Master password errata o vault corrotto
        }

        try (InputStreamReader reader = new InputStreamReader(
                new ByteArrayInputStream(decryptedJson), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, VaultData.class);
        } catch (IOException e) {
            System.err.println("Errore durante il parsing del vault: " + e.getMessage());
        }

        throw new UnknownErrorException();
    }
}