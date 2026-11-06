package fr.td.domaine;

/** L'exception levée quand une règle du dossier client refuse une action. Elle porte le code de la règle (« A15 »…). */
public class RegleRefusee extends IllegalStateException {

    private final String code;

    public RegleRefusee(String code, String message) {
        super("[" + code + "] " + message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
