package fr.mediatech.pret;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Lancez ce programme (clic droit > Run) après chaque TODO.
 * Objectif : toutes les lignes en OK. Ne modifiez pas ce fichier.
 */
public class Verifications {

    private static final Isbn DUNE = new Isbn("978-0-441-17271-9");
    private static final LocalDate LE_1ER_MARS = LocalDate.of(2026, 3, 1);
    private static final LocalDate LE_10_MARS = LocalDate.of(2026, 3, 10);
    private static final LocalDate LE_15_MARS = LocalDate.of(2026, 3, 15);
    private static final LocalDate LE_24_MARS = LocalDate.of(2026, 3, 24);

    private static int reussies = 0;
    private static int total = 0;

    public static void main(String[] args) {
        titre("PeriodeEmprunt (value object) - TODO 1 à 3");
        verifier("TODO 1", "refuse un retour prévu avant le début",
                () -> creationRefusee(() -> new PeriodeEmprunt(LE_15_MARS, LE_1ER_MARS)));
        verifier("TODO 1", "refuse un retour prévu le jour même du début",
                () -> creationRefusee(() -> new PeriodeEmprunt(LE_1ER_MARS, LE_1ER_MARS)));
        verifier("TODO 2", "n'est pas en retard le jour du retour prévu",
                () -> attendu(false, periode().estEnRetardLe(LE_15_MARS)));
        verifier("TODO 2", "est en retard le lendemain du retour prévu",
                () -> attendu(true, periode().estEnRetardLe(LE_15_MARS.plusDays(1))));
        verifier("TODO 3", "prolongee() repart du jour de la demande",
                () -> attendu(LE_24_MARS, periode().prolongee(LE_10_MARS, 14).retourPrevu()));
        verifier("TODO 3", "prolongee() ne modifie pas la période d'origine", () -> {
            PeriodeEmprunt origine = periode();
            origine.prolongee(LE_10_MARS, 14);
            attendu(LE_15_MARS, origine.retourPrevu());
        });
        verifier("record", "deux périodes aux mêmes dates sont égales",
                () -> attendu(periode(), periode()));

        titre("Emprunt (entité) - TODO 4 et 5");
        verifier("TODO 4", "une première prolongation donne 14 jours à partir de la demande", () -> {
            Emprunt emprunt = emprunt("E1");
            emprunt.prolonger(LE_10_MARS, false);
            attendu(LE_24_MARS, emprunt.periode().retourPrevu());
        });
        verifier("TODO 4", "une deuxième prolongation est refusée", () -> {
            Emprunt emprunt = emprunt("E1");
            emprunt.prolonger(LE_10_MARS, false);
            prolongationRefusee(() -> emprunt.prolonger(LocalDate.of(2026, 3, 20), false));
        });
        verifier("TODO 4", "un emprunt déjà en retard ne peut pas être prolongé",
                () -> prolongationRefusee(() -> emprunt("E1").prolonger(LE_15_MARS.plusDays(1), false)));
        verifier("TODO 4", "un titre attendu par un autre adhérent ne peut pas être prolongé",
                () -> prolongationRefusee(() -> emprunt("E1").prolonger(LE_10_MARS, true)));
        verifier("TODO 4", "une prolongation refusée ne change pas la date de retour", () -> {
            Emprunt emprunt = emprunt("E1");
            try {
                emprunt.prolonger(LE_10_MARS, true);
            } catch (ProlongationRefusee refus) {
                // attendu
            }
            attendu(LE_15_MARS, emprunt.periode().retourPrevu());
        });
        verifier("TODO 5", "deux emprunts de même identifiant sont le même emprunt", () -> {
            Emprunt avant = emprunt("E1");
            Emprunt apres = new Emprunt(new EmpruntId("E1"), DUNE,
                    new PeriodeEmprunt(LE_1ER_MARS, LE_24_MARS));
            attendu(avant, apres);
        });
        verifier("TODO 5", "deux emprunts d'identifiants différents sont différents", () -> {
            if (emprunt("E1").equals(emprunt("E2")))
                throw new AssertionError("ils devraient être différents");
        });

        System.out.println();
        System.out.println(reussies + " / " + total + " vérifications réussies");
        if (reussies == total)
            System.out.println("Bravo : la prolongation est enfin correcte !");
    }

    // ---------------------------------------------------------------- outils

    private interface Verification {
        void executer() throws Exception;
    }

    private static void titre(String texte) {
        System.out.println();
        System.out.println("=== " + texte + " ===");
    }

    private static void verifier(String todo, String description, Verification verification) {
        total++;
        String ligne = String.format("%-7s %s", todo, description);
        try {
            verification.executer();
            reussies++;
            System.out.println("  [ OK      ] " + ligne);
        } catch (UnsupportedOperationException aFaire) {
            System.out.println("  [ À FAIRE ] " + ligne);
        } catch (Throwable echec) {
            System.out.println("  [ ÉCHEC   ] " + ligne + "  ->  " + echec.getMessage());
        }
    }

    private static void attendu(Object attendu, Object obtenu) {
        if (!Objects.equals(attendu, obtenu))
            throw new AssertionError("attendu : " + attendu + ", obtenu : " + obtenu);
    }

    private static void creationRefusee(Runnable creation) {
        try {
            creation.run();
        } catch (IllegalArgumentException | NullPointerException refus) {
            return;
        }
        throw new AssertionError("la création aurait dû être refusée");
    }

    private static void prolongationRefusee(Runnable prolongation) {
        try {
            prolongation.run();
        } catch (ProlongationRefusee refus) {
            return;
        }
        throw new AssertionError("la prolongation aurait dû être refusée");
    }

    private static PeriodeEmprunt periode() {
        return new PeriodeEmprunt(LE_1ER_MARS, LE_15_MARS);
    }

    private static Emprunt emprunt(String id) {
        return new Emprunt(new EmpruntId(id), DUNE, periode());
    }
}
