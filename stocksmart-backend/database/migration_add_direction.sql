-- =============================================================================
-- StockSmart Database Migration: Structured Movement Direction
-- Version: Phase 11 / Phase 12 Production Hardening
-- =============================================================================

USE `stocksmart`;

-- Step 1: Add direction column if it doesn't already exist
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'stock_movements'
      AND column_name = 'direction'
);

SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE stock_movements ADD COLUMN direction VARCHAR(10) NULL AFTER movement_type;',
    'SELECT "Column direction already exists in stock_movements." AS status;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Backfill historical movements based on movement_type and remarks
-- 2a. RECEIPT movements are always IN
UPDATE stock_movements
SET direction = 'IN'
WHERE direction IS NULL AND UPPER(movement_type) = 'RECEIPT';

-- 2b. SALE movements are always OUT
UPDATE stock_movements
SET direction = 'OUT'
WHERE direction IS NULL AND UPPER(movement_type) = 'SALE';

-- 2c. TRANSFER movements
UPDATE stock_movements
SET direction = 'OUT'
WHERE direction IS NULL AND UPPER(movement_type) = 'TRANSFER' AND LOWER(remarks) LIKE '%out%';

UPDATE stock_movements
SET direction = 'IN'
WHERE direction IS NULL AND UPPER(movement_type) = 'TRANSFER' AND (LOWER(remarks) LIKE '%in%' OR direction IS NULL);

-- 2d. ADJUSTMENT movements
UPDATE stock_movements
SET direction = 'OUT'
WHERE direction IS NULL AND UPPER(movement_type) = 'ADJUSTMENT' AND LOWER(remarks) LIKE '%decreased%';

UPDATE stock_movements
SET direction = 'IN'
WHERE direction IS NULL AND UPPER(movement_type) = 'ADJUSTMENT' AND (LOWER(remarks) LIKE '%increased%' OR direction IS NULL);

-- 2e. Default any remaining unclassified records to IN
UPDATE stock_movements
SET direction = 'IN'
WHERE direction IS NULL;

-- Step 3: Verify backfill
SELECT 
    direction, 
    movement_type, 
    COUNT(*) AS count 
FROM stock_movements 
GROUP BY direction, movement_type;
