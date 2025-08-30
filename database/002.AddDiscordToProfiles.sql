CREATE TABLE `discord_to_profiles` (
  `nick` varchar(128) NOT NULL,
  `discord_id` bigint NOT NULL,
  KEY `nick` (`nick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;