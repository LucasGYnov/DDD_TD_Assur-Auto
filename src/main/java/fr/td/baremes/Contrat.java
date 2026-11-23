package fr.td.baremes;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * « Un mot, deux modèles » : le Contrat vu par Barèmes et bonus-malus.
 * Il ne connaît que son coefficient de bonus-malus et son échéance annuelle (A13, A14).
 * Il ignore les garanties, les suspensions et les dossiers sinistres : c'est
 * fr.td.indemnisation.Contrat qui les connaît. Les deux partagent la valeur du ContratId.
 */
public class Contrat {

    private final ContratId id;
    private CoefficientBonusMalus coefficient;
    private LocalDate echeanceAnnuelle;

    public Contrat(ContratId id, CoefficientBonusMalus coefficient, LocalDate echeanceAnnuelle) {
        this.id = Objects.requireNonNull(id);
        this.coefficient = Objects.requireNonNull(coefficient);
        this.echeanceAnnuelle = Objects.requireNonNull(echeanceAnnuelle);
    }

    /**
     * A14, le coefficient ne se recalcule qu'à l'échéance annuelle, jamais le jour du sinistre.
     * Après le recalcul, la prochaine échéance est un an plus tard.
     */
    public void recalculerLeCoefficient(LocalDate jour, List<SinistreResponsable> sinistresRetenus) {
        if (!jour.equals(echeanceAnnuelle)) {
            throw new IllegalStateException("A14, le coefficient du contrat " + id
                    + " ne se recalcule qu'à son échéance annuelle (" + echeanceAnnuelle + "), pas le " + jour);
        }
        coefficient = coefficient.recalculerALEcheance(sinistresRetenus);
        echeanceAnnuelle = echeanceAnnuelle.plusYears(1);
    }

    public ContratId id() {
        return id;
    }

    public CoefficientBonusMalus coefficient() {
        return coefficient;
    }

    public LocalDate echeanceAnnuelle() {
        return echeanceAnnuelle;
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof Contrat c && id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
