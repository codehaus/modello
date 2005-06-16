package org.codehaus.modello.plugin.jpox;

/*
 * Copyright (c) 2005, Codehaus.org
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.plugin.AbstractModelloGenerator;
import org.codehaus.modello.plugin.store.metadata.StoreAssociationMetadata;
import org.codehaus.modello.plugin.store.metadata.StoreFieldMetadata;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class JPoxJdoMappingModelloGenerator
    extends AbstractModelloGenerator
{
    private final static Map PRIMITVE_IDENTITY_MAP;

    static
    {
        PRIMITVE_IDENTITY_MAP = new HashMap();

        // TODO: These should be the fully qualified class names
        PRIMITVE_IDENTITY_MAP.put( "short", "javax.jdo.identity.ShortIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "Short", "javax.jdo.identity.ShortIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "int", "javax.jdo.identity.IntIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "Integer", "javax.jdo.identity.IntIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "long", "javax.jdo.identity.LongIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "Long", "javax.jdo.identity.LongIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "String", "javax.jdo.identity.StringIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "char", "javax.jdo.identity.CharIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "Character", "javax.jdo.identity.CharIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "byte", "javax.jdo.identity.ByteIdentity" );
        PRIMITVE_IDENTITY_MAP.put( "Byte", "javax.jdo.identity.ByteIdentity" );
    }

    public void generate( Model model, Properties properties )
        throws ModelloException
    {
        initialize( model, properties );

        // ----------------------------------------------------------------------
        // Generate the JDO files
        // ----------------------------------------------------------------------

        try
        {
            File packageJdo = new File( getOutputDirectory(), "META-INF/package.jdo" );

            File parent = packageJdo.getParentFile();

            if ( !parent.exists() )
            {
                if( !parent.mkdirs() )
                {
                    throw new ModelloException( "Error while creating parent directories for the file " +
                                                "'" + packageJdo.getAbsolutePath() + "'." );
                }
            }

            generatePackageJdo( packageJdo, model );
        }
        catch ( IOException e )
        {
            throw new ModelloException( "Error while writing package.jdo.", e );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void generatePackageJdo( File file, Model model )
        throws IOException, ModelloException
    {
        OutputStreamWriter fileWriter = new OutputStreamWriter( new FileOutputStream( file ) , "UTF-8" );

        PrintWriter printWriter = new PrintWriter( fileWriter );

        XMLWriter writer = new PrettyPrintXMLWriter( printWriter );

        Map classes = new HashMap();

        for ( Iterator it = model.getClasses( getGeneratedVersion() ).iterator(); it.hasNext(); )
        {
            ModelClass modelClass = (ModelClass) it.next();

//            StoreClassMetadata metadata = (StoreClassMetadata) modelClass.getMetadata( StoreClassMetadata.ID );
//
//            if ( !metadata.isStorable() )
//            {
//                continue;
//            }
//
            String packageName = modelClass.getPackageName( isPackageWithVersion(), getGeneratedVersion() );

            List list = (List) classes.get( packageName );

            if ( list == null )
            {
                list = new ArrayList();

                classes.put( packageName, list );
            }

            list.add( modelClass );
        }

        printWriter.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
        printWriter.println();
        printWriter.println( "<!DOCTYPE jdo PUBLIC" );
        printWriter.println( "  \"-//Sun Microsystems, Inc.//DTD Java Data Objects Metadata 2.0//EN\"" );
        printWriter.println( "  \"http://java.sun.com/dtd/jdo_2_0.dtd\">" );
        printWriter.println();

        writer.startElement( "jdo" );

        for ( Iterator it = classes.values().iterator(); it.hasNext(); )
        {
            List list = (List) it.next();

            if ( list.size() == 0 )
            {
                continue;
            }

            String packageName = ((ModelClass) list.get( 0 ) ).getPackageName( isPackageWithVersion(),
                                                                               getGeneratedVersion() );

            writer.startElement( "package" );

            writer.addAttribute( "name", packageName );

            for ( Iterator it2 = list.iterator(); it2.hasNext(); )
            {
                ModelClass modelClass = (ModelClass) it2.next();

                writeClass( writer, modelClass );
            }

            writer.endElement(); // package
        }

        writer.endElement(); // jdo

        printWriter.println();

        printWriter.close();
    }

    private void writeClass( XMLWriter writer, ModelClass modelClass )
        throws ModelloException
    {
        writer.startElement( "class" );

        writer.addAttribute( "name", modelClass.getName() );

        ModelClass persistenceCapableSuperclass = null;

        if ( modelClass.getSuperClass() != null )
        {
            persistenceCapableSuperclass = getModel().getClass( modelClass.getSuperClass(),
                                                                getGeneratedVersion() );
        }

        if ( persistenceCapableSuperclass != null )
        {
            String superPackageName = persistenceCapableSuperclass.getPackageName( isPackageWithVersion(),
                                                                                   getGeneratedVersion() );

            writer.addAttribute( "persistence-capable-superclass",
                                 superPackageName + "." + persistenceCapableSuperclass.getName() );
        }

        writer.addAttribute( "detachable", "true" );

        // ----------------------------------------------------------------------
        // If this class has a primary key field mark make jpox manage the id
        // as a autoincrement variable
        // ----------------------------------------------------------------------

        List fields = Collections.unmodifiableList( modelClass.getFields( getGeneratedVersion() ) );

        boolean applicationIdentityType = false;

        for ( Iterator it = fields.iterator(); it.hasNext(); )
        {
            ModelField modelField = (ModelField) it.next();

            if ( modelField.getName().equals( "id" ) )
            {
                String type = modelField.getType();

                String objectIdClass = (String) PRIMITVE_IDENTITY_MAP.get( type );

                if ( objectIdClass == null )
                {
                    throw new ModelloException( "The JDO mapping generator does not support the specified " +
                                                "field type '" + modelField.getType() + "'. " +
                                                "Supported types: " + PRIMITVE_IDENTITY_MAP.keySet() );
                }

                applicationIdentityType = true;

                break;
            }
        }

        // TODO: for now, assume that any primary key will be set in the super class

        if ( persistenceCapableSuperclass == null )
        {
            if ( applicationIdentityType )
            {
                writer.addAttribute( "identity-type", "application" );
            }
            else
            {
                writer.addAttribute( "identity-type", "datastore" );
            }
        }
        else
        {
            writer.startElement( "inheritance" );

            writer.addAttribute( "strategy", "new-table");

            writer.endElement();
        }

        // ----------------------------------------------------------------------
        // Write all fields
        // ----------------------------------------------------------------------

        for ( Iterator it = fields.iterator(); it.hasNext(); )
        {
            ModelField modelField = (ModelField) it.next();

            writeModelField( writer,  modelField );
        }

        // ----------------------------------------------------------------------
        // Write out the "detailed" fetch group. This group will by default
        // contain all fields in a object. The default fetch group will contain
        // all the primitives in a class as by JDO defaults.
        // ----------------------------------------------------------------------

        List detailedFields = new ArrayList();

        for ( Iterator it = fields.iterator(); it.hasNext(); )
        {
            ModelField field = (ModelField) it.next();

            if ( field.isPrimitive() )
            {
                continue;
            }

            if ( field instanceof ModelAssociation )
            {
                StoreAssociationMetadata metaData = getAssociationMetadata( (ModelAssociation) field );

                if ( metaData.isPart() != null && !metaData.isPart().booleanValue() )
                {
                    continue;
                }
            }

            detailedFields.add( field );
        }

        writeFetchGroup( writer, modelClass.getName() + "_detail", detailedFields );

        writer.endElement(); // class
    }

    private void writeFetchGroup( XMLWriter writer, String fetchGroupName, List fields )
    {
        writer.startElement( "fetch-group");

        writer.addAttribute( "name", fetchGroupName );

        for ( Iterator it = fields.iterator(); it.hasNext(); )
        {
            ModelField field = (ModelField) it.next();

            writer.startElement( "field" );

            writer.addAttribute( "name", field.getName() );

            writer.endElement();
        }

        writer.endElement(); // fetch-group
    }

    private void writeModelField( XMLWriter writer, ModelField modelField )
    {
        writer.startElement( "field" );

        StoreFieldMetadata metaData = (StoreFieldMetadata) modelField.getMetadata( StoreFieldMetadata.ID );

        writer.addAttribute( "name", modelField.getName() );

        if ( metaData.isStorable() )
        {
            writer.addAttribute( "persistence-modifier", "persistent" );
        }
        else
        {
            writer.addAttribute( "persistence-modifier", "none" );
        }

        if ( modelField.isRequired() )
        {
            writer.addAttribute( "null-value", "exception" );
        }

        if ( modelField.getName().equals( "id" ) )
        {
            writer.addAttribute( "primary-key", "true" );

            writer.addAttribute( "value-strategy", "native" );
        }

        if ( modelField instanceof ModelAssociation )
        {
            writeAssociation( writer, (ModelAssociation) modelField );
        }
        else
        {
            if ( metaData.getMaxSize() > 0 )
            {
                writeExtension( writer, "jpox", "length", "max " + metaData.getMaxSize() );
            }
        }

        writer.endElement(); // field
    }

    private void writeAssociation( XMLWriter writer, ModelAssociation association )
    {
        StoreAssociationMetadata am =
            (StoreAssociationMetadata) association.getAssociationMetadata( StoreAssociationMetadata.ID );

        if ( am.isPart() != null )
        {
            writer.addAttribute( "default-fetch-group", am.isPart().toString() );
        }

        boolean dependent = true;

        if ( am.isPart() != null )
        {
            dependent = am.isPart().booleanValue();
        }

        if ( association.getType().equals( "java.util.List" ) ||
             association.getType().equals( "java.util.Set" ) )
        {
            writer.startElement( "collection" );

            writer.addAttribute( "element-type", association.getTo() );

            if ( dependent )
            {
                writer.addAttribute( "dependent-element", "true" );
            }

            writer.endElement();
        }
        else if ( association.getType().equals( "java.util.Map" ) )
        {
            writer.startElement( "map" );

            writer.addAttribute( "key-type", "java.lang.Object" );

            writer.addAttribute( "value-type", association.getTo() );

            writer.addAttribute( "dependent-key", "true" );

            if ( dependent )
            {
                writer.addAttribute( "dependent-element", "true" );
            }

            writer.endElement();

            writer.startElement( "join" );

            writer.endElement();
        }
        else if ( association.getType().equals( "java.util.Properties" ) )
        {
            writer.addAttribute( "embedded", "false" );

            writer.startElement( "map" );

            writer.addAttribute( "key-type", "java.lang.String" );

            writer.addAttribute( "value-type", "java.lang.String" );

            writer.addAttribute( "embedded-key", "true" );

            writer.addAttribute( "embedded-value", "true" );

            writer.addAttribute( "dependent-key", "true" );

            writer.addAttribute( "dependent-value", "true" );

            writer.endElement();

            writer.startElement( "join" );

            writer.endElement();
        }
        else // One association
        {
            if ( dependent )
            {
                writer.addAttribute( "dependent", "true" );
            }
        }
    }

    private void writeExtension( XMLWriter writer, String vendorName, String key, String value )
    {
        writer.startElement( "extension" );

        writer.addAttribute( "vendor-name", vendorName );

        writer.addAttribute( "key", key );

        writer.addAttribute( "value", value );

        writer.endElement();
    }

    private StoreAssociationMetadata getAssociationMetadata( ModelAssociation association )
    {
        return (StoreAssociationMetadata) association.getAssociationMetadata( StoreAssociationMetadata.ID );
    }
}
