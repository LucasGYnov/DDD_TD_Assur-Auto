package fr.td.domaine;

/** Où en est un dossier sinistre. L'assuré, l'agence et le gestionnaire voient le même état (priorité 3). */
public enum EtatDossier {
    OUVERT, MONTANT_PROPOSE, MONTANT_ACCEPTE, MONTANT_CONTESTE, INDEMNISE, REFUSE, CLASSE_SANS_SUITE
}
