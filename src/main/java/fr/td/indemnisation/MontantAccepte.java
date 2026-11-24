package fr.td.indemnisation;

import java.time.LocalDate;

/**
 * Événement « Montant accepté ».
 * Il porte la date limite de versement : 30 jours après l'accord de l'assuré (A10).
 * Le Règlement s'en sert pour verser l'indemnité.
 */
public record MontantAccepte(DossierId dossier, Montant montant, LocalDate jour, LocalDate verseAuPlusTardLe) {
}
