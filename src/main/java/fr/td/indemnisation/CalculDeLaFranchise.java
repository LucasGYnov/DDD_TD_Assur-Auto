package fr.td.indemnisation;

import java.util.Objects;

/**
 * Domain service : le calcul de la franchise (A5, A6, barème de la section 8 du dossier client).
 *
 * Il n'appartient à aucun objet : la garantie vient du contrat, la responsabilité et le tiers
 * identifié du dossier sinistre, et le statut du réparateur (agréé ou non) de la convention des
 * garages. Aucun objet ne possède les trois.
 *
 * Sans état : il reçoit tout ce dont il a besoin et renvoie la franchise.
 */
public class CalculDeLaFranchise {

    public static final Montant FRANCHISE_TOUS_RISQUES = Montant.euros(300);
    public static final Montant FRANCHISE_VOL = Montant.euros(500);
    public static final Montant FRANCHISE_BRIS_DE_GLACE_AUTRE_GARAGE = Montant.euros(100);

    public Montant franchise(Garantie garantie, Responsabilite responsabilite,
                             boolean tiersIdentifie, boolean reparateurAgree) {
        Objects.requireNonNull(garantie);
        Objects.requireNonNull(responsabilite);
        return switch (garantie) {
            // A6 : aucune franchise si l'assuré est non responsable et que le tiers est identifié
            case TOUS_RISQUES -> sansFranchise(responsabilite, tiersIdentifie) ? Montant.ZERO : FRANCHISE_TOUS_RISQUES;
            case VOL -> sansFranchise(responsabilite, tiersIdentifie) ? Montant.ZERO : FRANCHISE_VOL;
            // A5 : bris de glace, 0 € chez un réparateur agréé, 100 € ailleurs
            case BRIS_DE_GLACE -> reparateurAgree ? Montant.ZERO : FRANCHISE_BRIS_DE_GLACE_AUTRE_GARAGE;
            // H13 : A5 ne prévoit aucune franchise pour la garantie tiers
            case TIERS -> Montant.ZERO;
        };
    }

    /** A6, et H12 : à 50 % de responsabilité, la franchise s'applique en entier. */
    private static boolean sansFranchise(Responsabilite responsabilite, boolean tiersIdentifie) {
        return responsabilite.estNulle() && tiersIdentifie;
    }
}
