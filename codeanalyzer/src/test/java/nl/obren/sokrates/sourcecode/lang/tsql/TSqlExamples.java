package nl.obren.sokrates.sourcecode.lang.tsql;

public class TSqlExamples {

    public static final String CONTENT_1 = "-- Variable declaration\n" +
            "DECLARE @name NVARCHAR(20), \n" +
            "        @company NVARCHAR(30), \n" +
            "        @introduction NVARCHAR(MAX), \n" +
            "        @choice CHAR(1);\n" +
            "\n" +
            "SET @name = 'John Smith'; \n" +
            "SET @company = 'Infotech'; \n" +
            "SET @introduction = ' Hello! I''m John Smith from Infotech.'; \n" +
            "SET @choice = 'y';\n" +
            "\n" +
            "IF @choice = 'y'\n" +
            "BEGIN\n" +
            "    PRINT @name;\n" +
            "    PRINT @company;\n" +
            "    PRINT @introduction;\n" +
            "END";

    public static final String CONTENT_1_CLEANED = "DECLARE @name NVARCHAR(20), \n" +
            "        @company NVARCHAR(30), \n" +
            "        @introduction NVARCHAR(MAX), \n" +
            "        @choice CHAR(1);\n" +
            "SET @name = 'John Smith'; \n" +
            "SET @company = 'Infotech'; \n" +
            "SET @introduction = ' Hello! I''m John Smith from Infotech.'; \n" +
            "SET @choice = 'y';\n" +
            "IF @choice = 'y'\n" +
            "BEGIN\n" +
            "    PRINT @name;\n" +
            "    PRINT @company;\n" +
            "    PRINT @introduction;\n" +
            "END";

    public static final String CONTENT_2 = "-- Variable and exception declaration\n" +
            "DECLARE @c_id INT = &cc_id;\n" +
            "DECLARE @c_name NVARCHAR(50), @c_addr NVARCHAR(100);\n" +
            "DECLARE @ex_invalid_id EXCEPTION;\n" +
            "\n" +
            "BEGIN TRY\n" +
            "    IF @c_id <= 0\n" +
            "        THROW 50000, 'ID must be greater than zero!', 1;\n" +
            "    ELSE\n" +
            "    BEGIN\n" +
            "        SELECT @c_name = name, @c_addr = address\n" +
            "        FROM customers\n" +
            "        WHERE id = @c_id;\n" +
            "        PRINT 'Name: ' + @c_name;\n" +
            "        PRINT 'Address: ' + @c_addr;\n" +
            "    END\n" +
            "END TRY\n" +
            "BEGIN CATCH\n" +
            "    IF ERROR_NUMBER() = 50000\n" +
            "        PRINT ERROR_MESSAGE();\n" +
            "    ELSE IF ERROR_NUMBER() = 104 -- Example error number for no data found, actual may vary\n" +
            "        PRINT 'No such customer!';\n" +
            "    ELSE\n" +
            "        PRINT 'Error!';\n" +
            "END CATCH";

    public static final String CONTENT_2_CLEANED_FOR_DUPLICATION = "DECLARE @c_id INT = &cc_id;\n" +
            "DECLARE @c_name NVARCHAR(50), @c_addr NVARCHAR(100);\n" +
            "DECLARE @ex_invalid_id EXCEPTION;\n" +
            "BEGIN TRY\n" +
            "IF @c_id <= 0\n" +
            "THROW 50000, 'ID must be greater than zero!', 1;\n" +
            "ELSE\n" +
            "BEGIN\n" +
            "SELECT @c_name = name, @c_addr = address\n" +
            "FROM customers\n" +
            "WHERE id = @c_id;\n" +
            "PRINT 'Name: ' + @c_name;\n" +
            "PRINT 'Address: ' + @c_addr;\n" +
            "END\n" +
            "END TRY\n" +
            "BEGIN CATCH\n" +
            "IF ERROR_NUMBER() = 50000\n" +
            "PRINT ERROR_MESSAGE();\n" +
            "ELSE IF ERROR_NUMBER() = 104\n" +
            "PRINT 'No such customer!';\n" +
            "ELSE\n" +
            "PRINT 'Error!';\n" +
            "END CATCH";

