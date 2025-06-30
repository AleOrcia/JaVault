package javault.controller;

import java.io.IOException;
import java.util.Arrays;

import javax.crypto.SecretKey;

import javault.exception.InvalidPasswordException;
import javault.exception.UnknownErrorException;
import javault.exception.VaultException;
import javault.model.LoginEntry;
import javault.model.PasswordGenerator;
import javault.model.VaultData;
import javault.utils.EncryptionUtils;
import javault.utils.VaultUtils;

/**
 * Controller principale per la gestione del vault sicuro di JaVault.
 * Si occupa dell'apertura, modifica, lettura e salvataggio delle credenziali utente.
 */
public class JaVaultController {
    private SecretKey key;
    private VaultData vault;

    /**
     * Apre il vault usando la master password fornita.
     * Se il vault è già in uso da un altro processo, lancia eccezione.
     *
     * @param masterPassword la password principale per derivare la chiave di cifratura
     * @throws InvalidPasswordException se la master password è errata
     * @throws IllegalStateException se il vault è già bloccato da un'altra istanza
     * @throws UnknownErrorException per altri errori imprevisti
     */
    public void openVault(char[] masterPassword) throws InvalidPasswordException, UnknownErrorException {
        try {
            VaultUtils.acquireVaultLock();
            this.key = EncryptionUtils.getKeyFromPassword(masterPassword);
            Arrays.fill(masterPassword, '\0');
            this.vault = VaultUtils.loadVault(key);
        } catch (IOException e) {
            System.err.println("Un'altra istanza di JaVault è già in esecuzione");
            throw new IllegalStateException("Un'altra istanza di JaVault è già in esecuzione "+e);
        } catch (InvalidPasswordException e) {
			System.err.println("Master password errata, controller");
			throw new InvalidPasswordException("Master password errata");
		}
    }
    
    /**
     * Cancella i dati dalla memoria RAM.
     * Rilascia il lock sul vault.
     * In caso di errore, stampa lo stacktrace ma non rilancia eccezioni.
     */
    public void closeVault() {
    	
    	if(this.vault != null) vault.clearAll();
    	
    	if(this.key != null) this.key = null; //aiuta il GC anche se non azzera

    	
    	try {
			VaultUtils.releaseVaultLock();
		} catch (VaultException e) {
			System.err.println("Impossibile rilasciare il lock");
			e.printStackTrace();
		}
    }

    /**
     * Genera una nuova password e la salva nel vault associata a un login.
     *
     * @param name     nome identificativo del login
     * @param userEmail username/email associata
     * @param length   lunghezza della password (da 1 a 64)
     * @param lower    true per includere lettere minuscole
     * @param upper    true per includere lettere maiuscole
     * @param digits   true per includere cifre
     * @param special  true per includere caratteri speciali
     * @return messaggio di esito (successo o errore), ad esempio "Login già esistente." o "Devi selezionare almeno un tipo di carattere."
     */
    public String generateAndSaveLogin(String name, String userEmail, int length,
                                       boolean lower, boolean upper, boolean digits, boolean special) {
        char[] passwordArray = null;
        char[] usernameArray = null;

        try {
            if (vault.hasLogin(name)) {
                return "Login già esistente.";
            }

            if (!lower && !upper && !digits && !special) {
                return "Devi selezionare almeno un tipo di carattere.";
            }

            if (length < 1 || length > 64) {
                return "La lunghezza deve essere tra 1 e 64.";
            }

            PasswordGenerator generator = new PasswordGenerator(lower, upper, digits, special);
            passwordArray = generator.generatePassword(length);
            usernameArray = userEmail.toCharArray();

            vault.addLogin(name, usernameArray, passwordArray);
            VaultUtils.saveVault(vault, key);

            return "Login generato:\n";
        } catch (VaultException e) {
            e.printStackTrace();
            return "Errore nella generazione o salvataggio.";
        } finally {
            if (passwordArray != null) Arrays.fill(passwordArray, '\0');
            if (usernameArray != null) Arrays.fill(usernameArray, '\0');
        }
    }

    /**
     * Restituisce i dati di un login come array di caratteri formattato con nome, username e password.
     *
     * @param name nome del login da cercare
     * @return array di caratteri con le informazioni del login formattate, oppure messaggio di errore "Login non trovato."
     */
    public char[] printLogin(String name) {
        if (!vault.hasLogin(name)) {
            return "Login non trovato.".toCharArray();
        }

        LoginEntry entry = vault.getLogin(name);
        char[] usernameCopy = entry.getUsername();
        char[] passwordCopy = entry.getPassword();
        char[] result;

        try {
            char[] label1 = "Nome login: ".toCharArray();
            char[] label2 = "\nUsername: ".toCharArray();
            char[] label3 = "\nPassword: ".toCharArray();
            char[] newline = "\n".toCharArray();

            int totalLength = label1.length + name.length() +
                              label2.length + usernameCopy.length +
                              label3.length + passwordCopy.length +
                              newline.length;

            result = new char[totalLength];
            int pos = 0;

            System.arraycopy(label1, 0, result, pos, label1.length); pos += label1.length;
            name.getChars(0, name.length(), result, pos); pos += name.length();
            System.arraycopy(label2, 0, result, pos, label2.length); pos += label2.length;
            System.arraycopy(usernameCopy, 0, result, pos, usernameCopy.length); pos += usernameCopy.length;
            System.arraycopy(label3, 0, result, pos, label3.length); pos += label3.length;
            System.arraycopy(passwordCopy, 0, result, pos, passwordCopy.length); pos += passwordCopy.length;
            System.arraycopy(newline, 0, result, pos, newline.length);

            return result;

        } finally {
            Arrays.fill(usernameCopy, '\0');
            Arrays.fill(passwordCopy, '\0');
        }
    }

    /**
     * Rimuove un login dal vault e salva le modifiche.
     *
     * @param name nome del login da rimuovere
     * @throws IllegalStateException se il salvataggio fallisce
     */
    public void removeLogin(String name) {
        if (vault.hasLogin(name)) {
            vault.removeLogin(name);
            try {
                VaultUtils.saveVault(vault, key);
            } catch (VaultException e) {
                System.err.println("Impossibile salvare il vault");
                throw new IllegalStateException("Impossibile salvare il vault "+ e);
            }
        }
    }

    /**
     * Restituisce l'oggetto Vault attualmente in uso.
     *
     * @return il VaultData corrente
     */
    public VaultData getVault() {
        return vault;
    }

    /**
     * Salva manualmente lo stato attuale del vault.
     *
     * @throws IllegalStateException se il salvataggio fallisce
     */
    public void saveVault() {
        try {
            VaultUtils.saveVault(vault, key);
        } catch (VaultException e) {
            System.err.println("Impossibile salvare il vault");
            throw new IllegalStateException();
        }
    }

    /**
     * Verifica se una password è considerata "forte".
     * Richiede almeno 10 caratteri e la presenza di: minuscola, maiuscola, cifra e simbolo.
     *
     * @param password array di caratteri da valutare
     * @return true se la password è forte, false altrimenti
     */
    public boolean isStrongPassword(char[] password) {
        if (password.length < 10) return false;

        boolean hasLower = false, hasUpper = false, hasDigit = false, hasSpecial = false;

        for (char c : password) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if ("!@#$%&*_+-=/?".indexOf(c) >= 0) hasSpecial = true;

            if (hasLower && hasUpper && hasDigit && hasSpecial) break;
        }

        return hasLower && hasUpper && hasDigit && hasSpecial;
    }
}
