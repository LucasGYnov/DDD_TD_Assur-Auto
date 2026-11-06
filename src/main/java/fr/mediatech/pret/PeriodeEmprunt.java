package fr.mediatech.pret;

import java.time.LocalDate;

/**
 * La période d'un emprunt : une date de début et une date de retour prévue.
 * C'est un value object : immuable, égal par valeur, toujours valide.
 */
public record PeriodeEmprunt(LocalDate debut, LocalDate retourPrevu) {

    // Rappel : dans un record, le « constructeur compact » s'écrit sans parenthèses,
    // comme ci-dessous. Il sert à vérifier les valeurs : les champs sont affectés
    // automatiquement juste après.
    public PeriodeEmprunt {
        // TODO 1 : refuser une période invalide.
        //   - les deux dates sont obligatoires (Objects.requireNonNull)
        //   - la date de retour prévue doit être APRÈS la date de début
        // En cas de problème, lever une IllegalArgumentException.
        // Inspirez-vous de Isbn.java.
    }

    /**
     * Un emprunt est en retard si l'on a dépassé la date de retour prévue.
     * Le jour même du retour prévu, il est encore à l'heure.
     */
    public boolean estEnRetardLe(LocalDate jour) {
        // TODO 2
        throw new UnsupportedOperationException("À compléter");
    }

    /**
     * Renvoie une NOUVELLE période, dont le retour prévu est fixé
     * à « jourDeLaDemande + jours ». La période d'origine ne change pas.
     */
    public PeriodeEmprunt prolongee(LocalDate jourDeLaDemande, int jours) {
        // TODO 3 : créer et renvoyer une nouvelle PeriodeEmprunt.
        //   - la date de début ne change pas
        //   - le nouveau retour prévu = jourDeLaDemande + jours (méthode plusDays)
        throw new UnsupportedOperationException("À compléter");
    }
}
