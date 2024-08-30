DELIMITER $$
DROP table IF EXISTS `generated_id`$$
CREATE TABLE `generated_id` (
  `id` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`))$$

DROP procedure IF EXISTS `GetNewNumber`$$

CREATE PROCEDURE GetNewNumber (
	IN  objectIn VARCHAR(25),
    OUT newNumber VARCHAR(20)
)
BEGIN
	DECLARE inserted BOOLEAN default false;
    DECLARE num  VARCHAR(20);
    DECLARE CONTINUE HANDLER FOR SQLSTATE '23000'
    BEGIN
		SET inserted = false;
    END;
	WHILE inserted <> true DO
		SET inserted = true;
		SET num = generateNumber(objectIn);
		INSERT INTO `generated_id` (`id`) VALUES (CONCAT(objectIn,'-',num));
        SELECT num INTO newNumber;
	END WHILE;
END$$
DELIMITER ;