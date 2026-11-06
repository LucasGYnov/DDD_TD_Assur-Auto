package fr.td.domaine;

/** L'identifiant d'un dossier sinistre. */
public record DossierId(String valeur) {

    public DossierId {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("un dossier sinistre a un identifiant");
        }
    }

    @Override
    public String toString() {
        return valeur;
    }
}
