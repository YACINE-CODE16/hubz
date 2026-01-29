-- Script pour fixer la relation membre-organisation
-- À exécuter dans H2 Console: http://localhost:8080/h2-console

-- 1. Voir tous les users
SELECT id, email, first_name, last_name FROM users;

-- 2. Voir toutes les organisations
SELECT id, name, owner_id FROM organizations;

-- 3. Voir les relations existantes
SELECT * FROM organization_members;

-- 4. Créer la relation membre-organisation
-- REMPLACE 'TON_USER_ID' par ton ID d'utilisateur (de la requête 1)
INSERT INTO organization_members (id, organization_id, user_id, role, joined_at)
VALUES (
  RANDOM_UUID(),
  '94007e10-a9a5-4413-93fb-4ef83e8e81fb',
  'TON_USER_ID',
  'OWNER',
  CURRENT_TIMESTAMP()
);

-- 5. Vérifier que ça a marché
SELECT * FROM organization_members WHERE organization_id = '94007e10-a9a5-4413-93fb-4ef83e8e81fb';
