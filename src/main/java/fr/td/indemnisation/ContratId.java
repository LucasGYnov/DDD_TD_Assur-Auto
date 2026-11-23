package fr.td.indemnisation;

/**
 * L'identifiant d'un contrat, tel que ContratPlus le connaît.
 * Même valeur que fr.td.baremes.ContratId : les deux contextes partagent l'identifiant, pas l'objet.
 */
public record ContratId(String valeur) {

    public ContratId {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("un contrat a un identifiant");
        }
    }

    @Override
    public String toString() {
        return valeur;
    }
}
