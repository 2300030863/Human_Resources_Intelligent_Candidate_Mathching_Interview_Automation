-- Create Candidate records for existing users who don't have them
-- This is useful for users who registered before the auto-creation feature was added

INSERT INTO candidates (email, first_name, last_name, phone, skills, experience_years, status, created_at, updated_at)
SELECT 
    u.email,
    u.first_name,
    u.last_name,
    '' as phone,
    '' as skills,
    0 as experience_years,
    'NEW' as status,
    NOW() as created_at,
    NOW() as updated_at
FROM users u
LEFT JOIN candidates c ON u.email = c.email
WHERE u.role = 'CANDIDATE' 
  AND c.id IS NULL;

-- Verify the candidates were created
SELECT 
    u.email,
    u.first_name,
    u.last_name,
    c.id as candidate_id,
    u.role
FROM users u
LEFT JOIN candidates c ON u.email = c.email
WHERE u.role = 'CANDIDATE';
