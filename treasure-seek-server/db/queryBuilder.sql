


SELECT * FROM (
    SELECT *, 0 as found 
    FROM treasure 
    WHERE (1, treasure.id) 
    IN (select * from user_treasure) 
    
    UNION 
    
    SELECT *, 1 as found 
    FROM treasure 
    WHERE (1, treasure.id) 
    NOT IN (select * from user_treasure)
);