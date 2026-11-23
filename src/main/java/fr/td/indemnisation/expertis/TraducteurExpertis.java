package fr.td.indemnisation.expertis;

import fr.td.indemnisation.ConclusionDExpertise;
import fr.td.indemnisation.Montant;

import java.math.BigDecimal;
import java.util.Set;

/**
 * L'ACL Expertis → Indemnisation : le seul endroit qui connaît la langue d'Expertis.
 * Il reçoit un RapportExpertis et rend une ConclusionDExpertise, avec des Montant en centimes.
 * Si Expertis change de format demain, seules RapportExpertis et cette classe changent.
 */
public class TraducteurExpertis {

    private static final String CODE_VEHICULE_ECONOMIQUEMENT_IRREPARABLE = "VEI";
    private static final Set<String> CODES_CONNUS = Set.of(CODE_VEHICULE_ECONOMIQUEMENT_IRREPARABLE);

    public ConclusionDExpertise traduire(RapportExpertis rapport) {
        Montant valeurDuVehicule = montant("valeurVehicule", rapport.valeurVehicule(), rapport);
        Montant coutDeLaReparation = montant("coutReparation", rapport.coutReparation(), rapport);
        String code = rapport.codeConclusion() == null ? "" : rapport.codeConclusion().trim();

        if (!code.isEmpty() && !CODES_CONNUS.contains(code)) {
            throw new RapportExpertisIntraduisible("mission " + rapport.referenceMission()
                    + " : code Expertis inconnu « " + code + " »");
        }

        // H18 : A8 fait foi, ce sont les chiffres qui décident. Le code ne sert qu'à vérifier.
        ConclusionDExpertise conclusion = ConclusionDExpertise.depuis(valeurDuVehicule, coutDeLaReparation);
        boolean expertisDitIrreparable = CODE_VEHICULE_ECONOMIQUEMENT_IRREPARABLE.equals(code);
        boolean a8DitIrreparable = conclusion instanceof ConclusionDExpertise.VehiculeEconomiquementIrreparable;
        if (expertisDitIrreparable != a8DitIrreparable) {
            throw new RapportExpertisIntraduisible("mission " + rapport.referenceMission()
                    + " : le code « " + code + " » contredit les chiffres (valeur " + valeurDuVehicule
                    + ", réparation " + coutDeLaReparation + ")");
        }
        return conclusion;
    }

    /** Expertis envoie des euros en texte (« 3200.00 ») ; le core compte en centimes. */
    private static Montant montant(String champ, String texte, RapportExpertis rapport) {
        try {
            BigDecimal euros = new BigDecimal(texte.trim().replace(',', '.'));
            return new Montant(euros.movePointRight(2).longValueExact());
        } catch (RuntimeException illisible) {
            throw new RapportExpertisIntraduisible("mission " + rapport.referenceMission()
                    + " : champ Expertis « " + champ + " » illisible : " + texte);
        }
    }
}
