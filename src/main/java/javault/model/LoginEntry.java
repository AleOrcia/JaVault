package javault.model;

import java.util.Arrays;

/**
 * Rappresenta una entry di login con username e password in chiaro (char[]),
 * da mantenere in RAM il meno possibile e pulire subito dopo l'uso.
 * Costruisce una nuova entry di login copiando username e password.
 * Si usa una copia per evitare che modifiche esterne agli array originali
 * compromettano l'integrità interna dell'oggetto e per ridurre i rischi
 * legati alla gestione dei dati sensibili in memoria.
 */
public class LoginEntry {
    private char[] username;
    private char[] password;

    /**
     * Costruisce un'istanza di LoginEntry copiando username e password.
     * 
     * @param username array di caratteri contenente lo username
     * @param password array di caratteri contenente la password
     */
    public LoginEntry(char[] username, char[] password) {
        this.username = Arrays.copyOf(username, username.length);
        this.password = Arrays.copyOf(password, password.length);
    }

    /**
     * Evita di esporre il riferimento originale grazie ad una copia
     * per evitare perdite o manipolazioni di dati.
     * @return char[] username
     */
    public char[] getUsername() {
        return Arrays.copyOf(username, username.length);
    }
    
    /**
     * Evita di esporre il riferimento originale grazie ad una copia
     * * per evitare perdite o manipolazioni di dati.
     * @return char[] password
     */
    public char[] getPassword() {
        return Arrays.copyOf(password, password.length);
    }

    /**
     * Pulisce username e password dalla memoria sovrascrivendo gli array.
     */
    public void clear() {
        if (username != null) {
            Arrays.fill(username, '\0');
            username = null;
        }
        if (password != null) {
            Arrays.fill(password, '\0');
            password = null;
        }
    }
}
