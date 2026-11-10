-- 2026-11-10: 为 litemall_order 增加 fulfillment_date 字段并回填历史数据
ALTER TABLE `litemall_order` 
ADD COLUMN `fulfillment_date` DATE DEFAULT NULL COMMENT '履约日期(送餐/自提业务发生日)' AFTER `message`;

CREATE INDEX `idx_fulfillment_date` ON `litemall_order` (`fulfillment_date`);

-- 回填已有历史订单的 fulfillment_date
UPDATE `litemall_order`
SET `fulfillment_date` = CASE 
    WHEN `message` LIKE '%市二%' THEN 
      IF(TIME(`add_time`) > '07:00:00', DATE_ADD(DATE(`add_time`), INTERVAL 1 DAY), DATE(`add_time`))
    WHEN `message` LIKE '%师大%' THEN 
      IF(TIME(`add_time`) > '16:30:00', DATE_ADD(DATE(`add_time`), INTERVAL 1 DAY), DATE(`add_time`))
    WHEN `message` LIKE '%明天%' THEN 
      DATE_ADD(DATE(`add_time`), INTERVAL 1 DAY)
    ELSE 
      IF(TIME(`add_time`) > '18:00:00', DATE_ADD(DATE(`add_time`), INTERVAL 1 DAY), DATE(`add_time`))
END
WHERE `fulfillment_date` IS NULL;
