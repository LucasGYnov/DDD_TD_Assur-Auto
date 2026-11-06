package fr.td.domaine;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Value object, ce que le contrat dit au moment d'un sinistre. C'est une photographie de ce que
 * ContratPlus répond. La mutuelle ne gère pas les contrats (hors périmètre) et ne la modifie jamais.
 *
 * A1, la garantie tiers est obligatoire. A3, actif à la date du sinistre. A17, un contrat
 * suspendu (prime non payée) ne couvre pas un sinistre survenu pendant la suspension.
 */
public record Couverture(PeriodeCouverture periode, Set<Garantie> garanties, Montant valeurDeclaree,
                         LocalDateTime suspenduDepuis) {

    public Couverture {
        garanties = Set.copyOf(garanties);
        if (!garanties.contains(Garantie.TIERS)) {
            throw new IllegalArgumentException("A1, la garantie tiers est obligatoire");
        }
    }

    public static Couverture de(PeriodeCouverture periode, Set<Garantie> garanties, Montant valeurDeclaree) {
        return new Couverture(periode, garanties, valeurDeclaree, null);
    }

    public Couverture suspendueDepuis(LocalDateTime instant) {
        return new Couverture(periode, garanties, valeurDeclaree, instant);
    }

    public boolean estActiveLe(LocalDateTime instant) {
        return periode.contient(instant) && (suspenduDepuis == null || instant.isBefore(suspenduDepuis));
    }

    /**
     * La garantie qui joue pour une nature de sinistre. Pour les dommages de la voiture de l'assuré
     * (accrochage, autres dommages), tous risques si souscrit, sinon tiers (A9).
     * Si la garantie n'est pas souscrite, le sinistre n'est pas couvert (A3).
     */
    public Garantie garantiePour(NatureSinistre nature) {
        return switch (nature) {
            case VOL -> exiger(Garantie.VOL);
            case BRIS_DE_GLACE -> exiger(Garantie.BRIS_DE_GLACE);
            case ACCROCHAGE, AUTRE_DOMMAGES ->
                    garanties.contains(Garantie.TOUS_RISQUES) ? Garantie.TOUS_RISQUES : Garantie.TIERS;
        };
    }

    private Garantie exiger(Garantie garantie) {
        if (!garanties.contains(garantie)) {
            throw new RegleRefusee("A3", "la garantie " + garantie + " n'était pas souscrite");
        }
        return garantie;
    }
}
