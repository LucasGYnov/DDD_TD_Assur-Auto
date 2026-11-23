package fr.td.indemnisation;

/**
 * Événement du core : « Gestionnaire affecté » (mur, étape 4).
 * Le dossier sinistre a désormais un gestionnaire, et un seul (A15).
 * L'espace en ligne s'en sert pour dire à l'assuré qui suit son dossier (priorité 3).
 */
public record GestionnaireAffecte(DossierId dossier, GestionnaireId gestionnaire) {
}
