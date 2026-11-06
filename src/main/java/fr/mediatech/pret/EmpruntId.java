package fr.mediatech.pret;

import java.util.Objects;

/**
 * L'identifiant d'un emprunt.
 * Value object fourni complet : un identifiant vide ne peut pas exister.
 */
public record EmpruntId(String valeur) {

    public EmpruntId {
        Objects.requireNonNull(valeur, "l'identifiant est obligatoire");
        if (valeur.isBlank())
            throw new IllegalArgumentException("l'identifiant ne peut pas être vide");
    }
}