    public static final String CONTENT_3 = "CREATE PROCEDURE CreateEmailAddress\n" +
            "    @name1 NVARCHAR(50),\n" +
            "    @name2 NVARCHAR(50),\n" +
            "    @company NVARCHAR(50),\n" +
            "    @email NVARCHAR(150) OUTPUT\n" +
            "AS\n" +
            "BEGIN\n" +
            "    SET @email = @name1 + '.' + @name2 + '@' + @company;\n" +
            "    \n" +
            "    -- This is where the logic happens:\n" +
            "    IF LEN(@email) > 150\n" +
            "    BEGIN\n" +
            "        PRINT 'Email address is too long.';\n" +
            "    END\n" +
            "END";

    public static final String CONTENT_4 = "CREATE TYPE aa_type AS TABLE\n" +
            "(\n" +
            "    KeyColumn VARCHAR(15),\n" +
            "    ValueColumn INT\n" +
            ");\n" +
            "\n" +
            "-- T-SQL does not support package creation like PL/SQL.\n" +
            "-- Instead, we might use stored procedures or functions for similar operations.\n" +
            "CREATE PROCEDURE PrintAA\n" +
            "    @aa aa_type READONLY\n" +
            "AS\n" +
            "BEGIN\n" +
            "    DECLARE @i VARCHAR(15);\n" +
            "    \n" +
            "    SELECT TOP 1 @i = KeyColumn FROM @aa ORDER BY KeyColumn;\n" +
            "    \n" +
            "    WHILE @i IS NOT NULL\n" +
            "    BEGIN\n" +
            "        PRINT CONVERT(VARCHAR(15), (SELECT ValueColumn FROM @aa WHERE KeyColumn = @i)) + '  ' + @i;\n" +
            "        SELECT TOP 1 @i = KeyColumn \n" +
            "        FROM @aa \n" +
            "        WHERE KeyColumn > @i \n" +
            "        ORDER BY KeyColumn;\n" +
            "    END\n" +
            "END;\n" +
            "\n" +
            "-- Example usage of the procedure\n" +
            "DECLARE @aa_var aa_type;\n" +
            "\n" +
            "-- Insert values into the table variable\n" +
            "INSERT INTO @aa_var (KeyColumn, ValueColumn)\n" +
            "VALUES \n" +
            "    ('zero', 0),\n" +
            "    ('one', 1),\n" +
            "    ('two', 2);\n" +
            "\n" +
            "-- Call the stored procedure with the table variable\n" +
            "EXEC PrintAA @aa_var;\n";

    public static final String CONTENT_5 = "-- T-SQL does not support packages like PL/SQL. Instead, we create separate stored procedures.\n" +
            "\n" +
            "-- Add Customer Procedure\n" +
            "CREATE PROCEDURE AddCustomer\n" +
            "    @c_id INT,\n" +
            "    @c_name NVARCHAR(50),\n" +
            "    @c_age INT,\n" +
            "    @c_addr NVARCHAR(100),\n" +
            "    @c_sal DECIMAL(10, 2)\n" +
            "AS\n" +
            "BEGIN\n" +
            "    BEGIN TRY\n" +
            "        INSERT INTO customers (id, name, age, address, salary)\n" +
            "        VALUES (@c_id, @c_name, @c_age, @c_addr, @c_sal);\n" +
            "    END TRY\n" +
            "    BEGIN CATCH\n" +
            "        IF ERROR_NUMBER() = 2627 -- Violation of PRIMARY KEY constraint\n" +
            "            PRINT 'Customer ID already exists.';\n" +
            "        ELSE\n" +
            "            PRINT 'An error occurred adding the customer.';\n" +
            "    END CATCH\n" +
            "END;\n" +
            "\n" +
            "-- Delete Customer Procedure\n" +
            "CREATE PROCEDURE DeleteCustomer\n" +
            "    @c_id INT\n" +
            "AS\n" +
            "BEGIN\n" +
            "    DELETE FROM customers\n" +
            "    WHERE id = @c_id;\n" +
            "END;\n" +
            "\n" +
            "-- List Customers Procedure\n" +
            "CREATE PROCEDURE ListCustomers\n" +
            "AS\n" +
            "BEGIN\n" +
            "    DECLARE @name NVARCHAR(50);\n" +
            "    DECLARE customer_cursor CURSOR FOR\n" +
            "    SELECT name FROM customers;\n" +
            "    \n" +
            "    OPEN customer_cursor;\n" +
            "    \n" +
            "    FETCH NEXT FROM customer_cursor INTO @name;\n" +
            "    WHILE @@FETCH_STATUS = 0\n" +
            "    BEGIN\n" +
            "        PRINT 'Customer: ' + @name;\n" +
            "        FETCH NEXT FROM customer_cursor INTO @name;\n" +
            "    END\n" +
            "    \n" +
            "    CLOSE customer_cursor;\n" +
            "    DEALLOCATE customer_cursor;\n" +
            "END;\n" +
            "\n" +
            "-- Note: In T-SQL, you typically would not encapsulate these procedures within a single script\n" +
            "-- like you might with a PL/SQL package body. Each would be a standalone script or stored in the database.\n";

