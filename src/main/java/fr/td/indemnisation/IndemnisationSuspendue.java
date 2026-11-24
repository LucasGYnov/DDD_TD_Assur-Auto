package fr.td.indemnisation;

/** Événement « Indemnisation suspendue » (A12) : plus rien n'est proposé ni accepté jusqu'à la levée. */
public record IndemnisationSuspendue(DossierId dossier) {
}
