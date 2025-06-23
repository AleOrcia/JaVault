package javault.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Gestisce un archivio di credenziali tramite
 * una mappa nome login - LoginEntry con username e password.
 */
public class VaultData {

    private final Map<String, LoginEntry> passwordMap = new HashMap<>();

    /**
     * Aggiunge una nuova credenziale al vault.
     * Dopo l'aggiunta, pulisce i char array username e password per sicurezza,
     * sovrascrivendo i dati sensibili in RAM.
     *
     * @param name nome identificativo del login
     * @param username username come char array
     * @param password password come char array
     */
    public void addLogin(String name, char[] username, char[] password) {
    	
        passwordMap.put(name, new LoginEntry(username, password));
        Arrays.fill(username, '\0'); // Pulizia dati sensibili dalla RAM
        Arrays.fill(password, '\0'); // Pulizia dati sensibili dalla RAM
    }

    /**
     * Recupera la LoginEntry associata al nome login specificato.
     *
     * @param name nome identificativo del login
     * @return la LoginEntry corrispondente, o null se non presente
     */
    public LoginEntry getLogin(String name) {
        return passwordMap.get(name);
    }

    /**
     * Verifica se esiste un login con il nome specificato.
     *
     * @param name nome identificativo del login
     * @return true se il login esiste, false altrimenti
     */
    public boolean hasLogin(String name) {
        return passwordMap.containsKey(name);
    }

    /**
     * Rimuove il login dal vault e pulisce i dati sensibili associati.
     *
     * @param name nome del login da rimuovere
     */
    public void removeLogin(String name) {
        if (passwordMap.containsKey(name)) {
            passwordMap.get(name).clear();
            passwordMap.remove(name);
        }
    }

    /**
     * Restituisce l'insieme di tutti i nomi login salvati.
     *
     * @return set di stringhe con tutti i nomi login
     */
    public Set<String> getAllLogins() {
        return passwordMap.keySet();
    }

    /**
     * Pulisce tutte le credenziali dalla memoria, sovrascrivendo i dati sensibili.
     */
    public void clearAll() {
        for (LoginEntry entry : passwordMap.values()) {
            entry.clear();
        }
        passwordMap.clear();
    }
}
