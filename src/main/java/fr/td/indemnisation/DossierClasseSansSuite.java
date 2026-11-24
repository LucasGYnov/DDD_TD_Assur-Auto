package fr.td.indemnisation;

/**
 * Événement « Dossier classé sans suite » (A18) : doublon ou déclaration retirée.
 * Il porte le contrat : Vol et fraude et Barèmes l'excluent de leurs comptes.
 */
public record DossierClasseSansSuite(DossierId dossier, ContratId contrat) {
}
