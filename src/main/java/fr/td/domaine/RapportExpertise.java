package fr.td.domaine;

/**
 * Value object, ce que l'expert (cabinet d'expertise, via Expertis) a fixé, c'est-à-dire la valeur du véhicule
 * et le coût de la réparation. Un rapport ne se modifie pas, une contre-expertise en produit un autre.
 *
 * A8, si la réparation coûte plus que la valeur, le véhicule est économiquement irréparable.
 * C'est la règle qui décide, pas le code « VEI » d'Expertis.
 */
public record RapportExpertise(Montant valeurVehicule, Montant coutReparation) {

    public boolean estEconomiquementIrreparable() {
        return coutReparation.estSuperieurA(valeurVehicule);
    }
}
