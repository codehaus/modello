package org.codehaus.modello.plugin;

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

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.ModelloParameterConstants;
import org.codehaus.modello.ModelloRuntimeException;
import org.codehaus.modello.model.BaseElement;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.model.ModelInterface;
import org.codehaus.modello.model.Version;
import org.codehaus.modello.plugin.java.JavaFieldMetadata;
import org.codehaus.modello.plugin.java.javasource.JClass;
import org.codehaus.modello.plugin.java.javasource.JSourceWriter;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@modello.org">Jason van Zyl</a>
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public abstract class AbstractModelloGenerator
    extends AbstractLogEnabled
    implements ModelloGenerator
{
    private Model model;

    private File outputDirectory;

    private Version generatedVersion;

    private boolean packageWithVersion;

    private String encoding;

    protected void initialize( Model model, Properties parameters )
        throws ModelloException
    {
        this.model = model;

        outputDirectory = new File( getParameter( parameters, ModelloParameterConstants.OUTPUT_DIRECTORY ) );

        String version = getParameter( parameters, ModelloParameterConstants.VERSION );

        generatedVersion = new Version( version );

        packageWithVersion = Boolean.valueOf(
            getParameter( parameters, ModelloParameterConstants.PACKAGE_WITH_VERSION ) ).booleanValue();

        encoding = parameters.getProperty( ModelloParameterConstants.ENCODING );
    }

    protected Model getModel()
    {
        return model;
    }

    protected Version getGeneratedVersion()
    {
        return generatedVersion;
    }

    protected boolean isPackageWithVersion()
    {
        return packageWithVersion;
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    protected String getEncoding()
    {
        return encoding;
    }

    /**
     * Create a new java source file writer, with configured encoding.
     * 
     * @param packageName the package of the source file to create
     * @param className the class of the source file to create
     * @return a JSourceWriter with configured encoding
     * @throws IOException
     */
    protected JSourceWriter newJSourceWriter( String packageName, String className )
    throws IOException
    {
        String directory = packageName.replace( '.', File.separatorChar );

        File f = new File( new File( getOutputDirectory(), directory ), className + ".java" );

        if ( !f.getParentFile().exists() )
        {
            f.getParentFile().mkdirs();
        }

        Writer writer = ( encoding == null ) ? WriterFactory.newPlatformWriter( f )
                        : WriterFactory.newWriter( f, encoding );

        return new JSourceWriter( writer );
    }

    protected boolean isClassInModel( String fieldType, Model model )
    {
        try
        {
            return model.getClass( fieldType, generatedVersion ) != null;
        }
        catch ( Exception e )
        {
        }

        return false;
    }

    /**
     * Return the child fields of this class.
     * @param modelClass current class
     * @return the list of fields of this class
     */
    protected List getFieldsForClass( ModelClass modelClass )
    {
        List fields = new ArrayList();

        while ( modelClass != null )
        {
            fields.addAll( modelClass.getFields( getGeneratedVersion() ) );

            String superClass = modelClass.getSuperClass();
            if ( superClass != null )
            {
                modelClass = getModel().getClass( superClass, getGeneratedVersion() );
            }
            else
            {
                modelClass = null;
            }
        }

        return fields;
    }

    protected boolean isInnerAssociation( ModelField field )
    {
        return field instanceof ModelAssociation
            && isClassInModel( ( (ModelAssociation) field ).getTo(), getModel() );
    }

    protected boolean isMap( String fieldType )
    {
        if ( fieldType == null )
        {
            return false;
        }

        if ( fieldType.equals( "java.util.Map" ) )
        {
            return true;
        }
        else if ( fieldType.equals( "java.util.Properties" ) )
        {
            return true;
        }

        return false;
    }

    protected boolean isCollection( String fieldType )
    {
        if ( fieldType == null )
        {
            return false;
        }

        if ( fieldType.equals( "java.util.List" ) )
        {
            return true;
        }
        else if ( fieldType.equals( "java.util.SortedSet" ) )
        {
            return true;
        }

        return false;
    }

    protected String capitalise( String str )
    {
        if ( StringUtils.isEmpty( str ) )
        {
            return str;
        }

        return new StringBuffer( str.length() )
            .append( Character.toTitleCase( str.charAt( 0 ) ) )
            .append( str.substring( 1 ) )
            .toString();
    }

    protected String singular( String name )
    {
        if ( StringUtils.isEmpty( name ) )
        {
            return name;
        }

        if ( name.endsWith( "ies" ) )
        {
            return name.substring( 0, name.length() - 3 ) + "y";
        }
        else if ( name.endsWith( "es" ) && name.endsWith( "ches" ) )
        {
            return name.substring( 0, name.length() - 2 );
        }
        else if ( name.endsWith( "s" ) && ( name.length() != 1 ) )
        {
            return name.substring( 0, name.length() - 1 );
        }

        return name;
    }

    public static String uncapitalise( String str )
    {
        if ( StringUtils.isEmpty( str ) )
        {
            return str;
        }

        return new StringBuffer( str.length() )
            .append( Character.toLowerCase( str.charAt( 0 ) ) )
            .append( str.substring( 1 ) )
            .toString();
    }

    protected void addModelImports( JClass jClass, BaseElement baseElem )
        throws ModelloException
    {
        for ( Iterator i = getModel().getInterfaces( getGeneratedVersion() ).iterator(); i.hasNext(); )
        {
            ModelInterface modelInterface = (ModelInterface) i.next();

            if ( baseElem != null && baseElem instanceof ModelInterface )
            {
                if ( modelInterface.equals( (ModelInterface) baseElem )
                     || modelInterface.getPackageName( isPackageWithVersion(), getGeneratedVersion() ).equals(
                        ( (ModelInterface) baseElem ).getPackageName( isPackageWithVersion(),
                                                                      getGeneratedVersion() ) ) )
                {
                    continue;
                }
            }

            if ( isPackageWithVersion() )
            {
                jClass.addImport(
                    modelInterface.getPackageName( true, getGeneratedVersion() ) + "." + modelInterface.getName() );
            }
            else
            {
                jClass.addImport( modelInterface.getPackageName( false, null ) + "." + modelInterface.getName() );
            }
        }

        for ( Iterator i = getModel().getClasses( getGeneratedVersion() ).iterator(); i.hasNext(); )
        {
            ModelClass modelClass = (ModelClass) i.next();

            if ( baseElem != null && baseElem instanceof ModelClass )
            {
                if ( modelClass.equals( (ModelClass) baseElem )
                     || modelClass.getPackageName( isPackageWithVersion(), getGeneratedVersion() ).equals(
                        ( (ModelClass) baseElem ).getPackageName( isPackageWithVersion(), getGeneratedVersion() ) ) )
                {
                    continue;
                }
            }

            if ( isPackageWithVersion() )
            {
                jClass.addImport(
                    modelClass.getPackageName( true, getGeneratedVersion() ) + "." + modelClass.getName() );
            }
            else
            {
                jClass.addImport( modelClass.getPackageName( false, null ) + "." + modelClass.getName() );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Text utils
    // ----------------------------------------------------------------------

    protected boolean isEmpty( String string )
    {
        return string == null || string.trim().length() == 0;
    }

    // ----------------------------------------------------------------------
    // Parameter utils
    // ----------------------------------------------------------------------

    /**
     * @deprecated @{link Use getParameter( Properties, String )} instead
     */
    protected String getParameter( String name, Properties parameters )
    {
        return getParameter( parameters, name );
    }

    protected String getParameter( Properties parameters, String name )
    {
        String value = parameters.getProperty( name );

        if ( value == null )
        {
            throw new ModelloRuntimeException( "Missing parameter '" + name + "'." );
        }

        return value;
    }

    protected String getParameter( Properties parameters, String name, String defaultValue )
    {
        String value = parameters.getProperty( name );

        if ( value == null )
        {
            return defaultValue;
        }

        return value;
    }

    protected String getPrefix( JavaFieldMetadata javaFieldMetadata )
    {
        return javaFieldMetadata.isBooleanGetter() ? "is" : "get";
    }
}
