package fr.td.indemnisation;

import java.time.LocalDate;

/**
 * Événement « Montant proposé ».
 * definitif vaut vrai pour la proposition qui suit une contestation : elle ne peut plus être contestée (A11, H15).
 */
public record MontantPropose(DossierId dossier, Montant montant, LocalDate jour, boolean definitif) {
}
