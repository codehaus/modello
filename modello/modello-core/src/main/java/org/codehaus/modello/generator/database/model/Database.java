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
package org.codehaus.modello.generator.database.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Models a database.
 *
 * @author John Marshall/Connectria
 * @author Matthew Hawthorne
 * @version $Id$
 */
public class Database
{
    private String name;

    private String idMethod;

    /**
     * Database version id
     */
    private String version;

    private List tables = new ArrayList();

    public Database()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String ver )
    {
        version = ver;
    }


    public void setIdMethod( String idMethod )
    {
        this.idMethod = idMethod;
    }


    public void addTable( Table table )
    {
        tables.add( table );
    }

    public List getTables()
    {
        return tables;
    }

    // Helper methods

    /**
     * Finds the table with the specified name, using case insensitive matching.
     * Note that this method is not called getTable(String) to avoid introspection
     * problems.
     */
    public Table findTable( String name )
    {
        for ( Iterator iter = tables.iterator(); iter.hasNext(); )
        {
            Table table = (Table) iter.next();

            // table names are typically case insensitive
            if ( table.getName().equalsIgnoreCase( name ) )
            {
                return table;
            }
        }
        return null;
    }


    // Additions for PropertyUtils

    public void setTable( int index, Table table )
    {
        addTable( table );
    }

    public Table getTable( int index )
    {
        return (Table) tables.get( index );
    }


    public String toString()
    {
        return super.toString() + "[name=" + name + ";tableCount=" + tables.size() + "]";
    }
}
