package org.codehaus.modello;

/*
 * Copyright (c) 2004, Jason van Zyl
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

import org.codehaus.modello.metadata.FieldMetadata;

/**
 * @author <a href="mailto:jason@modello.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class ModelField
    extends BaseElement
{
    private String type;

    private String specification;

    private String defaultValue;

    private String typeValidator;

    private boolean required;

    transient private ModelClass modelClass;

    transient private boolean primitive;

    public ModelField()
    {
        super( true );
    }

    public ModelField( ModelClass modelClass, String name )
    {
        super( true, name );

        this.modelClass = modelClass;
    }

    // ----------------------------------------------------------------------
    // Property accessors
    // ----------------------------------------------------------------------

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue( String defaultValue )
    {
        this.defaultValue = defaultValue;
    }

    public String getSpecification()
    {
        return specification;
    }

    public void setSpecifiaction( String specification )
    {
        this.specification = specification;
    }

    public String getTypeValidator()
    {
        return typeValidator;
    }

    public void setTypeValidator( String typeValidator )
    {
        this.typeValidator = typeValidator;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired( boolean required )
    {
        this.required = required;
    }

    // ----------------------------------------------------------------------
    // Misc
    // ----------------------------------------------------------------------

    public ModelClass getModelClass()
    {
        return modelClass;
    }

    /**
     * @return Returns true if this field is a java primitive.
     */
    public boolean isPrimitive()
    {
        return primitive;
    }

    public FieldMetadata getMetadata( String key )
    {
        return (FieldMetadata) getMetadata( FieldMetadata.class, key );
    }

    public void initialize( ModelClass modelClass )
    {
        this.modelClass = modelClass;
    }

    public void validateElement()
        throws ModelValidationException
    {
        validateFieldNotEmpty( "Field", "name", getName() );

        validateFieldNotEmpty( "Field " + getName(), "type", type );

        String[] primitiveTypes = new String[]{
            "boolean",
            "char",
            "short",
            "int",
            "long",
            "float",
            "double",
            "String"
        };

        for ( int i = 0; i < primitiveTypes.length; i++ )
        {
            String validType = primitiveTypes[i];

            if ( type.equals( validType ) )
            {
                primitive = true;

                return;
            }
        }

        ModelClass modelClass = getModelClass().getModel().getClass( type );

        if ( modelClass != null )
        {
            return;
        }

        throw new ModelValidationException( "Field '" + getName() + "': Illegal type: '" + type + "'." );
    }
}