    public static final String CONTENT_6 = "-- Assuming procedures AddCustomer, DeleteCustomer, and ListCustomers are defined\n" +
            "DECLARE @code INT = 8;\n" +
            "\n" +
            "EXEC AddCustomer @c_id = 7, @c_name = 'Rajnish', @c_age = 25, @c_addr = 'Chennai', @c_sal = 3500;\n" +
            "EXEC AddCustomer @c_id = 8, @c_name = 'Subham', @c_age = 32, @c_addr = 'Delhi', @c_sal = 7500;\n" +
            "\n" +
            "EXEC ListCustomers;\n" +
            "\n" +
            "EXEC DeleteCustomer @c_id = @code;\n" +
            "\n" +
            "EXEC ListCustomers;\n";

    public static final String CONTENT_7 = "-- Creating a procedure instead of a package for bonus calculation\n" +
            "CREATE PROCEDURE CalculateBonus\n" +
            "    @date_hired DATE\n" +
            "AS\n" +
            "BEGIN\n" +
            "    PRINT 'Employees hired on ' + CAST(@date_hired AS NVARCHAR(30)) + ' get bonus.';\n" +
            "END;\n" +
            "\n" +
            "-- Note: This would typically be called with a date parameter,\n" +
            "-- like EXEC CalculateBonus @date_hired = '2023-01-01';\n";

    public static final String CONTENT_8 = "-- This would be a stored procedure to fetch from a cursor\n" +
            "-- Assuming sr_pkg.c is a cursor defined somewhere else or replaced here with direct SQL\n" +
            "CREATE PROCEDURE FetchFromCursor\n" +
            "AS\n" +
            "BEGIN\n" +
            "    DECLARE @v_name NVARCHAR(50);\n" +
            "    DECLARE @CursorStatus INT;\n" +
            "\n" +
            "    -- Here we assume a cursor named 'customerCursor' for demonstration\n" +
            "    DECLARE customerCursor CURSOR LOCAL FOR\n" +
            "        SELECT name FROM people;\n" +
            "\n" +
            "    OPEN customerCursor;\n" +
            "    \n" +
            "    IF @@CURSOR_ROWS > 0  -- Check if cursor has rows\n" +
            "    BEGIN\n" +
            "        FETCH NEXT FROM customerCursor INTO @v_name;\n" +
            "        WHILE @@FETCH_STATUS = 0\n" +
            "        BEGIN\n" +
            "            EXEC DeleteCustomer @c_id = 8; -- Assuming DeleteCustomer procedure exists and takes customer ID\n" +
            "            PRINT 'Fetched: ' + @v_name;\n" +
            "            FETCH NEXT FROM customerCursor INTO @v_name;\n" +
            "        END\n" +
            "    END\n" +
            "    ELSE\n" +
            "        PRINT 'Cursor is empty.';\n" +
            "\n" +
            "    CLOSE customerCursor;\n" +
            "    DEALLOCATE customerCursor;\n" +
            "END;\n";

