package fr.td.indemnisation.expertis;

/**
 * Un rapport Expertis qu'on ne sait pas traduire : code inconnu, montant illisible,
 * ou code « VEI » qui contredit les chiffres (A8). Rien ne passe en silence :
 * le rapport n'est pas appliqué au dossier sinistre et le gestionnaire est alerté (H18).
 */
public class RapportExpertisIntraduisible extends RuntimeException {

    public RapportExpertisIntraduisible(String message) {
        super(message);
    }
}
