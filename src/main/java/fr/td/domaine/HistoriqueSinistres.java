package fr.td.domaine;

import java.time.LocalDate;
import java.util.List;

/**
 * Value object, les dates des sinistres déjà retenus d'un contrat (classés sans suite exclus, A18).
 * Il sert à repérer le « 3e sinistre en 12 mois » (A12). Il vient de l'extérieur du dossier,
 * car un dossier ne connaît pas les autres.
 * H4, tous les sinistres retenus comptent, bris de glace et non responsables compris.
 */
public record HistoriqueSinistres(List<LocalDate> dates) {

    public HistoriqueSinistres {
        dates = List.copyOf(dates);
    }

    public static HistoriqueSinistres vide() {
        return new HistoriqueSinistres(List.of());
    }

    /** Combien de sinistres retenus sur les 12 mois qui précèdent (ce jour compris). */
    public long nombreSurDouzeMoisJusqua(LocalDate jour) {
        LocalDate debut = jour.minusMonths(12);
        return dates.stream().filter(d -> !d.isBefore(debut) && !d.isAfter(jour)).count();
    }
}
