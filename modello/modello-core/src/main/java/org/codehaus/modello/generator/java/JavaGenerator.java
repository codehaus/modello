package org.codehaus.modello.generator.java;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.codehaus.modello.CodeSegment;
import org.codehaus.modello.Model;
import org.codehaus.modello.ModelClass;
import org.codehaus.modello.ModelField;
import org.codehaus.modello.ModelloException;
import org.codehaus.modello.generator.AbstractGenerator;
import org.codehaus.modello.generator.java.javasource.JClass;
import org.codehaus.modello.generator.java.javasource.JField;
import org.codehaus.modello.generator.java.javasource.JMethod;
import org.codehaus.modello.generator.java.javasource.JParameter;
import org.codehaus.modello.generator.java.javasource.JSourceWriter;
import org.codehaus.modello.generator.java.javasource.JType;

/**
 * @author <a href="mailto:jason@modello.org">Jason van Zyl</a>
 * @version $Id$
 */
public class JavaGenerator
    extends AbstractGenerator
{
    public JavaGenerator( String model, String outputDirectory, String modelVersion, boolean packageWithVersion )
    {
        super( model, outputDirectory, modelVersion, packageWithVersion );
    }

    protected String getBasePackageName( Model model )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( model.getPackageName() );

        if ( isPackageWithVersion() )
        {
            sb.append( "." );

            sb.append( getModelVersion().toString() );
        }

        return sb.toString();
    }

    protected void addModelImports( JClass jClass )
        throws ModelloException
    {
        for ( Iterator i = getModel().getClasses().iterator(); i.hasNext(); )
        {
            ModelClass modelClass = (ModelClass) i.next();

            if ( outputElement( modelClass.getVersion(), modelClass.getName() ) )
            {
                jClass.addImport( getBasePackageName( getModel() ) + "." + modelClass.getName() );
            }
        }
    }

    public void generate()
        throws ModelloException
    {
        try
        {
            generateJava();
        }
        catch( IOException ex )
        {
            throw new ModelloException( "Exception while generating XDoc.", ex );
        }
    }

    private void generateJava()
        throws ModelloException, IOException
    {
        Model objectModel = getModel();

        String packageName = getBasePackageName( objectModel );

        String directory = packageName.replace( '.', '/' );

        for ( Iterator i = objectModel.getClasses().iterator(); i.hasNext(); )
        {
            ModelClass modelClass = (ModelClass) i.next();

            if ( outputElement( modelClass.getVersion(), modelClass.getName() ) )
            {
                File f = new File( new File( getOutputDirectory(), directory ), modelClass.getName() + ".java" );

                if ( !f.getParentFile().exists() )
                {
                    f.getParentFile().mkdirs();
                }

                FileWriter writer = new FileWriter( f );

                JSourceWriter sourceWriter = new JSourceWriter( writer );

                JClass jClass = new JClass( modelClass.getName() );

                jClass.addImport( "java.util.*" );

                jClass.setPackageName( packageName );

                if ( modelClass.getSuperClass() != null )
                {
                    jClass.setSuperClass( modelClass.getSuperClass() );
                }

                if ( modelClass.getFields() != null )
                {
                    int count = 1;

                    for ( Iterator j = modelClass.getFields().iterator(); j.hasNext(); )
                    {
                        ModelField modelField = (ModelField) j.next();

                        if ( outputElement( modelField.getVersion(), modelClass.getName() + "." + modelField.getName() ) )
                        {
                            if ( modelField.getName() == null )
                            {
                                throw new IllegalStateException( "Field name can't be null jField: element " + count + " in the definition of the class " + modelClass.getName() );
                            }

                            JField field = createField( modelField, modelClass, count );

                            jClass.addField( field );

                            jClass.addMethod( createGetter( field ) );

                            jClass.addMethod( createSetter( field ) );

                            createAdder( field, jClass, objectModel );
                        }
                    }

                    if ( modelClass.getCodeSegments() != null )
                    {
                        for ( Iterator iterator = modelClass.getCodeSegments().iterator(); iterator.hasNext(); )
                        {
                            CodeSegment codeSegment = (CodeSegment) iterator.next();

                            if ( outputElement( codeSegment.getVersion(), "" ) )
                            {
                                jClass.addSourceCode( codeSegment.getCode() );
                            }
                        }
                    }

                    count++;
                }

                jClass.print( sourceWriter );

                writer.flush();

                writer.close();
            }
        }
    }

    private JField createField( ModelField modelField, ModelClass modelClass, int entry )
    {
        if ( modelField.getType() == null )
        {
            throw new IllegalStateException( "Field type can't be null: jField element " + entry + " in the definition of the class " + modelClass.getName() );
        }

        JType type;

        if ( modelField.getType().equals( "boolean" ) )
        {
            type = JType.Boolean;
        }
        else if ( modelField.getType().equals( "byte" ) )
        {
            type = JType.Byte;
        }
        else if ( modelField.getType().equals( "char" ) )
        {
            type = JType.Char;
        }
        else if ( modelField.getType().equals( "double" ) )
        {
            type = JType.Double;
        }
        else if ( modelField.getType().equals( "float" ) )
        {
            type = JType.Float;
        }
        else if ( modelField.getType().equals( "int" ) )
        {
            type = JType.Int;
        }
        else if ( modelField.getType().equals( "short" ) )
        {
            type = JType.Short;
        }
        else if ( modelField.getType().equals( "long" ) )
        {
            type = JType.Long;
        }
        else
        {
            type = new JClass( modelField.getType() );
        }

        JField field = new JField( type, modelField.getName() );

        if ( modelField.getDefaultValue() != null )
        {
            if ( modelField.getType().equals( "String" ) )
            {
                field.setInitString( "\"" + modelField.getDefaultValue() + "\"" );
            }
            else
            {
                field.setInitString( modelField.getDefaultValue() );
            }
        }

        return field;
    }

    private JMethod createGetter( JField field )
    {
        String propertyName = capitalise( field.getName() );

        JMethod getter = new JMethod( field.getType(), "get" + propertyName );

        getter.getSourceCode().add( "return this." + field.getName() + ";" );

        return getter;
    }

    private JMethod createSetter( JField field )
    {
        String propertyName = capitalise( field.getName() );

        JMethod setter = new JMethod( null, "set" + propertyName );

        setter.addParameter( new JParameter( field.getType(), field.getName() ) );

        setter.getSourceCode().add( "this." + field.getName() + " = " + field.getName() + ";" );

        return setter;
    }

    private void createAdder( JField field, JClass jClass, Model objectModel )
    {
        if ( isCollection( field.getType().getName() ) )
        {
            String parameterName = singular( field.getName() );

            String className = capitalise( parameterName );

            JType addType;

            if ( objectModel.getClassNames().contains( className ) )
            {
                addType = new JClass( className );
            }
            else
            {
                addType = new JClass( "String" );
            }

            JMethod adder = new JMethod( null, "add" + className );

            adder.addParameter( new JParameter( addType, parameterName ) );

            adder.getSourceCode().add( field.getName() + ".add( " + parameterName + " );" );

            jClass.addMethod( adder );
        }
        else if ( field.getType().getName().equals( "java.util.Properties" ) )
        {
            String parameterName = singular( field.getName() );

            String className = capitalise( parameterName );

            JType addType = new JClass( "String" );

            JMethod adder = new JMethod( null, "add" + className );

            adder.addParameter( new JParameter( addType, "name" ) );

            adder.addParameter( new JParameter( addType, "value" ) );

            adder.getSourceCode().add( field.getName() + ".setProperty( name, value );" );

            jClass.addMethod( adder );
        }
    }
}
