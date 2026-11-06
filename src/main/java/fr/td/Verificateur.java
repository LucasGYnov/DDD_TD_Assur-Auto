package fr.td;

import java.util.Objects;

/**
 * L'outil de vérification. Vous n'avez pas besoin de le modifier :
 * écrivez vos vérifications dans Verifications.java.
 *
 * Trois résultats possibles pour chaque vérification :
 *   [ OK      ]  la règle est respectée
 *   [ ÉCHEC   ]  la règle n'est pas respectée (le message dit pourquoi)
 *   [ À FAIRE ]  le code lève UnsupportedOperationException : il n'est pas encore écrit
 */
public final class Verificateur {

    /** Le contenu d'une vérification : du code qui lève une erreur si la règle n'est pas respectée. */
    public interface Verification {
        void executer() throws Exception;
    }

    private static int reussies = 0;
    private static int total = 0;

    private Verificateur() {
    }

    /** Affiche un titre, pour regrouper les vérifications (par exemple par classe). */
    public static void titre(String texte) {
        System.out.println();
        System.out.println("=== " + texte + " ===");
    }

    /**
     * Lance une vérification et affiche son résultat.
     *
     * @param code        le code de la règle ou de l'hypothèse vérifiée (« G4 », « H2 »…)
     * @param description ce que la vérification montre, en une phrase, avec les mots du glossaire
     */
    public static void verifier(String code, String description, Verification verification) {
        total++;
        String ligne = String.format("%-6s %s", code, description);
        try {
            verification.executer();
            reussies++;
            System.out.println("  [ OK      ] " + ligne);
        } catch (UnsupportedOperationException pasEncoreEcrit) {
            System.out.println("  [ À FAIRE ] " + ligne);
        } catch (Throwable echec) {
            System.out.println("  [ ÉCHEC   ] " + ligne + "  ->  " + echec.getMessage());
        }
    }

    /** Vérifie qu'une valeur est celle attendue (avec equals). */
    public static void attendu(Object attendu, Object obtenu) {
        if (!Objects.equals(attendu, obtenu))
            throw new AssertionError("attendu : " + attendu + ", obtenu : " + obtenu);
    }

    /** Vérifie que deux objets sont différents (avec equals). */
    public static void differents(Object premier, Object second) {
        if (Objects.equals(premier, second))
            throw new AssertionError("ils devraient être différents : " + premier + " et " + second);
    }

    /**
     * Vérifie qu'une action est refusée : elle doit lever une exception.
     * N'importe quelle exception convient (IllegalArgumentException,
     * IllegalStateException, ou une exception à vous), sauf
     * UnsupportedOperationException, qui veut dire « pas encore écrit ».
     */
    public static void refuse(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException pasEncoreEcrit) {
            throw pasEncoreEcrit;
        } catch (RuntimeException refus) {
            return;
        }
        throw new AssertionError("l'action aurait dû être refusée");
    }

    /** Affiche le bilan. À appeler une fois, à la fin. */
    public static void bilan() {
        System.out.println();
        System.out.println(reussies + " / " + total + " vérifications réussies");
    }
}