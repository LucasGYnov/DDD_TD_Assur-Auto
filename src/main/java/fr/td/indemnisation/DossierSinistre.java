package fr.td.indemnisation;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Agrégat principal du core Indemnisation : un dossier sinistre, un par sinistre.
 * Racine : DossierSinistre, identifié par DossierId.
 *
 * Tout est privé, sans setter : on ne le change qu'avec une commande, qui vérifie les invariants
 * et refuse (RegleRefusee) ce qui les violerait. Chaque commande acceptée renvoie son événement.
 * Les autres agrégats ne sont connus que par leur identifiant (GestionnaireId, ContratId) ;
 * ce qui vient d'ailleurs (le Contrat, la conclusion d'expertise, la date du jour) arrive en paramètre.
 *
 * Invariants protégés (section 6 du dossier) :
 * A15 un seul gestionnaire (H16) · A3, A17 pas de montant pour un sinistre non couvert ·
 * A4 pas de montant avant la responsabilité fixée · A7 au-delà de 1 500 €, pas de montant sans expertise (H6) ·
 * A8 jamais plus que la valeur du véhicule, jamais négatif (H9) · A12 rien de proposé ni d'accepté pendant la suspension ·
 * A11 une seule contestation, puis une proposition définitive (H15) · A2 seul son gestionnaire le refuse pour retard ·
 * A18 classé sans suite : plus aucune commande, sauf sa clôture.
 */
public class DossierSinistre {

    /** A7, H6 : l'expertise est obligatoire au-delà de 1 500 € (strictement). */
    public static final Montant SEUIL_D_EXPERTISE = Montant.euros(1500);
    /** A10 : l'indemnité est versée sous 30 jours après l'accord de l'assuré. */
    public static final int JOURS_POUR_VERSER = 30;

    private final DossierId id;
    private final ContratId contrat;
    private final LocalDate dateDuSinistre;
    private final Garantie garantie;
    private final boolean retardDeDeclaration;

    private EtatDuDossier etat = EtatDuDossier.OUVERT;
    private GestionnaireId gestionnaire;
    private Responsabilite responsabilite;
    private boolean tiersIdentifie;
    private Montant estimationDesDommages;
    private ConclusionDExpertise conclusionDExpertise;
    private Montant montantPropose;
    private boolean indemnisationSuspendue;
    private boolean dejaConteste;

    /**
     * Ouvre le dossier sinistre, à partir de « Sinistre déclaré » (contexte Déclaration) :
     * le retard éventuel a été noté par la Déclaration (A2, H5).
     */
    public DossierSinistre(DossierId id, ContratId contrat, LocalDate dateDuSinistre,
                           Garantie garantie, boolean retardDeDeclaration) {
        this.id = Objects.requireNonNull(id);
        this.contrat = Objects.requireNonNull(contrat);
        this.dateDuSinistre = Objects.requireNonNull(dateDuSinistre);
        this.garantie = Objects.requireNonNull(garantie);
        this.retardDeDeclaration = retardDeDeclaration;
    }

    // ------------------------------------------------------------------ commandes

    /**
     * A15, H16 : un dossier sinistre a un seul gestionnaire, qui ne change pas jusqu'à la clôture.
     * Reçue de la policy « Quand un gestionnaire est affecté, alors l'inscrire sur le dossier sinistre » :
     * l'événement « Gestionnaire affecté » est déjà produit par le Gestionnaire, le dossier ne fait que l'inscrire.
     */
    public void affecterLeGestionnaire(GestionnaireId nouveau) {
        Objects.requireNonNull(nouveau);
        exigerEnCours();
        if (gestionnaire != null && !gestionnaire.equals(nouveau)) {
            throw new RegleRefusee("A15", "le dossier sinistre " + id + " a déjà un gestionnaire (" + gestionnaire
                    + ") : il le garde jusqu'à la clôture");
        }
        gestionnaire = nouveau;
    }

