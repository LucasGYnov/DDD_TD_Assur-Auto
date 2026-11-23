package fr.td.baremes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Value object du contexte Barèmes et bonus-malus, le coefficient de bonus-malus d'un contrat.
 *
 * A13, x 1,25 par sinistre responsable (x 1,125 à 50 %), x 0,95 par année sans sinistre
 * responsable, toujours entre 0,50 et 3,50.
 * A14, il ne se recalcule qu'à l'échéance annuelle. Il n'existe donc volontairement aucune
 * méthode « après un sinistre », la seule façon de le faire évoluer est recalculerALEcheance.
 * Le coefficient est arrondi à 2 décimales (au plus proche), une fois le calcul de
 * l'échéance terminé.
 */
public record CoefficientBonusMalus(BigDecimal valeur) {

    public static final BigDecimal MINIMUM = new BigDecimal("0.50");
    public static final BigDecimal MAXIMUM = new BigDecimal("3.50");

    private static final BigDecimal PAR_SINISTRE_RESPONSABLE = new BigDecimal("1.25");
    private static final BigDecimal PAR_SINISTRE_A_MOITIE = new BigDecimal("1.125");
    private static final BigDecimal PAR_ANNEE_SANS_SINISTRE_RESPONSABLE = new BigDecimal("0.95");

    public CoefficientBonusMalus {
        valeur = valeur.setScale(2, RoundingMode.HALF_UP);
        if (valeur.compareTo(MINIMUM) < 0 || valeur.compareTo(MAXIMUM) > 0) {
            throw new IllegalArgumentException("A13, le coefficient reste entre 0,50 et 3,50, pas " + valeur);
        }
    }

    public static CoefficientBonusMalus de(String valeur) {
        return new CoefficientBonusMalus(new BigDecimal(valeur));
    }

    public static CoefficientBonusMalus initial() {
        return de("1.00");
    }

    /**
     * Recalcul à l'échéance annuelle, avec les sinistres responsables retenus pour cette échéance (H3).
     * Un sinistre non responsable (0 %) n'est pas un SinistreResponsable : il n'est jamais dans la liste.
     */
    public CoefficientBonusMalus recalculerALEcheance(List<SinistreResponsable> sinistresRetenus) {
        BigDecimal resultat = valeur;
        for (SinistreResponsable sinistre : sinistresRetenus) {
            resultat = resultat.multiply(sinistre.estAMoitie()
                    ? PAR_SINISTRE_A_MOITIE
                    : PAR_SINISTRE_RESPONSABLE);
        }
        if (sinistresRetenus.isEmpty()) {
            resultat = resultat.multiply(PAR_ANNEE_SANS_SINISTRE_RESPONSABLE);
        }
        return new CoefficientBonusMalus(resultat.max(MINIMUM).min(MAXIMUM));
    }

    @Override
    public String toString() {
        return valeur.toPlainString();
    }
}
