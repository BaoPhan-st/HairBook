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

-- =========================================================
-- 1. TẠO MỚI DATABASE (chỉ chạy khi setup lần đầu)
-- =========================================================

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

INSERT INTO `barbers` VALUES (1, 'Anh Minh', 'Tóc nam Hàn Quốc, Two-block', 'https://i.pravatar.cc/150?img=11', 4.9, 1);
INSERT INTO `barbers` VALUES (2, 'Anh Tuấn', 'Undercut, Fade, Buzz cut', 'https://i.pravatar.cc/150?img=12', 4.8, 1);
INSERT INTO `barbers` VALUES (3, 'Chị Lan', 'Tóc nữ, Nhuộm, Uốn xoăn', 'https://i.pravatar.cc/150?img=13', 4.7, 1);
INSERT INTO `barbers` VALUES (4, 'Anh Hùng', 'Tóc cổ điển, Pompadour', 'https://i.pravatar.cc/150?img=14', 4.6, 1);

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

INSERT INTO `users` VALUES (1, 'admin@hairbook.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh3y', 'Admin HairBook', '0900000000', 'ADMIN', '2026-06-04 16:05:05');
INSERT INTO `users` VALUES (2, 'khach@gmail.com', '$2a$10$TqF6/D8wY2HqaKibgNV1S.gVG1p8Bq3Mb0Uk8t45EAaZ9R1pJNQZO', 'Nguyễn Văn An', '0912345678', 'CUSTOMER', '2026-06-04 16:05:05');
INSERT INTO `users` VALUES (3, '12345', '$2a$10$ebW26y7cpu.KBwO3IOkGfOVVDn/ial8LBjPyqBwn6QSMfNu0jQpZC', '12345', '12345', 'CUSTOMER', '2026-06-04 16:17:52');

