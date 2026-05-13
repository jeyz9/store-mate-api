DO $$
BEGIN 
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name='users' AND column_name='suspended_at'
    ) THEN
ALTER TABLE users RENAME COLUMN suspended_at TO suspend_at;
END IF;
END $$;