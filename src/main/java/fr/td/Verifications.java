package fr.td;

import static fr.td.Verificateur.*;

import fr.td.domaine.CoefficientBonusMalus;
import fr.td.domaine.DossierId;
import fr.td.domaine.Gestionnaire;
import fr.td.domaine.GestionnaireId;
import fr.td.domaine.Montant;
import fr.td.domaine.Responsabilite;

import java.util.List;

/**
 * VOS vérifications. Une par règle, qui montre que la règle est respectée... et qu'on ne peut pas la contourner.
 * Règles vérifiées ici : A4 (Responsabilite), A13 (CoefficientBonusMalus) et A15 (Gestionnaire).
 */
public class Verifications {

    public static void main(String[] args) {

        // ------------------------------------------------------------------
        titre("Montant (value object)");
        verifier("A5", "un montant n'est jamais négatif, et retrancher une franchise ne descend pas sous zéro", () -> {
            refuse(() -> new Montant(-1));
            attendu(Montant.ZERO, Montant.euros(200).retrancher(Montant.euros(300)));
            attendu(Montant.euros(600), Montant.euros(900).retrancher(Montant.euros(300)));
        });

        // ------------------------------------------------------------------
        titre("Responsabilite (value object), règle A4");
        verifier("A4", "la responsabilité de l'assuré est de 0, 50 ou 100 %, pas 30 %", () -> {
            attendu(50, Responsabilite.MOITIE.pourcentage());
            refuse(() -> new Responsabilite(30));
            refuse(() -> new Responsabilite(-50));
        });

        // ------------------------------------------------------------------
        titre("CoefficientBonusMalus (value object), règle A13");
        verifier("A13", "un sinistre responsable multiplie par 1,25 et, à 50 %, par 1,125", () -> {
            CoefficientBonusMalus deux = CoefficientBonusMalus.de("2.00");
            attendu(CoefficientBonusMalus.de("2.50"), deux.recalculerALEcheance(List.of(Responsabilite.TOTALE)));
            attendu(CoefficientBonusMalus.de("2.25"), deux.recalculerALEcheance(List.of(Responsabilite.MOITIE)));
        });
        verifier("A13", "une année sans sinistre responsable multiplie par 0,95, et un sinistre non responsable ne compte pas", () -> {
            CoefficientBonusMalus deux = CoefficientBonusMalus.de("2.00");
            attendu(CoefficientBonusMalus.de("1.90"), deux.recalculerALEcheance(List.of()));
            attendu(CoefficientBonusMalus.de("1.90"), deux.recalculerALEcheance(List.of(Responsabilite.AUCUNE)));
        });
        verifier("A13", "le coefficient reste entre 0,50 et 3,50", () -> {
            attendu(CoefficientBonusMalus.de("3.50"),
                    CoefficientBonusMalus.de("3.00").recalculerALEcheance(List.of(Responsabilite.TOTALE)));
            attendu(CoefficientBonusMalus.de("0.50"), CoefficientBonusMalus.de("0.50").recalculerALEcheance(List.of()));
            refuse(() -> CoefficientBonusMalus.de("3.51"));
            refuse(() -> CoefficientBonusMalus.de("0.49"));
        });
        verifier("A13", "le coefficient est arrondi à 2 décimales (1,125 donne 1,13)", () -> {
            attendu(CoefficientBonusMalus.de("1.13"),
                    CoefficientBonusMalus.initial().recalculerALEcheance(List.of(Responsabilite.MOITIE)));
        });

        // ------------------------------------------------------------------
        titre("Gestionnaire (entité), règle A15");
        verifier("A15", "un gestionnaire ne peut pas avoir 61 dossiers ouverts", () -> {
            Gestionnaire antoine = new Gestionnaire(new GestionnaireId("Antoine"));
            for (int i = 1; i <= 60; i++) {
                antoine.prendreEnCharge(new DossierId("D" + i));
            }
            attendu(60, antoine.nombreDeDossiersOuverts());
            refuse(() -> antoine.prendreEnCharge(new DossierId("D61")));
            attendu(60, antoine.nombreDeDossiersOuverts());
        });
        verifier("A15", "un dossier clos libère une place, et un dossier déjà suivi ne compte pas deux fois", () -> {
            Gestionnaire antoine = new Gestionnaire(new GestionnaireId("Antoine"));
            for (int i = 1; i <= 60; i++) {
                antoine.prendreEnCharge(new DossierId("D" + i));
            }
            antoine.prendreEnCharge(new DossierId("D1"));          // déjà suivi, aucune place prise
            attendu(60, antoine.nombreDeDossiersOuverts());
            antoine.cloturer(new DossierId("D1"));
            antoine.prendreEnCharge(new DossierId("D61"));
            attendu(60, antoine.nombreDeDossiersOuverts());
        });
        verifier("A15", "deux gestionnaires de même identifiant sont le même gestionnaire", () -> {
            attendu(new Gestionnaire(new GestionnaireId("Antoine")), new Gestionnaire(new GestionnaireId("Antoine")));
            differents(new Gestionnaire(new GestionnaireId("Antoine")), new Gestionnaire(new GestionnaireId("Sophie")));
        });

        bilan();
    }
}