package org.codehaus.modello.generator.database.sql;

/*
 * Copyright (c) 2004, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.IOException;

import org.codehaus.modello.generator.database.model.Column;
import org.codehaus.modello.generator.database.model.Table;

/**
 * An SQL Builder for PostgresSqlL
 * 
 * @author <a href="mailto:john@zenplex.com">John Thorhauer</a>
 * @version $Revision$
 */
public class PostgreSqlBuilder extends SqlBuilder
    {

    public PostgreSqlBuilder()
    {

    }

    protected void printAutoIncrementColumn( Table table, Column column ) throws IOException
    {
        print( " " );
        print( "serial" );
        print( " " );

    }

    /**
     * Outputs the DDL to add a column to a table.
     */
    public void createColumn( Table table, Column column ) throws IOException
    {
        print( column.getName() );
        print( " " );
        if ( column.isAutoIncrement() )
        {
            printAutoIncrementColumn( table, column );
        }
        else
        {

            print( getSqlType( column ) );
            print( " " );

            if ( column.getDefaultValue() != null )
            {
                print( "DEFAULT '" + column.getDefaultValue() + "' " );
            }
            if ( column.isRequired() )
            {
                printNotNullable();
            }
            else
            {
                printNullable();
            }
            print( " " );
        }
    }

    /**
     * @return the full SQL type string including the size
     */
    protected String getSqlType( Column column )
    {

        if ( column.getTypeCode() == java.sql.Types.VARBINARY )
        {
            return "OID";
        }
        else
        {
            return super.getSqlType( column );
        }
    }

}
