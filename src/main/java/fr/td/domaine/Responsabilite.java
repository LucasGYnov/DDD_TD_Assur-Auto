package fr.td.domaine;

/**
 * Value object, la part de responsabilité de l'assuré dans un sinistre.
 * Règle A4, la part vaut 0 %, 50 % ou 100 %, rien d'autre.
 */
public record Responsabilite(int pourcentage) {

    public static final Responsabilite AUCUNE = new Responsabilite(0);
    public static final Responsabilite MOITIE = new Responsabilite(50);
    public static final Responsabilite TOTALE = new Responsabilite(100);

    public Responsabilite {
        if (pourcentage != 0 && pourcentage != 50 && pourcentage != 100) {
            throw new IllegalArgumentException("A4, la responsabilité est de 0, 50 ou 100 %, pas " + pourcentage + " %");
        }
    }

    public boolean estNulle() {
        return pourcentage == 0;
    }

    /** La part que l'assuré n'a pas à supporter, soit 100 % moins sa responsabilité. */
    public int partNonResponsable() {
        return 100 - pourcentage;
    }

    @Override
    public String toString() {
        return pourcentage + " %";
    }
}
