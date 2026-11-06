package fr.td.domaine;

/**
 * Value object, une somme d'argent, en centimes (comme PaySûr), jamais négative.
 * Tout arrondi se fait au centime inférieur, en faveur de la mutuelle
 * (priorité 1, ne jamais payer plus que ce que le contrat prévoit).
 */
public record Montant(long centimes) {

    public static final Montant ZERO = new Montant(0);

    public Montant {
        if (centimes < 0) {
            throw new IllegalArgumentException("un montant ne peut pas être négatif, reçu " + centimes + " centimes");
        }
    }

    public static Montant euros(long euros) {
        return new Montant(Math.multiplyExact(euros, 100L));
    }

    public Montant plus(Montant autre) {
        return new Montant(Math.addExact(centimes, autre.centimes));
    }

    /** Retire un montant sans jamais descendre sous zéro (une franchise ne rend pas une indemnité négative). */
    public Montant retrancher(Montant autre) {
        return new Montant(Math.max(0, centimes - autre.centimes));
    }

    public Montant plusPetitEntre(Montant autre) {
        return centimes <= autre.centimes ? this : autre;
    }

    public boolean estSuperieurA(Montant autre) {
        return centimes > autre.centimes;
    }

    /** Une part de ce montant, arrondie au centime inférieur. */
    public Montant part(int pourcentage) {
        if (pourcentage < 0 || pourcentage > 100) {
            throw new IllegalArgumentException("une part va de 0 à 100 %, reçu " + pourcentage);
        }
        return new Montant(centimes * pourcentage / 100);
    }

    @Override
    public String toString() {
        return String.format("%d,%02d €", centimes / 100, centimes % 100);
    }
}
