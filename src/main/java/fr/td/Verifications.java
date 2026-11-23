package fr.td;

import static fr.td.Verificateur.*;

import fr.td.baremes.CoefficientBonusMalus;
import fr.td.baremes.SinistreResponsable;
import fr.td.indemnisation.ConclusionDExpertise;
import fr.td.indemnisation.DossierId;
import fr.td.indemnisation.DossierSinistreCloture;
import fr.td.indemnisation.Garantie;
import fr.td.indemnisation.Gestionnaire;
import fr.td.indemnisation.GestionnaireAffecte;
import fr.td.indemnisation.GestionnaireId;
import fr.td.indemnisation.Montant;
import fr.td.indemnisation.Responsabilite;
import fr.td.indemnisation.expertis.RapportExpertis;
import fr.td.indemnisation.expertis.RapportExpertisIntraduisible;
import fr.td.indemnisation.expertis.TraducteurExpertis;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * VOS vérifications. Une par règle, qui montre que la règle est respectée... et qu'on ne peut pas la contourner.
 * Jour 1 : A4 (Responsabilite), A13 (CoefficientBonusMalus) et A15 (Gestionnaire).
 * Jour 2 : un mot, deux modèles (Contrat : A1, A3, A17 / A14), les événements du core (A15)
 * et l'ACL Expertis (A8, H18).
 */
public class Verifications {

    private static final GestionnaireId ANTOINE = new GestionnaireId("Antoine");
    private static final GestionnaireId SOPHIE = new GestionnaireId("Sophie");
    private static final DossierId DOSSIER_DIALLO = new DossierId("S-DIALLO");

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
        titre("CoefficientBonusMalus (value object, contexte Barèmes), règle A13");
        verifier("A13", "un sinistre responsable multiplie par 1,25 et, à 50 %, par 1,125", () -> {
            CoefficientBonusMalus deux = CoefficientBonusMalus.de("2.00");
            attendu(CoefficientBonusMalus.de("2.50"), deux.recalculerALEcheance(List.of(SinistreResponsable.TOTALEMENT)));
            attendu(CoefficientBonusMalus.de("2.25"), deux.recalculerALEcheance(List.of(SinistreResponsable.A_MOITIE)));
        });
        verifier("A13", "une année sans sinistre responsable multiplie par 0,95, et un sinistre non responsable ne compte pas", () -> {
            CoefficientBonusMalus deux = CoefficientBonusMalus.de("2.00");
            attendu(CoefficientBonusMalus.de("1.90"), deux.recalculerALEcheance(List.of()));
            attendu(true, SinistreResponsable.retenu(0).isEmpty());
            attendu(CoefficientBonusMalus.de("1.90"), deux.recalculerALEcheance(SinistreResponsable.retenu(0).stream().toList()));
            refuse(() -> new SinistreResponsable(0));
            refuse(() -> SinistreResponsable.retenu(30));
        });
        verifier("A13", "le coefficient reste entre 0,50 et 3,50", () -> {
            attendu(CoefficientBonusMalus.de("3.50"),
                    CoefficientBonusMalus.de("3.00").recalculerALEcheance(List.of(SinistreResponsable.TOTALEMENT)));
            attendu(CoefficientBonusMalus.de("0.50"), CoefficientBonusMalus.de("0.50").recalculerALEcheance(List.of()));
            refuse(() -> CoefficientBonusMalus.de("3.51"));
            refuse(() -> CoefficientBonusMalus.de("0.49"));
        });
        verifier("A13", "le coefficient est arrondi à 2 décimales (1,125 donne 1,13)", () -> {
            attendu(CoefficientBonusMalus.de("1.13"),
                    CoefficientBonusMalus.initial().recalculerALEcheance(List.of(SinistreResponsable.A_MOITIE)));
        });

        // ------------------------------------------------------------------
        titre("Gestionnaire (entité), règle A15");
        verifier("A15", "un gestionnaire ne peut pas avoir 61 dossiers ouverts", () -> {
            Gestionnaire antoine = gestionnaireAvec60Dossiers();
            attendu(60, antoine.nombreDeDossiersOuverts());
            refuse(() -> antoine.prendreEnCharge(new DossierId("D61")));
            attendu(60, antoine.nombreDeDossiersOuverts());
        });
        verifier("A15", "un dossier clos libère une place, et un dossier déjà suivi ne compte pas deux fois", () -> {
            Gestionnaire antoine = gestionnaireAvec60Dossiers();
            antoine.prendreEnCharge(new DossierId("D1"));          // déjà suivi, aucune place prise
            attendu(60, antoine.nombreDeDossiersOuverts());
            antoine.cloturer(new DossierId("D1"));
            antoine.prendreEnCharge(new DossierId("D61"));
            attendu(60, antoine.nombreDeDossiersOuverts());
        });
        verifier("A15", "deux gestionnaires de même identifiant sont le même gestionnaire", () -> {
            attendu(new Gestionnaire(ANTOINE), new Gestionnaire(ANTOINE));
            differents(new Gestionnaire(ANTOINE), new Gestionnaire(SOPHIE));
        });

