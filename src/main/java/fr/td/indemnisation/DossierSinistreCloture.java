package fr.td.indemnisation;

/**
 * Événement du core : « Dossier sinistre clôturé ».
 * Produit par DossierSinistre.cloturer(). La policy LiberationDuGestionnaire y réagit
 * en libérant la place chez son gestionnaire (A15) ; Barèmes et bonus-malus et le suivi
 * de l'assuré peuvent aussi y réagir.
 */
public record DossierSinistreCloture(DossierId dossier, GestionnaireId gestionnaire) {
}
