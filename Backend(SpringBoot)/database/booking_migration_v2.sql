USE hairbook;

ALTER TABLE bookings
    ADD COLUMN booking_end_time DATETIME NULL AFTER booking_time;

ALTER TABLE bookings
    ADD COLUMN updated_at DATETIME NULL AFTER created_at;

ALTER TABLE bookings
    ADD COLUMN cancelled_at DATETIME NULL AFTER updated_at;

ALTER TABLE bookings
    ADD COLUMN cancel_reason VARCHAR(500) NULL AFTER cancelled_at;

UPDATE bookings
SET status = 'PENDING'
WHERE status IS NULL
   OR status = '';

ALTER TABLE bookings
    MODIFY COLUMN status ENUM(
        'PENDING',
        'CONFIRMED',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED_BY_CUSTOMER',
        'CANCELLED_BY_SALON',
        'NO_SHOW',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING';

UPDATE bookings
SET booking_end_time = DATE_ADD(
        booking_time,
        INTERVAL COALESCE(
            (
                SELECT services.duration_minutes
                FROM services
                WHERE services.id = bookings.service_id
            ),
            30
        ) MINUTE
    )
WHERE booking_end_time IS NULL;

UPDATE bookings
SET updated_at = COALESCE(updated_at, created_at, NOW())
WHERE updated_at IS NULL;

UPDATE bookings
SET status = 'CANCELLED_BY_CUSTOMER',
    cancelled_at = COALESCE(cancelled_at, updated_at, created_at, NOW()),
    cancel_reason = COALESCE(
        NULLIF(TRIM(cancel_reason), ''),
        'Lịch hẹn được hủy từ dữ liệu cũ.'
    )
WHERE status = 'CANCELLED';

ALTER TABLE bookings
    MODIFY COLUMN booking_end_time DATETIME NOT NULL;

CREATE INDEX idx_bookings_barber_status_time
ON bookings (barber_id, status, booking_time, booking_end_time);

CREATE INDEX idx_bookings_user_booking_time
ON bookings (user_id, booking_time);