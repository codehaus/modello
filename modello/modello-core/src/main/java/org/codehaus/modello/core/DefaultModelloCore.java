package org.codehaus.modello.core;

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

import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.ModelloRuntimeException;
import org.codehaus.modello.core.io.ModelReader;
import org.codehaus.modello.metadata.AssociationMetadata;
import org.codehaus.modello.metadata.ClassMetadata;
import org.codehaus.modello.metadata.FieldMetadata;
import org.codehaus.modello.metadata.MetadataPlugin;
import org.codehaus.modello.metadata.ModelMetadata;
import org.codehaus.modello.model.CodeSegment;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelDefault;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.model.ModelValidationException;
import org.codehaus.modello.plugin.ModelloGenerator;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 *
 * @version $Id$
 */
public class DefaultModelloCore
    extends AbstractModelloCore
{
    /**
     * @requirement
     */
    private MetadataPluginManager metadataPluginManager;

    /**
     * @requirement
     */
    private GeneratorPluginManager generatorPluginManager;

public MetadataPluginManager getMetadataPluginManager()
{
    return metadataPluginManager;
}
    public Model loadModel( Reader reader )
        throws ModelloException, ModelValidationException
    {
        Model model;

        ModelReader modelReader = new ModelReader();

        model = modelReader.loadModel( reader );

        model.initialize();

        // ----------------------------------------------------------------------
        // Handle Metadata
        // ----------------------------------------------------------------------

        for( Iterator plugins = metadataPluginManager.getPlugins(); plugins.hasNext(); )
        {
            MetadataPlugin plugin = (MetadataPlugin) plugins.next();
System.out.println( "Plugin : " + plugin.getClass().getName() );

            Map attributes = Collections.EMPTY_MAP;

            attributes = Collections.unmodifiableMap( attributes );

            ModelMetadata metadata = plugin.getModelMetadata( model, attributes );

            if ( metadata == null )
            {
                throw new ModelloException( "A meta data plugin must not return null." );
            }

            model.addMetadata( metadata );
        }

        for( Iterator classes = model.getAllClasses().iterator(); classes.hasNext(); )
        {
            ModelClass clazz = (ModelClass) classes.next();

            Map attributes = Collections.unmodifiableMap( Collections.EMPTY_MAP );

            attributes = Collections.unmodifiableMap( attributes );

            for( Iterator plugins = metadataPluginManager.getPlugins(); plugins.hasNext(); )
            {
                MetadataPlugin plugin = (MetadataPlugin) plugins.next();

                ClassMetadata metadata = plugin.getClassMetadata( clazz, attributes );

                if ( metadata == null )
                {
                    throw new ModelloException( "A meta data plugin must not return null." );
                }

                clazz.addMetadata( metadata );
            }

            for( Iterator fields = clazz.getAllFields().iterator(); fields.hasNext(); )
            {
                Object field = fields.next();

                if ( field instanceof ModelAssociation )
                {
                    ModelAssociation modelAssociation = (ModelAssociation) field;

                    Map fieldAttributes = modelReader.getAttributesForField( modelAssociation );

                    fieldAttributes = Collections.unmodifiableMap( fieldAttributes );

                    Map associationAttributes = modelReader.getAttributesForAssociation( modelAssociation );

                    associationAttributes = Collections.unmodifiableMap( associationAttributes );

                    for( Iterator plugins = metadataPluginManager.getPlugins(); plugins.hasNext(); )
                    {
                        MetadataPlugin plugin = (MetadataPlugin) plugins.next();

                        FieldMetadata fieldMetadata = plugin.getFieldMetadata( modelAssociation, fieldAttributes );

                        if ( fieldMetadata == null )
                        {
                            throw new ModelloException( "A meta data plugin must not return null." );
                        }

                        modelAssociation.addMetadata( fieldMetadata );

                        AssociationMetadata associationMetadata = plugin.getAssociationMetadata( modelAssociation, associationAttributes );

                        if ( associationMetadata == null )
                        {
                            throw new ModelloException( "A meta data plugin must not return null." );
                        }

                        modelAssociation.addMetadata( associationMetadata );
                    }
                }
                else
                {
                    ModelField modelField = (ModelField) field;

                    attributes = modelReader.getAttributesForField( modelField );

                    attributes = Collections.unmodifiableMap( attributes );

                    for( Iterator plugins = metadataPluginManager.getPlugins(); plugins.hasNext(); )
                    {
                        MetadataPlugin plugin = (MetadataPlugin) plugins.next();

                        FieldMetadata metadata = plugin.getFieldMetadata( modelField, attributes );

                        if ( metadata == null )
                        {
                            throw new ModelloException( "A meta data plugin must not return null." );
                        }

                        modelField.addMetadata( metadata );
                    }
                }
            }
        }

        // ----------------------------------------------------------------------
        // Validate the entire model
        // ----------------------------------------------------------------------

        model.validate();

        for( Iterator defaults = model.getDefaults().iterator(); defaults.hasNext(); )
        {
            ModelDefault modelDefault = (ModelDefault) defaults.next();

            modelDefault.validateElement();
        }

        for( Iterator classes = model.getAllClasses().iterator(); classes.hasNext(); )
        {
            ModelClass modelClass = (ModelClass) classes.next();

            modelClass.validate();
        }

        for( Iterator classes = model.getAllClasses().iterator(); classes.hasNext(); )
        {
            ModelClass modelClass = (ModelClass) classes.next();

            for( Iterator fields = modelClass.getAllFields().iterator(); fields.hasNext(); )
            {
                Object field = fields.next();

                if ( field instanceof ModelAssociation )
                {
                    ModelAssociation modelAssociation = (ModelAssociation) field;

                    modelAssociation.validate();
                }
                else
                {
                    ModelField modelField = (ModelField) field;

                    modelField.validate();
                }
            }

            for( Iterator codeSegments = modelClass.getAllCodeSegments().iterator(); codeSegments.hasNext(); )
            {
                CodeSegment codeSegment = (CodeSegment) codeSegments.next();

                codeSegment.validate();
            }
        }

        return model;
    }

    public void saveModel( Model model, Writer writer )
        throws ModelloException
    {
        throw new ModelloRuntimeException( "Not implemented." );
    }

    public Model translate( Reader reader, String inputType, Properties parameters )
        throws ModelloException
    {
        throw new ModelloRuntimeException( "Not implemented." );
    }

    public void generate( Model model, String outputType, Properties parameters )
        throws ModelloException
    {
        if ( model == null )
        {
            throw new ModelloRuntimeException( "Illegal argument: model == null." );
        }

        if ( outputType == null )
        {
            throw new ModelloRuntimeException( "Illegal argument: outputType == null." );
        }

        if ( parameters == null )
        {
            parameters = new Properties();
        }

        ModelloGenerator generator = generatorPluginManager.getGeneratorPlugin( outputType );

        generator.generate( model, parameters );
    }
}
