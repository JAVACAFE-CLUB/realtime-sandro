-- Create harvest_db database
CREATE
DATABASE IF NOT EXISTS harvest_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant all privileges to realtime user
GRANT ALL PRIVILEGES ON harvest_db.* TO
'realtime'@'%';

-- Flush privileges to ensure changes take effect
FLUSH
PRIVILEGES;
