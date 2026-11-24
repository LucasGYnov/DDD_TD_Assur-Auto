/**
 * Contexte Indemnisation (core, DDD complet) : décide si un sinistre est couvert, fixe la
 * responsabilité, missionne l'expert, calcule l'indemnité et la fait accepter ; un seul
 * gestionnaire par dossier sinistre (A15).
 * Ne fait pas : la réception des déclarations, l'enquête de fraude, le virement, le bonus-malus.
 * Agrégats : DossierSinistre (principal) et Gestionnaire. Domain service : CalculDeLaFranchise.
 * Policy : LiberationDuGestionnaire. Événements : un record au passé par commande acceptée.
 */
package fr.td.indemnisation;
