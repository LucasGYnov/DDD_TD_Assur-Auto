package fr.td.domaine;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Value object, ce que l'assuré (ou l'agence) déclare, c'est-à-dire la nature, quand c'est arrivé et quand il l'a dit.
 * H1, « conducteurDeclare » est noté mais ne change pas la couverture.
 */
public record Declaration(NatureSinistre nature, LocalDateTime survenuLe, LocalDate declareLe,
                          boolean tiersIdentifie, boolean conducteurDeclare) {

    public Declaration {
        if (declareLe.isBefore(survenuLe.toLocalDate())) {
            throw new IllegalArgumentException("on ne déclare pas un sinistre avant qu'il ait eu lieu");
        }
    }
}
