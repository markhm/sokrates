/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.tsql;

public class TSqlExamples {

    // Simple T-SQL with a line comment and a block comment; after cleaning both should be gone and empty
    // lines removed. String literals are kept by the cleaner (they are emptied only for McCabe analysis).
    public static final String CONTENT_LOC = ""
            + "-- line comment\n"
            + "DECLARE @x INT;\n"
            + "/* block\n"
            + "   comment */\n"
            + "\n"
            + "SET @x = 1;\n";

    public static final String CONTENT_LOC_CLEANED = ""
            + "DECLARE @x INT;\n"
            + "SET @x = 1;";

    // Duplication cleaning strips comments and trims per-line whitespace (collapses runs of spaces).
    // String literals are kept intact; they are emptied only during McCabe analysis.
    public static final String CONTENT_DUP = ""
            + "-- comment\n"
            + "  SELECT   col, @name \n"
            + "  FROM   dbo.t\n";

    public static final String CONTENT_DUP_CLEANED = ""
            + "SELECT col, @name\n"
            + "FROM dbo.t";

    // Simple procedure with one IF/ELSE branch. McCabe = 1 + 1 (IF) = 2, params = 1.
    public static final String SIMPLE_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.foo @a INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  IF @a > 0\n"
            + "    SELECT 1\n"
            + "  ELSE\n"
            + "    SELECT 0\n"
            + "END\n";

    // Procedure exercising WHILE + CASE/WHEN counting. McCabe = 1 + WHILE(1) + CASE(1) + WHEN(2) = 5.
    public static final String WHILE_AND_CASE_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.bar\n"
            + "AS\n"
            + "BEGIN\n"
            + "  DECLARE @i INT = 0\n"
            + "  WHILE @i < 10\n"
            + "  BEGIN\n"
            + "    SELECT CASE WHEN @i = 0 THEN 'a' WHEN @i = 1 THEN 'b' ELSE 'c' END\n"
            + "    SET @i = @i + 1\n"
            + "  END\n"
            + "END\n";

    // TRY/CATCH pairs must NOT affect BEGIN/END depth (self-balancing). McCabe = 1 + BEGIN_CATCH(1) = 2.
    public static final String TRY_CATCH_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.baz\n"
            + "AS\n"
            + "BEGIN\n"
            + "  BEGIN TRY\n"
            + "    SELECT 1\n"
            + "  END TRY\n"
            + "  BEGIN CATCH\n"
            + "    SELECT 2\n"
            + "  END CATCH\n"
            + "END\n";

    // Parenthesised parameter list: (@a INT, @b VARCHAR(10)).
    public static final String PAREN_PARAMS_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.p1 (@a INT, @b VARCHAR(10))\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // Bareword parameter list: @a INT, @b VARCHAR(10) — T-SQL only.
    public static final String BAREWORD_PARAMS_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.p2 @a INT, @b VARCHAR(10)\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // Inline table-valued function: no BEGIN/END, terminated by statement semicolon.
    public static final String INLINE_TVF = ""
            + "CREATE FUNCTION dbo.f1 (@a INT) RETURNS TABLE AS RETURN (SELECT @a AS x);\n";

    // Two procedures separated by a GO batch boundary — must be extracted as 2 distinct units.
    public static final String TWO_PROCS_WITH_GO = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // CREATE OR ALTER introduced in SQL Server 2016+.
    public static final String CREATE_OR_ALTER = ""
            + "CREATE OR ALTER PROCEDURE dbo.p3 @a INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT @a\n"
            + "END\n";

    // Bracket-quoted name preserves the bracketed, schema-qualified form in shortName.
    public static final String BRACKETED_NAME = ""
            + "CREATE PROCEDURE [dbo].[weird name]\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";
}
