# Fix 403 Authorization Errors

## Problème
L'organisation existe dans la base de données mais ta relation de membre (membership) est manquante.

## Solution

### Option 1: Effacer les données et recommencer (RECOMMANDÉ)

1. Ouvre la console développeur dans ton navigateur (F12 ou Cmd+Option+I)

2. Va dans l'onglet "Console"

3. Exécute cette commande:
```javascript
localStorage.clear();
location.reload();
```

4. Tu seras déconnecté. Reconnecte-toi avec ton compte.

5. Sur la page Hub, clique sur "Nouvelle organisation" et crée une organisation fraîche.

6. Tout devrait fonctionner correctement maintenant.

### Option 2: Fixer via H2 Console (Si tu veux garder les données)

1. Ouvre http://localhost:8080/h2-console dans ton navigateur

2. Configure la connexion:
   - JDBC URL: `jdbc:h2:file:./data/hubzdb`
   - User Name: `sa`
   - Password: (laisse vide)

3. Clique "Connect"

4. Exécute cette requête pour trouver ton user ID:
```sql
SELECT * FROM users;
```

5. Copie ton user ID, puis exécute:
```sql
INSERT INTO organization_members (id, organization_id, user_id, role, joined_at)
VALUES (
  RANDOM_UUID(),
  '94007e10-a9a5-4413-93fb-4ef83e8e81fb',
  'TON_USER_ID_ICI',
  'OWNER',
  CURRENT_TIMESTAMP()
);
```

6. Rafraîchis ton navigateur.

## Pourquoi ça arrive?

Quand tu crées une organisation, le système devrait automatiquement créer une entrée dans `organization_members` pour te définir comme OWNER. Si cette entrée est manquante, tu n'as plus accès à l'organisation même si elle existe.
