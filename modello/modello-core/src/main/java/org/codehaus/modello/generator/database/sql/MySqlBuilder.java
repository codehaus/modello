/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.modello.generator.database.sql;

import org.codehaus.modello.generator.database.model.Column;
import org.codehaus.modello.generator.database.model.Table;

import java.io.IOException;
import java.util.List;

/**
 * An SQL Builder for MySQL
 * 
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @author John Marshall/Connectria
 * @version $Revision$
 */
public class MySqlBuilder extends SqlBuilder
{

    public MySqlBuilder()
    {
        setForeignKeysEmbedded( true );
    }

    public void dropTable( Table table ) throws IOException
    {
        print( "drop table if exists " );
        print( table.getName() );
        printEndOfStatement();
    }

    protected void printAutoIncrementColumn( Table table, Column column ) throws IOException
    {
        print( "AUTO_INCREMENT" );
    }

    protected boolean shouldGeneratePrimaryKeys( List primaryKeyColumns )
    {
        /*
         * mySQL requires primary key indication for autoincrement key columns
         * I'm not sure why the default skips the pk statement if all are identity
         */
        return true;
    }

    protected String getNativeType( Column column )
    {
        if ( "timestamp".equalsIgnoreCase( column.getType() ) )
        {
            return "DATETIME";
        }
        else if ( "longvarchar".equalsIgnoreCase( column.getType() ) )
        {
            return "TEXT";
        }
        else
        {
            return column.getType();
        }
    }
}
