package fr.td.domaine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Entité, un dossier sinistre, identifié par son DossierId. Un dossier par sinistre, suivi de bout en
 * bout par un seul gestionnaire (A15). L'égalité repose sur l'identifiant.
 *
 * C'est la v1, elle porte presque toutes les règles du traitement d'un sinistre (A2, A3, A4, A7 à A12,
 * A16, A18). Les systèmes externes ne sont pas codés, leurs réponses arrivent en paramètres
 * (Couverture pour ContratPlus, RapportExpertise pour Expertis, jours fériés pour JoursOuvres).
 */
public class DossierSinistre {

    private static final Montant SEUIL_EXPERTISE = Montant.euros(1500);                 // A7
    private static final int DELAI_DE_DECLARATION_EN_JOURS_OUVRES = 5;                  // A2
    private static final int DELAI_DE_DECLARATION_DU_VOL_EN_JOURS_OUVRES = 2;           // A2
    private static final long JOURS_APRES_SOUSCRIPTION_SUSPECTS = 15;                   // A12
    private static final long SINISTRES_SUSPECTS_SUR_DOUZE_MOIS = 3;                    // A12
    private static final int JOURS_POUR_VERSER = 30;                                    // A10
    private static final int JOURS_POUR_REPONDRE_A_UNE_PROPOSITION = 30;                // H2

    private final DossierId id;
    private final GestionnaireId gestionnaire;
    private final Declaration declaration;
    private final Couverture couverture;
    private final Garantie garantie;
    private final int joursDeRetard;

    private EtatDossier etat = EtatDossier.OUVERT;
    private boolean suspendu;

    private Responsabilite responsabilite;
    private LocalDate responsabiliteFixeeLe;
    private boolean comptabiliseAuBonusMalus;

    private Montant dommagesEstimes;
    private RapportExpertise rapport;
    private boolean contreExpertiseAttendue;
    private boolean reparateurAgree;

    private Montant montantPropose;
    private LocalDate proposeLe;
    private LocalDate accordLe;
    private boolean contesteUneFois;

    private DossierSinistre(DossierId id, GestionnaireId gestionnaire, Declaration declaration, Couverture couverture,
                            Garantie garantie, int joursDeRetard, boolean suspendu) {
        this.id = Objects.requireNonNull(id);
        this.gestionnaire = gestionnaire;
        this.declaration = declaration;
        this.couverture = couverture;
        this.garantie = garantie;
        this.joursDeRetard = joursDeRetard;
        this.suspendu = suspendu;
        if (declaration.nature() == NatureSinistre.VOL || declaration.nature() == NatureSinistre.BRIS_DE_GLACE) {
            this.responsabilite = Responsabilite.AUCUNE;
            this.responsabiliteFixeeLe = declaration.declareLe();
        }
    }

    /**
     * Ouvre le dossier d'un sinistre déclaré. Refuse si le sinistre n'est pas couvert (A3).
     * Une déclaration en retard n'est pas refusée, le retard est noté (A2).
     * Un sinistre suspect ouvre un dossier dont l'indemnisation est suspendue (A12).
     * Le gestionnaire prend le dossier en charge, et c'est refusé s'il en a déjà 60 (A15).
     */
    public static DossierSinistre ouvrir(DossierId id, Gestionnaire gestionnaire, Declaration declaration,
                                         Couverture couverture, HistoriqueSinistres historique,
                                         JoursOuvres joursOuvres) {
        LocalDateTime survenuLe = declaration.survenuLe();
        if (!couverture.estActiveLe(survenuLe)) {
            throw new RegleRefusee("A3", "le contrat n'était pas actif le " + survenuLe);
        }
        Garantie garantie = couverture.garantiePour(declaration.nature());

        LocalDate jour = survenuLe.toLocalDate();
        boolean vol = declaration.nature() == NatureSinistre.VOL;
        int delai = vol ? DELAI_DE_DECLARATION_DU_VOL_EN_JOURS_OUVRES : DELAI_DE_DECLARATION_EN_JOURS_OUVRES;
        int retard = Math.max(0, joursOuvres.ecoulesEntre(jour, declaration.declareLe()) - delai);

        boolean suspect = couverture.periode().joursDepuisSouscription(jour) < JOURS_APRES_SOUSCRIPTION_SUSPECTS
                || historique.nombreSurDouzeMoisJusqua(jour) >= SINISTRES_SUSPECTS_SUR_DOUZE_MOIS - 1;

        gestionnaire.prendreEnCharge(id);
        return new DossierSinistre(id, gestionnaire.id(), declaration, couverture, garantie, retard, suspect);
    }

    // ---- Instruction -------------------------------------------------------------------------

