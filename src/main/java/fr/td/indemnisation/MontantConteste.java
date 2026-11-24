package fr.td.indemnisation;

import java.time.LocalDate;

/** Événement « Montant contesté » (A11) : une seule fois, avec une contre-expertise possible, aux frais de l'assuré. */
public record MontantConteste(DossierId dossier, Montant montantConteste, LocalDate jour) {
}
