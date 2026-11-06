package fr.td;

import fr.td.domaine.CoefficientBonusMalus;
import fr.td.domaine.Couverture;
import fr.td.domaine.Declaration;
import fr.td.domaine.DossierId;
import fr.td.domaine.DossierSinistre;
import fr.td.domaine.Garantie;
import fr.td.domaine.Gestionnaire;
import fr.td.domaine.GestionnaireId;
import fr.td.domaine.HistoriqueSinistres;
import fr.td.domaine.JoursOuvres;
import fr.td.domaine.Montant;
import fr.td.domaine.NatureSinistre;
import fr.td.domaine.PeriodeCouverture;
import fr.td.domaine.RapportExpertise;
import fr.td.domaine.RegleRefusee;
import fr.td.domaine.Responsabilite;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Pour aller plus loin, ce programme rejoue les cinq situations vécues du dossier client (section 10), étape par
 * étape. Si une étape est impossible à jouer, c'est le modèle qui le dit.
 * Pour le lancer, clic droit puis Run 'Situations.main()'.
 */
public class Situations {

    private static final JoursOuvres CALENDRIER = new JoursOuvres(Set.of(LocalDate.of(2026, 11, 11)));
    private static final Gestionnaire ANTOINE = new Gestionnaire(new GestionnaireId("Antoine"));

    public static void main(String[] args) {
        lePareBriseDeMmeRoux();
        lAccrochageDeMDiallo();
        laClioDeMLambert();
        leVolDeMmePerrin();
        leContratToutNeuf();
    }

    private static void lePareBriseDeMmeRoux() {
        titre("Le pare-brise de Mme Roux");
        Couverture couverture = couverture("2025-06-02T10:00", 6000, Garantie.TIERS, Garantie.BRIS_DE_GLACE);
        DossierSinistre dossier = DossierSinistre.ouvrir(new DossierId("ROUX-1"), ANTOINE,
                new Declaration(NatureSinistre.BRIS_DE_GLACE, h("2026-10-13T15:00"), j("2026-10-13"), false, true),
                couverture, HistoriqueSinistres.vide(), CALENDRIER);
        dire("Mme Roux déclare le bris de glace le soir même. " + dossier + ", retard de " + dossier.joursDeRetard() + " jour");
        dossier.choisirReparateurAgree();
        dossier.estimerDommages(Montant.euros(620));
        Montant montant = dossier.proposerIndemnisation(j("2026-10-15"));
        dire("Réparateur agréé de Saumur, facture 620 €. Montant proposé " + montant + ", versé à "
                + dossier.beneficiaireDeLIndemnite());
        dossier.accepterMontant(j("2026-10-15"));
        dire("Accord. Date limite de versement le " + dossier.dateLimiteDeVersement());
    }

    private static void lAccrochageDeMDiallo() {
        titre("L'accrochage de M. Diallo");
        Couverture couverture = couverture("2024-03-12T10:00", 7000, Garantie.TIERS, Garantie.TOUS_RISQUES);
        DossierSinistre dossier = DossierSinistre.ouvrir(new DossierId("DIALLO-1"), ANTOINE,
                new Declaration(NatureSinistre.ACCROCHAGE, h("2026-10-13T11:00"), j("2026-10-13"), true, true),
                couverture, HistoriqueSinistres.vide(), CALENDRIER);
        dossier.estimerDommages(Montant.euros(900));
        dossier.fixerResponsabilite(Responsabilite.MOITIE, j("2026-10-14"));
        dire("Constat, 50 % chacun. Dommages 900 €, tous risques, montant proposé "
                + dossier.proposerIndemnisation(j("2026-10-15")) + " (900 - 300 de franchise)");
        CoefficientBonusMalus avant = CoefficientBonusMalus.de("1.00");
        dire("Le jour de l'accident, le coefficient reste " + avant + " (A14).");
        List<Responsabilite> retenus = dossier.responsabiliteRetenuePourLEcheance(j("2027-03-12")).stream().toList();
        dire("À l'échéance de mars 2027, le coefficient passe de " + avant + " à " + avant.recalculerALEcheance(retenus));
    }

