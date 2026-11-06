package fr.td.domaine;

/**
 * Value object, la franchise que l'assuré garde à sa charge sur une indemnité.
 *
 * A5, 300 € en tous risques, 500 € pour le vol, et pour un bris de glace 0 € chez un réparateur agréé,
 * 100 € ailleurs. La garantie tiers n'a pas de franchise.
 * A6, pas de franchise si l'assuré est non responsable et que le tiers est identifié.
 * Dès que l'assuré est responsable, même à 50 %, la franchise est entière.
 *
 * Le barème est revu chaque année (priorité 2), donc c'est ici, et seulement ici, qu'il est écrit.
 */
public record Franchise(Montant montant) {

    private static final Montant TOUS_RISQUES = Montant.euros(300);
    private static final Montant VOL = Montant.euros(500);
    private static final Montant BRIS_DE_GLACE_AUTRE_GARAGE = Montant.euros(100);

    public static Franchise pour(Garantie garantie, boolean reparateurAgree, Responsabilite responsabilite,
                                 boolean tiersIdentifie) {
        if (responsabilite.estNulle() && tiersIdentifie) {
            return new Franchise(Montant.ZERO);
        }
        return new Franchise(switch (garantie) {
            case TIERS -> Montant.ZERO;
            case TOUS_RISQUES -> TOUS_RISQUES;
            case VOL -> VOL;
            case BRIS_DE_GLACE -> reparateurAgree ? Montant.ZERO : BRIS_DE_GLACE_AUTRE_GARAGE;
        });
    }
}
