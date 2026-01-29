#!/bin/bash

# Script pour vérifier la base de données H2

echo "🔍 Vérification de la base de données..."
echo ""

# Créer un script SQL temporaire
cat > /tmp/check.sql << 'EOF'
SELECT '=== USERS ===' AS info FROM DUAL;
SELECT id, email, first_name, last_name FROM users;

SELECT '' AS space FROM DUAL;
SELECT '=== ORGANIZATIONS ===' AS info FROM DUAL;
SELECT id, name, owner_id FROM organizations;

SELECT '' AS space FROM DUAL;
SELECT '=== ORGANIZATION MEMBERS ===' AS info FROM DUAL;
SELECT om.id, om.organization_id, om.user_id, om.role, o.name as org_name, u.email as user_email
FROM organization_members om
LEFT JOIN organizations o ON om.organization_id = o.id
LEFT JOIN users u ON om.user_id = u.id;

SELECT '' AS space FROM DUAL;
SELECT '=== PROBLEME: Organisations sans membres ===' AS info FROM DUAL;
SELECT o.id, o.name, o.owner_id
FROM organizations o
LEFT JOIN organization_members om ON o.id = om.organization_id AND o.owner_id = om.user_id
WHERE om.id IS NULL;
EOF

# Se connecter à H2 et exécuter le script
cd /Users/yacinebask/hubz/hubz-backend

java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar org.h2.tools.Shell \
  -url "jdbc:h2:file:./data/hubzdb" \
  -user "sa" \
  -password "" \
  -sql "$(cat /tmp/check.sql)"

echo ""
echo "✅ Vérification terminée"
