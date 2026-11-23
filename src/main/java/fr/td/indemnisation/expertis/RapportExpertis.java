package fr.td.indemnisation.expertis;

/**
 * Ce qu'Expertis envoie, tel qu'il l'envoie : sa langue, pas la nôtre (dossier client, section 7).
 * Un rapport PDF et quelques champs structurés : valeur du véhicule, coût de réparation,
 * et le code « VEI » quand le véhicule est irréparable. Les montants arrivent en euros, en texte.
 *
 * Cette classe ne sort jamais du package expertis : le core ne la connaît pas.
 */
public record RapportExpertis(
        String referenceMission,
        String valeurVehicule,
        String coutReparation,
        String codeConclusion,
        String lienRapportPdf) {
}
