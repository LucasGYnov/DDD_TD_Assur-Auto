package fr.td.indemnisation;

/**
 * Les états du dossier sinistre (son cycle de vie, section 6 du dossier).
 * Ouvert → En instruction → Montant proposé → (Montant contesté → Montant proposé) → Montant accepté → Clôturé.
 * Sorties sans indemnité : couverture refusée, refusé pour retard, classé sans suite (puis clôturé).
 * La suspension (A12) n'est pas un état : c'est une parenthèse, portée à part par le dossier.
 */
public enum EtatDuDossier {
    OUVERT,
    EN_INSTRUCTION,
    MONTANT_PROPOSE,
    MONTANT_CONTESTE,
    MONTANT_ACCEPTE,
    COUVERTURE_REFUSEE,
    REFUSE_POUR_RETARD,
    CLASSE_SANS_SUITE,
    CLOTURE;

    /** Fermé sans indemnité : il n'attend plus que sa clôture. */
    boolean estFermeSansIndemnite() {
        return this == COUVERTURE_REFUSEE || this == REFUSE_POUR_RETARD || this == CLASSE_SANS_SUITE;
    }
}
