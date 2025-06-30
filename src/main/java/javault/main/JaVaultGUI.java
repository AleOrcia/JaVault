package javault.main;

import java.awt.*;
import java.util.Set;
import javax.swing.*;
import javault.controller.JaVaultController;
import javault.exception.InvalidPasswordException;
import javault.exception.UnknownErrorException;
import javault.model.VaultData;
import java.util.Arrays;

public class JaVaultGUI {
    private JFrame frame;
    private JTextField accountField;
    private JTextField usernameField;
    private JTextField lengthField;
    private JCheckBox lowerCaseCheck;
    private JCheckBox upperCaseCheck;
    private JCheckBox digitsCheck;
    private JCheckBox specialCheck;
    private JButton generateButton;
    private JTextArea outputArea;
    private JaVaultController controller;

    /**
     * Costruisce e inizializza l'interfaccia grafica dell'applicazione.
     * Richiede all'utente l'inserimento di una master password sicura prima di procedere.
     *
     * @param controller Il controller principale che gestisce la logica dell'applicazione.
     */
    public JaVaultGUI(JaVaultController controller) {
        this.controller = controller;
        askMasterPassword();
        initUI();  
    }

    /**
     * Richiede la master password all'utente tramite un dialogo.
     * Verifica che la password sia valida, sicura e corretta prima di sbloccare il vault.
     * In caso di errore critico, chiude l'applicazione.
     */
    private void askMasterPassword() {
        while (true) {
            JPasswordField pwdField = new JPasswordField();
            int option = JOptionPane.showConfirmDialog(
                    frame,
                    pwdField,
                    "Inserisci la master password:",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (option != JOptionPane.OK_OPTION) {
                int choice = JOptionPane.showConfirmDialog(frame, "Vuoi uscire dall'app?", "Conferma uscita", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    controller.closeVault();
                    System.exit(0);
                }
                continue;
            }

            char[] password = null;
            try {
                password = pwdField.getPassword();

                if (password.length == 0 || isAllBlank(password)) {
                    JOptionPane.showMessageDialog(frame, "Inserisci una master password valida.", "Errore", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                if (!controller.isStrongPassword(password)) {
                    JOptionPane.showMessageDialog(frame, "La Master password è sbagliata o non abbastanza sicura. Deve contenere almeno un carattere minuscolo, uno maiuscolo, un numero, un carattere speciale (!@#$%&*_+-=/?) e deve essere lunga almeno dieci caratteri", "Errore", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                try {
                    controller.openVault(password);
                    controller.saveVault();
                    break;  // se va bene esci dal loop
                } catch (InvalidPasswordException e) {
                    JOptionPane.showMessageDialog(frame, "Master password errata", "Errore", JOptionPane.ERROR_MESSAGE);
                    controller.closeVault();
                    continue;
                } catch (UnknownErrorException e) {
                    JOptionPane.showMessageDialog(frame, "Errore critico", "Errore sconosciuto", JOptionPane.ERROR_MESSAGE);
                    controller.closeVault();
                    System.exit(1);
                }
            } finally {
                if (password != null) {
                    Arrays.fill(password, '\0'); // Pulizia garantita
                }
            }

        }
    }

    /**
     * Inizializza la finestra principale e mostra la dashboard iniziale.
     * Imposta anche il comportamento alla chiusura della finestra.
     */
    private void initUI() {
        frame = new JFrame("JaVault - Password Manager");
        frame.setSize(600, 450);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                controller.closeVault();
                System.exit(0);
            }
        });

        frame.setLayout(new BorderLayout());
        frame.setVisible(true);
        showDashboard();
    }

    /**
     * Mostra la dashboard principale con l'elenco dei login salvati.
     * Fornisce opzioni per visualizzare, eliminare o creare nuove voci.
     */
    private void showDashboard() {
        frame.getContentPane().removeAll();

        // Pannello che contiene tutte le righe
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Set<String> logins = controller.getVault().getAllLogins();
        if (logins.isEmpty()) {
            listPanel.add(new JLabel("Nessun login salvato."));
        } else {
        	for (String login : logins) {
        	    JPanel row = new JPanel(new GridBagLayout());
        	    row.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        	    GridBagConstraints gbc = new GridBagConstraints();

        	    // Nome login 
        	    JLabel loginLabel = new JLabel(login);
        	    loginLabel.setFont(new Font("Arial", Font.PLAIN, 16));  // Aumenta la dimensione del testo
        	    gbc.gridx = 0;
        	    gbc.gridy = 0;
        	    gbc.weightx = 1.0;
        	    gbc.fill = GridBagConstraints.HORIZONTAL;
        	    gbc.anchor = GridBagConstraints.WEST;
        	    row.add(loginLabel, gbc);

        	    // Bottoni
        	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        	    JButton viewButton = new JButton("Visualizza");
        	    viewButton.setPreferredSize(new Dimension(100, 25));
        	    viewButton.addActionListener(_ -> showLoginDetails(login));
        	    buttonPanel.add(viewButton);

        	    JButton deleteButton = new JButton("Elimina");
        	    deleteButton.setPreferredSize(new Dimension(100, 25));
        	    deleteButton.addActionListener(_ -> deleteLoginFromDashboard(login));
        	    buttonPanel.add(deleteButton);

        	    gbc.gridx = 1;
        	    gbc.gridy = 0;
        	    gbc.weightx = 0;
        	    gbc.anchor = GridBagConstraints.EAST;
        	    gbc.fill = GridBagConstraints.NONE;
        	    row.add(buttonPanel, gbc);

        	    listPanel.add(row);
        	}

        }

        // Wrapper per evitare che listPanel non si allarghi in verticale
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(listPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        // Bottoni sotto
        JButton newLoginButton = new JButton("➕ Nuovo Login");
        newLoginButton.addActionListener(_ -> showCreateLoginView());

        JButton clearAllButton = new JButton("⚠️ Cancella Tutto");
        clearAllButton.setForeground(Color.WHITE);
        clearAllButton.setBackground(Color.RED);
        clearAllButton.setFocusPainted(false);
        clearAllButton.setPreferredSize(new Dimension(150, 30));

        clearAllButton.addActionListener(_ -> {
            int result = JOptionPane.showConfirmDialog(
                frame,
                "Sei sicuro di voler eliminare TUTTI i login?\nQuesta azione è irreversibile.",
                "Conferma Eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
            	try {
                    controller.getVault().clearAll();
                    controller.saveVault();
                    JOptionPane.showMessageDialog(frame, "Tutti i login sono stati eliminati.", "Fatto", JOptionPane.INFORMATION_MESSAGE);
                    showDashboard();
                } catch (IllegalStateException e) {
                    // Il salvataggio è fallito
                    JOptionPane.showMessageDialog(frame, 
                        "Errore critico: impossibile salvare il vault. L'app verrà chiusa.", 
                        "Errore", JOptionPane.ERROR_MESSAGE);
                    controller.closeVault(); // Azione di pulizia
                    System.exit(1); // Chiusura controllata
                }
            }
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(newLoginButton, BorderLayout.WEST);
        bottomPanel.add(clearAllButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();
    }


    /**
     * Visualizza i dettagli completi del login specificato.
     *
     * @param name Il nome del login da visualizzare.
     */
    private void showLoginDetails(String name) {
    	
        char[] login = controller.printLogin(name);
        JTextArea detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setLineWrap(false); 
        detailArea.setWrapStyleWord(false);

        
        for(char c : login) {
        	detailArea.append(String.valueOf(c));
        }
      
        
        JScrollPane scrollPane = new JScrollPane(detailArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JOptionPane.showMessageDialog(frame, scrollPane, "Dettagli login", JOptionPane.INFORMATION_MESSAGE);
        Arrays.fill(login, '\0'); // Pulizia array
    }

    /**
     * Mostra l'interfaccia per creare un nuovo login.
     * Permette di impostare criteri personalizzati per la generazione della password.
     */
    private void showCreateLoginView() {
        frame.getContentPane().removeAll();

        JPanel inputPanel = new JPanel(new GridLayout(9, 2, 5, 5));

        accountField = new JTextField();
        usernameField = new JTextField();
        lengthField = new JTextField("16");

        lowerCaseCheck = new JCheckBox("Lowercase (a-z)", true);
        upperCaseCheck = new JCheckBox("Uppercase (A-Z)", true);
        digitsCheck = new JCheckBox("Digits (0-9)", true);
        specialCheck = new JCheckBox("Special (!@#$%&*_+-=/?)", true);

        inputPanel.add(new JLabel("Nome Login:"));
        inputPanel.add(accountField);
        inputPanel.add(new JLabel("Email/Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(new JLabel("Lunghezza Password (1-64):"));
        inputPanel.add(lengthField);
        inputPanel.add(lowerCaseCheck);
        inputPanel.add(upperCaseCheck);
        inputPanel.add(digitsCheck);
        inputPanel.add(specialCheck);

        generateButton = new JButton("Genera e Salva");
        JButton backButton = new JButton("⬅ Torna alla Dashboard");

        generateButton.addActionListener(_ -> {
            generateAndSavePassword();
            showDashboard();
        });

        backButton.addActionListener(_ -> showDashboard());

        inputPanel.add(generateButton);
        inputPanel.add(backButton);

        frame.add(inputPanel, BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        frame.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        frame.revalidate();
        frame.repaint();
    }

    /**
     * Genera una password secondo i criteri selezionati e salva il nuovo login nel vault.
     * Mostra il risultato nell'area di output.
     */
    private void generateAndSavePassword() {
        String accountName = accountField.getText().trim();
        
        if (controller.getVault().hasLogin(accountName)) {
            JOptionPane.showMessageDialog(frame, "Login già esistente con questo nome", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String username = usernameField.getText().trim();
        int length;

        try {
            length = Integer.parseInt(lengthField.getText().trim());
            if (length < 1 || length > 64) {
                throw new NumberFormatException("Lunghezza fuori range");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Inserisci una lunghezza valida (1-64)", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (accountName.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Account e Username sono obbligatori", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean useLower = lowerCaseCheck.isSelected();
        boolean useUpper = upperCaseCheck.isSelected();
        boolean useDigits = digitsCheck.isSelected();
        boolean useSpecial = specialCheck.isSelected();        
        
        String check = controller.generateAndSaveLogin(accountName, username, length, useLower, useUpper, useDigits, useSpecial);

        
        outputArea.setText(check + "\n");

        char[] loginText = controller.printLogin(accountName); //Modo più sicuro per evitare che i dati vengano letti dalla RAM
        for (char c : loginText) {
            outputArea.append(String.valueOf(c));
        }
        Arrays.fill(loginText, '\0'); //Pulizia dati sensibili dalla RAM
    }

    /**
     * Verifica se tutti i caratteri in un array sono spazi bianchi.
     *
     * @param password Array di caratteri da controllare.
     * @return true se tutti i caratteri sono spazi bianchi, false altrimenti.
     */
    private boolean isAllBlank(char[] password) {
        for (char c : password) {
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Elimina il login specificato dopo conferma dell'utente.
     * Salva il vault aggiornato e ricarica la dashboard.
     *
     * @param loginName Il nome del login da eliminare.
     */
    private void deleteLoginFromDashboard(String loginName) {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Vuoi davvero eliminare il login \"" + loginName + "\"?",
                "Conferma Eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            VaultData vault = controller.getVault();
            if (!vault.hasLogin(loginName)) {
                JOptionPane.showMessageDialog(frame, "Login non trovato.", "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }

            vault.removeLogin(loginName);
            try {
                controller.saveVault();
            } catch (IllegalStateException e) {
                // Il salvataggio è fallito
                JOptionPane.showMessageDialog(frame, 
                    "Errore critico: impossibile salvare il vault. L'app verrà chiusa.", 
                    "Errore", JOptionPane.ERROR_MESSAGE);
                controller.closeVault(); //Azione di pulizia
                System.exit(1); //Chiusura controllata
            }

            JOptionPane.showMessageDialog(frame, "Login eliminato con successo.", "Successo", JOptionPane.INFORMATION_MESSAGE);
            showDashboard();
        }
    }

    /**
     * Avvia l'applicazione JaVault inizializzando il controller e l'interfaccia grafica.
     *
     * @param args Argomenti da linea di comando (non utilizzati).
     */
    public static void main(String[] args) {
        JaVaultController controller = new JaVaultController();
        SwingUtilities.invokeLater(() -> {
            new JaVaultGUI(controller);
        });
    }
}
