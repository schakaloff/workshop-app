/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-12.1.2-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: workshopdb
-- ------------------------------------------------------
-- Server version	12.1.2-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `client`
--

DROP TABLE IF EXISTS `client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `client` (
  `firstName` varchar(100) NOT NULL,
  `lastNAME` varchar(100) NOT NULL,
  `phone` varchar(100) NOT NULL,
  `adress` varchar(100) NOT NULL,
  `city` varchar(100) NOT NULL,
  `postCode` varchar(100) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `client`
--

LOCK TABLES `client` WRITE;
/*!40000 ALTER TABLE `client` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `client` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `customer`
--

DROP TABLE IF EXISTS `customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `first_name` varchar(100) NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `additional_names` varchar(255) DEFAULT NULL,
  `phone` varchar(50) NOT NULL,
  `additional_phone` varchar(50) DEFAULT NULL,
  `address` varchar(255) NOT NULL,
  `postal_code` varchar(20) NOT NULL,
  `town` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer`
--

LOCK TABLES `customer` WRITE;
/*!40000 ALTER TABLE `customer` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `customer` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `payment_id` int(11) NOT NULL AUTO_INCREMENT,
  `workorder_id` int(11) NOT NULL,
  `method` varchar(50) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `paid_at` datetime NOT NULL DEFAULT current_timestamp(),
  `processed_by` varchar(100) NOT NULL,
  PRIMARY KEY (`payment_id`),
  KEY `idx_payment_workorder` (`workorder_id`),
  CONSTRAINT `fk_payment_workorder` FOREIGN KEY (`workorder_id`) REFERENCES `work_order` (`workorder`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `technician`
--

DROP TABLE IF EXISTS `technician`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `technician` (
  `username` varchar(100) NOT NULL,
  `first_name` varchar(50) DEFAULT NULL,
  `last_name` varchar(50) DEFAULT NULL,
  `password` varchar(40) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `technician`
--

LOCK TABLES `technician` WRITE;
/*!40000 ALTER TABLE `technician` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `technician` VALUES
('FS','Filip','Shakalau','ea842d14c5b1b85002af9272e5eb23fa5409c643',1),
('KON','Konstantin','Schakalof','c9b36a4c7a11efcbe16bcb13f74db1609c28c6bc',2);
/*!40000 ALTER TABLE `technician` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `work_order`
--

DROP TABLE IF EXISTS `work_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_order` (
  `workorder` int(11) NOT NULL AUTO_INCREMENT,
  `status` varchar(50) NOT NULL,
  `type` varchar(100) NOT NULL,
  `model` varchar(100) NOT NULL,
  `serialNumber` varchar(100) NOT NULL,
  `problemDesc` text NOT NULL,
  `createdAt` datetime DEFAULT current_timestamp(),
  `customer_id` int(11) DEFAULT NULL,
  `vendorId` varchar(50) DEFAULT NULL,
  `warrantyNumber` varchar(255) DEFAULT NULL,
  `service_notes` text DEFAULT NULL,
  `invoice_total` decimal(10,2) NOT NULL DEFAULT 0.00,
  `deposit_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `tech_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`workorder`),
  KEY `idx_work_order_customer_id` (`customer_id`),
  KEY `idx_workorder_warrantyNumber` (`warrantyNumber`),
  KEY `fk_work_order_tech` (`tech_id`),
  CONSTRAINT `fk_work_order_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_work_order_tech` FOREIGN KEY (`tech_id`) REFERENCES `technician` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_order`
--

LOCK TABLES `work_order` WRITE;
/*!40000 ALTER TABLE `work_order` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `work_order` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `work_order_files`
--

DROP TABLE IF EXISTS `work_order_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_order_files` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workorder_id` int(11) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_data` longblob NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_files_by_workorder` (`workorder_id`),
  CONSTRAINT `fk_file_wo` FOREIGN KEY (`workorder_id`) REFERENCES `work_order` (`workorder`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_order_files`
--

LOCK TABLES `work_order_files` WRITE;
/*!40000 ALTER TABLE `work_order_files` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `work_order_files` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `work_order_parts`
--

DROP TABLE IF EXISTS `work_order_parts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_order_parts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workorder_id` int(11) NOT NULL,
  `part_name` varchar(255) NOT NULL,
  `quantity` int(11) NOT NULL DEFAULT 0,
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `total_price` decimal(10,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  KEY `idx_parts_by_workorder` (`workorder_id`),
  CONSTRAINT `fk_workorder` FOREIGN KEY (`workorder_id`) REFERENCES `work_order` (`workorder`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_order_parts`
--

LOCK TABLES `work_order_parts` WRITE;
/*!40000 ALTER TABLE `work_order_parts` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `work_order_parts` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `work_order_repairs`
--

DROP TABLE IF EXISTS `work_order_repairs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_order_repairs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workorder_id` int(11) NOT NULL,
  `repair_date` date NOT NULL,
  `tech` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  KEY `idx_repairs_by_workorder` (`workorder_id`),
  CONSTRAINT `fk_repairs_workorder` FOREIGN KEY (`workorder_id`) REFERENCES `work_order` (`workorder`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_order_repairs`
--

LOCK TABLES `work_order_repairs` WRITE;
/*!40000 ALTER TABLE `work_order_repairs` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `work_order_repairs` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `work_order_steps`
--

DROP TABLE IF EXISTS `work_order_steps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_order_steps` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `workorder_id` int(11) NOT NULL,
  `step_date` datetime NOT NULL,
  `technician` varchar(100) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `price` decimal(10,2) DEFAULT 0.00,
  PRIMARY KEY (`id`),
  KEY `workorder_id` (`workorder_id`),
  CONSTRAINT `work_order_steps_ibfk_1` FOREIGN KEY (`workorder_id`) REFERENCES `work_order` (`workorder`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_order_steps`
--

LOCK TABLES `work_order_steps` WRITE;
/*!40000 ALTER TABLE `work_order_steps` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `work_order_steps` ENABLE KEYS */;
UNLOCK TABLES;
commit;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-03-03 14:45:14
