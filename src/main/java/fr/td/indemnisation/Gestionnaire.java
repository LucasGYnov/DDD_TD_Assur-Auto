package fr.td.indemnisation;

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

    /**
     * A15, refusé au 61e dossier ouvert. Reprendre un dossier déjà suivi ne compte pas deux fois.
     * Produit l'événement « Gestionnaire affecté ».
     */
    public GestionnaireAffecte prendreEnCharge(DossierId dossier) {
        if (!dossiersOuverts.contains(dossier) && dossiersOuverts.size() >= DOSSIERS_OUVERTS_MAXIMUM) {
            throw new RegleRefusee("A15", "un gestionnaire a au plus " + DOSSIERS_OUVERTS_MAXIMUM
                    + " dossiers ouverts");
        }
        dossiersOuverts.add(dossier);
        return new GestionnaireAffecte(dossier, id);
    }

    /**
     * A15, un dossier sinistre est suivi par un seul gestionnaire, de l'ouverture à la clôture :
     * seul celui qui le suit peut le clôturer. La clôture libère une place.
     * Produit l'événement « Dossier sinistre clôturé ».
     */
    public DossierSinistreCloture cloturer(DossierId dossier) {
        if (!dossiersOuverts.contains(dossier)) {
            throw new RegleRefusee("A15", "le dossier sinistre " + dossier
                    + " n'est pas suivi par le gestionnaire " + id + " : il ne peut pas le clôturer");
        }
        dossiersOuverts.remove(dossier);
        return new DossierSinistreCloture(dossier, id);
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
