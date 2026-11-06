package fr.mediatech.pret;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Le prêt d'un exemplaire à un adhérent.
 * C'est une entité : il a une identité et un cycle de vie (créé, prolongé, rendu).
 */
public class Emprunt {

    static final int DUREE_PROLONGATION_EN_JOURS = 14;

    private final EmpruntId id;
    private final Isbn titre;
    private PeriodeEmprunt periode;

    public Emprunt(EmpruntId id, Isbn titre, PeriodeEmprunt periode) {
        this.id = Objects.requireNonNull(id);
        this.titre = Objects.requireNonNull(titre);
        this.periode = Objects.requireNonNull(periode);
    }

    /**
     * Prolonge l'emprunt, si les règles de Mme Leroy le permettent.
     * Sinon, lève une ProlongationRefusee qui explique pourquoi.
     *
     * @param jourDeLaDemande        le jour où l'adhérent demande la prolongation
     * @param titreReserveParUnAutre vrai si un autre adhérent attend ce titre
     */
    public void prolonger(LocalDate jourDeLaDemande, boolean titreReserveParUnAutre) {
        // TODO 4 : appliquer les règles de Mme Leroy.
        //   4a. Refuser si l'emprunt a déjà été prolongé une fois.
        //       Indice : l'emprunt doit s'en souvenir. Ajoutez un champ.
        //   4b. Refuser si l'emprunt est déjà en retard le jour de la demande.
        //       Indice : PeriodeEmprunt sait le calculer (TODO 2).
        //   4c. Refuser si un autre adhérent attend le titre.
        //   4d. Sinon : remplacer la période par une période prolongée
        //       de DUREE_PROLONGATION_EN_JOURS (TODO 3), et retenir
        //       que l'emprunt a été prolongé.
        //
        // Pour refuser : throw new ProlongationRefusee("la raison, en langage métier");
        // Attention : vérifiez TOUTES les règles avant de modifier quoi que ce soit.
        throw new UnsupportedOperationException("À compléter");
    }

    public EmpruntId id() { return id; }
    public Isbn titre() { return titre; }
    public PeriodeEmprunt periode() { return periode; }

    // TODO 5 : un emprunt est une entité.
    // Redéfinir equals() et hashCode() en conséquence :
    // sur quoi deux emprunts doivent-ils être comparés ?
}