    private static void laClioDeMLambert() {
        titre("La Clio de M. Lambert");
        Couverture couverture = couverture("2022-01-10T10:00", 5000, Garantie.TIERS, Garantie.TOUS_RISQUES);
        DossierSinistre dossier = DossierSinistre.ouvrir(new DossierId("LAMBERT-1"), ANTOINE,
                new Declaration(NatureSinistre.AUTRE_DOMMAGES, h("2026-10-13T07:30"), j("2026-10-13"), false, true),
                couverture, HistoriqueSinistres.vide(), CALENDRIER);
        dossier.estimerDommages(Montant.euros(4800));
        dossier.fixerResponsabilite(Responsabilite.TOTALE, j("2026-10-13"));
        dire("Réparation estimée 4 800 €. Expertise obligatoire ? " + dossier.expertiseObligatoire());
        try {
            dossier.proposerIndemnisation(j("2026-10-14"));
        } catch (RegleRefusee refus) {
            dire("Refusé. " + refus.getMessage());
        }
        RapportExpertise rapport = new RapportExpertise(Montant.euros(3200), Montant.euros(4800));
        dossier.recevoirRapport(rapport);
        dire("L'expert fixe la valeur à 3 200 €. Irréparable ? " + rapport.estEconomiquementIrreparable()
                + ". Montant proposé " + dossier.proposerIndemnisation(j("2026-10-20"))
                + " (la valeur déclarée de 5 000 € est sans effet)");
        dossier.contester();
        dire("M. Lambert conteste, contre-expertise à ses frais, état " + dossier.etat());
        dossier.recevoirRapport(new RapportExpertise(Montant.euros(3500), Montant.euros(4800)));
        dire("Contre-expertise, valeur 3 500 €, nouveau montant " + dossier.proposerIndemnisation(j("2026-10-28")));
        try {
            dossier.contester();
        } catch (RegleRefusee refus) {
            dire("Seconde contestation refusée. " + refus.getMessage());
        }
    }

    private static void leVolDeMmePerrin() {
        titre("Le vol de Mme Perrin");
        Couverture couverture = couverture("2023-05-02T10:00", 9000, Garantie.TIERS, Garantie.VOL);
        DossierSinistre dossier = DossierSinistre.ouvrir(new DossierId("PERRIN-1"), ANTOINE,
                new Declaration(NatureSinistre.VOL, h("2026-10-16T23:30"), j("2026-10-21"), false, true),
                couverture, HistoriqueSinistres.vide(), CALENDRIER);
        dire("Vol dans la nuit du vendredi au samedi, déclaré le mercredi. Retard de " + dossier.joursDeRetard()
                + " jour, dossier " + dossier.etat() + " (pas refusé d'office)");
        try {
            dossier.refuserPourRetard(new GestionnaireId("Claire"));
        } catch (RegleRefusee refus) {
            dire("Refus par un autre gestionnaire. " + refus.getMessage());
        }
        dire("Antoine, le gestionnaire du dossier, choisit de ne pas refuser, le dossier continue.");
        dossier.retrouverVehicule(Montant.euros(1200));
        dire("Voiture retrouvée abîmée, même dossier, 1 200 € de dommages, montant proposé "
                + dossier.proposerIndemnisation(j("2026-10-28")) + " (une seule franchise de 500 €)");
    }

    private static void leContratToutNeuf() {
        titre("Le contrat tout neuf");
        Couverture couverture = couverture("2026-09-02T10:00", 8000, Garantie.TIERS, Garantie.TOUS_RISQUES);
        DossierSinistre dossier = DossierSinistre.ouvrir(new DossierId("CARON-1"), ANTOINE,
                new Declaration(NatureSinistre.ACCROCHAGE, h("2026-09-14T16:00"), j("2026-09-14"), true, true),
                couverture, HistoriqueSinistres.vide(), CALENDRIER);
        dossier.estimerDommages(Montant.euros(1200));
        dossier.fixerResponsabilite(Responsabilite.AUCUNE, j("2026-09-15"));
        dire("Sinistre à 12 jours de la souscription. Suspendu ? " + dossier.estSuspendu());
        try {
            dossier.proposerIndemnisation(j("2026-09-16"));
        } catch (RegleRefusee refus) {
            dire("Refusé. " + refus.getMessage());
        }
        dossier.leverSuspension();
        dire("L'équipe vol et fraude lève la suspension, montant proposé "
                + dossier.proposerIndemnisation(j("2026-09-20")) + " (non responsable et tiers identifié, donc pas de franchise)");
    }

    private static Couverture couverture(String souscritLe, long valeurDeclaree, Garantie... garanties) {
        return Couverture.de(new PeriodeCouverture(h(souscritLe), h("2030-01-01T00:00")), Set.of(garanties),
                Montant.euros(valeurDeclaree));
    }

    private static LocalDateTime h(String instant) {
        return LocalDateTime.parse(instant);
    }

    private static LocalDate j(String jour) {
        return LocalDate.parse(jour);
    }

    private static void titre(String texte) {
        System.out.println();
        System.out.println("=== " + texte + " ===");
    }

    private static void dire(String texte) {
        System.out.println("  " + texte);
    }
}