    public static final String CONTENT_9 = "-- Note: T-SQL does not support packages, so we'll create procedures for each function.\n" +
            "-- For the purpose of this example, we'll assume a naming convention similar to PL/SQL packages for organization.\n" +
            "\n" +
            "-- Add Customer Procedure\n" +
            "CREATE PROCEDURE GF_PR_Print_P049_AddCustomer\n" +
            "    @c_id INT,\n" +
            "    @c_name NVARCHAR(50),\n" +
            "    @c_age INT,\n" +
            "    @c_addr NVARCHAR(100),\n" +
            "    @c_sal DECIMAL(10, 2)\n" +
            "AS\n" +
            "BEGIN\n" +
            "    BEGIN TRY\n" +
            "        INSERT INTO customers (id, name, age, address, salary)\n" +
            "        VALUES (@c_id, @c_name, @c_age, @c_addr, @c_sal);\n" +
            "        PRINT 'Customer added successfully.';\n" +
            "    END TRY\n" +
            "    BEGIN CATCH\n" +
            "        IF ERROR_NUMBER() = 2627 -- Violation of PRIMARY KEY constraint\n" +
            "            PRINT 'Customer ID already exists.';\n" +
            "        ELSE\n" +
            "            PRINT 'Error occurred: ' + ERROR_MESSAGE();\n" +
            "    END CATCH\n" +
            "END;\n" +
            "\n" +
            "-- Delete Customer Procedure\n" +
            "CREATE PROCEDURE GF_PR_Print_P049_DeleteCustomer\n" +
            "    @c_id INT\n" +
            "AS\n" +
            "BEGIN\n" +
            "    DELETE FROM customers\n" +
            "    WHERE id = @c_id;\n" +
            "    IF @@ROWCOUNT = 0\n" +
            "        PRINT 'No customer found with this ID.';\n" +
            "    ELSE\n" +
            "        PRINT 'Customer deleted.';\n" +
            "END;\n" +
            "\n" +
            "-- List Customers Procedure\n" +
            "CREATE PROCEDURE GF_PR_Print_P049_ListCustomers\n" +
            "AS\n" +
            "BEGIN\n" +
            "    DECLARE @Counter INT = 0;\n" +
            "    DECLARE @NameList TABLE (Name NVARCHAR(50));\n" +
            "    \n" +
            "    INSERT INTO @NameList\n" +
            "    SELECT name FROM customers;\n" +
            "    \n" +
            "    DECLARE name_cursor CURSOR LOCAL FOR\n" +
            "    SELECT Name FROM @NameList;\n" +
            "\n" +
            "    OPEN name_cursor;\n" +
            "    \n" +
            "    DECLARE @name NVARCHAR(50);\n" +
            "    FETCH NEXT FROM name_cursor INTO @name;\n" +
            "    WHILE @@FETCH_STATUS = 0\n" +
            "    BEGIN\n" +
            "        SET @Counter = @Counter + 1;\n" +
            "        PRINT 'Customer(' + CAST(@Counter AS NVARCHAR(10)) + '): ' + @name;\n" +
            "        FETCH NEXT FROM name_cursor INTO @name;\n" +
            "    END\n" +
            "    \n" +
            "    CLOSE name_cursor;\n" +
            "    DEALLOCATE name_cursor;\n" +
            "    \n" +
            "    IF @Counter = 0\n" +
            "        PRINT 'No customers to list.';\n" +
            "END;\n";

    public static final String CONTENT_10 = "-- T-SQL script for demonstrating the use of the previously defined procedures\n" +
            "\n" +
            "-- Adding a customer\n" +
            "EXEC GF_PR_Print_P049_AddCustomer @c_id = 10, @c_name = 'Alice Wonderland', @c_age = 30, @c_addr = 'Wonderland', @c_sal = 8000;\n" +
            "\n" +
            "-- Listing all customers\n" +
            "EXEC GF_PR_Print_P049_ListCustomers;\n" +
            "\n" +
            "-- Attempting to add a duplicate customer to see error handling\n" +
            "EXEC GF_PR_Print_P049_AddCustomer @c_id = 10, @c_name = 'Another Alice', @c_age = 31, @c_addr = 'Another Place', @c_sal = 8500;\n" +
            "\n" +
            "-- Deleting a customer\n" +
            "EXEC GF_PR_Print_P049_DeleteCustomer @c_id = 10;\n" +
            "\n" +
            "-- Listing customers again to verify deletion\n" +
            "EXEC GF_PR_Print_P049_ListCustomers;\n" +
            "\n" +
            "-- Attempting to delete a non-existent customer\n" +
            "EXEC GF_PR_Print_P049_DeleteCustomer @c_id = 100;\n";
}
