package javault.exception;

public class VaultException extends Exception{
	 	/**
		 * Eccezione per problemi con salvataggio o caricamento vault
		 */
		private static final long serialVersionUID = 1L;

		public VaultException(String message) {
	        super(message);
	    }

	    public VaultException(String message, Throwable cause) {
	        super(message, cause);
	    }

}