    /** A3, A17 : couvert si le contrat était actif à la date du sinistre et la garantie concernée souscrite. */
    public VerificationDeLaCouverture verifierLaCouverture(Contrat contratRecu) {
        Objects.requireNonNull(contratRecu);
        exigerEnCours();
        if (etat != EtatDuDossier.OUVERT) {
            throw new RegleRefusee("A3", "la couverture du dossier sinistre " + id + " a déjà été vérifiée");
        }
        if (!contratRecu.id().equals(contrat)) {
            throw new RegleRefusee("A3", "le contrat " + contratRecu.id() + " n'est pas celui du dossier sinistre " + id);
        }
        if (contratRecu.couvre(dateDuSinistre, garantie)) {
            etat = EtatDuDossier.EN_INSTRUCTION;
            return new VerificationDeLaCouverture.CouvertureConfirmee(id, contrat, garantie);
        }
        etat = EtatDuDossier.COUVERTURE_REFUSEE;
        return new VerificationDeLaCouverture.CouvertureRefusee(id, contrat, garantie, dateDuSinistre);
    }

    /** A2 : une déclaration en retard n'est jamais refusée d'office ; seul le gestionnaire du dossier peut la refuser. */
    public DeclarationRefuseePourRetard refuserPourRetard(GestionnaireId parQui) {
        exigerEnCours();
        if (etat == EtatDuDossier.MONTANT_ACCEPTE) {
            throw new RegleRefusee("A10", "le montant est déjà accepté : l'indemnité est due");
        }
        if (!retardDeDeclaration) {
            throw new RegleRefusee("A2", "aucun retard de déclaration n'a été noté sur le dossier sinistre " + id);
        }
        if (gestionnaire == null || !gestionnaire.equals(parQui)) {
            throw new RegleRefusee("A2", "seul le gestionnaire du dossier sinistre " + id + " peut le refuser pour retard");
        }
        etat = EtatDuDossier.REFUSE_POUR_RETARD;
        return new DeclarationRefuseePourRetard(id, parQui);
    }

    /** A4 : la responsabilité est fixée d'après le constat amiable, ou par le gestionnaire (0, 50 ou 100 %). */
    public ResponsabiliteFixee fixerLaResponsabilite(Responsabilite fixee, boolean tiersEstIdentifie) {
        Objects.requireNonNull(fixee);
        exigerEnInstruction("A4", "fixer la responsabilité");
        responsabilite = fixee;
        tiersIdentifie = tiersEstIdentifie;
        return new ResponsabiliteFixee(id, contrat, dateDuSinistre, fixee, tiersEstIdentifie);
    }

    /** A7, H6 : l'estimation décide si une expertise est obligatoire (plus de 1 500 €). */
    public DommagesEstimes estimerLesDommages(Montant estimation) {
        Objects.requireNonNull(estimation);
        exigerEnInstruction("A7", "estimer les dommages");
        estimationDesDommages = estimation;
        return new DommagesEstimes(id, estimation, estimation.estSuperieurA(SEUIL_D_EXPERTISE));
    }

    /**
     * A7, A8 : la conclusion d'expertise arrive traduite par l'ACL Expertis.
     * Après une contestation, c'est la contre-expertise : elle remplace la première (H15).
     */
    public RapportDExpertiseRecu recevoirLaConclusionDExpertise(ConclusionDExpertise conclusion) {
        Objects.requireNonNull(conclusion);
        exigerEnCours();
        if (etat != EtatDuDossier.EN_INSTRUCTION && etat != EtatDuDossier.MONTANT_CONTESTE) {
            throw new RegleRefusee("A7", "le dossier sinistre " + id + " n'attend pas de rapport d'expertise (état "
                    + etat + ")");
        }
        boolean contreExpertise = etat == EtatDuDossier.MONTANT_CONTESTE;
        conclusionDExpertise = conclusion;
        return new RapportDExpertiseRecu(id, conclusion, contreExpertise);
    }

    /** A12 : le dossier passe à l'équipe vol et fraude, l'indemnisation est suspendue (avant l'acceptation). */
    public IndemnisationSuspendue suspendreLIndemnisation() {
        exigerEnCours();
        if (etat == EtatDuDossier.MONTANT_ACCEPTE) {
            throw new RegleRefusee("A12", "le montant est déjà accepté : l'indemnisation ne peut plus être suspendue");
        }
        if (indemnisationSuspendue) {
            throw new RegleRefusee("A12", "l'indemnisation du dossier sinistre " + id + " est déjà suspendue");
        }
        indemnisationSuspendue = true;
        return new IndemnisationSuspendue(id);
    }

