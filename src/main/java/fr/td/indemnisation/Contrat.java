package fr.td.indemnisation;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * « Un mot, deux modèles » : le Contrat vu par l'Indemnisation.
 * Il ne sait qu'une chose : couvre-t-il ce sinistre (A1, A3, A17) ?
 * Il ignore le coefficient de bonus-malus et l'échéance annuelle : c'est
 * fr.td.baremes.Contrat qui les connaît. Les deux partagent la valeur du ContratId.
 */
public class Contrat {

    /** A17, une période où le contrat est suspendu pour prime impayée, bornes comprises. */
    public record Suspension(LocalDate du, LocalDate au) {
        public Suspension {
            if (au.isBefore(du)) {
                throw new IllegalArgumentException("une suspension finit après son début");
            }
        }

        boolean contient(LocalDate jour) {
            return !jour.isBefore(du) && !jour.isAfter(au);
        }
    }

    private final ContratId id;
    private final LocalDate dateDeSouscription;
    private final Set<Garantie> garantiesSouscrites;
    private final List<Suspension> suspensions;

    public Contrat(ContratId id, LocalDate dateDeSouscription, Set<Garantie> garantiesSouscrites,
                   List<Suspension> suspensions) {
        if (!garantiesSouscrites.contains(Garantie.TIERS)) {
            throw new RegleRefusee("A1", "la garantie tiers est obligatoire");
        }
        this.id = Objects.requireNonNull(id);
        this.dateDeSouscription = Objects.requireNonNull(dateDeSouscription);
        this.garantiesSouscrites = EnumSet.copyOf(garantiesSouscrites);
        this.suspensions = List.copyOf(suspensions);
    }

    /**
     * A3, couvert si le contrat était actif à la date du sinistre et la garantie concernée souscrite.
     * A17, un sinistre survenu pendant une suspension pour impayé n'est pas couvert.
     */
    public boolean couvre(LocalDate dateDuSinistre, Garantie garantieConcernee) {
        boolean actif = !dateDuSinistre.isBefore(dateDeSouscription)
                && suspensions.stream().noneMatch(s -> s.contient(dateDuSinistre));
        return actif && garantiesSouscrites.contains(garantieConcernee);
    }

    public ContratId id() {
        return id;
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
