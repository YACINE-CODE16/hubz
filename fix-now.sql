-- Fix membership for yacineallam00@gmail.com

-- 1. Get user ID
SET @USER_EMAIL = 'yacineallam00@gmail.com';

-- 2. Find and fix organizations where user is owner but not member
INSERT INTO organization_members (id, organization_id, user_id, role, joined_at)
SELECT
    RANDOM_UUID() as id,
    o.id as organization_id,
    u.id as user_id,
    'OWNER' as role,
    CURRENT_TIMESTAMP() as joined_at
FROM organizations o
CROSS JOIN users u
LEFT JOIN organization_members om ON om.organization_id = o.id AND om.user_id = u.id
WHERE u.email = @USER_EMAIL
  AND o.owner_id = u.id
  AND om.id IS NULL;

-- 3. Show what was fixed
SELECT
    o.name as organization_name,
    o.id as organization_id,
    u.email as user_email,
    om.role as role,
    om.joined_at
FROM organization_members om
JOIN organizations o ON om.organization_id = o.id
JOIN users u ON om.user_id = u.id
WHERE u.email = @USER_EMAIL;
