/**
 * Contexte Indemnisation (core, DDD complet) : décide si un sinistre est couvert, fixe la
 * responsabilité, missionne l'expert, calcule l'indemnité et la fait accepter ; un seul
 * gestionnaire par dossier sinistre (A15).
 * Ne fait pas : la réception des déclarations, l'enquête de fraude, le virement, le bonus-malus.
 * Événements produits : GestionnaireAffecte, DossierSinistreCloture.
 */
package fr.td.indemnisation;
