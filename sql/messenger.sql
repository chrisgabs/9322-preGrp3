-- MySQL dump 10.13  Distrib 5.7.31, for Win64 (x86_64)
--
-- Host: localhost    Database: messenger
-- ------------------------------------------------------
-- Server version	5.7.31

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `group_msg`
--

DROP TABLE IF EXISTS `group_msg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `group_msg` (
  `group_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `is_fav` tinyint(1) DEFAULT NULL,
  KEY `group_id` (`group_id`),
  KEY `user_id` (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_msg`
--

LOCK TABLES `group_msg` WRITE;
/*!40000 ALTER TABLE `group_msg` DISABLE KEYS */;
/*!40000 ALTER TABLE `group_msg` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_repo`
--

DROP TABLE IF EXISTS `group_repo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `group_repo` (
  `group_id` int(11) NOT NULL AUTO_INCREMENT,
  `is_admin` tinyint(1) DEFAULT NULL,
  `alias` varchar(30) DEFAULT NULL,
  `uid_admin` int(11) NOT NULL,
  PRIMARY KEY (`group_id`),
  KEY `uid_admin` (`uid_admin`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_repo`
--

LOCK TABLES `group_repo` WRITE;
/*!40000 ALTER TABLE `group_repo` DISABLE KEYS */;
/*!40000 ALTER TABLE `group_repo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `message` (
  `from_user` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `time_sent` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `from_user` (`from_user`),
  CONSTRAINT `message_ibfk_1` FOREIGN KEY (`from_user`) REFERENCES `user_acc` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message`
--

LOCK TABLES `message` WRITE;
/*!40000 ALTER TABLE `message` DISABLE KEYS */;
/*!40000 ALTER TABLE `message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_acc`
--

DROP TABLE IF EXISTS `user_acc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_acc` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(50) NOT NULL,
  `pwd_hash` varchar(100) NOT NULL,
  `salt` varchar(100) DEFAULT NULL,
  `user_fname` tinytext NOT NULL,
  `user_lname` tinytext NOT NULL,
  `verified` tinyint(1) NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_acc`
--

LOCK TABLES `user_acc` WRITE;
/*!40000 ALTER TABLE `user_acc` DISABLE KEYS */;
INSERT INTO `user_acc` VALUES (1,'lancearevalo2000@gmail.com','bB8vDbs+Jk5/bked8OIwydnAsMoeIp7CWhE+1aT9bm9/V7OPmAsp2axEp671xrUWGuMX6VeLCSqi6gaQdZeXoA==','Fo/se3IPt5gRoHC9OLo59jBFzkMznL7BBBQcdJymv+MMyA1bsT/ZYEqSoM4/SNW3PgpcX7nwgl6xQgfKr8Pwhg==','Lance','Arevalo',0);
/*!40000 ALTER TABLE `user_acc` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2021-02-16 17:09:08
