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

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.plugin.AbstractModelloGenerator;
import org.codehaus.modello.plugin.jpox.metadata.JPoxAssociationMetadata;
import org.codehaus.modello.plugin.jpox.metadata.JPoxClassMetadata;
import org.codehaus.modello.plugin.jpox.metadata.JPoxFieldMetadata;
import org.codehaus.modello.plugin.store.metadata.StoreAssociationMetadata;
import org.codehaus.modello.plugin.store.metadata.StoreFieldMetadata;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

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

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class JPoxJdoMappingModelloGenerator
    extends AbstractModelloGenerator
{
    private final static Map PRIMITIVE_IDENTITY_MAP;
    
    private final static List IDENTITY_TYPES;
    
    private final static List VALUE_STRATEGY_LIST;

    static
    {
        PRIMITIVE_IDENTITY_MAP = new HashMap();

        // TODO: These should be the fully qualified class names
        PRIMITIVE_IDENTITY_MAP.put( "short", "javax.jdo.identity.ShortIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "Short", "javax.jdo.identity.ShortIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "int", "javax.jdo.identity.IntIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "Integer", "javax.jdo.identity.IntIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "long", "javax.jdo.identity.LongIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "Long", "javax.jdo.identity.LongIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "String", "javax.jdo.identity.StringIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "char", "javax.jdo.identity.CharIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "Character", "javax.jdo.identity.CharIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "byte", "javax.jdo.identity.ByteIdentity" );
        PRIMITIVE_IDENTITY_MAP.put( "Byte", "javax.jdo.identity.ByteIdentity" );
        
        IDENTITY_TYPES = new ArrayList();
        IDENTITY_TYPES.add( "application" );
        IDENTITY_TYPES.add( "datastore" );
        IDENTITY_TYPES.add( "nondurable" );
        
        VALUE_STRATEGY_LIST = new ArrayList();
        VALUE_STRATEGY_LIST.add( "off" );
        VALUE_STRATEGY_LIST.add( "native" );
        VALUE_STRATEGY_LIST.add( "sequence" );
        VALUE_STRATEGY_LIST.add( "identity" );
        VALUE_STRATEGY_LIST.add( "increment" );
        VALUE_STRATEGY_LIST.add( "uuid-string" );
        VALUE_STRATEGY_LIST.add( "uuid-hex" );
        VALUE_STRATEGY_LIST.add( "datastore-uuid-hex" );
        VALUE_STRATEGY_LIST.add( "max" );
        VALUE_STRATEGY_LIST.add( "auid" );
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
            String fileName;

            if ( isPackageWithVersion() )
            {
                fileName = "package-" + getGeneratedVersion() + ".jdo";
            }
            else
            {
                fileName = "package.jdo";
            }

            File packageJdo = new File( getOutputDirectory(), fileName );

            File parent = packageJdo.getParentFile();

            if ( !parent.exists() )
            {
                if ( !parent.mkdirs() )
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
        OutputStreamWriter fileWriter = new OutputStreamWriter( new FileOutputStream( file ), "UTF-8" );

        PrintWriter printWriter = new PrintWriter( fileWriter );

        XMLWriter writer = new PrettyPrintXMLWriter( printWriter );

        Map classes = new HashMap();

        for ( Iterator it = model.getClasses( getGeneratedVersion() ).iterator(); it.hasNext(); )
        {
            ModelClass modelClass = (ModelClass) it.next();
            
            JPoxClassMetadata jpoxMetadata = (JPoxClassMetadata) modelClass.getMetadata( JPoxClassMetadata.ID );
            
            if ( !jpoxMetadata.isEnabled() )
            {
                // Skip generation of those classes that are not enabled for the jpox plugin.
                continue;
            }

            String packageName = modelClass.getPackageName( isPackageWithVersion(), getGeneratedVersion() );

            List list = (List) classes.get( packageName );

            if ( list == null )
            {
                list = new ArrayList();
            }

            list.add( modelClass );
            
            classes.put( packageName, list );
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

            String packageName = ( (ModelClass) list.get( 0 ) ).getPackageName( isPackageWithVersion(), getGeneratedVersion() );

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
        JPoxClassMetadata jpoxMetadata = (JPoxClassMetadata) modelClass.getMetadata( JPoxClassMetadata.ID );
        
        if ( !jpoxMetadata.isEnabled() )
        {
            // Skip generation of those classes that are not enabled for the jpox plugin.
            return;
        }

        writer.startElement( "class" );

        writer.addAttribute( "name", modelClass.getName() );

        ModelClass persistenceCapableSuperclass = null;

        if ( modelClass.hasSuperClass() && modelClass.isInternalSuperClass() )
        {
            persistenceCapableSuperclass = getModel().getClass( modelClass.getSuperClass(), getGeneratedVersion() );
        }

        if ( persistenceCapableSuperclass != null )
        {
            String superPackageName =
                persistenceCapableSuperclass.getPackageName( isPackageWithVersion(), getGeneratedVersion() );

            writer.addAttribute( "persistence-capable-superclass",
                                 superPackageName + "." + persistenceCapableSuperclass.getName() );
        }

        writer.addAttribute( "detachable", String.valueOf( jpoxMetadata.isDetachable() ) );

        if ( !StringUtils.isEmpty( jpoxMetadata.getTable() ) )
        {
            // Test Substitute Table Name.            
            if( DBKeywords.isReserved( jpoxMetadata.getTable() ) )
            {
                throw new ModelloException( "The JDO mapping generator has detected the use of the " +
                                            "SQL Reserved word '" + jpoxMetadata.getTable() + "' as an alternative" +
                                            "table name for the " + modelClass.getName() + " class.  Please use" +
                                            "a different name for the <class jpox.table=\"" + 
                                            jpoxMetadata.getTable() + "\"> attribute.  See " + 
                                            DBKeywords.URL_SQL92 + " for complete list.");
            }
            
            writer.addAttribute( "table", jpoxMetadata.getTable() );
        }
        else
        {
            // Test base table name.
            if( DBKeywords.isReserved( modelClass.getName() ) )
            {
                throw new ModelloException( "The JDO mapping generator has detected the use of the " +
                                            "SQL Reserved word '" + modelClass.getName() + "' as a class name.  " +
                                            "Please specify an alternative jpox table name using the " +
                                            "<class jpox.table=\"\"> attribute, or use a different class name.  See " + 
                                            DBKeywords.URL_SQL92 + " for complete list."); 
            }
        }

        // ----------------------------------------------------------------------
        // If this class has a primary key field mark make jpox manage the id
        // as a autoincrement variable
        // ----------------------------------------------------------------------

        List fields = Collections.unmodifiableList( modelClass.getFields( getGeneratedVersion() ) );

        // TODO: for now, assume that any primary key will be set in the super class
        // While it should be possible to have abstract super classes and have the
        // key defined in the sub class this is not implemented yet.
        
        boolean needInheritance = false;

        if ( persistenceCapableSuperclass == null )
        {
            if(StringUtils.isNotEmpty( jpoxMetadata.getIdentityType() ))
            {
                String identityType = jpoxMetadata.getIdentityType();
                if ( !IDENTITY_TYPES.contains( identityType ) )
                {
                    throw new ModelloException( "The JDO mapping generator does not support the specified " +
                                                "class identity type '" + identityType + "'. " +
                                                "Supported types: " + IDENTITY_TYPES );
                }
                writer.addAttribute( "identity-type", identityType );
            }
            else if ( isInstantionApplicationType( modelClass ) )
            {
                writer.addAttribute( "identity-type", "application" );
            }
        }
        else
        {
            needInheritance = true;
        }

        if ( StringUtils.isNotEmpty( jpoxMetadata.getIdentityClass() ) )
        {
            // Use user provided objectId class.
            writer.addAttribute( "objectid-class", jpoxMetadata.getIdentityClass() );
        }
        else
        {
            // Calculate the objectId class.

            List primaryKeys = getPrimaryKeyFields( modelClass );

            // TODO: write generation support for multi-primary-key support.
            //       would likely need to write a java class that can supply an ObjectIdentity
            //       to the jpox/jdo implementation.
            if ( primaryKeys.size() > 1 )
            {
                throw new ModelloException( "The JDO mapping generator does not yet support Object Identifier generation "
                                                + "for the " + primaryKeys.size()
                                                + " fields specified as <identifier> or "
                                                + "with jpox.primary-key=\"true\"" );
            }

            if ( primaryKeys.size() == 1 )
            {
                ModelField modelField = (ModelField) primaryKeys.get( 0 );
                String objectIdClass = (String) PRIMITIVE_IDENTITY_MAP.get( modelField.getType() );

                if ( StringUtils.isNotEmpty( objectIdClass ) )
                {
                    writer.addAttribute( "objectid-class", objectIdClass );
                }
            }
        }
        
        if ( needInheritance )
        {
            writer.startElement( "inheritance" );

            // TODO: The table strategy should be customizable
            // http://www.jpox.org/docs/1_1/inheritance.html - in particular
            // the strategy="subclass-table" and strategy="new-table" parts

            writer.addAttribute( "strategy", "new-table" );

            writer.endElement();
        }

        // ----------------------------------------------------------------------
        // Write all fields
        // ----------------------------------------------------------------------

        for ( Iterator it = fields.iterator(); it.hasNext(); )
        {
            ModelField modelField = (ModelField) it.next();

            writeModelField( writer, modelField );
        }
        
        // Write ignored fields.
        List ignoredFields = jpoxMetadata.getNotPersisted();
        if ( ignoredFields != null )
        {
            Iterator it = ignoredFields.iterator();
            while ( it.hasNext() )
            {
                String fieldName = (String) it.next();
                writer.startElement( "field" );
                writer.addAttribute( "name", fieldName );
                writer.addAttribute( "persistence-modifier", "none" );
                writer.endElement();
            }
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
                StoreAssociationMetadata storeMetadata = getAssociationMetadata( (ModelAssociation) field );

                if ( storeMetadata.isPart() != null && storeMetadata.isPart().booleanValue() )
                {
                    continue;
                }
            }

            detailedFields.add( field );
        }

        // ----------------------------------------------------------------------
        // Write all fetch groups
        // ----------------------------------------------------------------------

        // Write defaut detail fetch group
        writeFetchGroup( writer, modelClass.getName() + "_detail", detailedFields, true );

        // Write user fetch groups
        Map fetchsMap = new HashMap();

        for ( Iterator it = fields.iterator(); it.hasNext(); )
        {
            ModelField field = (ModelField) it.next();

            JPoxFieldMetadata jpoxFieldMetadata = (JPoxFieldMetadata) field.getMetadata( JPoxFieldMetadata.ID );

            List names = jpoxFieldMetadata.getFetchGroupNames();

            if ( names != null )
            {
                for ( Iterator i = names.iterator(); i.hasNext(); )
                {
                    String fetchGroupName = (String) i.next();

                    List fetchList;

                    if ( fetchsMap.get( fetchGroupName ) == null )
                    {
                        fetchList = new ArrayList();
                    }
                    else
                    {
                        fetchList = (List) fetchsMap.get( fetchGroupName );
                    }

                    fetchList.add( field );

                    fetchsMap.put( fetchGroupName, fetchList );
                }
            }
        }

        for ( Iterator it = fetchsMap.keySet().iterator(); it.hasNext(); )
        {
            String fetchName = (String) it.next();

            writeFetchGroup( writer, fetchName, (List) fetchsMap.get( fetchName ), false );
        }

        writer.endElement(); // class
    }

    private void writeFetchGroup( XMLWriter writer, String fetchGroupName, List fields, boolean onlyIfIsStashPart )
    {
        if ( !fields.isEmpty() )
        {
            writer.startElement( "fetch-group" );

            writer.addAttribute( "name", fetchGroupName );

            for ( Iterator it = fields.iterator(); it.hasNext(); )
            {
                ModelField field = (ModelField) it.next();

                if ( onlyIfIsStashPart && ( field instanceof ModelAssociation ) )
                {
                    StoreAssociationMetadata storeMetadata = getAssociationMetadata( (ModelAssociation) field );

                    if ( storeMetadata.isPart() != null && storeMetadata.isPart().booleanValue() )
                    {
                        continue;
                    }
                }

                writer.startElement( "field" );

                writer.addAttribute( "name", field.getName() );

                writer.endElement();
            }

            writer.endElement(); // fetch-group
        }
    }

    private void writeModelField( XMLWriter writer, ModelField modelField ) throws ModelloException
    {
        writer.startElement( "field" );

        StoreFieldMetadata storeMetadata = (StoreFieldMetadata) modelField.getMetadata( StoreFieldMetadata.ID );

        JPoxFieldMetadata jpoxMetadata = (JPoxFieldMetadata) modelField.getMetadata( JPoxFieldMetadata.ID );

        writer.addAttribute( "name", modelField.getName() );

        if ( !storeMetadata.isStorable() )
        {
            writer.addAttribute( "persistence-modifier", "none" );
        }
        else if ( StringUtils.isNotEmpty( jpoxMetadata.getPersistenceModifier() ) )
        {
            writer.addAttribute( "persistence-modifier", jpoxMetadata.getPersistenceModifier() );
        }

        if ( modelField.isRequired() )
        {
            writer.addAttribute( "null-value", "exception" );
        }
        else if ( jpoxMetadata.getNullValue() != null )
        {
            writer.addAttribute( "null-value", jpoxMetadata.getNullValue() );
        }
        
        if ( StringUtils.isNotEmpty( jpoxMetadata.getColumnName() ) )
        {
            // Test proposed Column Name.            
            if( DBKeywords.isReserved( jpoxMetadata.getColumnName() ) )
            {
                throw new ModelloException( "The JDO mapping generator has detected the use of the " +
                                            "SQL Reserved word '" + jpoxMetadata.getColumnName() + "' as an alternative" +
                                            "column name for the " + modelField.getName() + " field of the " + 
                                            modelField.getModelClass().getName() + " class.  Please use" +
                                            "a different name for the <field jpox.column=\"" + 
                                            jpoxMetadata.getColumnName() + "\"> attribute.  See " + 
                                            DBKeywords.URL_SQL92 + " for complete list.");
            }
            
            writer.addAttribute( "column", jpoxMetadata.getColumnName() );
        }
        else
        {
            // Test proposed Field name.
            if( DBKeywords.isReserved( modelField.getName() ) )
            {
                throw new ModelloException( "The JDO mapping generator has detected the use of the " +
                                            "SQL Reserved word '" + modelField.getName() + "' as a field name of the " +
                                            modelField.getModelClass().getName() + " class.  Please specify an " +
                                            "alternative jpox column name using the <field jpox.column=\"\"> " +
                                            "attribute, or use a different class name.  See " + 
                                            DBKeywords.URL_SQL92 + " for complete list."); 
            }
        }
        
        if ( StringUtils.isNotEmpty( jpoxMetadata.getJoinTableName() ) )
        {
            // Test proposed name for the Join Table for the field.
            if( DBKeywords.isReserved( jpoxMetadata.getJoinTableName() ) )
            {
                throw new ModelloException( "The JDO mapping generator has detected the use of the " +
                                            "SQL Reserved word '" + jpoxMetadata.getJoinTableName() + "' as name of a" +
                                            "join table for the " + modelField.getName() + " field of the " + 
                                            modelField.getModelClass().getName() + " class.  Please specify" +
                                            "an alternative name for the <field jpox.join-table=\"" + 
                                            jpoxMetadata.getColumnName() + "\"> attribute.  See " + 
                                            DBKeywords.URL_SQL92 + " for complete list.");
            }
            writer.addAttribute( "table", jpoxMetadata.getJoinTableName() );
        }

        if ( jpoxMetadata.isPrimaryKey() )
        {
            writer.addAttribute( "primary-key", "true" );

            // value-strategy is only useful when you have a primary-key defined for the field.
            if ( StringUtils.isNotEmpty( jpoxMetadata.getValueStrategy() ) )
            {
                String valueStrategy = jpoxMetadata.getValueStrategy();
                if ( !VALUE_STRATEGY_LIST.contains( valueStrategy ) )
                {
                    throw new ModelloException( "The JDO mapping generator does not support the specified " +
                                                "value-strategy '" + valueStrategy + "'. " +
                                                "Supported types: " + VALUE_STRATEGY_LIST );
                }
                
                writer.addAttribute( "value-strategy", jpoxMetadata.getValueStrategy() );
            }
        }
        
        if ( StringUtils.isNotEmpty( jpoxMetadata.getIndexed() ) )
        {
            writer.addAttribute( "indexed", jpoxMetadata.getIndexed() );
        }

        if ( StringUtils.isNotEmpty( jpoxMetadata.getMappedBy() ) )
        {
            writer.addAttribute( "mapped-by", jpoxMetadata.getMappedBy() );
        }

        if ( modelField instanceof ModelAssociation )
        {
            writeAssociation( writer, (ModelAssociation) modelField );
        }
        else
        {
            if ( modelField.isPrimitiveArray() )
            {
                writer.startElement( "array" );
                writer.endElement();
            }

            if ( storeMetadata.getMaxSize() > 0 ||
                ( jpoxMetadata.getNullValue() != null && modelField.getDefaultValue() != null ) )
            {
                writer.startElement( "column" );

                if ( storeMetadata.getMaxSize() > 0 )
                {
                    writer.addAttribute( "length", String.valueOf( storeMetadata.getMaxSize() ) );
                }

                if ( jpoxMetadata.getNullValue() != null && "default".equals( jpoxMetadata.getNullValue() ) )
                {
                    writer.addAttribute( "default-value", modelField.getDefaultValue() );
                }
                writer.endElement();
            }
        }

        writer.endElement(); // field
    }

    private void writeAssociation( XMLWriter writer, ModelAssociation association )
    {
        StoreAssociationMetadata am =
            (StoreAssociationMetadata) association.getAssociationMetadata( StoreAssociationMetadata.ID );

        JPoxAssociationMetadata jpoxMetadata =
            (JPoxAssociationMetadata) association.getAssociationMetadata( JPoxAssociationMetadata.ID );

        if ( am.isPart() != null )
        {
            // This gets added onto the <field> element
            writer.addAttribute( "default-fetch-group", am.isPart().toString() );
        }
        
        if ( association.getType().equals( "java.util.List" ) || association.getType().equals( "java.util.Set" ) )
        {
            // Start <collection> element
            writer.startElement( "collection" );

            if ( association.getTo().equals( "String" ) )
            {
                writer.addAttribute( "element-type", "java.lang.String" );
            }
            else
            {
                writer.addAttribute( "element-type", association.getTo() );
            }

            if ( jpoxMetadata.isDependent() )
            {
                writer.addAttribute( "dependent-element", "true" );
            }
            else
            {
                writer.addAttribute( "dependent-element", "false" );
            }

            // End <collection> element
            writer.endElement();

            if ( jpoxMetadata.isJoin() )
            {
                writer.startElement( "join" );

                writer.endElement();
            }
        }
        else if ( association.getType().equals( "java.util.Map" ) )
        {
            writer.startElement( "map" );

            writer.addAttribute( "key-type", am.getKeyType() );

            if ( association.getTo().equals( "String" ) )
            {
                writer.addAttribute( "value-type", "java.lang.String" );
            }
            else
            {
                writer.addAttribute( "value-type", association.getTo() );
            }

            if ( jpoxMetadata.isDependent() )
            {
                writer.addAttribute( "dependent-key", "true" );

                writer.addAttribute( "dependent-value", "true" );
            }
            else
            {
                writer.addAttribute( "dependent-key", "false" );

                writer.addAttribute( "dependent-value", "false" );
            }

            writer.endElement();

            if ( jpoxMetadata.isJoin() )
            {
                writer.startElement( "join" );

                writer.endElement();
            }
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

            if ( jpoxMetadata.isJoin() )
            {
                writer.startElement( "join" );

                writer.endElement();
            }
        }
        else // One association
        {
            if ( jpoxMetadata.isDependent() )
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
    
    private boolean isInstantionApplicationType(ModelClass modelClass)
    {
        List identifierFields = modelClass.getIdentifierFields( getGeneratedVersion() );
        
        return identifierFields.size() > 0;
    }
    
    private List getPrimaryKeyFields(ModelClass modelClass) throws ModelloException
    {
        List primaryKeys = new ArrayList();
        List fields = modelClass.getFields( getGeneratedVersion() );
        JPoxClassMetadata jpoxClassMetadata = (JPoxClassMetadata) modelClass.getMetadata( JPoxClassMetadata.ID );
        
        for ( Iterator it = fields.iterator(); it.hasNext(); )
        {
            ModelField modelField = (ModelField) it.next();
            JPoxFieldMetadata jpoxFieldMetadata = (JPoxFieldMetadata) modelField.getMetadata( JPoxFieldMetadata.ID );

            if ( jpoxClassMetadata.useIdentifiersAsPrimaryKey() )
            {
                if ( modelField.isIdentifier() )
                {
                    assertSupportedIdentityPrimitive( modelField );
                    primaryKeys.add( modelField );
                }
            }
            else
            {
                if ( jpoxFieldMetadata.isPrimaryKey() )
                {
                    assertSupportedIdentityPrimitive( modelField );
                    primaryKeys.add( modelField );
                }
            }
        }
        
        return primaryKeys;
    }

    private void assertSupportedIdentityPrimitive( ModelField modelField )
        throws ModelloException
    {
        if ( !PRIMITIVE_IDENTITY_MAP.containsKey( modelField.getType() ) )
        {
            throw new ModelloException( "The JDO mapping generator does not support the specified " +
                "field type '" + modelField.getType() + "'. " +
                "Supported types: " + PRIMITIVE_IDENTITY_MAP.keySet() );
        }
    }
    
    private StoreAssociationMetadata getAssociationMetadata( ModelAssociation association )
    {
        return (StoreAssociationMetadata) association.getAssociationMetadata( StoreAssociationMetadata.ID );
    }
}