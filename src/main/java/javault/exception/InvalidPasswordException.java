package javault.exception;

public class InvalidPasswordException extends Exception {
    /**
	 * Eccezione per inserimento di master password errata
	 */
	private static final long serialVersionUID = 1L;

	public InvalidPasswordException(String message) {
        super(message);
    }

    public InvalidPasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}