        // ------------------------------------------------------------------
        titre("Un mot, deux modèles : Contrat (Indemnisation), règles A1, A3, A17");
        verifier("A3", "le pare-brise de Mme Roux est couvert : contrat actif et bris de glace souscrit", () -> {
            fr.td.indemnisation.Contrat roux = contratDeMmeRoux(List.of());
            attendu(true, roux.couvre(LocalDate.of(2026, 3, 10), Garantie.BRIS_DE_GLACE));
        });
        verifier("A3", "pas couvert si la garantie n'est pas souscrite, ou avant la souscription", () -> {
            fr.td.indemnisation.Contrat roux = contratDeMmeRoux(List.of());
            attendu(false, roux.couvre(LocalDate.of(2026, 3, 10), Garantie.VOL));
            attendu(false, roux.couvre(LocalDate.of(2025, 12, 31), Garantie.BRIS_DE_GLACE));
        });
        verifier("A17", "un sinistre survenu pendant une suspension pour impayé n'est pas couvert", () -> {
            fr.td.indemnisation.Contrat roux = contratDeMmeRoux(List.of(
                    new fr.td.indemnisation.Contrat.Suspension(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))));
            attendu(false, roux.couvre(LocalDate.of(2026, 3, 10), Garantie.BRIS_DE_GLACE));
            attendu(true, roux.couvre(LocalDate.of(2026, 4, 1), Garantie.BRIS_DE_GLACE));
        });
        verifier("A1", "un contrat sans la garantie tiers est refusé", () ->
                refuse(() -> new fr.td.indemnisation.Contrat(new fr.td.indemnisation.ContratId("C-ROUX"),
                        LocalDate.of(2026, 1, 1), Set.of(Garantie.VOL), List.of())));

        // ------------------------------------------------------------------
        titre("Un mot, deux modèles : Contrat (Barèmes et bonus-malus), règle A14");
        verifier("A14", "à l'échéance de mars, le coefficient de M. Diallo (50 %) passe de 1,00 à 1,13", () -> {
            fr.td.baremes.Contrat diallo = contratDeMDiallo();
            diallo.recalculerLeCoefficient(LocalDate.of(2027, 3, 1), List.of(SinistreResponsable.A_MOITIE));
            attendu(CoefficientBonusMalus.de("1.13"), diallo.coefficient());
            attendu(LocalDate.of(2028, 3, 1), diallo.echeanceAnnuelle());
        });
        verifier("A14", "jamais le jour du sinistre : refusé, et le coefficient ne change pas", () -> {
            fr.td.baremes.Contrat diallo = contratDeMDiallo();
            refuse(() -> diallo.recalculerLeCoefficient(LocalDate.of(2026, 11, 23), List.of(SinistreResponsable.A_MOITIE)));
            attendu(CoefficientBonusMalus.initial(), diallo.coefficient());
            attendu(LocalDate.of(2027, 3, 1), diallo.echeanceAnnuelle());
        });
        verifier("J2", "les deux Contrat partagent l'identifiant, pas l'objet", () ->
                attendu(new fr.td.indemnisation.ContratId("C-DIALLO").valeur(),
                        contratDeMDiallo().id().valeur()));

        // ------------------------------------------------------------------
        titre("Événements du core : GestionnaireAffecte et DossierSinistreCloture, règle A15");
        verifier("A15", "prendre en charge un dossier sinistre produit GestionnaireAffecte", () -> {
            Gestionnaire antoine = new Gestionnaire(ANTOINE);
            attendu(new GestionnaireAffecte(DOSSIER_DIALLO, ANTOINE), antoine.prendreEnCharge(DOSSIER_DIALLO));
            attendu(1, antoine.nombreDeDossiersOuverts());
        });
        verifier("A15", "au 61e dossier, refus : aucun GestionnaireAffecte, et le dossier n'est pas suivi", () -> {
            Gestionnaire antoine = gestionnaireAvec60Dossiers();
            refuse(() -> antoine.prendreEnCharge(DOSSIER_DIALLO));
            attendu(60, antoine.nombreDeDossiersOuverts());
            refuse(() -> antoine.cloturer(DOSSIER_DIALLO));     // il ne le suit pas : rien n'a changé
        });
        verifier("A15", "clôturer un dossier sinistre suivi produit DossierSinistreCloture et libère une place", () -> {
            Gestionnaire antoine = new Gestionnaire(ANTOINE);
            antoine.prendreEnCharge(DOSSIER_DIALLO);
            attendu(new DossierSinistreCloture(DOSSIER_DIALLO, ANTOINE), antoine.cloturer(DOSSIER_DIALLO));
            attendu(0, antoine.nombreDeDossiersOuverts());
        });
        verifier("A15", "un gestionnaire ne clôture pas un dossier qu'il ne suit pas : refus, rien ne change", () -> {
            Gestionnaire antoine = new Gestionnaire(ANTOINE);
            Gestionnaire sophie = new Gestionnaire(SOPHIE);
            sophie.prendreEnCharge(DOSSIER_DIALLO);
            refuse(() -> antoine.cloturer(DOSSIER_DIALLO));
            attendu(1, sophie.nombreDeDossiersOuverts());
            attendu(0, antoine.nombreDeDossiersOuverts());
        });

        // ------------------------------------------------------------------
        titre("ACL Expertis → Indemnisation, règle A8 et hypothèse H18");
        TraducteurExpertis acl = new TraducteurExpertis();
        verifier("H18", "la Clio de M. Lambert : « VEI », 3 200 € / 4 800 € donne un véhicule économiquement irréparable", () ->
                attendu(new ConclusionDExpertise.VehiculeEconomiquementIrreparable(Montant.euros(3200), Montant.euros(4800)),
                        acl.traduire(new RapportExpertis("M-0042", "3200.00", "4800.00", "VEI", "rapports/M-0042.pdf"))));
        verifier("H18", "sans code, une réparation de 900 € sur un véhicule à 8 000 € donne un véhicule réparable", () ->
                attendu(new ConclusionDExpertise.VehiculeReparable(Montant.euros(8000), Montant.euros(900)),
                        acl.traduire(new RapportExpertis("M-0043", "8000.00", "900.00", null, "rapports/M-0043.pdf"))));
        verifier("H18", "les euros en texte d'Expertis deviennent des Montant en centimes", () ->
                attendu(new Montant(320050),
                        acl.traduire(new RapportExpertis("M-0044", "3200,50", "450.00", "", "x.pdf")).valeurDuVehicule()));
        verifier("H18", "un code Expertis inconnu ne passe pas en silence", () ->
                rejeteParLACL(() -> acl.traduire(new RapportExpertis("M-0045", "5000", "1200", "PTE", "x.pdf"))));
        verifier("H18", "un code « VEI » qui contredit les chiffres est rejeté (A8 fait foi)", () ->
                rejeteParLACL(() -> acl.traduire(new RapportExpertis("M-0046", "5000", "1200", "VEI", "x.pdf"))));
        verifier("H18", "un montant illisible, négatif ou absent est rejeté", () -> {
            rejeteParLACL(() -> acl.traduire(new RapportExpertis("M-0047", "trois mille", "1200", "", "x.pdf")));
            rejeteParLACL(() -> acl.traduire(new RapportExpertis("M-0048", "-5000", "1200", "", "x.pdf")));
            rejeteParLACL(() -> acl.traduire(new RapportExpertis("M-0049", null, "1200", "", "x.pdf")));
        });
        verifier("A8", "une conclusion d'expertise ne peut pas dire réparable si la réparation coûte plus que le véhicule", () ->
                refuse(() -> new ConclusionDExpertise.VehiculeReparable(Montant.euros(3200), Montant.euros(4800))));

        bilan();
    }

    private static Gestionnaire gestionnaireAvec60Dossiers() {
        Gestionnaire antoine = new Gestionnaire(ANTOINE);
        for (int i = 1; i <= 60; i++) {
            antoine.prendreEnCharge(new DossierId("D" + i));
        }
        return antoine;
    }

    private static fr.td.indemnisation.Contrat contratDeMmeRoux(List<fr.td.indemnisation.Contrat.Suspension> suspensions) {
        return new fr.td.indemnisation.Contrat(new fr.td.indemnisation.ContratId("C-ROUX"),
                LocalDate.of(2026, 1, 1), Set.of(Garantie.TIERS, Garantie.BRIS_DE_GLACE), suspensions);
    }

    private static fr.td.baremes.Contrat contratDeMDiallo() {
        return new fr.td.baremes.Contrat(new fr.td.baremes.ContratId("C-DIALLO"),
                CoefficientBonusMalus.initial(), LocalDate.of(2027, 3, 1));
    }

    /** L'ACL doit refuser explicitement : une autre exception voudrait dire qu'elle a laissé passer le problème. */
    private static void rejeteParLACL(Runnable traduction) {
        try {
            traduction.run();
        } catch (RapportExpertisIntraduisible attendu) {
            return;
        }
        throw new AssertionError("le rapport aurait dû être rejeté par l'ACL (RapportExpertisIntraduisible)");
    }
}
