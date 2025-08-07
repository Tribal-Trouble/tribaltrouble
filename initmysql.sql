-- 
-- Create Database `oddlabs`
--
CREATE DATABASE oddlabs;
USE oddlabs;

--
-- Table structure for table `connections`
--

DROP TABLE IF EXISTS `connections`;
CREATE TABLE `connections` (
  `game_id` int DEFAULT NULL,
  `nick1` varchar(128) DEFAULT NULL,
  `nick2` varchar(128) DEFAULT NULL,
  `priority` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `connections`
--

LOCK TABLES `connections` WRITE;
UNLOCK TABLES;

--
-- Table structure for table `deleted_profiles`
--

DROP TABLE IF EXISTS `deleted_profiles`;
CREATE TABLE `deleted_profiles` (
  `nick` varchar(128) DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `wins` int DEFAULT NULL,
  `losses` int DEFAULT NULL,
  `invalid` int DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  `reg_id` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `deleted_profiles`
--

LOCK TABLES `deleted_profiles` WRITE;
UNLOCK TABLES;

--
-- Table structure for table `game_reports`
--

DROP TABLE IF EXISTS `game_reports`;
CREATE TABLE `game_reports` (
  `game_id` int DEFAULT NULL,
  `tick` int DEFAULT NULL,
  `team1` int DEFAULT NULL,
  `team2` int DEFAULT NULL,
  `team3` int DEFAULT NULL,
  `team4` int DEFAULT NULL,
  `team5` int DEFAULT NULL,
  `team6` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `games`
--

DROP TABLE IF EXISTS `games`;
CREATE TABLE `games` (
  `time_create` datetime DEFAULT (curdate()),
  `name` varchar(128) DEFAULT NULL,
  `rated` varchar(1) DEFAULT NULL,
  `speed` int DEFAULT NULL,
  `size` int DEFAULT NULL,
  `hills` int DEFAULT NULL,
  `trees` int DEFAULT NULL,
  `resources` int DEFAULT NULL,
  `mapcode` varchar(128) DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  `winner` int DEFAULT NULL,
  `time_stop` datetime DEFAULT (curdate()),
  `player1_name` varchar(128) DEFAULT NULL,
  `player1_race` varchar(1) DEFAULT NULL,
  `player1_team` int DEFAULT NULL,
  `player2_name` varchar(128) DEFAULT NULL,
  `player2_race` varchar(1) DEFAULT NULL,
  `player2_team` int DEFAULT NULL,
  `player3_name` varchar(128) DEFAULT NULL,
  `player3_race` varchar(1) DEFAULT NULL,
  `player3_team` int DEFAULT NULL,
  `player4_name` varchar(128) DEFAULT NULL,
  `player4_race` varchar(1) DEFAULT NULL,
  `player4_team` int DEFAULT NULL,
  `player5_name` varchar(128) DEFAULT NULL,
  `player5_race` varchar(1) DEFAULT NULL,
  `player5_team` int DEFAULT NULL,
  `player6_name` varchar(128) DEFAULT NULL,
  `player6_race` varchar(1) DEFAULT NULL,
  `player6_team` int DEFAULT NULL,
  `player7_name` varchar(128) DEFAULT NULL,
  `player7_race` varchar(1) DEFAULT NULL,
  `player7_team` int DEFAULT NULL,
  `player8_name` varchar(128) DEFAULT NULL,
  `player8_race` varchar(1) DEFAULT NULL,
  `player8_team` int DEFAULT NULL,
  `time_start` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `online_profiles`
--

DROP TABLE IF EXISTS `online_profiles`;
CREATE TABLE `online_profiles` (
  `nick` varchar(128) DEFAULT NULL,
  `game_id` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `online_profiles`
--

LOCK TABLES `online_profiles` WRITE;
UNLOCK TABLES;

--
-- Table structure for table `profiles`
--

DROP TABLE IF EXISTS `profiles`;
CREATE TABLE `profiles` (
  `nick` varchar(128) DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `wins` int DEFAULT NULL,
  `losses` int DEFAULT NULL,
  `invalid` int DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  `reg_id` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `registrations`
--

DROP TABLE IF EXISTS `registrations`;
CREATE TABLE `registrations` (
  `username` varchar(128) DEFAULT NULL,
  `reg_key` varchar(128) DEFAULT NULL,
  `disabled` tinyint(1) DEFAULT '0',
  `banned` tinyint(1) DEFAULT '0',
  `password` varchar(128) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  `last_used_profile` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `settings`
--

DROP TABLE IF EXISTS `settings`;
CREATE TABLE `settings` (
  `value` varchar(255) DEFAULT NULL,
  `property` varchar(128) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `settings`
--

LOCK TABLES `settings` WRITE;
INSERT INTO `settings` VALUES ('4','min_username_length'),('64','max_username_length'),('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789','allowed_chars'),('10','max_profiles');
UNLOCK TABLES;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages` (
  `time` datetime DEFAULT (curdate()),
  `message` varchar(512) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
