package fr.td.indemnisation;

/**
 * Événement « Dommages estimés » (A7).
 * expertiseObligatoire dit à la policy s'il faut missionner un expert (plus de 1 500 €, H6).
 */
public record DommagesEstimes(DossierId dossier, Montant estimation, boolean expertiseObligatoire) {
}
