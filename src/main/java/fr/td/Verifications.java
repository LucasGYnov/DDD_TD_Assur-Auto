package fr.td;

import static fr.td.Verificateur.*;

import fr.td.baremes.CoefficientBonusMalus;
import fr.td.baremes.SinistreResponsable;
import fr.td.indemnisation.CalculDeLaFranchise;
import fr.td.indemnisation.ConclusionDExpertise;
import fr.td.indemnisation.ContratId;
import fr.td.indemnisation.DeclarationRefuseePourRetard;
import fr.td.indemnisation.DommagesEstimes;
import fr.td.indemnisation.DossierClasseSansSuite;
import fr.td.indemnisation.DossierId;
import fr.td.indemnisation.DossierSinistre;
import fr.td.indemnisation.DossierSinistreCloture;
import fr.td.indemnisation.EtatDuDossier;
import fr.td.indemnisation.Garantie;
import fr.td.indemnisation.Gestionnaire;
import fr.td.indemnisation.GestionnaireAffecte;
import fr.td.indemnisation.GestionnaireId;
import fr.td.indemnisation.IndemnisationSuspendue;
import fr.td.indemnisation.LiberationDuGestionnaire;
import fr.td.indemnisation.Montant;
import fr.td.indemnisation.MontantAccepte;
import fr.td.indemnisation.MontantConteste;
import fr.td.indemnisation.MontantPropose;
import fr.td.indemnisation.PlaceLiberee;
import fr.td.indemnisation.RapportDExpertiseRecu;
import fr.td.indemnisation.Responsabilite;
import fr.td.indemnisation.ResponsabiliteFixee;
import fr.td.indemnisation.SuspensionLevee;
import fr.td.indemnisation.VerificationDeLaCouverture;
import fr.td.indemnisation.expertis.RapportExpertis;
import fr.td.indemnisation.expertis.RapportExpertisIntraduisible;
import fr.td.indemnisation.expertis.TraducteurExpertis;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * VOS vérifications. Une par règle, qui montre que la règle est respectée... et qu'on ne peut pas la contourner.
 * Jour 1 : A4 (Responsabilite), A13 (CoefficientBonusMalus) et A15 (Gestionnaire).
 * Jour 2 : un mot, deux modèles (Contrat : A1, A3, A17 / A14), les événements du core (A15)
 * et l'ACL Expertis (A8, H18).
 * Jour 3 : le domain service CalculDeLaFranchise (A5, A6), l'agrégat principal DossierSinistre
 * (deux vérifications par invariant), la policy LiberationDuGestionnaire (A15)
 * et trois scénarios rejoués : SC1 (Mme Roux), SC2 (M. Caron), SC3 (Mme Roux, par la policy).
 */
public class Verifications {