    /** A12 : l'équipe vol et fraude lève la suspension, l'indemnisation reprend. */
    public SuspensionLevee leverLaSuspension() {
        exigerEnCours();
        if (!indemnisationSuspendue) {
            throw new RegleRefusee("A12", "l'indemnisation du dossier sinistre " + id + " n'est pas suspendue");
        }
        indemnisationSuspendue = false;
        return new SuspensionLevee(id);
    }

    /**
     * Calcule l'indemnité et la propose à l'assuré.
     * La franchise vient du domain service, avec les informations du dossier (garantie, responsabilité,
     * tiers identifié) et celle qu'il n'a pas (le réparateur est-il agréé ?).
     * A3, A4, A7, A8, A9, A12, A11 (H6, H9, H12, H13, H15).
     */
    public MontantPropose proposerUnMontant(CalculDeLaFranchise calcul, boolean reparateurAgree, LocalDate jour) {
        Objects.requireNonNull(calcul);
        Objects.requireNonNull(jour);
        exigerEnCours();
        if (etat == EtatDuDossier.OUVERT) {
            throw new RegleRefusee("A3", "la couverture du dossier sinistre " + id + " n'est pas confirmée");
        }
        if (etat != EtatDuDossier.EN_INSTRUCTION && etat != EtatDuDossier.MONTANT_CONTESTE) {
            throw new RegleRefusee("A11", "un montant a déjà été proposé sur le dossier sinistre " + id
                    + " (état " + etat + ")");
        }
        if (indemnisationSuspendue) {
            throw new RegleRefusee("A12", "l'indemnisation du dossier sinistre " + id + " est suspendue");
        }
        if (responsabilite == null) {
            throw new RegleRefusee("A4", "la responsabilité doit être fixée avant de proposer un montant");
        }
        if (estimationDesDommages == null) {
            throw new RegleRefusee("A7", "les dommages doivent être estimés avant de proposer un montant");
        }
        if (estimationDesDommages.estSuperieurA(SEUIL_D_EXPERTISE) && conclusionDExpertise == null) {
            throw new RegleRefusee("A7", "dommages estimés à " + estimationDesDommages
                    + " : une expertise est obligatoire avant de proposer un montant");
        }

        Montant franchise = calcul.franchise(garantie, responsabilite, tiersIdentifie, reparateurAgree);
        Montant indemnite = dommagesIndemnisables().retrancher(franchise);       // H9 : jamais négatif
        if (conclusionDExpertise != null) {
            indemnite = indemnite.plusPetitEntre(conclusionDExpertise.valeurDuVehicule()); // A8 : jamais plus
        }

        montantPropose = indemnite;
        etat = EtatDuDossier.MONTANT_PROPOSE;
        return new MontantPropose(id, indemnite, jour, dejaConteste);
    }

    /** A10 : l'assuré accepte ; l'indemnité doit être versée sous 30 jours. */
    public MontantAccepte accepterLeMontant(LocalDate jour) {
        Objects.requireNonNull(jour);
        exigerEnCours();
        if (etat != EtatDuDossier.MONTANT_PROPOSE) {
            throw new RegleRefusee("A10", "aucun montant n'attend l'accord de l'assuré sur le dossier sinistre " + id);
        }
        if (indemnisationSuspendue) {
            throw new RegleRefusee("A12", "l'indemnisation du dossier sinistre " + id + " est suspendue");
        }
        etat = EtatDuDossier.MONTANT_ACCEPTE;
        return new MontantAccepte(id, montantPropose, jour, jour.plusDays(JOURS_POUR_VERSER));
    }

    /** A11 : l'assuré conteste le montant, une seule fois. */
    public MontantConteste contesterLeMontant(LocalDate jour) {
        Objects.requireNonNull(jour);
        exigerEnCours();
        if (etat != EtatDuDossier.MONTANT_PROPOSE) {
            throw new RegleRefusee("A11", "aucun montant à contester sur le dossier sinistre " + id);
        }
        if (dejaConteste) {
            throw new RegleRefusee("A11", "le montant ne peut être contesté qu'une seule fois (H15 : la nouvelle "
                    + "proposition est définitive)");
        }
        dejaConteste = true;
        etat = EtatDuDossier.MONTANT_CONTESTE;
        return new MontantConteste(id, montantPropose, jour);
    }

