-- We are currently not using this.
-- This is a function that converts a lowercase string to cammel case.
-- example of how to use it: update source set external_name = pCase(lcase(external_name)) where external_id like '%CAPS%'

CREATE FUNCTION pCase (str TEXT)
RETURNS text
DETERMINISTIC
BEGIN
  DECLARE result TEXT default '';
  DECLARE space INT default 0;
  DECLARE last_space INT default 0;

  # handle NULL
  IF (str IS NULL) THEN
    RETURN NULL;
  END IF;

  # if 0 length string given
  IF (char_length(str) = 0) THEN
    RETURN '';
  END IF;

  # upper case the first letter
  SET result = upper(left(str,1));
  SET space = locate(' ', str);

  # loop through remaining spaces
  WHILE space > 0 DO

    # add everything up to that space
    SET result = CONCAT(result, SUBSTRING(str, last_space+2, space-last_space-1));

    # upper case the letter after the found space
    SET result = CONCAT(result, UPPER(SUBSTRING(str, space+1, 1)));

    # find next space
    SET last_space = space;
    SET space = locate(' ', str, space+2);

  END WHILE;

  # add final section
  SET result = CONCAT(result, SUBSTRING(str, last_space+2));

  RETURN result;

END