package org.codehaus.modello;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.modello.metadata.MetaData;

/**
 * @author <a href="mailto:jason@modello.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public abstract class BaseElement
{
    private String name;

    private String description;

    private String version;

    private String comment;

    private transient Map metaData = new HashMap();

    public abstract void validate()
        throws ModelValidationException;

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public String getVersion()
    {
        return version;
    }

    public String getComment()
    {
        return comment;
    }

    public boolean hasMetaData( String key )
    {
        return metaData.containsKey( key );
    }

    public void addMetaData( MetaData metaData )
    {
        this.metaData.put( metaData.getClass().getName(), metaData );
    }

    public MetaData getMetaData( String key )
    {
        MetaData metaData = (MetaData) this.metaData.get( key );

        if ( metaData == null )
        {
            throw new ModelloRuntimeException( "No such metadata: " + key );
        }

        return metaData;
    }

    // ----------------------------------------------------------------------
    // Validation utils
    // ----------------------------------------------------------------------

    protected void validateFieldNotEmpty( String objectName, String fieldName, String value )
        throws ModelValidationException
    {
        if ( value == null )
        {
            throw new ModelValidationException( "Missing value '" + fieldName + "' from " + objectName + "." );
        }

        if ( isEmpty( value ) )
        {
            throw new ModelValidationException( "Empty value '" + fieldName + "' from " + objectName + "." );
        }
    }

    protected boolean isEmpty( String string )
    {
        return string == null || string.trim().length() == 0;
    }
}
