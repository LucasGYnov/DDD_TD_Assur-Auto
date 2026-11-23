Assur'Auto — Gestion des sinistres auto (TD DDD, jour 2)
=======================================================

1. Dans IntelliJ : File > Open, puis choisissez ce dossier (celui qui contient pom.xml).
2. Ouvrez src/main/java/fr/td/Verifications.java et lancez-le
   (flèche verte à gauche de main, ou clic droit > Run).
3. Objectif : toutes les lignes en OK.

Un package par contexte (sous fr.td)
------------------------------------
  indemnisation   core, DDD complet : couverture (A3, A17), responsabilité (A4),
                  expertise (A8), gestionnaire (A15).
                  Événements produits : GestionnaireAffecte, DossierSinistreCloture.
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
