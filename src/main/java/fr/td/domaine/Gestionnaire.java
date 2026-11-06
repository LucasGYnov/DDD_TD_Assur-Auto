package fr.td.domaine;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entité, un gestionnaire sinistres, identifié par son GestionnaireId.
 * A15, il a au plus 60 dossiers ouverts. Il les connaît par leur identifiant, pas par l'objet.
 */
public class Gestionnaire {

    public static final int DOSSIERS_OUVERTS_MAXIMUM = 60;

    private final GestionnaireId id;
    private final Set<DossierId> dossiersOuverts = new LinkedHashSet<>();

    public Gestionnaire(GestionnaireId id) {
        this.id = Objects.requireNonNull(id);
    }

    public GestionnaireId id() {
        return id;
    }

    public int nombreDeDossiersOuverts() {
        return dossiersOuverts.size();
    }

    /** A15, refusé au 61e dossier ouvert. Reprendre un dossier déjà suivi ne compte pas deux fois. */
    public void prendreEnCharge(DossierId dossier) {
        if (!dossiersOuverts.contains(dossier) && dossiersOuverts.size() >= DOSSIERS_OUVERTS_MAXIMUM) {
            throw new RegleRefusee("A15", "un gestionnaire a au plus " + DOSSIERS_OUVERTS_MAXIMUM
                    + " dossiers ouverts");
        }
        dossiersOuverts.add(dossier);
    }

    public void cloturer(DossierId dossier) {
        dossiersOuverts.remove(dossier);
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof Gestionnaire g && id.equals(g.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Gestionnaire " + id;
    }
}
