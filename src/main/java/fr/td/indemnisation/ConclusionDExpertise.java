package fr.td.indemnisation;

/**
 * Ce que le core retient d'une expertise, avec les mots du glossaire :
 * le véhicule est réparable, ou économiquement irréparable (A8).
 * Le core ne voit jamais le code « VEI », le PDF ni la référence de mission d'Expertis.
 *
 * A8, la valeur du véhicule fixée par l'expert plafonne l'indemnité ; si la réparation
 * coûte plus que cette valeur, le véhicule est économiquement irréparable.
 */
public sealed interface ConclusionDExpertise
        permits ConclusionDExpertise.VehiculeReparable, ConclusionDExpertise.VehiculeEconomiquementIrreparable {

    Montant valeurDuVehicule();

    Montant coutDeLaReparation();

    /** A8, c'est la comparaison des deux montants qui décide, pas un code. */
    static ConclusionDExpertise depuis(Montant valeurDuVehicule, Montant coutDeLaReparation) {
        return coutDeLaReparation.estSuperieurA(valeurDuVehicule)
                ? new VehiculeEconomiquementIrreparable(valeurDuVehicule, coutDeLaReparation)
                : new VehiculeReparable(valeurDuVehicule, coutDeLaReparation);
    }

    record VehiculeReparable(Montant valeurDuVehicule, Montant coutDeLaReparation) implements ConclusionDExpertise {
        public VehiculeReparable {
            if (coutDeLaReparation.estSuperieurA(valeurDuVehicule)) {
                throw new RegleRefusee("A8", "une réparation plus chère que le véhicule le rend économiquement irréparable");
            }
        }
    }

    record VehiculeEconomiquementIrreparable(Montant valeurDuVehicule, Montant coutDeLaReparation)
            implements ConclusionDExpertise {
        public VehiculeEconomiquementIrreparable {
            if (!coutDeLaReparation.estSuperieurA(valeurDuVehicule)) {
                throw new RegleRefusee("A8", "un véhicule n'est irréparable que si la réparation coûte plus que sa valeur");
            }
        }
    }
}
