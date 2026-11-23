package fr.td.indemnisation;

/**
 * Événement du core : « Dossier sinistre clôturé » (mur, étape 23).
 * Le gestionnaire a une place de libre (A15) ; Barèmes et bonus-malus et le suivi
 * de l'assuré réagissent à la clôture.
 */
public record DossierSinistreCloture(DossierId dossier, GestionnaireId gestionnaire) {
}
