/*
 Navicat Premium Dump SQL

 Source Server         : ebookstore
 Source Server Type    : MySQL
 Source Server Version : 100432 (10.4.32-MariaDB)
 Source Host           : localhost:3306
 Source Schema         : hairbook

 Target Server Type    : MySQL
 Target Server Version : 100432 (10.4.32-MariaDB)
 File Encoding         : 65001

 Date: 04/06/2026 19:56:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for barbers
-- ----------------------------
DROP TABLE IF EXISTS `barbers`;
CREATE TABLE `barbers`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `specialty` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `rating` double NULL DEFAULT NULL,
  `available` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of barbers
-- ----------------------------
INSERT INTO `barbers` VALUES (1, 'Anh Minh', 'Tóc nam Hàn Quốc, Two-block', 'https://i.pravatar.cc/150?img=11', 4.9, 1);
INSERT INTO `barbers` VALUES (2, 'Anh Tuấn', 'Undercut, Fade, Buzz cut', 'https://i.pravatar.cc/150?img=12', 4.8, 1);
INSERT INTO `barbers` VALUES (3, 'Chị Lan', 'Tóc nữ, Nhuộm, Uốn xoăn', 'https://i.pravatar.cc/150?img=13', 4.7, 1);
INSERT INTO `barbers` VALUES (4, 'Anh Hùng', 'Tóc cổ điển, Pompadour', 'https://i.pravatar.cc/150?img=14', 4.6, 1);

-- ----------------------------
-- Table structure for bookings
-- ----------------------------
DROP TABLE IF EXISTS `bookings`;
CREATE TABLE `bookings`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `barber_id` bigint NULL DEFAULT NULL,
  `service_id` bigint NOT NULL,
  `booking_time` datetime NOT NULL,
  `status` enum('PENDING','CONFIRMED','CANCELLED','COMPLETED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_booking_user`(`user_id` ASC) USING BTREE,
  INDEX `fk_booking_barber`(`barber_id` ASC) USING BTREE,
  INDEX `fk_booking_service`(`service_id` ASC) USING BTREE,
  CONSTRAINT `fk_booking_barber` FOREIGN KEY (`barber_id`) REFERENCES `barbers` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_booking_service` FOREIGN KEY (`service_id`) REFERENCES `services` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bookings
-- ----------------------------
INSERT INTO `bookings` VALUES (1, 3, 1, 5, '2026-06-18 13:00:00', 'CANCELLED', '', '2026-06-04 16:21:35');
INSERT INTO `bookings` VALUES (2, 3, 1, 2, '2026-06-04 15:00:00', 'PENDING', '', '2026-06-04 19:52:40');

-- ----------------------------
-- Table structure for services
-- ----------------------------
DROP TABLE IF EXISTS `services`;
CREATE TABLE `services`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `price` double NULL DEFAULT NULL,
  `duration_minutes` int NULL DEFAULT NULL,
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of services
-- ----------------------------
INSERT INTO `services` VALUES (1, 'Cắt tóc nam', 'Cắt tạo kiểu theo yêu cầu, phù hợp mọi phong cách', 50000, 30, 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=300');
INSERT INTO `services` VALUES (2, 'Cắt + Gội đầu', 'Cắt tóc và gội đầu massage thư giãn', 80000, 45, 'https://images.unsplash.com/photo-1560066984-138dadb4c035?w=300');
INSERT INTO `services` VALUES (3, 'Nhuộm tóc', 'Nhuộm màu thời trang, dưỡng tóc sau nhuộm', 250000, 90, 'https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=300');
INSERT INTO `services` VALUES (4, 'Uốn tóc', 'Uốn xoăn Hàn Quốc, bảo hành 3 tháng', 350000, 120, 'https://images.unsplash.com/photo-1595476108010-b4d1f102b1b1?w=300');
INSERT INTO `services` VALUES (5, 'Gội đầu', 'Gội đầu dưỡng tóc, massage đầu thư giãn', 40000, 20, 'https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=300');
INSERT INTO `services` VALUES (6, 'Cắt tóc nữ', 'Cắt tạo kiểu tóc nữ, tư vấn theo khuôn mặt', 100000, 45, 'https://images.unsplash.com/photo-1562322140-8baeececf3df?w=300');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `full_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `role` enum('CUSTOMER','ADMIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin@hairbook.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh3y', 'Admin HairBook', '0900000000', 'ADMIN', '2026-06-04 16:05:05');
INSERT INTO `users` VALUES (2, 'khach@gmail.com', '$2a$10$TqF6/D8wY2HqaKibgNV1S.gVG1p8Bq3Mb0Uk8t45EAaZ9R1pJNQZO', 'Nguyễn Văn An', '0912345678', 'CUSTOMER', '2026-06-04 16:05:05');
INSERT INTO `users` VALUES (3, '12345', '$2a$10$ebW26y7cpu.KBwO3IOkGfOVVDn/ial8LBjPyqBwn6QSMfNu0jQpZC', '12345', '12345', 'CUSTOMER', '2026-06-04 16:17:52');

SET FOREIGN_KEY_CHECKS = 1;
