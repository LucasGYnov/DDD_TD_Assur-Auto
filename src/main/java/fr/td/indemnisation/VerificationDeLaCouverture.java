package fr.td.indemnisation;

import java.time.LocalDate;

/**
 * Ce que produit la commande « Vérifier la couverture » (A3, A17) :
 * l'un des deux événements « Couverture confirmée » ou « Couverture refusée ».
 */
public sealed interface VerificationDeLaCouverture
        permits VerificationDeLaCouverture.CouvertureConfirmee, VerificationDeLaCouverture.CouvertureRefusee {

    DossierId dossier();

    /** Événement « Couverture confirmée » : le dossier passe en instruction. */
    record CouvertureConfirmee(DossierId dossier, ContratId contrat, Garantie garantie)
            implements VerificationDeLaCouverture {
    }

    /** Événement « Couverture refusée » : le dossier est fermé sans indemnité. */
    record CouvertureRefusee(DossierId dossier, ContratId contrat, Garantie garantie, LocalDate dateDuSinistre)
            implements VerificationDeLaCouverture {
    }
}