-- ----------------------------
-- Table structure for bookings — DÙNG CHO DATABASE MỚI
-- Enum đã bao gồm đủ 7 trạng thái ngay từ đầu.
-- ----------------------------
DROP TABLE IF EXISTS `bookings`;
CREATE TABLE `bookings`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `barber_id` bigint NOT NULL,
  `service_id` bigint NOT NULL,
  `booking_time` datetime NOT NULL,
  `booking_end_time` datetime NOT NULL,
  `status` enum('PENDING','CONFIRMED','IN_PROGRESS','COMPLETED',
                'CANCELLED_BY_CUSTOMER','CANCELLED_BY_SALON','NO_SHOW')
           CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp(),
  `cancelled_at` datetime NULL DEFAULT NULL,
  `cancel_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_booking_user`(`user_id` ASC) USING BTREE,
  INDEX `fk_booking_barber`(`barber_id` ASC) USING BTREE,
  INDEX `fk_booking_service`(`service_id` ASC) USING BTREE,
  INDEX `idx_bookings_barber_time`(`barber_id` ASC, `booking_time` ASC, `booking_end_time` ASC) USING BTREE,
  INDEX `idx_bookings_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_booking_barber` FOREIGN KEY (`barber_id`) REFERENCES `barbers` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_booking_service` FOREIGN KEY (`service_id`) REFERENCES `services` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- Dữ liệu mẫu cho DB mới (status đã ở dạng mới ngay từ đầu, booking_end_time tính sẵn theo duration dịch vụ)
INSERT INTO `bookings`
  (id, user_id, barber_id, service_id, booking_time, booking_end_time, status, note, created_at, updated_at)
VALUES
  (1, 3, 1, 5, '2026-06-18 13:00:00', '2026-06-18 13:20:00', 'CANCELLED_BY_CUSTOMER', '', '2026-06-04 16:21:35', '2026-06-04 16:21:35'),
  (2, 3, 1, 2, '2026-06-04 15:00:00', '2026-06-04 15:45:00', 'PENDING', '', '2026-06-04 19:52:40', '2026-06-04 19:52:40');

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 2. MIGRATION — CHỈ CHẠY TRÊN DATABASE CŨ ĐÃ CÓ SẴN BẢNG bookings
--    (enum cũ: 'PENDING','CONFIRMED','CANCELLED','COMPLETED')
--    KHÔNG chạy phần "1. TẠO MỚI DATABASE" ở trên nếu áp dụng phần này.
--    Thứ tự các bước dưới đây BẮT BUỘC giữ nguyên.
-- =========================================================

-- Bước 1: Thêm các cột mới (cho phép NULL trước, để không vỡ dữ liệu cũ)
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS booking_end_time DATETIME NULL,
    ADD COLUMN IF NOT EXISTS updated_at        DATETIME NULL,
    ADD COLUMN IF NOT EXISTS cancelled_at      DATETIME NULL,
    ADD COLUMN IF NOT EXISTS cancel_reason     VARCHAR(255) NULL;

-- Bước 2: Tính booking_end_time cho dữ liệu cũ (booking_time + duration của service)
UPDATE bookings b
JOIN services s ON b.service_id = s.id
SET b.booking_end_time = DATE_ADD(b.booking_time, INTERVAL COALESCE(s.duration_minutes, 30) MINUTE)
WHERE b.booking_end_time IS NULL;

-- Phòng trường hợp còn sót (service_id null bất thường) -> mặc định +30 phút
UPDATE bookings
SET booking_end_time = DATE_ADD(booking_time, INTERVAL 30 MINUTE)
WHERE booking_end_time IS NULL;

-- Bước 3: Set updated_at mặc định = created_at cho dữ liệu cũ
UPDATE bookings
SET updated_at = COALESCE(created_at, NOW())
WHERE updated_at IS NULL;

-- Bước 4: Đổi 2 cột thành NOT NULL sau khi đã có dữ liệu đầy đủ
ALTER TABLE bookings
    MODIFY COLUMN booking_end_time DATETIME NOT NULL,
    MODIFY COLUMN updated_at DATETIME NOT NULL;

-- Bước 5 (QUAN TRỌNG — phải chạy TRƯỚC bước migrate dữ liệu status):
-- Mở rộng enum của cột status để chấp nhận đủ 7 giá trị mới.
-- Nếu bỏ qua bước này, bước 6 bên dưới (UPDATE status='CANCELLED_BY_CUSTOMER')
-- sẽ lỗi "Data truncated for column 'status'" vì enum cũ chưa có giá trị đó,
-- và bất kỳ booking nào bị dữ liệu status không hợp lệ cũng sẽ làm cả API
-- GET /api/bookings/my (và toàn bộ danh sách lịch sử) lỗi 500 khi Hibernate
-- đọc bảng — đây chính là nguyên nhân hiện tượng "hủy 1 lịch làm mất hết
-- các lịch khác trên màn hình".
ALTER TABLE bookings
    MODIFY COLUMN status enum('PENDING','CONFIRMED','IN_PROGRESS','COMPLETED',
                               'CANCELLED_BY_CUSTOMER','CANCELLED_BY_SALON','NO_SHOW')
                     CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING';

-- Bước 6: Migrate giá trị status cũ sang enum mới (an toàn, enum đã mở rộng ở bước 5)
UPDATE bookings
SET status = 'CANCELLED_BY_CUSTOMER'
WHERE status = 'CANCELLED';

-- Booking nào status bị NULL (do dữ liệu rác trước đây) -> đưa về PENDING để không vỡ luồng nghiệp vụ
UPDATE bookings
SET status = 'PENDING'
WHERE status IS NULL;

-- Bước 7: barber_id không còn cho phép NULL (nghiệp vụ bắt buộc chọn thợ)
-- Nếu có booking cũ thiếu barber_id, cần xử lý tay TRƯỚC khi chạy lệnh dưới,
-- ví dụ gán tạm 1 thợ mặc định hoặc xóa các booking rác đó:
-- UPDATE bookings SET barber_id = (SELECT id FROM barbers LIMIT 1) WHERE barber_id IS NULL;
ALTER TABLE bookings
    MODIFY COLUMN barber_id BIGINT NOT NULL;

-- Bước 8: Index hỗ trợ truy vấn overlap theo thợ + khoảng thời gian (rất hay dùng)
CREATE INDEX IF NOT EXISTS idx_bookings_barber_time
    ON bookings (barber_id, booking_time, booking_end_time);

-- Bước 9: Index hỗ trợ "GET /api/bookings/my" sắp xếp theo created_at
CREATE INDEX IF NOT EXISTS idx_bookings_user_created
    ON bookings (user_id, created_at);
