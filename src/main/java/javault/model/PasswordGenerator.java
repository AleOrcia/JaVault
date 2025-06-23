package javault.model;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;


/**
 * Generatore di password sicure con possibilità di includere lettere minuscole,
 * maiuscole, cifre e caratteri speciali.
 */
public final class PasswordGenerator {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%&*_+-=/?";

    private final boolean useLowercase;
    private final boolean useUppercase;
    private final boolean useDigits;
    private final boolean useSpecial;

    /**
     * Costruttore che definisce quali categorie di caratteri usare nella password.
     *
     * @param useLowercase se includere lettere minuscole
     * @param useUppercase se includere lettere maiuscole
     * @param useDigits se includere cifre
     * @param useSpecial se includere caratteri speciali
     */
    public PasswordGenerator(boolean useLowercase, boolean useUppercase, boolean useDigits, boolean useSpecial) {
        this.useLowercase = useLowercase;
        this.useUppercase = useUppercase;
        this.useDigits = useDigits;
        this.useSpecial = useSpecial;
    }

    /**
     * Genera una password casuale della lunghezza specificata.
     *
     * @param length lunghezza della password da generare, deve essere >= numero di categorie abilitate
     * @return array di char con la password generata
     * @throws IllegalStateException se nessuna categoria di caratteri è stata abilitata
     * @throws IllegalArgumentException se la lunghezza è minore del numero di categorie abilitate
     */
    public char[] generatePassword(int length) {
    	 if (length <= 0) {
             return new char[0];
         }
    	 
    	 
         // Categorie disponibili
         List<String> charCategories = new ArrayList<>();
         if (useLowercase) charCategories.add(LOWERCASE);
         if (useUppercase) charCategories.add(UPPERCASE);
         if (useDigits) charCategories.add(DIGITS);
         if (useSpecial) charCategories.add(SPECIAL);

         if (charCategories.isEmpty()) {
             throw new IllegalStateException("Devi abilitare almeno una categoria di caratteri.");
         }
         if (length < charCategories.size()) {
             throw new IllegalArgumentException("La lunghezza deve essere almeno pari al numero di categorie abilitate.");
         }

        char[] password = new char[length];
        SecureRandom random = new SecureRandom();


        // Generazione password
        for (int i = 0; i < length; i++) {
            String charCategory = charCategories.get(random.nextInt(charCategories.size()));
            int position = random.nextInt(charCategory.length());
            password[i] = charCategory.charAt(position); 
        }

        return password; 
    }
}
