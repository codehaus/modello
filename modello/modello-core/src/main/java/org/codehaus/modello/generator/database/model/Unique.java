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

import java.util.List;

/**
 * Provides compatibility with Torque-style xml with separate &lt;index&gt; and
 * &lt;unique&gt; tags, but adds no functionality.  All indexes are treated the
 * same by the Table.
 * 
 * @author <a href="mailto:jmarshall@connectria.com">John Marshall</a>
 * @version $Revision$
 */
public class Unique extends Index
{
    public Unique()
    {
        setUnique( true );
    }

    public void setUnique( boolean unique )
    {
        if ( unique == false )
        {
            throw new IllegalArgumentException( "Unique index cannot be made non-unique" );
        }
        super.setUnique( unique );
    }

    public boolean isUnique()
    {
        return true;
    }

    public void addUniqueColumn( UniqueColumn indexColumn )
    {
        super.addIndexColumn( indexColumn );
    }

    public List getUniqueColumns()
    {
        return super.getIndexColumns();
    }

}
