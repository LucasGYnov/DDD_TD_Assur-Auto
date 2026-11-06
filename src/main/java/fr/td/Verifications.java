package fr.td;

import static fr.td.Verificateur.*;

import fr.td.domaine.Beneficiaire;
import fr.td.domaine.CoefficientBonusMalus;
import fr.td.domaine.Couverture;
import fr.td.domaine.Declaration;
import fr.td.domaine.DossierId;
import fr.td.domaine.DossierSinistre;
import fr.td.domaine.EtatDossier;
import fr.td.domaine.Garantie;
import fr.td.domaine.Gestionnaire;
import fr.td.domaine.GestionnaireId;
import fr.td.domaine.HistoriqueSinistres;
import fr.td.domaine.JoursOuvres;
import fr.td.domaine.Montant;
import fr.td.domaine.NatureSinistre;
import fr.td.domaine.PeriodeCouverture;
import fr.td.domaine.RapportExpertise;
import fr.td.domaine.Responsabilite;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * VOS vérifications. Une par règle (A1 à A18) et une par hypothèse (H1 à H7). Chacune montre que la
 * règle est respectée... et qu'on ne peut pas la contourner.
 * Les réponses des systèmes externes (ContratPlus, Expertis, jours fériés) sont simulées par des valeurs.
 */
public class Verifications {