    public void estimerDommages(Montant dommages) {
        exigerEtat(EtatDossier.OUVERT);
        this.dommagesEstimes = Objects.requireNonNull(dommages);
    }

    /** Un véhicule volé puis retrouvé abîmé reste le même sinistre, le même dossier, la même franchise. */
    public void retrouverVehicule(Montant dommagesConstates) {
        if (declaration.nature() != NatureSinistre.VOL) {
            throw new IllegalStateException("seul un véhicule volé peut être retrouvé");
        }
        exigerEtat(EtatDossier.OUVERT);
        this.dommagesEstimes = Objects.requireNonNull(dommagesConstates);
    }

    /** A4, la part de responsabilité est fixée d'après le constat, ou par le gestionnaire sans constat. */
    public void fixerResponsabilite(Responsabilite part, LocalDate le) {
        if (declaration.nature() == NatureSinistre.VOL || declaration.nature() == NatureSinistre.BRIS_DE_GLACE) {
            throw new IllegalStateException("Un vol ou un bris de glace est toujours à 0 % de responsabilité");
        }
        this.responsabilite = Objects.requireNonNull(part);
        this.responsabiliteFixeeLe = Objects.requireNonNull(le);
    }

    /** A7, une expertise est obligatoire au-delà de 1 500 € de dommages estimés (strictement plus). */
    public boolean expertiseObligatoire() {
        return dommagesEstimes != null && dommagesEstimes.estSuperieurA(SEUIL_EXPERTISE);
    }

    public void recevoirRapport(RapportExpertise nouveauRapport) {
        this.rapport = Objects.requireNonNull(nouveauRapport);
        this.contreExpertiseAttendue = false;
    }

    /** A16, chez un réparateur agréé, l'assuré n'avance rien et l'indemnité part directement au garage. */
    public void choisirReparateurAgree() {
        exigerEtat(EtatDossier.OUVERT);
        this.reparateurAgree = true;
    }

    public Beneficiaire beneficiaireDeLIndemnite() {
        return reparateurAgree ? Beneficiaire.REPARATEUR_AGREE : Beneficiaire.ASSURE;
    }

    // ---- Indemnisation -----------------------------------------------------------------------

    /**
     * Calcule le montant proposé à l'assuré.
     * A12, refusé tant que l'indemnisation est suspendue. A7, refusé sans expertise quand elle est obligatoire.
     * A8, jamais plus que la valeur du véhicule (celle de l'expert, sinon celle déclarée, H6). Si la
     * réparation coûte plus, c'est la valeur moins la franchise. A9, avec la seule garantie tiers,
     * seule la part non responsable est indemnisée. A5 et A6, la franchise. En tous risques, la
     * responsabilité ne réduit pas l'indemnité.
     */
    public Montant proposerIndemnisation(LocalDate le) {
        if (suspendu) {
            throw new RegleRefusee("A12", "l'indemnisation est suspendue, le dossier est à l'équipe vol et fraude");
        }
        if (etat != EtatDossier.OUVERT && etat != EtatDossier.MONTANT_CONTESTE) {
            throw new IllegalStateException("on ne propose pas de montant à un dossier " + etat);
        }
        if (dommagesEstimes == null) {
            throw new IllegalStateException("il faut d'abord estimer les dommages");
        }
        if (responsabilite == null) {
            throw new IllegalStateException("il faut d'abord fixer la responsabilité (A4)");
        }
        if (expertiseObligatoire() && rapport == null) {
            throw new RegleRefusee("A7", "dommages estimés à " + dommagesEstimes + ", une expertise est obligatoire");
        }
        if (contreExpertiseAttendue) {
            throw new IllegalStateException("la contre-expertise demandée n'est pas encore reçue");
        }

        Montant degats = rapport != null ? rapport.coutReparation() : dommagesEstimes;
        Montant plafond = rapport != null ? rapport.valeurVehicule() : couverture.valeurDeclaree();
        Montant base = degats.plusPetitEntre(plafond);
        if (garantie == Garantie.TIERS) {
            base = base.part(responsabilite.partNonResponsable());
        }
        Franchise franchise = Franchise.pour(garantie, reparateurAgree, responsabilite, declaration.tiersIdentifie());

        this.montantPropose = base.retrancher(franchise.montant());
        this.proposeLe = Objects.requireNonNull(le);
        this.etat = EtatDossier.MONTANT_PROPOSE;
        return montantPropose;
    }

    /**
     * L'assuré accepte le montant. H2, le silence n'est pas un accord. Une proposition qui reste sans
     * réponse plus de 30 jours est caduque, et on ne peut plus l'accepter.
     */
    public void accepterMontant(LocalDate le) {
        exigerEtat(EtatDossier.MONTANT_PROPOSE);
        if (le.isAfter(proposeLe.plusDays(JOURS_POUR_REPONDRE_A_UNE_PROPOSITION))) {
            throw new RegleRefusee("H2", "la proposition du " + proposeLe + " est caduque");
        }
        this.accordLe = le;
        this.etat = EtatDossier.MONTANT_ACCEPTE;
    }

