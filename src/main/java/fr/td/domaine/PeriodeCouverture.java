package fr.td.domaine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Value object, la période pendant laquelle un contrat est actif (A3), de la souscription à la fin.
 * Un contrat n'est actif qu'à partir de l'heure de sa souscription, parce que la priorité 1
 * est de ne jamais payer plus que le contrat ne prévoit, un accident survenu avant la signature
 * n'était pas assuré. La période se compte donc avec une date et une heure.
 * Les échéances annuelles prolongent la période, elles ne la coupent pas.
 */
public record PeriodeCouverture(LocalDateTime debut, LocalDateTime fin) {

    public PeriodeCouverture {
        if (!debut.isBefore(fin)) {
            throw new IllegalArgumentException("une période de couverture commence avant de finir");
        }
    }

    public boolean contient(LocalDateTime instant) {
        return !instant.isBefore(debut) && instant.isBefore(fin);
    }

    /** Nombre de jours calendaires entre le jour de la souscription et le jour donné. */
    public long joursDepuisSouscription(LocalDate jour) {
        return ChronoUnit.DAYS.between(debut.toLocalDate(), jour);
    }
}
