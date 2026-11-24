Assur'Auto — Gestion des sinistres auto (TD DDD, jour 3)
=======================================================

1. Dans IntelliJ : File > Open, puis choisissez ce dossier (celui qui contient pom.xml).
2. Ouvrez src/main/java/fr/td/Verifications.java et lancez-le
   (flèche verte à gauche de main, ou clic droit > Run).
3. Objectif : toutes les lignes en OK.

Un package par contexte (sous fr.td)
------------------------------------
  indemnisation   core, DDD complet : couverture (A3, A17), responsabilité (A4),
                  expertise (A7, A8), indemnité (A9), accord et contestation (A10, A11),
                  suspension (A12), retard (A2), sans suite (A18), gestionnaire (A15).
                  Agrégat principal : DossierSinistre (racine, une commande par règle,
                    un événement au passé par commande acceptée).
                  Autre agrégat : Gestionnaire (au plus 60 dossiers ouverts, A15).
                  Domain service : CalculDeLaFranchise (A5, A6, barème de la section 8).
                  Policy : LiberationDuGestionnaire (quand un dossier sinistre est clôturé,
                    alors libérer sa place chez son gestionnaire).
  indemnisation.expertis
                  ACL Expertis -> Indemnisation : RapportExpertis (la langue d'Expertis),
                  TraducteurExpertis, RapportExpertisIntraduisible (rien ne passe en silence).
  baremes         Barèmes et bonus-malus, supporting : coefficient (A13), recalcul à
                  l'échéance annuelle (A14).
  declaration     Déclaration, supporting : délai de déclaration (A2). Pas encore de classe.
  fraude          Vol et fraude, supporting : suspicion de fraude (A12, A18). Pas encore de classe.
  reglement       Règlement, supporting : virement via PaySûr (A10, A16). Pas encore de classe.

Un mot, deux modèles : Contrat
------------------------------
  fr.td.indemnisation.Contrat  sait s'il couvre un sinistre (garanties, souscription, suspensions).
  fr.td.baremes.Contrat        connaît son coefficient de bonus-malus et son échéance annuelle.
  Les deux partagent la valeur du ContratId, pas l'objet.

Scénarios rejoués (fin de Verifications.java)
---------------------------------------------
  SC1  Le pare-brise de Mme Roux : tout se passe bien, 620 € sans franchise, acceptés.
  SC2  Le contrat tout neuf de M. Caron : suspicion de fraude (A12), aucun montant proposé.
  SC3  Mme Roux, par la policy : dossier clôturé, place libérée chez son gestionnaire.
