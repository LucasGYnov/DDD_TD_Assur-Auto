package fr.mediatech.pret;

import java.util.Objects;

/**
 * L'ISBN d'un titre.
 * Exemple complet de value object : immuable, égal par valeur,
 * et impossible à créer avec une valeur invalide.
 */
public record Isbn(String valeur) {

    public Isbn {
        Objects.requireNonNull(valeur, "l'ISBN est obligatoire");
        valeur = valeur.replace("-", "");
        if (!valeur.matches("\\d{13}"))
            throw new IllegalArgumentException("un ISBN contient 13 chiffres : " + valeur);
        if (!cleDeControleValide(valeur))
            throw new IllegalArgumentException("clé de contrôle invalide : " + valeur);
    }

    private static boolean cleDeControleValide(String isbn) {
        int somme = 0;
        for (int i = 0; i < 12; i++) {
            int chiffre = isbn.charAt(i) - '0';
            somme += (i % 2 == 0) ? chiffre : chiffre * 3;
        }
        int cle = (10 - somme % 10) % 10;
        return cle == isbn.charAt(12) - '0';
    }
}
