package fr.td.indemnisation;

/**
 * Policy : « Quand un dossier sinistre est clôturé, alors libérer sa place chez son gestionnaire » (A15).
 * Elle relie les deux agrégats du core, DossierSinistre et Gestionnaire, sans qu'aucun ne connaisse l'autre.
 *
 * Elle ne vérifie aucune règle : c'est le Gestionnaire qui décide (il refuse un dossier qu'il ne suit pas).
 * Celui qui l'appelle charge le gestionnaire dont l'identifiant est dans l'événement.
 */
public class LiberationDuGestionnaire {

    public PlaceLiberee quand(DossierSinistreCloture evenement, Gestionnaire gestionnaire) {
        return gestionnaire.libererLaPlace(evenement.dossier());
    }
}