    /** A10, l'indemnité est versée au plus tard 30 jours après l'accord de l'assuré. */
    public LocalDate dateLimiteDeVersement() {
        if (accordLe == null) {
            throw new IllegalStateException("A10, les 30 jours ne courent qu'à partir de l'accord de l'assuré");
        }
        return accordLe.plusDays(JOURS_POUR_VERSER);
    }

    /**
     * A11, l'assuré conteste le montant une seule fois, et demande une contre-expertise à ses frais.
     * Le montant qui suit la contre-expertise est définitif.
     */
    public void contester() {
        exigerEtat(EtatDossier.MONTANT_PROPOSE);
        if (contesteUneFois) {
            throw new RegleRefusee("A11", "le montant a déjà été contesté une fois");
        }
        this.contesteUneFois = true;
        this.contreExpertiseAttendue = true;
        this.etat = EtatDossier.MONTANT_CONTESTE;
    }

    public void verser() {
        if (suspendu) {
            throw new RegleRefusee("A12", "l'indemnisation est suspendue");
        }
        exigerEtat(EtatDossier.MONTANT_ACCEPTE);
        this.etat = EtatDossier.INDEMNISE;
    }

    // ---- Décisions du gestionnaire et de l'équipe vol et fraude ------------------------------

    /** A2, un retard ne se refuse pas d'office, et seul le gestionnaire du dossier peut décider d'un refus. */
    public void refuserPourRetard(GestionnaireId decideur) {
        exigerEtat(EtatDossier.OUVERT);
        if (!gestionnaire.equals(decideur)) {
            throw new RegleRefusee("A2", "seul le gestionnaire du dossier peut décider d'un refus");
        }
        if (joursDeRetard == 0) {
            throw new RegleRefusee("A2", "la déclaration n'est pas en retard");
        }
        this.etat = EtatDossier.REFUSE;
    }

    /** A12, l'équipe vol et fraude lève la suspension, et l'indemnisation peut reprendre. */
    public void leverSuspension() {
        this.suspendu = false;
    }

    /** A18, doublon ou déclaration retirée, le dossier ne compte plus ni pour le bonus-malus ni pour A12. */
    public void classerSansSuite() {
        if (etat == EtatDossier.INDEMNISE) {
            throw new IllegalStateException("un dossier indemnisé ne se classe pas sans suite");
        }
        this.etat = EtatDossier.CLASSE_SANS_SUITE;
    }

    // ---- Bonus-malus et historique -----------------------------------------------------------

    /** A12, A18, ce dossier entre dans l'historique d'un contrat sauf s'il est classé sans suite. */
    public boolean estRetenuPourLHistorique() {
        return etat != EtatDossier.CLASSE_SANS_SUITE;
    }

    /**
     * A14, H3, la responsabilité de ce dossier compte pour le bonus-malus de l'échéance, mais seulement si elle
     * était fixée avant cette échéance. Sinon, le dossier attend l'échéance suivante.
     */
    public Optional<Responsabilite> responsabiliteRetenuePourLEcheance(LocalDate echeance) {
        if (!estRetenuPourLHistorique() || comptabiliseAuBonusMalus
                || responsabiliteFixeeLe == null || responsabiliteFixeeLe.isAfter(echeance)) {
            return Optional.empty();
        }
        return Optional.of(responsabilite);
    }

    public void comptabiliserAuBonusMalus() {
        this.comptabiliseAuBonusMalus = true;
    }

    // ---- Consultation ------------------------------------------------------------------------

    public DossierId id() {
        return id;
    }

    public GestionnaireId gestionnaire() {
        return gestionnaire;
    }

    public EtatDossier etat() {
        return etat;
    }

    public Garantie garantie() {
        return garantie;
    }

    public int joursDeRetard() {
        return joursDeRetard;
    }

    public boolean estSuspendu() {
        return suspendu;
    }

    public boolean conducteurNonDeclare() {
        return !declaration.conducteurDeclare();
    }

    public LocalDate survenuLe() {
        return declaration.survenuLe().toLocalDate();
    }

    public Montant montantPropose() {
        return montantPropose;
    }

    private void exigerEtat(EtatDossier attendu) {
        if (etat != attendu) {
            throw new IllegalStateException("le dossier est " + etat + ", il devrait être " + attendu);
        }
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof DossierSinistre d && id.equals(d.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Dossier " + id + " (" + declaration.nature() + ", " + etat + ")";
    }
}
