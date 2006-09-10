package org.codehaus.modello.plugin.java;

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

import org.codehaus.modello.metadata.AbstractMetadataPlugin;
import org.codehaus.modello.metadata.AssociationMetadata;
import org.codehaus.modello.metadata.ClassMetadata;
import org.codehaus.modello.metadata.FieldMetadata;
import org.codehaus.modello.metadata.MetadataPlugin;
import org.codehaus.modello.metadata.ModelMetadata;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelField;
import org.codehaus.plexus.util.StringUtils;

import java.util.Map;

/**
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse </a>
 * @version $Id$
 */
public class JavaMetadataPlugin
    extends AbstractMetadataPlugin
    implements MetadataPlugin
{
    // ----------------------------------------------------------------------
    // Map to Metadata
    // ----------------------------------------------------------------------

    public ModelMetadata getModelMetadata( Model model, Map data )
    {
        return new JavaModelMetadata();
    }

    public ClassMetadata getClassMetadata( ModelClass clazz, Map data )
    {
        JavaClassMetadata metadata = new JavaClassMetadata();

        String abstractMode = (String) data.get( "java.abstract" );

        if ( abstractMode != null )
        {
            metadata.setAbstract( Boolean.valueOf( abstractMode ).booleanValue() );
        }

        return metadata;
    }

    public FieldMetadata getFieldMetadata( ModelField field, Map data )
    {
        JavaFieldMetadata metadata = new JavaFieldMetadata();

        String adder = (String) data.get( "java.adder" );

        if ( adder != null )
        {
            metadata.setAdder( Boolean.valueOf( adder ).booleanValue() );
        }

        String getter = (String) data.get( "java.getter" );

        if ( getter != null )
        {
            Boolean isGetter = Boolean.valueOf( getter );
            metadata.setGetter( isGetter.booleanValue() );
        }

        String fieldType = field.getType();
        if ( fieldType != null && fieldType.endsWith( "oolean" ) )
        {
            metadata.setBooleanGetter( true );
        }

        String setter = (String) data.get( "java.setter" );

        if ( adder != null )
        {
            metadata.setSetter( Boolean.valueOf( setter ).booleanValue() );
        }

        return metadata;
    }

    public AssociationMetadata getAssociationMetadata( ModelAssociation association, Map data )
    {
        JavaAssociationMetadata metadata = new JavaAssociationMetadata();

        metadata.setGenerateAdd( getBoolean( data, "java.generate-add", true ) );
        metadata.setGenerateRemove( getBoolean( data, "java.generate-remove", true ) );
        metadata.setGenerateBreak( getBoolean( data, "java.generate-break", true ) );
        metadata.setGenerateCreate( getBoolean( data, "java.generate-create", true ) );

        String interfaceName = (String) data.get( "java.use-interface" );

        if ( StringUtils.isNotEmpty( interfaceName ) )
        {
            metadata.setInterfaceName( interfaceName );
        }
        
        String initMode = (String) data.get( "java.init" );
        // default to lazy. (for backwards compatibilty reasons)
        metadata.setInitializationMode( JavaAssociationMetadata.LAZY_INIT );
        
        if ( StringUtils.isNotEmpty( initMode ) )
        {
            metadata.setInitializationMode( initMode );
        }

        return metadata;
    }
}