    /** A18 : doublon ou déclaration retirée ; le dossier ne comptera ni pour le bonus-malus ni pour A12. */
    public DossierClasseSansSuite classerSansSuite() {
        exigerEnCours();
        if (etat == EtatDuDossier.MONTANT_ACCEPTE) {
            throw new RegleRefusee("A10", "le montant est déjà accepté : l'indemnité est due");
        }
        etat = EtatDuDossier.CLASSE_SANS_SUITE;
        return new DossierClasseSansSuite(id, contrat);
    }

    /**
     * Clôture le dossier sinistre : une fois l'indemnité versée (montant accepté),
     * ou quand il est fermé sans indemnité. A15 : il a toujours son gestionnaire à ce moment-là.
     */
    public DossierSinistreCloture cloturer() {
        if (etat != EtatDuDossier.MONTANT_ACCEPTE && !etat.estFermeSansIndemnite()) {
            throw new RegleRefusee("A15", "le dossier sinistre " + id + " ne peut pas être clôturé dans l'état " + etat);
        }
        if (gestionnaire == null) {
            throw new RegleRefusee("A15", "le dossier sinistre " + id + " n'a pas de gestionnaire pour le clôturer");
        }
        etat = EtatDuDossier.CLOTURE;
        return new DossierSinistreCloture(id, gestionnaire);
    }

    // ------------------------------------------------------------------ règles internes

    /**
     * Ce qui est indemnisable avant la franchise.
     * Avec une expertise : la valeur du véhicule s'il est irréparable, sinon le coût de la réparation (A8).
     * Sans expertise : l'estimation des dommages (H6).
     * Avec la seule garantie tiers : la part non responsable seulement (A9, H13).
     */
    private Montant dommagesIndemnisables() {
        Montant dommages;
        if (conclusionDExpertise instanceof ConclusionDExpertise.VehiculeEconomiquementIrreparable irreparable) {
            dommages = irreparable.valeurDuVehicule();
        } else if (conclusionDExpertise != null) {
            dommages = conclusionDExpertise.coutDeLaReparation();
        } else {
            dommages = estimationDesDommages;
        }
        if (garantie == Garantie.TIERS) {
            dommages = dommages.part(responsabilite.partNonResponsable());
        }
        return dommages;
    }

    /** Un dossier clôturé ou fermé sans indemnité n'accepte plus aucune commande (sauf sa clôture). */
    private void exigerEnCours() {
        switch (etat) {
            case CLOTURE -> throw new RegleRefusee("A15", "le dossier sinistre " + id + " est clôturé");
            case CLASSE_SANS_SUITE -> throw new RegleRefusee("A18", "le dossier sinistre " + id + " est classé sans suite");
            case COUVERTURE_REFUSEE -> throw new RegleRefusee("A3", "le sinistre du dossier " + id + " n'est pas couvert");
            case REFUSE_POUR_RETARD -> throw new RegleRefusee("A2", "le dossier sinistre " + id + " a été refusé pour retard");
            default -> { }
        }
    }

    private void exigerEnInstruction(String regle, String action) {
        exigerEnCours();
        if (etat == EtatDuDossier.OUVERT) {
            throw new RegleRefusee("A3", "la couverture du dossier sinistre " + id + " n'est pas confirmée : impossible de " + action);
        }
        if (etat != EtatDuDossier.EN_INSTRUCTION) {
            throw new RegleRefusee(regle, "impossible de " + action + " : un montant a déjà été proposé sur le dossier sinistre " + id);
        }
    }

    // ------------------------------------------------------------------ lecture seule

    public DossierId id() {
        return id;
    }

    public EtatDuDossier etat() {
        return etat;
    }

    public Optional<GestionnaireId> gestionnaire() {
        return Optional.ofNullable(gestionnaire);
    }

    public Optional<Responsabilite> responsabilite() {
        return Optional.ofNullable(responsabilite);
    }

    public Optional<Montant> montantPropose() {
        return Optional.ofNullable(montantPropose);
    }

    public boolean indemnisationSuspendue() {
        return indemnisationSuspendue;
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
        return "Dossier sinistre " + id + " (" + etat + ")";
    }
}
