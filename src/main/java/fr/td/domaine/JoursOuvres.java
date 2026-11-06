package fr.td.domaine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Value object, le calendrier des jours ouvrés (du lundi au vendredi, hors jours fériés).
 * Les jours fériés viennent de l'extérieur, le dossier ne les donne pas.
 *
 * H5, on compte à partir du lendemain du sinistre, et le jour de la déclaration compte.
 */
public record JoursOuvres(Set<LocalDate> joursFeries) {

    public JoursOuvres {
        joursFeries = Set.copyOf(joursFeries);
    }

    public boolean estOuvre(LocalDate jour) {
        DayOfWeek semaine = jour.getDayOfWeek();
        return semaine != DayOfWeek.SATURDAY && semaine != DayOfWeek.SUNDAY && !joursFeries.contains(jour);
    }

    /** Nombre de jours ouvrés écoulés après le jour du sinistre, jusqu'au jour de la déclaration compris. */
    public int ecoulesEntre(LocalDate sinistreLe, LocalDate declareLe) {
        if (declareLe.isBefore(sinistreLe)) {
            throw new IllegalArgumentException("on ne déclare pas un sinistre avant qu'il ait eu lieu");
        }
        int ecoules = 0;
        for (LocalDate jour = sinistreLe.plusDays(1); !jour.isAfter(declareLe); jour = jour.plusDays(1)) {
            if (estOuvre(jour)) {
                ecoules++;
            }
        }
        return ecoules;
    }
}
