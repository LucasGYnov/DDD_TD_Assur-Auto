package fr.td.indemnisation;

/** Événement « Déclaration refusée pour retard » (A2) : décidé par le gestionnaire du dossier, jamais d'office. */
public record DeclarationRefuseePourRetard(DossierId dossier, GestionnaireId gestionnaire) {
}
