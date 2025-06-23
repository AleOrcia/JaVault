package javault.exception;

/**
 * Eccezione generica per errori imprevisti.
 * Viene usata per chiudere l'applicazione con un messaggio predefinito.
 */
public class UnknownErrorException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnknownErrorException() {
        super("Errore sconosciuto. Chiusura applicazione");
    }

    public UnknownErrorException(Throwable cause) {
        super("Errore sconosciuto. Chiusura applicazione", cause);
    }
}