    public static void main(String[] args) {

        // ------------------------------------------------------------------
        titre("Responsabilite (value object), règle A4");
        verifier("A4", "la responsabilité de l'assuré est de 0, 50 ou 100 %, pas 30 %", () -> {
            attendu(50, Responsabilite.MOITIE.pourcentage());
            refuse(() -> new Responsabilite(30));
            refuse(() -> new Responsabilite(-50));
        });

        titre("CoefficientBonusMalus (value object), règles A13, A14, H3");
        verifier("A13", "un sinistre responsable multiplie par 1,25 et, à 50 %, par 1,125", () -> {
            CoefficientBonusMalus deux = CoefficientBonusMalus.de("2.00");
            attendu(CoefficientBonusMalus.de("2.50"), deux.recalculerALEcheance(List.of(Responsabilite.TOTALE)));
            attendu(CoefficientBonusMalus.de("2.25"), deux.recalculerALEcheance(List.of(Responsabilite.MOITIE)));
        });
        verifier("A13", "une année sans sinistre responsable multiplie par 0,95, et un sinistre non responsable ne compte pas comme responsable", () -> {
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
        verifier("A14", "le sinistre de M. Diallo (50 %, avril) ne touche le coefficient qu'à l'échéance de mars", () -> {
            DossierSinistre diallo = dossierAccrochage("2026-04-02T10:00", "2026-04-02", Responsabilite.MOITIE, true);
            CoefficientBonusMalus avant = CoefficientBonusMalus.initial();
            attendu(CoefficientBonusMalus.initial(), avant);   // rien n'a bougé le jour de l'accident
            List<Responsabilite> retenus = diallo.responsabiliteRetenuePourLEcheance(j("2027-03-15")).stream().toList();
            attendu(CoefficientBonusMalus.de("1.13"), avant.recalculerALEcheance(retenus));
        });
        verifier("H3", "un sinistre dont la responsabilité est fixée après l'échéance attend l'échéance suivante", () -> {
            DossierSinistre dossier = dossierAccrochageSansResponsabilite("2027-03-08T10:00", "2027-03-08");
            dossier.fixerResponsabilite(Responsabilite.TOTALE, j("2027-03-29"));
            attendu(true, dossier.responsabiliteRetenuePourLEcheance(j("2027-03-10")).isEmpty());
            attendu(Responsabilite.TOTALE, dossier.responsabiliteRetenuePourLEcheance(j("2028-03-10")).orElseThrow());
            dossier.comptabiliserAuBonusMalus();
            attendu(true, dossier.responsabiliteRetenuePourLEcheance(j("2029-03-10")).isEmpty());   // jamais deux fois
        });
        verifier("A13", "le coefficient est arrondi à 2 décimales et l'indemnité au centime inférieur", () -> {
            attendu(CoefficientBonusMalus.de("1.13"),
                    CoefficientBonusMalus.initial().recalculerALEcheance(List.of(Responsabilite.MOITIE)));
            attendu(new Montant(5000), new Montant(10001).part(50));
        });

        titre("Couverture et PeriodeCouverture (value objects), règles A1, A3, A17, H7");
        verifier("A1", "un contrat sans la garantie tiers est refusé, car elle est obligatoire", () ->
                refuse(() -> Couverture.de(periodeLongue(), Set.of(Garantie.VOL), Montant.euros(8000))));
        verifier("A3", "un contrat pas encore actif à la date du sinistre ne couvre pas", () ->
                refuse(() -> ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2024-12-31T10:00", "2024-12-31", true),
                        couvertureTousRisques())));
        verifier("A3", "une garantie non souscrite ne couvre pas, par exemple un vol sans la garantie vol, même en tous risques (H7)", () ->
                refuse(() -> ouvrir(declaration(NatureSinistre.VOL, "2026-10-13T03:00", "2026-10-13", false),
                        couvertureTousRisques())));
        verifier("A17", "un sinistre survenu pendant la suspension du contrat n'est pas couvert", () -> {
            Couverture suspendue = couvertureTousRisques().suspendueDepuis(h("2026-10-01T00:00"));
            refuse(() -> ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-10-05T10:00", "2026-10-05", true), suspendue));
            attendu(EtatDossier.OUVERT,
                    ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-09-30T12:00", "2026-09-30", true), suspendue).etat());
        });
        verifier("A3", "un contrat n'est actif qu'à partir de l'heure de sa souscription", () -> {
            Couverture souscriteA18h = Couverture.de(new PeriodeCouverture(h("2026-09-10T18:00"), h("2027-09-10T18:00")),
                    Set.of(Garantie.TIERS, Garantie.TOUS_RISQUES), Montant.euros(8000));
            refuse(() -> ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-09-10T09:00", "2026-09-10", true), souscriteA18h));
            attendu(EtatDossier.OUVERT,
                    ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-09-10T18:00", "2026-09-10", true), souscriteA18h).etat());
        });

        titre("Gestionnaire (entité), règle A15");
        verifier("A15", "un gestionnaire ne peut pas avoir 61 dossiers ouverts", () -> {
            Gestionnaire antoine = gestionnaire("Antoine");
            for (int i = 0; i < 60; i++) {
                ouvrirChez(antoine, declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", true),
                        couvertureTousRisques());
            }
            attendu(60, antoine.nombreDeDossiersOuverts());
            refuse(() -> ouvrirChez(antoine, declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", true),
                    couvertureTousRisques()));
            attendu(60, antoine.nombreDeDossiersOuverts());
        });
        verifier("A15", "un dossier clos libère une place chez le gestionnaire", () -> {
            Gestionnaire antoine = gestionnaire("Antoine");
            DossierSinistre premier = null;
            for (int i = 0; i < 60; i++) {
                DossierSinistre d = ouvrirChez(antoine,
                        declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", true), couvertureTousRisques());
                premier = (premier == null) ? d : premier;
            }
            antoine.cloturer(premier.id());
            attendu(EtatDossier.OUVERT, ouvrirChez(antoine,
                    declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", true), couvertureTousRisques()).etat());
        });
        verifier("A15", "un dossier garde son gestionnaire de bout en bout (pas de transfert aux seniors)", () -> {
            Gestionnaire antoine = gestionnaire("Antoine");
            DossierSinistre dossier = ouvrirChez(antoine,
                    declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", true), couvertureTousRisques());
            dossier.estimerDommages(Montant.euros(900));
            dossier.fixerResponsabilite(Responsabilite.MOITIE, j("2026-10-14"));
            dossier.proposerIndemnisation(j("2026-10-15"));
            attendu(antoine.id(), dossier.gestionnaire());
        });
        verifier("ENT", "deux entités sont égales si leur identifiant est le même, quel que soit le reste", () -> {
            attendu(new Gestionnaire(new GestionnaireId("Antoine")), new Gestionnaire(new GestionnaireId("Antoine")));
            differents(new Gestionnaire(new GestionnaireId("Antoine")), new Gestionnaire(new GestionnaireId("Claire")));
            DossierSinistre dossier = dossierAccrochage("2026-10-13T10:00", "2026-10-13", Responsabilite.TOTALE, true);
            dossier.classerSansSuite();
            attendu(dossier.id(), dossier.id());
            differents(dossierAccrochage("2026-10-13T10:00", "2026-10-13", Responsabilite.TOTALE, true),
                    dossierAccrochage("2026-10-13T10:00", "2026-10-13", Responsabilite.TOTALE, true));
        });

        titre("DossierSinistre, déclaration, règles A2, H1, H5");
        verifier("A2", "le vol de Mme Perrin, déclaré le mercredi, en retard d'un jour, noté, pas refusé", () -> {
            DossierSinistre perrin = ouvrir(declaration(NatureSinistre.VOL, "2026-10-16T23:30", "2026-10-21", false),
                    couvertureComplete());
            attendu(1, perrin.joursDeRetard());
            attendu(EtatDossier.OUVERT, perrin.etat());
        });
        verifier("A2", "2 jours ouvrés pour un vol et 5 pour un accident, pas de retard dans le délai", () -> {
            attendu(0, ouvrir(declaration(NatureSinistre.VOL, "2026-10-16T23:30", "2026-10-20", false), couvertureComplete()).joursDeRetard());
            attendu(0, ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-20", true), couvertureTousRisques()).joursDeRetard());
            attendu(1, ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-21", true), couvertureTousRisques()).joursDeRetard());
        });
        verifier("A2", "seul le gestionnaire du dossier peut refuser, et seulement pour un retard", () -> {
            Gestionnaire antoine = gestionnaire("Antoine");
            DossierSinistre enRetard = ouvrirChez(antoine, declaration(NatureSinistre.VOL, "2026-10-16T23:30", "2026-10-21", false),
                    couvertureComplete());
            refuse(() -> enRetard.refuserPourRetard(new GestionnaireId("Claire")));
            enRetard.refuserPourRetard(antoine.id());
            attendu(EtatDossier.REFUSE, enRetard.etat());
            DossierSinistre dansLesDelais = ouvrirChez(antoine,
                    declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-14", true), couvertureTousRisques());
            refuse(() -> dansLesDelais.refuserPourRetard(antoine.id()));
        });
        verifier("H5", "le jour du sinistre ne compte pas, le jour de la déclaration compte, un férié ne compte pas", () -> {
            // vol le vendredi 6 novembre, le mercredi 11 est férié
            attendu(0, ouvrir(declaration(NatureSinistre.VOL, "2026-11-06T22:00", "2026-11-11", false), couvertureComplete()).joursDeRetard());
            attendu(1, ouvrir(declaration(NatureSinistre.VOL, "2026-11-06T22:00", "2026-11-12", false), couvertureComplete()).joursDeRetard());
            attendu(2, new JoursOuvres(Set.of()).ecoulesEntre(j("2026-11-06"), j("2026-11-10")));
        });
        verifier("H1", "un conducteur non déclaré n'empêche pas la couverture et il est noté dans le dossier", () -> {
            Declaration filsDeMmeRoux = new Declaration(NatureSinistre.ACCROCHAGE, h("2026-10-13T10:00"), j("2026-10-13"), true, false);
            DossierSinistre dossier = ouvrir(filsDeMmeRoux, couvertureTousRisques());
            attendu(EtatDossier.OUVERT, dossier.etat());
            attendu(true, dossier.conducteurNonDeclare());
            attendu(Garantie.TOUS_RISQUES, dossier.garantie());
        });
        verifier("A4", "un vol ou un bris de glace est à 0 % de responsabilité, on ne peut pas le changer", () -> {
            DossierSinistre vol = ouvrir(declaration(NatureSinistre.VOL, "2026-10-13T03:00", "2026-10-13", false), couvertureComplete());
            refuse(() -> vol.fixerResponsabilite(Responsabilite.TOTALE, j("2026-10-14")));
            attendu(Responsabilite.AUCUNE, vol.responsabiliteRetenuePourLEcheance(j("2027-01-01")).orElseThrow());
        });

        titre("DossierSinistre, fraude (règles A12, A18, H4)");
        verifier("A12", "le contrat tout neuf de M. Caron, sinistre à 12 jours de la souscription, indemnisation suspendue", () -> {
            Couverture caron = couvertureSouscriteLe("2026-09-02T10:00");
            DossierSinistre dossier = ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-09-14T15:00", "2026-09-14", true), caron);
            attendu(true, dossier.estSuspendu());
            dossier.estimerDommages(Montant.euros(1200));
            dossier.fixerResponsabilite(Responsabilite.AUCUNE, j("2026-09-15"));
            refuse(() -> dossier.proposerIndemnisation(j("2026-09-16")));
        });
        verifier("A12", "à 15 jours de la souscription, ce n'est plus suspect (« moins de 15 jours »)", () -> {
            Couverture caron = couvertureSouscriteLe("2026-09-02T10:00");
            attendu(false, ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-09-17T15:00", "2026-09-17", true), caron).estSuspendu());
        });
        verifier("A12", "le 3e sinistre en 12 mois est suspect, pas le 2e ni un 3e qui compte un sinistre vieux de plus d'un an", () -> {
            Declaration troisieme = declaration(NatureSinistre.ACCROCHAGE, "2026-09-01T10:00", "2026-09-01", true);
            HistoriqueSinistres deux = new HistoriqueSinistres(List.of(j("2026-03-01"), j("2026-06-01")));
            HistoriqueSinistres un = new HistoriqueSinistres(List.of(j("2026-06-01")));
            HistoriqueSinistres unVieux = new HistoriqueSinistres(List.of(j("2025-08-30"), j("2026-06-01")));
            attendu(true, ouvrirAvecHistorique(troisieme, deux).estSuspendu());
            attendu(false, ouvrirAvecHistorique(troisieme, un).estSuspendu());
            attendu(false, ouvrirAvecHistorique(troisieme, unVieux).estSuspendu());
        });
        verifier("A12", "l'équipe vol et fraude lève la suspension et l'indemnisation peut reprendre", () -> {
            DossierSinistre dossier = ouvrirAvecHistorique(
                    declaration(NatureSinistre.ACCROCHAGE, "2026-09-01T10:00", "2026-09-01", true),
                    new HistoriqueSinistres(List.of(j("2026-03-01"), j("2026-06-01"))));
            dossier.estimerDommages(Montant.euros(1200));
            dossier.fixerResponsabilite(Responsabilite.AUCUNE, j("2026-09-02"));
            refuse(() -> dossier.proposerIndemnisation(j("2026-09-03")));
            dossier.leverSuspension();
            attendu(Montant.euros(1200), dossier.proposerIndemnisation(j("2026-09-03")));
        });
        verifier("H4", "le 3e sinistre compte un bris de glace et un sinistre non responsable", () -> {
            DossierSinistre brisDeGlace = ouvrir(declaration(NatureSinistre.BRIS_DE_GLACE, "2026-03-02T09:00", "2026-03-02", false), couvertureComplete());
            DossierSinistre nonResponsable = dossierAccrochage("2026-05-04T10:00", "2026-05-04", Responsabilite.AUCUNE, true);
            HistoriqueSinistres historique = historiqueDe(brisDeGlace, nonResponsable);
            attendu(true, ouvrirAvecHistorique(
                    declaration(NatureSinistre.ACCROCHAGE, "2026-09-01T10:00", "2026-09-01", true), historique).estSuspendu());
        });
        verifier("A18", "un dossier classé sans suite ne compte ni pour le bonus-malus ni pour A12", () -> {
            DossierSinistre doublon = dossierAccrochage("2026-03-02T10:00", "2026-03-02", Responsabilite.TOTALE, true);
            DossierSinistre autre = dossierAccrochage("2026-05-04T10:00", "2026-05-04", Responsabilite.TOTALE, true);
            doublon.classerSansSuite();
            attendu(true, doublon.responsabiliteRetenuePourLEcheance(j("2027-03-01")).isEmpty());
            attendu(false, doublon.estRetenuPourLHistorique());
            HistoriqueSinistres historique = historiqueDe(doublon, autre);
            attendu(false, ouvrirAvecHistorique(
                    declaration(NatureSinistre.ACCROCHAGE, "2026-09-01T10:00", "2026-09-01", true), historique).estSuspendu());
        });

        titre("DossierSinistre, franchises (règles A5, A6, A16)");
        verifier("A5", "le pare-brise de Mme Roux chez un réparateur agréé, 0 € de franchise et 620 € versés au garage (A16)", () -> {
            DossierSinistre roux = ouvrir(declaration(NatureSinistre.BRIS_DE_GLACE, "2026-10-13T14:00", "2026-10-13", false), couvertureComplete());
            roux.choisirReparateurAgree();
            roux.estimerDommages(Montant.euros(620));
            attendu(Montant.euros(620), roux.proposerIndemnisation(j("2026-10-15")));
            attendu(Beneficiaire.REPARATEUR_AGREE, roux.beneficiaireDeLIndemnite());
        });
        verifier("A5", "un bris de glace dans un autre garage, 100 € de franchise, versés à l'assuré", () -> {
            DossierSinistre dossier = ouvrir(declaration(NatureSinistre.BRIS_DE_GLACE, "2026-10-13T14:00", "2026-10-13", false), couvertureComplete());
            dossier.estimerDommages(Montant.euros(620));
            attendu(Montant.euros(520), dossier.proposerIndemnisation(j("2026-10-15")));
            attendu(Beneficiaire.ASSURE, dossier.beneficiaireDeLIndemnite());
        });
        verifier("A5", "M. Diallo, tous risques, 900 € de dommages, 300 € de franchise, donc 600 € (50 % ne réduit pas)", () -> {
            DossierSinistre diallo = dossierAccrochage("2026-10-13T10:00", "2026-10-13", Responsabilite.MOITIE, true);
            attendu(Montant.euros(600), diallo.proposerIndemnisation(j("2026-10-15")));
        });
        verifier("A5", "une franchise plus grande que les dommages donne une indemnité de 0 €, jamais négative", () -> {
            DossierSinistre dossier = ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", false), couvertureTousRisques());
            dossier.estimerDommages(Montant.euros(200));
            dossier.fixerResponsabilite(Responsabilite.TOTALE, j("2026-10-14"));
            attendu(Montant.ZERO, dossier.proposerIndemnisation(j("2026-10-15")));
        });
        verifier("A5", "le vol de Mme Perrin, retrouvé abîmé (1 200 €), la même franchise de 500 €, donc 700 € ", () -> {
            DossierSinistre perrin = ouvrir(declaration(NatureSinistre.VOL, "2026-10-16T23:30", "2026-10-21", false), couvertureComplete());
            perrin.retrouverVehicule(Montant.euros(1200));
            attendu(Montant.euros(700), perrin.proposerIndemnisation(j("2026-10-28")));
            attendu(false, perrin.expertiseObligatoire());
            refuse(() -> dossierAccrochage("2026-10-13T10:00", "2026-10-13", Responsabilite.TOTALE, true)
                    .retrouverVehicule(Montant.euros(100)));   // seul un véhicule volé se retrouve
        });
        verifier("A6", "pas de franchise si l'assuré est non responsable et le tiers identifié, sinon la franchise s'applique", () -> {
            DossierSinistre tiersIdentifie = dossierAccrochage("2026-10-13T10:00", "2026-10-13", Responsabilite.AUCUNE, true);
            DossierSinistre tiersInconnu = dossierAccrochage("2026-10-13T10:00", "2026-10-13", Responsabilite.AUCUNE, false);
            attendu(Montant.euros(900), tiersIdentifie.proposerIndemnisation(j("2026-10-15")));
            attendu(Montant.euros(600), tiersInconnu.proposerIndemnisation(j("2026-10-15")));
        });

        titre("DossierSinistre, expertise et plafond (règles A7, A8, A9, H6)");
        verifier("A7", "au-delà de 1 500 € de dommages estimés, une expertise est obligatoire, et à 1 500 € pile, non", () -> {
            DossierSinistre pile = ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", true), couvertureTousRisques());
            pile.estimerDommages(Montant.euros(1500));
            pile.fixerResponsabilite(Responsabilite.TOTALE, j("2026-10-14"));
            attendu(false, pile.expertiseObligatoire());
            attendu(Montant.euros(1200), pile.proposerIndemnisation(j("2026-10-15")));

            DossierSinistre audessus = ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", true), couvertureTousRisques());
            audessus.estimerDommages(new Montant(150001));
            audessus.fixerResponsabilite(Responsabilite.TOTALE, j("2026-10-14"));
            attendu(true, audessus.expertiseObligatoire());
            refuse(() -> audessus.proposerIndemnisation(j("2026-10-15")));
            audessus.recevoirRapport(new RapportExpertise(Montant.euros(8000), new Montant(150001)));
            attendu(new Montant(120001), audessus.proposerIndemnisation(j("2026-10-15")));
        });
        verifier("A8", "la Clio de M. Lambert, réparation 4 800 € au-dessus de la valeur 3 200 €, donc irréparable et 3 200 - 300 = 2 900 €", () -> {
            DossierSinistre lambert = dossierLambert();
            attendu(Montant.euros(2900), lambert.montantPropose());
        });
        verifier("A8", "l'indemnité ne dépasse jamais la valeur du véhicule fixée par l'expert", () -> {
            DossierSinistre dossier = ouvrir(declaration(NatureSinistre.AUTRE_DOMMAGES, "2026-10-13T10:00", "2026-10-13", false), couvertureTousRisques());
            dossier.estimerDommages(Montant.euros(4000));
            dossier.fixerResponsabilite(Responsabilite.TOTALE, j("2026-10-14"));
            dossier.recevoirRapport(new RapportExpertise(Montant.euros(3200), Montant.euros(2000)));
            attendu(Montant.euros(1700), dossier.proposerIndemnisation(j("2026-10-15")));   // réparable, donc 2 000 - 300
        });
        verifier("H6", "sans expertise, le plafond est la valeur déclarée à la souscription et, avec expertise, celle de l'expert", () -> {
            Couverture valeurDeclaree1000 = Couverture.de(periodeLongue(), Set.of(Garantie.TIERS, Garantie.TOUS_RISQUES), Montant.euros(1000));
            DossierSinistre sansExpertise = ouvrir(declaration(NatureSinistre.AUTRE_DOMMAGES, "2026-10-13T10:00", "2026-10-13", false), valeurDeclaree1000);
            sansExpertise.estimerDommages(Montant.euros(1400));
            sansExpertise.fixerResponsabilite(Responsabilite.TOTALE, j("2026-10-14"));
            attendu(Montant.euros(700), sansExpertise.proposerIndemnisation(j("2026-10-15")));   // 1 000 - 300
            attendu(Montant.euros(2900), dossierLambert().montantPropose());                     // 3 200 de l'expert, pas 5 000
        });
        verifier("A9", "avec la seule garantie tiers, rien à 100 %, la moitié à 50 % et tout à 0 %", () -> {
            attendu(Montant.ZERO, accrochageTiers(Responsabilite.TOTALE).proposerIndemnisation(j("2026-10-15")));
            attendu(Montant.euros(400), accrochageTiers(Responsabilite.MOITIE).proposerIndemnisation(j("2026-10-15")));
            attendu(Montant.euros(800), accrochageTiers(Responsabilite.AUCUNE).proposerIndemnisation(j("2026-10-15")));
        });

        titre("DossierSinistre, accord, contestation, versement (règles A10, A11, A16, H2)");
        verifier("A10", "l'indemnité est versée sous 30 jours après l'accord de l'assuré, pas avant l'accord", () -> {
            DossierSinistre dossier = dossierAvecMontantPropose("2026-10-20");
            refuse(dossier::dateLimiteDeVersement);
            refuse(dossier::verser);
            dossier.accepterMontant(j("2026-10-22"));
            attendu(j("2026-11-21"), dossier.dateLimiteDeVersement());
            dossier.verser();
            attendu(EtatDossier.INDEMNISE, dossier.etat());
        });
        verifier("H2", "le silence n'est pas un accord, et après 30 jours sans réponse la proposition est caduque", () -> {
            DossierSinistre aTemps = dossierAvecMontantPropose("2026-10-20");
            aTemps.accepterMontant(j("2026-11-19"));                        // 30e jour, encore possible
            attendu(EtatDossier.MONTANT_ACCEPTE, aTemps.etat());
            DossierSinistre tropTard = dossierAvecMontantPropose("2026-10-20");
            refuse(() -> tropTard.accepterMontant(j("2026-11-20")));         // 31e jour, caduque
            refuse(tropTard::dateLimiteDeVersement);                         // les 30 jours de A10 n'ont pas commencé
        });
        verifier("A11", "M. Lambert conteste une seule fois, et le montant de la contre-expertise est définitif", () -> {
            DossierSinistre lambert = dossierLambert();
            lambert.contester();
            attendu(EtatDossier.MONTANT_CONTESTE, lambert.etat());
            refuse(() -> lambert.proposerIndemnisation(j("2026-10-25")));    // la contre-expertise n'est pas reçue
            lambert.recevoirRapport(new RapportExpertise(Montant.euros(3500), Montant.euros(4800)));
            attendu(Montant.euros(3200), lambert.proposerIndemnisation(j("2026-10-26")));
            refuse(lambert::contester);                                      // une seule fois
            lambert.accepterMontant(j("2026-10-27"));
            attendu(EtatDossier.MONTANT_ACCEPTE, lambert.etat());
        });
        verifier("A16", "sans réparateur agréé, l'indemnité est versée à l'assuré, et avec, au garage", () -> {
            DossierSinistre sans = dossierAccrochage("2026-10-13T10:00", "2026-10-13", Responsabilite.TOTALE, true);
            attendu(Beneficiaire.ASSURE, sans.beneficiaireDeLIndemnite());
            DossierSinistre avec = ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", true), couvertureTousRisques());
            avec.choisirReparateurAgree();
            attendu(Beneficiaire.REPARATEUR_AGREE, avec.beneficiaireDeLIndemnite());
        });

        bilan();
    }

    // ---------------------------------------------------------------------
    // Vos méthodes utilitaires. Elles fabriquent des objets prêts à l'emploi
    // pour vos vérifications. Les réponses des systèmes externes sont simulées ici.
    // ---------------------------------------------------------------------

    private static final JoursOuvres CALENDRIER =
            new JoursOuvres(Set.of(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 12, 25)));   // jours fériés simulés
    private static int compteur = 0;

    private static LocalDateTime h(String instant) {
        return LocalDateTime.parse(instant);
    }

    private static LocalDate j(String jour) {
        return LocalDate.parse(jour);
    }

    private static DossierId nouvelId() {
        return new DossierId("S" + (++compteur));
    }

    private static Gestionnaire gestionnaire(String nom) {
        return new Gestionnaire(new GestionnaireId(nom));
    }

    private static PeriodeCouverture periodeLongue() {
        return new PeriodeCouverture(h("2025-01-06T09:00"), h("2030-01-01T00:00"));
    }

    private static Couverture couvertureTousRisques() {
        return Couverture.de(periodeLongue(), Set.of(Garantie.TIERS, Garantie.TOUS_RISQUES), Montant.euros(8000));
    }

    private static Couverture couvertureTiers() {
        return Couverture.de(periodeLongue(), Set.of(Garantie.TIERS), Montant.euros(8000));
    }

    private static Couverture couvertureComplete() {
        return Couverture.de(periodeLongue(),
                Set.of(Garantie.TIERS, Garantie.VOL, Garantie.BRIS_DE_GLACE, Garantie.TOUS_RISQUES), Montant.euros(8000));
    }

    private static Couverture couvertureSouscriteLe(String instant) {
        return Couverture.de(new PeriodeCouverture(h(instant), h("2030-01-01T00:00")),
                Set.of(Garantie.TIERS, Garantie.TOUS_RISQUES), Montant.euros(8000));
    }

    private static Declaration declaration(NatureSinistre nature, String survenuLe, String declareLe, boolean tiersIdentifie) {
        return new Declaration(nature, h(survenuLe), j(declareLe), tiersIdentifie, true);
    }

    private static DossierSinistre ouvrir(Declaration declaration, Couverture couverture) {
        return ouvrirChez(gestionnaire("Antoine"), declaration, couverture);
    }

    private static DossierSinistre ouvrirChez(Gestionnaire gestionnaire, Declaration declaration, Couverture couverture) {
        return DossierSinistre.ouvrir(nouvelId(), gestionnaire, declaration, couverture, HistoriqueSinistres.vide(), CALENDRIER);
    }

    private static DossierSinistre ouvrirAvecHistorique(Declaration declaration, HistoriqueSinistres historique) {
        return DossierSinistre.ouvrir(nouvelId(), gestionnaire("Antoine"), declaration, couvertureTousRisques(), historique, CALENDRIER);
    }

    private static HistoriqueSinistres historiqueDe(DossierSinistre... dossiers) {
        return new HistoriqueSinistres(Arrays.stream(dossiers)
                .filter(DossierSinistre::estRetenuPourLHistorique)
                .map(DossierSinistre::survenuLe)
                .toList());
    }

    /** Un accrochage en tous risques de 900 €, responsabilité fixée, prêt à être proposé. */
    private static DossierSinistre dossierAccrochage(String survenuLe, String declareLe, Responsabilite part, boolean tiersIdentifie) {
        DossierSinistre dossier = ouvrir(declaration(NatureSinistre.ACCROCHAGE, survenuLe, declareLe, tiersIdentifie), couvertureTousRisques());
        dossier.estimerDommages(Montant.euros(900));
        dossier.fixerResponsabilite(part, j(declareLe));
        return dossier;
    }

    private static DossierSinistre dossierAccrochageSansResponsabilite(String survenuLe, String declareLe) {
        DossierSinistre dossier = ouvrir(declaration(NatureSinistre.ACCROCHAGE, survenuLe, declareLe, true), couvertureTousRisques());
        dossier.estimerDommages(Montant.euros(900));
        return dossier;
    }

    /** Un accrochage de 800 € avec la seule garantie tiers. */
    private static DossierSinistre accrochageTiers(Responsabilite part) {
        DossierSinistre dossier = ouvrir(declaration(NatureSinistre.ACCROCHAGE, "2026-10-13T10:00", "2026-10-13", true), couvertureTiers());
        dossier.estimerDommages(Montant.euros(800));
        dossier.fixerResponsabilite(part, j("2026-10-14"));
        return dossier;
    }

    private static DossierSinistre dossierAvecMontantPropose(String proposeLe) {
        DossierSinistre dossier = dossierAccrochage("2026-10-13T10:00", "2026-10-13", Responsabilite.TOTALE, true);
        dossier.proposerIndemnisation(j(proposeLe));
        return dossier;
    }

    /** La Clio de M. Lambert, seul en cause (100 %), réparation estimée 4 800 €, valeur de l'expert 3 200 €, déclarée 5 000 €. */
    private static DossierSinistre dossierLambert() {
        Couverture couverture = Couverture.de(periodeLongue(), Set.of(Garantie.TIERS, Garantie.TOUS_RISQUES), Montant.euros(5000));
        DossierSinistre lambert = ouvrir(declaration(NatureSinistre.AUTRE_DOMMAGES, "2026-10-13T10:00", "2026-10-13", false), couverture);
        lambert.estimerDommages(Montant.euros(4800));
        lambert.fixerResponsabilite(Responsabilite.TOTALE, j("2026-10-14"));
        lambert.recevoirRapport(new RapportExpertise(Montant.euros(3200), Montant.euros(4800)));
        lambert.proposerIndemnisation(j("2026-10-20"));
        return lambert;
    }
}
