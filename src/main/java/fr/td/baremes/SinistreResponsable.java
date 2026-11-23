package fr.td.baremes;

import java.util.Optional;

/**
 * Value object du contexte Barèmes et bonus-malus : un sinistre où l'assuré est responsable
 * à 50 % ou à 100 %, retenu pour le recalcul du coefficient (A13, H3).
 *
 * Barèmes ne réutilise pas la Responsabilite de l'Indemnisation : il n'en sait que ce qui
 * compte pour le coefficient. Un sinistre non responsable (0 %) n'est pas un sinistre responsable.
 */
public record SinistreResponsable(int pourcentage) {

    public static final SinistreResponsable A_MOITIE = new SinistreResponsable(50);
    public static final SinistreResponsable TOTALEMENT = new SinistreResponsable(100);

    public SinistreResponsable {
        if (pourcentage != 50 && pourcentage != 100) {
            throw new IllegalArgumentException("A13, un sinistre responsable l'est à 50 ou 100 %, pas " + pourcentage + " %");
        }
    }

    /**
     * Ce que Barèmes retient d'une responsabilité fixée par l'Indemnisation :
     * rien à 0 %, un sinistre responsable à 50 ou 100 %. Toute autre part est refusée (A4).
     */
    public static Optional<SinistreResponsable> retenu(int pourcentageDeResponsabilite) {
        if (pourcentageDeResponsabilite == 0) {
            return Optional.empty();
        }
        return Optional.of(new SinistreResponsable(pourcentageDeResponsabilite));
    }

    public boolean estAMoitie() {
        return pourcentage == 50;
    }

    @Override
    public String toString() {
        return "sinistre responsable à " + pourcentage + " %";
    }
}