    private static final GestionnaireId ANTOINE = new GestionnaireId("Antoine");
    private static final GestionnaireId SOPHIE = new GestionnaireId("Sophie");
    private static final DossierId DOSSIER_DIALLO = new DossierId("S-DIALLO");
    private static final DossierId DOSSIER_LAMBERT = new DossierId("S-LAMBERT");
    private static final ContratId CONTRAT_DIALLO = new ContratId("C-DIALLO");
    private static final ContratId CONTRAT_LAMBERT = new ContratId("C-LAMBERT");
    private static final DossierId DOSSIER_ROUX = new DossierId("S-ROUX");
    private static final DossierId DOSSIER_CARON = new DossierId("S-CARON");
    private static final ContratId CONTRAT_ROUX = new ContratId("C-ROUX");
    private static final ContratId CONTRAT_CARON = new ContratId("C-CARON");
    private static final LocalDate LE_1ER_MARS_2024 = LocalDate.of(2024, 3, 1);
    private static final LocalDate LE_3_NOVEMBRE = LocalDate.of(2026, 11, 3);
    private static final LocalDate LE_20_NOVEMBRE = LocalDate.of(2026, 11, 20);
    private static final LocalDate LE_25_NOVEMBRE = LocalDate.of(2026, 11, 25);
    private static final LocalDate LE_10_DECEMBRE = LocalDate.of(2026, 12, 10);

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
        titre("Gestionnaire (agrégat), règle A15");
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
            antoine.libererLaPlace(new DossierId("D1"));
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
                refuse(() -> new fr.td.indemnisation.Contrat(CONTRAT_ROUX,
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
        titre("Événement du core (J2) : GestionnaireAffecte, règle A15");
        verifier("A15", "prendre en charge un dossier sinistre produit GestionnaireAffecte", () -> {
            Gestionnaire antoine = new Gestionnaire(ANTOINE);
            attendu(new GestionnaireAffecte(DOSSIER_DIALLO, ANTOINE), antoine.prendreEnCharge(DOSSIER_DIALLO));
            attendu(1, antoine.nombreDeDossiersOuverts());
        });
        verifier("A15", "au 61e dossier, refus : aucun GestionnaireAffecte, et le dossier n'est pas suivi", () -> {
            Gestionnaire antoine = gestionnaireAvec60Dossiers();
            refuse(() -> antoine.prendreEnCharge(DOSSIER_DIALLO));
            attendu(60, antoine.nombreDeDossiersOuverts());
            refuse(() -> antoine.libererLaPlace(DOSSIER_DIALLO));   // il ne le suit pas : rien n'a changé
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

        // ==================================================================
        //                              JOUR 3
        // ==================================================================

        // ------------------------------------------------------------------
        titre("Domain service : CalculDeLaFranchise, règles A5 et A6 (barème, section 8)");
        CalculDeLaFranchise calcul = new CalculDeLaFranchise();
        verifier("A5", "cas courant : M. Diallo, tous risques, responsable à 50 % : 300 € de franchise (H12)", () ->
                attendu(Montant.euros(300), calcul.franchise(Garantie.TOUS_RISQUES, Responsabilite.MOITIE, true, false)));
        verifier("A6", "cas limite : non responsable avec un tiers identifié, aucune franchise ; sans tiers identifié, 300 €", () -> {
            attendu(Montant.ZERO, calcul.franchise(Garantie.TOUS_RISQUES, Responsabilite.AUCUNE, true, false));
            attendu(Montant.euros(300), calcul.franchise(Garantie.TOUS_RISQUES, Responsabilite.AUCUNE, false, false));
            attendu(Montant.ZERO, calcul.franchise(Garantie.VOL, Responsabilite.AUCUNE, true, false));
        });
        verifier("A5", "le reste du barème : vol 500 €, bris de glace 0 € chez un réparateur agréé et 100 € ailleurs, tiers 0 € (H13)", () -> {
            attendu(Montant.euros(500), calcul.franchise(Garantie.VOL, Responsabilite.TOTALE, false, false));
            attendu(Montant.ZERO, calcul.franchise(Garantie.BRIS_DE_GLACE, Responsabilite.AUCUNE, false, true));
            attendu(Montant.euros(100), calcul.franchise(Garantie.BRIS_DE_GLACE, Responsabilite.AUCUNE, false, false));
            attendu(Montant.ZERO, calcul.franchise(Garantie.TIERS, Responsabilite.MOITIE, true, false));
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre (agrégat principal) : A15, un seul gestionnaire");
        verifier("A15", "le gestionnaire affecté est inscrit sur le dossier, et le réinscrire ne change rien", () -> {
            DossierSinistre dossier = nouveauDossierDiallo(false);
            dossier.affecterLeGestionnaire(ANTOINE);
            dossier.affecterLeGestionnaire(ANTOINE);
            attendu(Optional.of(ANTOINE), dossier.gestionnaire());
        });
        verifier("A15", "un deuxième gestionnaire est refusé : Antoine garde le dossier jusqu'à la clôture (H16)", () -> {
            DossierSinistre dossier = nouveauDossierDiallo(false);
            dossier.affecterLeGestionnaire(ANTOINE);
            refuse(() -> dossier.affecterLeGestionnaire(SOPHIE));
            attendu(Optional.of(ANTOINE), dossier.gestionnaire());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : A3 et A17, pas de montant pour un sinistre non couvert");
        verifier("A3", "M. Diallo : contrat actif et tous risques souscrit, Couverture confirmée, dossier en instruction", () -> {
            DossierSinistre dossier = nouveauDossierDiallo(false);
            attendu(new VerificationDeLaCouverture.CouvertureConfirmee(DOSSIER_DIALLO, CONTRAT_DIALLO, Garantie.TOUS_RISQUES),
                    dossier.verifierLaCouverture(contratTousRisques(CONTRAT_DIALLO, List.of())));
            attendu(EtatDuDossier.EN_INSTRUCTION, dossier.etat());
        });
        verifier("A17", "sinistre pendant une suspension pour impayé : Couverture refusée, et plus rien ne peut être fait", () -> {
            DossierSinistre dossier = nouveauDossierDiallo(false);
            fr.td.indemnisation.Contrat suspendu = contratTousRisques(CONTRAT_DIALLO,
                    List.of(new fr.td.indemnisation.Contrat.Suspension(LE_3_NOVEMBRE.minusDays(5), LE_3_NOVEMBRE.plusDays(25))));
            attendu(new VerificationDeLaCouverture.CouvertureRefusee(DOSSIER_DIALLO, CONTRAT_DIALLO, Garantie.TOUS_RISQUES, LE_3_NOVEMBRE),
                    dossier.verifierLaCouverture(suspendu));
            refuse(() -> dossier.fixerLaResponsabilite(Responsabilite.MOITIE, true));
            refuse(() -> dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE));
            attendu(EtatDuDossier.COUVERTURE_REFUSEE, dossier.etat());
        });
        verifier("A3", "le contrat d'un autre assuré est refusé, et le dossier reste ouvert", () -> {
            DossierSinistre dossier = nouveauDossierDiallo(false);
            refuse(() -> dossier.verifierLaCouverture(contratTousRisques(new ContratId("C-ROUX"), List.of())));
            attendu(EtatDuDossier.OUVERT, dossier.etat());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : A4, pas de montant avant la responsabilité fixée");
        verifier("A4", "fixer 50 % d'après le constat produit Responsabilité fixée, avec le contrat et la date (pour Barèmes)", () -> {
            DossierSinistre dossier = dossierDialloEnInstruction();
            attendu(new ResponsabiliteFixee(DOSSIER_DIALLO, CONTRAT_DIALLO, LE_3_NOVEMBRE, Responsabilite.MOITIE, true),
                    dossier.fixerLaResponsabilite(Responsabilite.MOITIE, true));
        });
        verifier("A4", "proposer un montant avant de fixer la responsabilité est refusé, et rien ne change", () -> {
            DossierSinistre dossier = dossierDialloEnInstruction();
            dossier.estimerLesDommages(Montant.euros(900));
            refuse(() -> dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE));
            attendu(EtatDuDossier.EN_INSTRUCTION, dossier.etat());
            attendu(Optional.empty(), dossier.montantPropose());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : A7, au-delà de 1 500 €, pas de montant sans expertise");
        verifier("A7", "900 € : pas d'expertise obligatoire, ni à 1 500 € pile (H6) ; à 1 500,01 €, si", () -> {
            DossierSinistre dossier = dossierDialloEnInstruction();
            attendu(new DommagesEstimes(DOSSIER_DIALLO, Montant.euros(900), false), dossier.estimerLesDommages(Montant.euros(900)));
            attendu(false, dossier.estimerLesDommages(Montant.euros(1500)).expertiseObligatoire());
            attendu(true, dossier.estimerLesDommages(new Montant(150001)).expertiseObligatoire());
        });
        verifier("A7", "la Clio de M. Lambert, 4 800 € sans conclusion d'expertise : proposition refusée, rien ne change", () -> {
            DossierSinistre dossier = dossierLambertSansExpertise();
            refuse(() -> dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE));
            attendu(EtatDuDossier.EN_INSTRUCTION, dossier.etat());
            attendu(Optional.empty(), dossier.montantPropose());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : A8, A9, jamais plus que la valeur du véhicule, jamais négatif");
        verifier("H12", "M. Diallo, 900 € en tous risques à 50 % : Montant proposé de 600 €", () -> {
            DossierSinistre dossier = dossierDialloPret();
            attendu(new MontantPropose(DOSSIER_DIALLO, Montant.euros(600), LE_20_NOVEMBRE, false),
                    dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE));
        });
        verifier("A8", "la Clio de M. Lambert, irréparable : 3 200 € − 300 € = 2 900 €, jamais plus que la valeur du véhicule", () -> {
            DossierSinistre dossier = dossierLambertAvecExpertise();
            attendu(new MontantPropose(DOSSIER_LAMBERT, Montant.euros(2900), LE_20_NOVEMBRE, false),
                    dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE));
        });
        verifier("H9", "une franchise plus grande que les dommages donne 0 €, jamais un montant négatif", () -> {
            DossierSinistre dossier = dossierDialloEnInstruction();
            dossier.fixerLaResponsabilite(Responsabilite.TOTALE, false);
            dossier.estimerLesDommages(Montant.euros(200));
            attendu(Montant.ZERO, dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE).montant());
        });
        verifier("A9", "avec la seule garantie tiers, à 50 % : la moitié des dommages, sans franchise (H13)", () -> {
            DossierSinistre dossier = new DossierSinistre(DOSSIER_DIALLO, CONTRAT_DIALLO, LE_3_NOVEMBRE, Garantie.TIERS, false);
            dossier.affecterLeGestionnaire(ANTOINE);
            dossier.verifierLaCouverture(new fr.td.indemnisation.Contrat(CONTRAT_DIALLO, LE_1ER_MARS_2024,
                    Set.of(Garantie.TIERS), List.of()));
            dossier.fixerLaResponsabilite(Responsabilite.MOITIE, true);
            dossier.estimerLesDommages(Montant.euros(900));
            attendu(Montant.euros(450), dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE).montant());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : A12, rien de proposé ni d'accepté pendant la suspension");
        verifier("A12", "suspendre produit Indemnisation suspendue, et aucun montant ne peut être proposé", () -> {
            DossierSinistre dossier = dossierDialloPret();
            attendu(new IndemnisationSuspendue(DOSSIER_DIALLO), dossier.suspendreLIndemnisation());
            refuse(() -> dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE));
            attendu(EtatDuDossier.EN_INSTRUCTION, dossier.etat());
        });
        verifier("A12", "un montant proposé n'est pas accepté pendant la suspension ; une fois la suspension levée, il l'est", () -> {
            DossierSinistre dossier = dossierDialloAvecMontantPropose();
            dossier.suspendreLIndemnisation();
            refuse(() -> dossier.accepterLeMontant(LE_25_NOVEMBRE));
            attendu(EtatDuDossier.MONTANT_PROPOSE, dossier.etat());
            attendu(new SuspensionLevee(DOSSIER_DIALLO), dossier.leverLaSuspension());
            dossier.accepterLeMontant(LE_25_NOVEMBRE);
            attendu(EtatDuDossier.MONTANT_ACCEPTE, dossier.etat());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : A11, une seule contestation, puis une proposition définitive");
        verifier("A11", "M. Lambert conteste : Montant contesté, puis une seule nouvelle proposition, définitive (H15)", () -> {
            DossierSinistre dossier = dossierLambertAvecExpertise();
            dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE);
            attendu(new MontantConteste(DOSSIER_LAMBERT, Montant.euros(2900), LE_25_NOVEMBRE), dossier.contesterLeMontant(LE_25_NOVEMBRE));
            ConclusionDExpertise contreExpertise =
                    new ConclusionDExpertise.VehiculeEconomiquementIrreparable(Montant.euros(3500), Montant.euros(4800));
            attendu(new RapportDExpertiseRecu(DOSSIER_LAMBERT, contreExpertise, true),
                    dossier.recevoirLaConclusionDExpertise(contreExpertise));
            attendu(new MontantPropose(DOSSIER_LAMBERT, Montant.euros(3200), LE_10_DECEMBRE, true),
                    dossier.proposerUnMontant(calcul, false, LE_10_DECEMBRE));
        });
        verifier("A11", "une deuxième contestation est refusée : le montant définitif ne bouge pas", () -> {
            DossierSinistre dossier = dossierLambertAvecExpertise();
            dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE);
            dossier.contesterLeMontant(LE_25_NOVEMBRE);
            dossier.proposerUnMontant(calcul, false, LE_10_DECEMBRE);
            refuse(() -> dossier.contesterLeMontant(LE_10_DECEMBRE.plusDays(2)));
            refuse(() -> dossier.proposerUnMontant(calcul, false, LE_10_DECEMBRE.plusDays(2)));
            attendu(EtatDuDossier.MONTANT_PROPOSE, dossier.etat());
            attendu(Optional.of(Montant.euros(2900)), dossier.montantPropose());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : A10, l'accord de l'assuré");
        verifier("A10", "M. Diallo accepte : Montant accepté, à verser au plus tard 30 jours après", () -> {
            DossierSinistre dossier = dossierDialloAvecMontantPropose();
            attendu(new MontantAccepte(DOSSIER_DIALLO, Montant.euros(600), LE_25_NOVEMBRE, LE_25_NOVEMBRE.plusDays(30)),
                    dossier.accepterLeMontant(LE_25_NOVEMBRE));
        });
        verifier("A10", "accepter alors qu'aucun montant n'est proposé est refusé, et rien ne change", () -> {
            DossierSinistre dossier = dossierDialloPret();
            refuse(() -> dossier.accepterLeMontant(LE_25_NOVEMBRE));
            attendu(EtatDuDossier.EN_INSTRUCTION, dossier.etat());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : A2, seul son gestionnaire peut le refuser pour retard");
        verifier("A2", "Antoine refuse le dossier déclaré en retard qu'il suit : Déclaration refusée pour retard", () -> {
            DossierSinistre dossier = nouveauDossierDiallo(true);
            dossier.affecterLeGestionnaire(ANTOINE);
            attendu(new DeclarationRefuseePourRetard(DOSSIER_DIALLO, ANTOINE), dossier.refuserPourRetard(ANTOINE));
            attendu(EtatDuDossier.REFUSE_POUR_RETARD, dossier.etat());
        });
        verifier("A2", "jamais d'office : Sophie ne peut pas refuser le dossier d'Antoine, et sans retard noté personne ne le peut", () -> {
            DossierSinistre enRetard = nouveauDossierDiallo(true);
            enRetard.affecterLeGestionnaire(ANTOINE);
            refuse(() -> enRetard.refuserPourRetard(SOPHIE));
            attendu(EtatDuDossier.OUVERT, enRetard.etat());
            DossierSinistre aLHeure = nouveauDossierDiallo(false);
            aLHeure.affecterLeGestionnaire(ANTOINE);
            refuse(() -> aLHeure.refuserPourRetard(ANTOINE));
            attendu(EtatDuDossier.OUVERT, aLHeure.etat());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : A18, un dossier classé sans suite");
        verifier("A18", "classer sans suite un doublon produit Dossier classé sans suite", () -> {
            DossierSinistre dossier = dossierDialloEnInstruction();
            attendu(new DossierClasseSansSuite(DOSSIER_DIALLO, CONTRAT_DIALLO), dossier.classerSansSuite());
            attendu(EtatDuDossier.CLASSE_SANS_SUITE, dossier.etat());
        });
        verifier("A18", "un dossier classé sans suite refuse toute commande, sauf sa clôture", () -> {
            DossierSinistre dossier = dossierDialloEnInstruction();
            dossier.classerSansSuite();
            refuse(() -> dossier.fixerLaResponsabilite(Responsabilite.MOITIE, true));
            refuse(() -> dossier.estimerLesDommages(Montant.euros(900)));
            refuse(dossier::suspendreLIndemnisation);
            attendu(EtatDuDossier.CLASSE_SANS_SUITE, dossier.etat());
            attendu(new DossierSinistreCloture(DOSSIER_DIALLO, ANTOINE), dossier.cloturer());
        });

        // ------------------------------------------------------------------
        titre("DossierSinistre : la clôture, règle A15");
        verifier("A15", "le dossier de M. Diallo, montant accepté, est clôturé : Dossier sinistre clôturé", () -> {
            DossierSinistre dossier = dossierDialloAvecMontantPropose();
            dossier.accepterLeMontant(LE_25_NOVEMBRE);
            attendu(new DossierSinistreCloture(DOSSIER_DIALLO, ANTOINE), dossier.cloturer());
            attendu(EtatDuDossier.CLOTURE, dossier.etat());
        });
        verifier("A15", "un dossier en instruction ne se clôture pas, et un dossier clôturé n'accepte plus rien", () -> {
            DossierSinistre enCours = dossierDialloPret();
            refuse(enCours::cloturer);
            attendu(EtatDuDossier.EN_INSTRUCTION, enCours.etat());
            DossierSinistre cloture = dossierDialloAvecMontantPropose();
            cloture.accepterLeMontant(LE_25_NOVEMBRE);
            cloture.cloturer();
            refuse(cloture::cloturer);
            refuse(cloture::suspendreLIndemnisation);
            attendu(EtatDuDossier.CLOTURE, cloture.etat());
        });

        // ------------------------------------------------------------------
        titre("Policy : LiberationDuGestionnaire (quand un dossier sinistre est clôturé, alors libérer sa place)");
        verifier("A15", "le dossier de M. Diallo est clôturé : Place libérée chez Antoine", () -> {
            Gestionnaire antoine = new Gestionnaire(ANTOINE);
            GestionnaireAffecte affecte = antoine.prendreEnCharge(DOSSIER_DIALLO);
            DossierSinistre dossier = nouveauDossierDiallo(false);
            dossier.affecterLeGestionnaire(affecte.gestionnaire());
            dossier.verifierLaCouverture(contratTousRisques(CONTRAT_DIALLO, List.of()));
            dossier.fixerLaResponsabilite(Responsabilite.MOITIE, true);
            dossier.estimerLesDommages(Montant.euros(900));
            dossier.proposerUnMontant(calcul, false, LE_20_NOVEMBRE);
            dossier.accepterLeMontant(LE_25_NOVEMBRE);
            DossierSinistreCloture cloture = dossier.cloturer();

            attendu(new PlaceLiberee(ANTOINE, DOSSIER_DIALLO, 0), new LiberationDuGestionnaire().quand(cloture, antoine));
            attendu(0, antoine.nombreDeDossiersOuverts());
        });
        verifier("A15", "la policy ne décide rien : si on lui donne un gestionnaire qui ne suit pas ce dossier, c'est lui qui refuse", () -> {
            Gestionnaire sophie = new Gestionnaire(SOPHIE);
            sophie.prendreEnCharge(DOSSIER_LAMBERT);
            DossierSinistreCloture cloture = new DossierSinistreCloture(DOSSIER_DIALLO, ANTOINE);
            refuse(() -> new LiberationDuGestionnaire().quand(cloture, sophie));
            attendu(1, sophie.nombreDeDossiersOuverts());
        });

        // ------------------------------------------------------------------
        titre("Scénarios rejoués (situations vécues, section 10)");
        verifier("SC1", "le pare-brise de Mme Roux : bris de glace chez un réparateur agréé, 620 € proposés sans franchise et acceptés", () -> {
            LocalDate mardi = LocalDate.of(2026, 11, 3);
            LocalDate jeudi = mardi.plusDays(2);
            DossierSinistre roux = new DossierSinistre(DOSSIER_ROUX, CONTRAT_ROUX, mardi, Garantie.BRIS_DE_GLACE, false);
            roux.affecterLeGestionnaire(ANTOINE);
            attendu(new VerificationDeLaCouverture.CouvertureConfirmee(DOSSIER_ROUX, CONTRAT_ROUX, Garantie.BRIS_DE_GLACE),
                    roux.verifierLaCouverture(contratDeMmeRoux(List.of())));
            attendu(new ResponsabiliteFixee(DOSSIER_ROUX, CONTRAT_ROUX, mardi, Responsabilite.AUCUNE, false),
                    roux.fixerLaResponsabilite(Responsabilite.AUCUNE, false));
            attendu(new DommagesEstimes(DOSSIER_ROUX, Montant.euros(620), false), roux.estimerLesDommages(Montant.euros(620)));
            attendu(new MontantPropose(DOSSIER_ROUX, Montant.euros(620), jeudi, false),
                    roux.proposerUnMontant(calcul, true, jeudi));
            attendu(new MontantAccepte(DOSSIER_ROUX, Montant.euros(620), jeudi, jeudi.plusDays(30)),
                    roux.accepterLeMontant(jeudi));
        });
        verifier("SC2", "le contrat tout neuf de M. Caron : sinistre 12 jours après la souscription, indemnisation suspendue, aucun montant", () -> {
            LocalDate souscription = LocalDate.of(2026, 11, 2);
            LocalDate accident = LocalDate.of(2026, 11, 14);
            fr.td.indemnisation.Contrat contrat = new fr.td.indemnisation.Contrat(CONTRAT_CARON, souscription,
                    Set.of(Garantie.TIERS, Garantie.TOUS_RISQUES), List.of());
            DossierSinistre caron = new DossierSinistre(DOSSIER_CARON, CONTRAT_CARON, accident, Garantie.TOUS_RISQUES, false);
            caron.affecterLeGestionnaire(ANTOINE);
            caron.verifierLaCouverture(contrat);
            caron.fixerLaResponsabilite(Responsabilite.AUCUNE, true);
            caron.estimerLesDommages(Montant.euros(1200));
            // A12 : moins de 15 jours après la souscription, l'équipe vol et fraude suspend l'indemnisation
            attendu(new IndemnisationSuspendue(DOSSIER_CARON), caron.suspendreLIndemnisation());
            refuse(() -> caron.proposerUnMontant(calcul, false, accident.plusDays(3)));
            attendu(EtatDuDossier.EN_INSTRUCTION, caron.etat());
            attendu(Optional.empty(), caron.montantPropose());
            // la suspicion levée, l'indemnisation reprend : non responsable, tiers identifié, aucune franchise (A6)
            caron.leverLaSuspension();
            attendu(new MontantPropose(DOSSIER_CARON, Montant.euros(1200), accident.plusDays(20), false),
                    caron.proposerUnMontant(calcul, false, accident.plusDays(20)));
        });
        verifier("SC3", "Mme Roux est indemnisée, son dossier est clôturé : la policy libère la place chez Antoine", () -> {
            LocalDate mardi = LocalDate.of(2026, 11, 3);
            LocalDate jeudi = mardi.plusDays(2);
            Gestionnaire antoine = new Gestionnaire(ANTOINE);
            antoine.prendreEnCharge(DOSSIER_DIALLO);
            GestionnaireAffecte affecte = antoine.prendreEnCharge(DOSSIER_ROUX);
            DossierSinistre roux = new DossierSinistre(DOSSIER_ROUX, CONTRAT_ROUX, mardi, Garantie.BRIS_DE_GLACE, false);
            roux.affecterLeGestionnaire(affecte.gestionnaire());
            roux.verifierLaCouverture(contratDeMmeRoux(List.of()));
            roux.fixerLaResponsabilite(Responsabilite.AUCUNE, false);
            roux.estimerLesDommages(Montant.euros(620));
            roux.proposerUnMontant(calcul, true, jeudi);
            roux.accepterLeMontant(jeudi);

            DossierSinistreCloture cloture = roux.cloturer();
            attendu(new DossierSinistreCloture(DOSSIER_ROUX, ANTOINE), cloture);
            attendu(new PlaceLiberee(ANTOINE, DOSSIER_ROUX, 1), new LiberationDuGestionnaire().quand(cloture, antoine));
            attendu(1, antoine.nombreDeDossiersOuverts());
        });

        bilan();
    }


    // ---------------------------------------------------------------- dossiers sinistres du jour 3

    /** L'accrochage de M. Diallo, tout juste ouvert (contexte Déclaration : retard noté ou non). */
    private static DossierSinistre nouveauDossierDiallo(boolean retardDeDeclaration) {
        return new DossierSinistre(DOSSIER_DIALLO, CONTRAT_DIALLO, LE_3_NOVEMBRE, Garantie.TOUS_RISQUES, retardDeDeclaration);
    }

    private static fr.td.indemnisation.Contrat contratTousRisques(ContratId id,
                                                                 List<fr.td.indemnisation.Contrat.Suspension> suspensions) {
        return new fr.td.indemnisation.Contrat(id, LE_1ER_MARS_2024, Set.of(Garantie.TIERS, Garantie.TOUS_RISQUES), suspensions);
    }

    /** Gestionnaire Antoine inscrit, couverture confirmée. */
    private static DossierSinistre dossierDialloEnInstruction() {
        DossierSinistre dossier = nouveauDossierDiallo(false);
        dossier.affecterLeGestionnaire(ANTOINE);
        dossier.verifierLaCouverture(contratTousRisques(CONTRAT_DIALLO, List.of()));
        return dossier;
    }

    /** Responsable à 50 % d'après le constat (tiers identifié), dommages estimés à 900 €. */
    private static DossierSinistre dossierDialloPret() {
        DossierSinistre dossier = dossierDialloEnInstruction();
        dossier.fixerLaResponsabilite(Responsabilite.MOITIE, true);
        dossier.estimerLesDommages(Montant.euros(900));
        return dossier;
    }

    /** 600 € proposés le 20 novembre. */
    private static DossierSinistre dossierDialloAvecMontantPropose() {
        DossierSinistre dossier = dossierDialloPret();
        dossier.proposerUnMontant(new CalculDeLaFranchise(), false, LE_20_NOVEMBRE);
        return dossier;
    }

    /** La Clio de M. Lambert : seul en cause (100 %), réparation estimée à 4 800 €, pas encore d'expertise. */
    private static DossierSinistre dossierLambertSansExpertise() {
        DossierSinistre dossier = new DossierSinistre(DOSSIER_LAMBERT, CONTRAT_LAMBERT, LE_3_NOVEMBRE, Garantie.TOUS_RISQUES, false);
        dossier.affecterLeGestionnaire(ANTOINE);
        dossier.verifierLaCouverture(contratTousRisques(CONTRAT_LAMBERT, List.of()));
        dossier.fixerLaResponsabilite(Responsabilite.TOTALE, false);
        dossier.estimerLesDommages(Montant.euros(4800));
        return dossier;
    }

    /** L'expert fixe la valeur de la Clio à 3 200 € : économiquement irréparable. */
    private static DossierSinistre dossierLambertAvecExpertise() {
        DossierSinistre dossier = dossierLambertSansExpertise();
        dossier.recevoirLaConclusionDExpertise(
                new ConclusionDExpertise.VehiculeEconomiquementIrreparable(Montant.euros(3200), Montant.euros(4800)));
        return dossier;
    }

    // ---------------------------------------------------------------- outils des jours 1 et 2

    private static Gestionnaire gestionnaireAvec60Dossiers() {
        Gestionnaire antoine = new Gestionnaire(ANTOINE);
        for (int i = 1; i <= 60; i++) {
            antoine.prendreEnCharge(new DossierId("D" + i));
        }
        return antoine;
    }

    private static fr.td.indemnisation.Contrat contratDeMmeRoux(List<fr.td.indemnisation.Contrat.Suspension> suspensions) {
        return new fr.td.indemnisation.Contrat(CONTRAT_ROUX,
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
