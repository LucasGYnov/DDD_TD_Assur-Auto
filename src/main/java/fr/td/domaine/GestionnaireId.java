package fr.td.domaine;

/** L'identifiant d'un gestionnaire sinistres. */
public record GestionnaireId(String valeur) {

    public GestionnaireId {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("un gestionnaire a un identifiant");
        }
    }

    @Override
    public String toString() {
        return valeur;
    }
}
