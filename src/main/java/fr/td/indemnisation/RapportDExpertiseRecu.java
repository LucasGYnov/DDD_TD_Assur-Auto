package fr.td.indemnisation;

/**
 * Événement « Rapport d'expertise reçu » (A7, A8).
 * contreExpertise vaut vrai quand c'est la contre-expertise demandée après une contestation (A11, H15).
 */
public record RapportDExpertiseRecu(DossierId dossier, ConclusionDExpertise conclusion, boolean contreExpertise) {
}
