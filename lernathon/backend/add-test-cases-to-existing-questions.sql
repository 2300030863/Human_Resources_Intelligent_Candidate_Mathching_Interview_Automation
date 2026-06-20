-- Add test cases to existing coding questions
USE recruit_db;

-- Question 101: Java SQL CRUD operations
UPDATE questions 
SET test_cases = '[
  {"input": "CREATE operation", "expectedOutput": "Record created successfully"},
  {"input": "READ operation", "expectedOutput": "Record retrieved successfully"},
  {"input": "UPDATE operation", "expectedOutput": "Record updated successfully"},
  {"input": "DELETE operation", "expectedOutput": "Record deleted successfully"},
  {"input": "Exception handling", "expectedOutput": "Error handled correctly"}
]'
WHERE id = 101 AND type = 'CODING';

-- Question 100: Python sqlite3 database operations
UPDATE questions 
SET test_cases = '[
  {"input": "Create database", "expectedOutput": "Database created"},
  {"input": "Create table", "expectedOutput": "Table created"},
  {"input": "Insert data", "expectedOutput": "Data inserted"},
  {"input": "Query data", "expectedOutput": "Data retrieved"},
  {"input": "Close connection", "expectedOutput": "Connection closed"}
]'
WHERE id = 100 AND type = 'CODING';

-- Question 88: Python SQL database program
UPDATE questions 
SET test_cases = '[
  {"input": "Connect to database", "expectedOutput": "Connection established"},
  {"input": "Execute query", "expectedOutput": "Query executed"},
  {"input": "Fetch results", "expectedOutput": "Results fetched"},
  {"input": "Handle errors", "expectedOutput": "Errors handled"}
]'
WHERE id = 88 AND type = 'CODING';

-- Question 87: Java HashMap
UPDATE questions 
SET test_cases = '[
  {"input": "Put key-value", "expectedOutput": "Value stored"},
  {"input": "Get value", "expectedOutput": "Value retrieved"},
  {"input": "Remove key", "expectedOutput": "Key removed"},
  {"input": "Check if empty", "expectedOutput": "Correct status"}
]'
WHERE id = 87 AND type = 'CODING';

-- Question 75: Java find middle element
UPDATE questions 
SET test_cases = '[
  {"input": "[1,2,3,4,5]", "expectedOutput": "3"},
  {"input": "[1,2,3,4]", "expectedOutput": "2"},
  {"input": "[1]", "expectedOutput": "1"},
  {"input": "[]", "expectedOutput": "null"}
]'
WHERE id = 75 AND type = 'CODING';

-- Verify the updates
SELECT id, type, LEFT(question, 60) as question_preview, 
       CASE WHEN test_cases IS NULL THEN 'NO TEST CASES' 
            ELSE CONCAT(JSON_LENGTH(test_cases), ' test cases') 
       END as status
FROM questions 
WHERE type = 'CODING' AND id IN (75, 87, 88, 100, 101)
ORDER BY id;

SELECT 'Test cases added successfully to existing coding questions!' as Result;
