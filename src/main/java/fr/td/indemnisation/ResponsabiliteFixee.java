package fr.td.indemnisation;

import java.time.LocalDate;

/**
 * Événement « Responsabilité fixée » (A4).
 * Il porte le contrat et la date du sinistre : Barèmes et bonus-malus en a besoin pour retenir
 * le sinistre à la prochaine échéance annuelle (A13, A14, H3).
 */
public record ResponsabiliteFixee(DossierId dossier, ContratId contrat, LocalDate dateDuSinistre,
                                  Responsabilite responsabilite, boolean tiersIdentifie) {
}
