DELIMITER $$
CREATE FUNCTION generateNumber(objectIn VARCHAR(50))
   RETURNS VARCHAR(20)
   DETERMINISTIC
   BEGIN
	DECLARE finalNumber VARCHAR(20);
	DECLARE newNumber int;
	DECLARE currentNumber VARCHAR(20);
    DECLARE currentPrefix varchar(20);
	SELECT current, prefix into currentNumber, currentPrefix FROM number_range where object = objectIn LIMIT 1;
	set currentPrefix = IFNULL(currentPrefix, '');
	SET newNumber = REPLACE(currentNumber, currentPrefix, '') + 1;
    SET finalNumber = CONCAT(currentPrefix, lpad(newNumber,10,0 ));
    UPDATE number_range SET current = finalNumber where object = objectIn;
	return finalNumber;
   END$$

CREATE PROCEDURE GetNewNumber (
	IN  objectIn VARCHAR(25),
	OUT newNumber VARCHAR(20)
)
BEGIN
	SELECT generateNumber(objectIn) INTO newNumber;
END$$

DELIMITER ;

