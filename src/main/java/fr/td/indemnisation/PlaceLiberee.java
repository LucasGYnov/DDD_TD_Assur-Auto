package fr.td.indemnisation;

/**
 * Événement « Place libérée » : le gestionnaire ne suit plus ce dossier sinistre clôturé,
 * il peut en prendre un autre (A15). Produit par la policy LiberationDuGestionnaire.
 */
public record PlaceLiberee(GestionnaireId gestionnaire, DossierId dossier, int dossiersOuverts) {
}
