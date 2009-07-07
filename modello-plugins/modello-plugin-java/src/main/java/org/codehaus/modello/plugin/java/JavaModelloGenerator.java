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

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.ModelloRuntimeException;
import org.codehaus.modello.model.CodeSegment;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelDefault;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.model.ModelInterface;
import org.codehaus.modello.plugin.java.javasource.JArrayType;
import org.codehaus.modello.plugin.java.javasource.JClass;
import org.codehaus.modello.plugin.java.javasource.JCollectionType;
import org.codehaus.modello.plugin.java.javasource.JConstructor;
import org.codehaus.modello.plugin.java.javasource.JField;
import org.codehaus.modello.plugin.java.javasource.JInterface;
import org.codehaus.modello.plugin.java.javasource.JMethod;
import org.codehaus.modello.plugin.java.javasource.JParameter;
import org.codehaus.modello.plugin.java.javasource.JSourceCode;
import org.codehaus.modello.plugin.java.javasource.JSourceWriter;
import org.codehaus.modello.plugin.java.javasource.JType;
import org.codehaus.modello.plugin.java.metadata.JavaAssociationMetadata;
import org.codehaus.modello.plugin.java.metadata.JavaClassMetadata;
import org.codehaus.modello.plugin.java.metadata.JavaFieldMetadata;
import org.codehaus.modello.plugin.model.ModelClassMetadata;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@modello.org">Jason van Zyl </a>
 * @version $Id$
 */
public class JavaModelloGenerator
    extends AbstractJavaModelloGenerator
{

    private Collection immutableTypes =
        new HashSet( Arrays.asList( new String[] { "boolean", "Boolean", "byte", "Byte", "char", "Character", "short",
            "Short", "int", "Integer", "long", "Long", "float", "Float", "double", "Double", "String" } ) );

    public void generate( Model model, Properties parameters )
        throws ModelloException
    {
        initialize( model, parameters );

        try
        {
            generateJava();
        }
        catch ( IOException ex )
        {
            throw new ModelloException( "Exception while generating Java.", ex );
        }
    }

    private void generateJava()
        throws ModelloException, IOException
    {
        Model objectModel = getModel();

        // ----------------------------------------------------------------------
        // Generate the interfaces.
        // ----------------------------------------------------------------------

        for ( Iterator i = objectModel.getInterfaces( getGeneratedVersion() ).iterator(); i.hasNext(); )
        {
            ModelInterface modelInterface = (ModelInterface) i.next();

            String packageName = modelInterface.getPackageName( isPackageWithVersion(), getGeneratedVersion() );

            JSourceWriter sourceWriter = newJSourceWriter( packageName, modelInterface.getName() );

            JInterface jInterface = new JInterface( packageName + '.' + modelInterface.getName() );

            initHeader( jInterface );

            if ( modelInterface.getSuperInterface() != null )
            {
                // check if we need an import: if it is a generated superInterface in another package
                try
                {
                    ModelInterface superInterface = objectModel.getInterface( modelInterface.getSuperInterface(),
                                                                              getGeneratedVersion() );
                    String superPackageName = superInterface.getPackageName( isPackageWithVersion(),
                                                                             getGeneratedVersion() );

                    if ( ! packageName.equals( superPackageName ) )
                    {
                        jInterface.addImport( superPackageName + '.' + superInterface.getName() );
                    }
                }
                catch ( ModelloRuntimeException mre )
                {
                    // no problem if the interface does not exist in the model, it can be in the jdk
                }

                jInterface.addInterface( modelInterface.getSuperInterface() );
            }

            if ( modelInterface.getCodeSegments( getGeneratedVersion() ) != null )
            {
                for ( Iterator iterator = modelInterface.getCodeSegments( getGeneratedVersion() ).iterator(); iterator.hasNext(); )
                {
                    CodeSegment codeSegment = (CodeSegment) iterator.next();

                    jInterface.addSourceCode( codeSegment.getCode() );
                }
            }

            jInterface.print( sourceWriter );

            sourceWriter.close();
        }

        // ----------------------------------------------------------------------
        // Generate the classes.
        // ----------------------------------------------------------------------

        for ( Iterator i = objectModel.getClasses( getGeneratedVersion() ).iterator(); i.hasNext(); )
        {
            ModelClass modelClass = (ModelClass) i.next();

            JavaClassMetadata javaClassMetadata = (JavaClassMetadata) modelClass.getMetadata( JavaClassMetadata.ID );

            if ( !javaClassMetadata.isEnabled() )
            {
                // Skip generation of those classes that are not enabled for the java plugin.
                continue;
            }

            String packageName = modelClass.getPackageName( isPackageWithVersion(), getGeneratedVersion() );

            JSourceWriter sourceWriter = newJSourceWriter( packageName, modelClass.getName() );

            JClass jClass = new JClass( packageName + '.' + modelClass.getName() );

            initHeader( jClass );

            if ( StringUtils.isNotEmpty( modelClass.getDescription() ) )
            {
                jClass.getJDocComment().setComment( appendPeriod( modelClass.getDescription() ) );
            }

            addModelImports( jClass, modelClass );

            jClass.getModifiers().setAbstract( javaClassMetadata.isAbstract() );

            if ( modelClass.getSuperClass() != null )
            {
                jClass.setSuperClass( modelClass.getSuperClass() );
            }

            for ( Iterator j = modelClass.getInterfaces().iterator(); j.hasNext(); )
            {
                String implementedInterface = (String) j.next();

                jClass.addInterface( implementedInterface );
            }

            jClass.addInterface( Serializable.class.getName() );

            JSourceCode jConstructorSource = new JSourceCode();

            for ( Iterator j = modelClass.getFields( getGeneratedVersion() ).iterator(); j.hasNext(); )
            {
                ModelField modelField = (ModelField) j.next();

                if ( modelField instanceof ModelAssociation )
                {
                    createAssociation( jClass, (ModelAssociation) modelField, jConstructorSource );
                }
                else
                {
                    createField( jClass, modelField );
                }
            }

            if ( !jConstructorSource.isEmpty() )
            {
                // Ironic that we are doing lazy init huh?
                JConstructor jConstructor = jClass.createConstructor();
                jConstructor.setSourceCode( jConstructorSource );
                jClass.addConstructor( jConstructor );
            }

            // ----------------------------------------------------------------------
            // equals() / hashCode() / toString()
            // ----------------------------------------------------------------------

            List identifierFields = modelClass.getIdentifierFields( getGeneratedVersion() );

            if ( identifierFields.size() != 0 )
            {
                JMethod equals = generateEquals( modelClass );

                jClass.addMethod( equals );

                JMethod hashCode = generateHashCode( modelClass );

                jClass.addMethod( hashCode );

                JMethod toString = generateToString( modelClass );

                jClass.addMethod( toString );
            }

            JMethod[] cloneMethods = generateClone( modelClass );
            if ( cloneMethods.length > 0 )
            {
                jClass.addInterface( Cloneable.class.getName() );
                jClass.addMethods( cloneMethods );
            }

            if ( modelClass.getCodeSegments( getGeneratedVersion() ) != null )
            {
                for ( Iterator iterator = modelClass.getCodeSegments( getGeneratedVersion() ).iterator();
                      iterator.hasNext(); )
                {
                    CodeSegment codeSegment = (CodeSegment) iterator.next();

                    jClass.addSourceCode( codeSegment.getCode() );
                }
            }

            ModelClassMetadata metadata = (ModelClassMetadata) modelClass.getMetadata( ModelClassMetadata.ID );

            if ( ( metadata != null ) && metadata.isRootElement() )
            {
                JField modelEncoding = new JField( new JType( "String" ), "modelEncoding" );
                modelEncoding.setInitString( "\"UTF-8\"" );
                jClass.addField( modelEncoding );

                // setModelEncoding(String) method
                JMethod setModelEncoding = new JMethod( "setModelEncoding" );
                setModelEncoding.addParameter( new JParameter( new JClass( "String" ), "modelEncoding" ) );

                setModelEncoding.getSourceCode().add( "this.modelEncoding = modelEncoding;" );

                setModelEncoding.getJDocComment().setComment( "Set an encoding used for reading/writing the model." );

                jClass.addMethod( setModelEncoding );

                // getModelEncoding() method
                JMethod getModelEncoding = new JMethod( "getModelEncoding", new JType( "String" ),
                                                        "the current encoding used when reading/writing this model" );

                getModelEncoding.getSourceCode().add( "return modelEncoding;" );

                jClass.addMethod( getModelEncoding );
            }

            jClass.print( sourceWriter );

            sourceWriter.close();
        }
    }

    private JMethod generateEquals( ModelClass modelClass )
    {
        JMethod equals = new JMethod( "equals", JType.BOOLEAN, null );

        equals.addParameter( new JParameter( new JClass( "Object" ), "other" ) );

        JSourceCode sc = equals.getSourceCode();

        sc.add( "if ( this == other )" );
        sc.add( "{" );
        sc.addIndented( "return true;" );
        sc.add( "}" );
        sc.add( "" );
        sc.add( "if ( !( other instanceof " + modelClass.getName() + " ) )" );
        sc.add( "{" );
        sc.addIndented( "return false;" );
        sc.add( "}" );
        sc.add( "" );
        sc.add( modelClass.getName() + " that = (" + modelClass.getName() + ") other;" );
        sc.add( "boolean result = true;" );

        sc.add( "" );

        for ( Iterator j = modelClass.getIdentifierFields( getGeneratedVersion() ).iterator(); j.hasNext(); )
        {
            ModelField identifier = (ModelField) j.next();

            String name = identifier.getName();
            if ( "boolean".equals( identifier.getType() ) || "byte".equals( identifier.getType() )
                 || "char".equals( identifier.getType() ) || "double".equals( identifier.getType() )
                 || "float".equals( identifier.getType() ) || "int".equals( identifier.getType() )
                 || "short".equals( identifier.getType() ) || "long".equals( identifier.getType() ) )
            {
                sc.add( "result = result && " + name + " == that." + name + ";" );
            }
            else
            {
                name = "get" + capitalise( name ) + "()";
                sc.add( "result = result && ( " + name + " == null ? that." + name + " == null : " + name
                        + ".equals( that." + name + " ) );" );
            }
        }

        if ( modelClass.getSuperClass() != null )
        {
            sc.add( "result = result && ( super.equals( other ) );" );
        }

        sc.add( "" );

        sc.add( "return result;" );

        return equals;
    }

    private JMethod generateToString( ModelClass modelClass )
    {
        JMethod toString = new JMethod( "toString", new JType( String.class.getName() ), null );

        List identifierFields = modelClass.getIdentifierFields( getGeneratedVersion() );

        JSourceCode sc = toString.getSourceCode();

        if ( identifierFields.size() == 0 )
        {
            sc.add( "return super.toString();" );

            return toString;
        }

        if ( useJava5 )
        {
            sc.add( "StringBuilder buf = new StringBuilder( 128 );" );
        }
        else
        {
            sc.add( "StringBuffer buf = new StringBuffer( 128 );" );
        }

        sc.add( "" );

        for ( Iterator j = identifierFields.iterator(); j.hasNext(); )
        {
            ModelField identifier = (ModelField) j.next();

            String getter = "boolean".equals( identifier.getType() ) ? "is" : "get";

            sc.add( "buf.append( \"" + identifier.getName() + " = '\" );" );
            sc.add( "buf.append( " + getter + capitalise( identifier.getName() ) + "() );" );
            sc.add( "buf.append( \"'\" );" );

            if ( j.hasNext() )
            {
                sc.add( "buf.append( \"\\n\" ); " );
            }
        }

        if ( modelClass.getSuperClass() != null )
        {
            sc.add( "buf.append( \"\\n\" );" );
            sc.add( "buf.append( super.toString() );" );
        }

        sc.add( "" );

        sc.add( "return buf.toString();" );

        return toString;
    }

    private JMethod generateHashCode( ModelClass modelClass )
    {
        JMethod hashCode = new JMethod( "hashCode", JType.INT, null );

        List identifierFields = modelClass.getIdentifierFields( getGeneratedVersion() );

        JSourceCode sc = hashCode.getSourceCode();

        if ( identifierFields.size() == 0 )
        {
            sc.add( "return super.hashCode();" );

            return hashCode;
        }

        sc.add( "int result = 17;" );

        sc.add( "" );

        for ( Iterator j = identifierFields.iterator(); j.hasNext(); )
        {
            ModelField identifier = (ModelField) j.next();

            sc.add( "result = 37 * result + " + createHashCodeForField( identifier ) + ";" );
        }

        if ( modelClass.getSuperClass() != null )
        {
            sc.add( "result = 37 * result + super.hashCode();" );
        }

        sc.add( "" );

        sc.add( "return result;" );

        return hashCode;
    }

    private JMethod[] generateClone( ModelClass modelClass )
        throws ModelloException
    {
        String cloneModeClass = getCloneMode( modelClass );

        if ( JavaClassMetadata.CLONE_NONE.equals( cloneModeClass ) )
        {
            return new JMethod[0];
        }

        JType returnType;
        if ( useJava5 )
        {
            returnType = new JClass( modelClass.getName() );
        }
        else
        {
            returnType = new JClass( "Object" );
        }

        JMethod cloneMethod = new JMethod( "clone", returnType, null );

        JSourceCode sc = cloneMethod.getSourceCode();

        sc.add( "try" );
        sc.add( "{" );
        sc.indent();

        sc.add( modelClass.getName() + " copy = (" + modelClass.getName() + ") super.clone();" );

        sc.add( "" );

        for ( Iterator j = modelClass.getFields( getGeneratedVersion() ).iterator(); j.hasNext(); )
        {
            ModelField modelField = (ModelField) j.next();

            String thisField = "this." + modelField.getName();
            String copyField = "copy." + modelField.getName();

            if ( "DOM".equals( modelField.getType() ) )
            {
                sc.add( "if ( " + thisField + " != null )" );
                sc.add( "{" );
                sc.addIndented( copyField
                    + " = new org.codehaus.plexus.util.xml.Xpp3Dom( (org.codehaus.plexus.util.xml.Xpp3Dom) "
                    + thisField + " );" );
                sc.add( "}" );
                sc.add( "" );
            }
            else if ( "Date".equalsIgnoreCase( modelField.getType() ) || "java.util.Date".equals( modelField.getType() ) )
            {
                sc.add( "if ( " + thisField + " != null )" );
                sc.add( "{" );
                sc.addIndented( copyField + " = (java.util.Date) " + thisField + ".clone();" );
                sc.add( "}" );
                sc.add( "" );
            }
            else if ( ModelDefault.PROPERTIES.equals( modelField.getType() ) )
            {
                sc.add( "if ( " + thisField + " != null )" );
                sc.add( "{" );
                sc.addIndented( copyField + " = (" + ModelDefault.PROPERTIES + ") " + thisField + ".clone();" );
                sc.add( "}" );
                sc.add( "" );
            }
            else if ( modelField instanceof ModelAssociation )
            {
                ModelAssociation modelAssociation = (ModelAssociation) modelField;

                String cloneModeAssoc = getCloneMode( modelAssociation, cloneModeClass );

                boolean deepClone =
                    JavaAssociationMetadata.CLONE_DEEP.equals( cloneModeAssoc )
                        && !immutableTypes.contains( modelAssociation.getTo() );

                if ( modelAssociation.isOneMultiplicity() )
                {
                    if ( deepClone )
                    {
                        sc.add( "if ( " + thisField + " != null )" );
                        sc.add( "{" );
                        sc.addIndented( copyField + " = (" + modelAssociation.getTo() + ") " + thisField + ".clone();" );
                        sc.add( "}" );
                        sc.add( "" );
                    }
                }
                else
                {
                    sc.add( "if ( " + thisField + " != null )" );
                    sc.add( "{" );
                    sc.indent();
                    sc.add( copyField + " = " + getDefaultValue( modelAssociation ) + ";" );

                    if ( isCollection( modelField.getType() ) )
                    {
                        if ( deepClone )
                        {
                            if ( useJava5 )
                            {
                                sc.add( "for ( " + modelAssociation.getTo() + " item : " + thisField + " )" );
                            }
                            else
                            {
                                sc.add( "for ( java.util.Iterator it = " + thisField + ".iterator(); it.hasNext(); )" );
                            }
                            sc.add( "{" );
                            sc.indent();
                            if ( useJava5 )
                            {
                                sc.add( copyField + ".add( item.clone() );" );
                            }
                            else
                            {
                                sc.add( copyField + ".add( ( (" + modelAssociation.getTo() + ") it.next() ).clone() );" );
                            }
                            sc.unindent();
                            sc.add( "}" );
                        }
                        else
                        {
                            sc.add( copyField + ".addAll( " + thisField + " );" );
                        }
                    }
                    else if ( isMap( modelField.getType() ) )
                    {
                        sc.add( copyField + ".clear();" );
                        sc.add( copyField + ".putAll( " + thisField + " );" );
                    }

                    sc.unindent();
                    sc.add( "}" );
                    sc.add( "" );
                }
            }
        }

        String cloneHook = getCloneHook( modelClass );

        if ( StringUtils.isNotEmpty( cloneHook ) && !"false".equalsIgnoreCase( cloneHook ) )
        {
            if ( "true".equalsIgnoreCase( cloneHook ) )
            {
                cloneHook = "cloneHook";
            }

            sc.add( cloneHook + "( copy );" );
            sc.add( "" );
        }

        sc.add( "return copy;" );

        sc.unindent();
        sc.add( "}" );
        sc.add( "catch ( " + Exception.class.getName() + " ex )" );
        sc.add( "{" );
        sc.indent();
        sc.add( "throw (" + RuntimeException.class.getName() + ") new " + UnsupportedOperationException.class.getName()
            + "( getClass().getName()" );
        sc.addIndented( "+ \" does not support clone()\" ).initCause( ex );" );
        sc.unindent();
        sc.add( "}" );

        return new JMethod[] { cloneMethod };
    }

    private String getCloneMode( ModelClass modelClass )
        throws ModelloException
    {
        String cloneMode = null;

        for ( ModelClass currentClass = modelClass;; )
        {
            JavaClassMetadata javaClassMetadata = (JavaClassMetadata) currentClass.getMetadata( JavaClassMetadata.ID );

            cloneMode = javaClassMetadata.getCloneMode();

            if ( cloneMode != null )
            {
                break;
            }

            String superClass = currentClass.getSuperClass();
            if ( StringUtils.isEmpty( superClass ) || !isClassInModel( superClass, getModel() ) )
            {
                break;
            }

            currentClass = getModel().getClass( superClass, getGeneratedVersion() );
        }

        if ( cloneMode == null )
        {
            cloneMode = JavaClassMetadata.CLONE_NONE;
        }
        else if ( !JavaClassMetadata.CLONE_MODES.contains( cloneMode ) )
        {
            throw new ModelloException( "The Java Modello Generator cannot use '" + cloneMode
                + "' as a value for <class java.clone=\"...\">, " + "only the following values are acceptable "
                + JavaClassMetadata.CLONE_MODES );
        }

        return cloneMode;
    }

    private String getCloneMode( ModelAssociation modelAssociation, String cloneModeClass )
        throws ModelloException
    {
        JavaAssociationMetadata javaAssociationMetadata =
            (JavaAssociationMetadata) modelAssociation.getAssociationMetadata( JavaAssociationMetadata.ID );

        String cloneModeAssoc = javaAssociationMetadata.getCloneMode();
        if ( cloneModeAssoc == null )
        {
            cloneModeAssoc = cloneModeClass;
        }
        else if ( !JavaAssociationMetadata.CLONE_MODES.contains( cloneModeAssoc ) )
        {
            throw new ModelloException( "The Java Modello Generator cannot use '" + cloneModeAssoc
                + "' as a value for <association java.clone=\"...\">, " + "only the following values are acceptable "
                + JavaAssociationMetadata.CLONE_MODES );
        }

        return cloneModeAssoc;
    }

    private String getCloneHook( ModelClass modelClass )
        throws ModelloException
    {
        JavaClassMetadata javaClassMetadata = (JavaClassMetadata) modelClass.getMetadata( JavaClassMetadata.ID );

        return javaClassMetadata.getCloneHook();
    }

    /**
     * Utility method that adds a period to the end of a string, if the last
     * non-whitespace character of the string is not a punctuation mark or an
     * end-tag.
     *
     * @param string The string to work with
     * @return The string that came in but with a period at the end
     */
    private String appendPeriod( String string )
    {
        if ( string == null )
        {
            return string;
        }

        String trimmedString = string.trim();
        if ( trimmedString.endsWith( "." ) || trimmedString.endsWith( "!" ) || trimmedString.endsWith( "?" )
            || trimmedString.endsWith( ">" ) )
        {
            return string;
        }
        else
        {
            return string + ".";
        }
    }

    private String createHashCodeForField( ModelField identifier )
    {
        String name = identifier.getName();
        String type = identifier.getType();

        if ( "boolean".equals( type ) )
        {
            return "( " + name + " ? 0 : 1 )";
        }
        else if ( "byte".equals( type ) || "char".equals( type ) || "short".equals( type ) || "int".equals( type ) )
        {
            return "(int) " + name;
        }
        else if ( "long".equals( type ) )
        {
            return "(int) ( " + name + " ^ ( " + name + " >>> 32 ) )";
        }
        else if ( "float".equals( type ) )
        {
            return "Float.floatToIntBits( " + name + " )";
        }
        else if ( "double".equals( type ) )
        {
            return "(int) ( Double.doubleToLongBits( " + identifier.getName() + " ) ^ ( Double.doubleToLongBits( " + identifier.getName() + " ) >>> 32 ) )";
        }
        else
        {
            return "( " + name + " != null ? " + name + ".hashCode() : 0 )";
        }
    }

    private JField createField( ModelField modelField )
        throws ModelloException
    {
        JType type;

        String baseType = modelField.getType();
        if ( modelField.isArray() )
        {
            // remove [] at the end of the type
            baseType = baseType.substring( 0, baseType.length() - 2 );
        }

        if ( baseType.equals( "boolean" ) )
        {
            type = JType.BOOLEAN;
        }
        else if ( baseType.equals( "byte" ) )
        {
            type = JType.BYTE;
        }
        else if ( baseType.equals( "char" ) )
        {
            type = JType.CHAR;
        }
        else if ( baseType.equals( "double" ) )
        {
            type = JType.DOUBLE;
        }
        else if ( baseType.equals( "float" ) )
        {
            type = JType.FLOAT;
        }
        else if ( baseType.equals( "int" ) )
        {
            type = JType.INT;
        }
        else if ( baseType.equals( "short" ) )
        {
            type = JType.SHORT;
        }
        else if ( baseType.equals( "long" ) )
        {
            type = JType.LONG;
        }
        else if ( baseType.equals( "Date" ) )
        {
            type = new JClass( "java.util.Date" );
        }
        else if ( baseType.equals( "DOM" ) )
        {
            // TODO: maybe DOM is not how to specify it in the model, but just Object and markup Xpp3Dom for the Xpp3Reader?
            //   not sure how we'll treat it for the other sources, eg sql.
            type = new JClass( "Object" );
        }
        else
        {
            type = new JClass( baseType );
        }

        if ( modelField.isArray() )
        {
            type = new JArrayType( type, useJava5 );
        }

        JField field = new JField( type, modelField.getName() );

        if ( modelField.isModelVersionField() )
        {
            field.setInitString( "\"" + getGeneratedVersion() + "\"" );
        }

        if ( modelField.getDefaultValue() != null )
        {
            field.setInitString( getJavaDefaultValue( modelField ) );
        }

        if ( StringUtils.isNotEmpty( modelField.getDescription() ) )
        {
            field.setComment( appendPeriod( modelField.getDescription() ) );
        }

        return field;
    }

    private void createField( JClass jClass, ModelField modelField )
        throws ModelloException
    {
        JavaFieldMetadata javaFieldMetadata = (JavaFieldMetadata) modelField.getMetadata( JavaFieldMetadata.ID );

        JField field = createField( modelField );

        jClass.addField( field );

        if ( javaFieldMetadata.isGetter() )
        {
            jClass.addMethod( createGetter( field, modelField ) );
        }

        if ( javaFieldMetadata.isSetter() )
        {
            jClass.addMethod( createSetter( field, modelField ) );
        }
    }

    private JMethod createGetter( JField field, ModelField modelField )
    {
        String propertyName = capitalise( field.getName() );

        JavaFieldMetadata javaFieldMetadata = (JavaFieldMetadata) modelField.getMetadata( JavaFieldMetadata.ID );

        String prefix = javaFieldMetadata.isBooleanGetter() ? "is" : "get";

        JType returnType = field.getType();
        String interfaceCast = "";

        if ( modelField instanceof ModelAssociation )
        {
            ModelAssociation modelAssociation = (ModelAssociation) modelField;

            JavaAssociationMetadata javaAssociationMetadata = (JavaAssociationMetadata) modelAssociation
                .getAssociationMetadata( JavaAssociationMetadata.ID );

            if ( StringUtils.isNotEmpty( javaAssociationMetadata.getInterfaceName() )
                 && !javaFieldMetadata.isBooleanGetter() )
            {
                returnType = new JClass( javaAssociationMetadata.getInterfaceName() );

                interfaceCast = "(" + javaAssociationMetadata.getInterfaceName() + ") ";
            }
        }

        JMethod getter = new JMethod( prefix + propertyName, returnType, null );

        StringBuffer comment = new StringBuffer( "Get " );
        if ( StringUtils.isEmpty( modelField.getDescription() ) )
        {
            comment.append( "the " );
            comment.append( field.getName() );
            comment.append( " field" );
        }
        else
        {
            comment.append( StringUtils.lowercaseFirstLetter( modelField.getDescription().trim() ) );
        }
        getter.getJDocComment().setComment( appendPeriod( comment.toString() ) );

        getter.getSourceCode().add( "return " + interfaceCast + "this." + field.getName() + ";" );

        return getter;
    }

    private JMethod createSetter( JField field, ModelField modelField )
        throws ModelloException
    {
        String propertyName = capitalise( field.getName() );

        JMethod setter = new JMethod( "set" + propertyName );

        StringBuffer comment = new StringBuffer( "Set " );
        if ( StringUtils.isEmpty( modelField.getDescription() ) )
        {
            comment.append( "the " );
            comment.append( field.getName() );
            comment.append( " field" );
        }
        else
        {
            comment.append( StringUtils.lowercaseFirstLetter( modelField.getDescription().trim() ) );
        }
        setter.getJDocComment().setComment( appendPeriod( comment.toString() ) );

        JType parameterType = getDesiredType( modelField, false );

        setter.addParameter( new JParameter( parameterType, field.getName() ) );

        JSourceCode sc = setter.getSourceCode();

        if ( modelField instanceof ModelAssociation )
        {
            ModelAssociation modelAssociation = (ModelAssociation) modelField;

            JavaAssociationMetadata javaAssociationMetadata = (JavaAssociationMetadata) modelAssociation
                .getAssociationMetadata( JavaAssociationMetadata.ID );

            boolean isOneMultiplicity = isBidirectionalAssociation( modelAssociation )
                 && modelAssociation.isOneMultiplicity();

            if ( isOneMultiplicity && javaAssociationMetadata.isBidi() )
            {
                sc.add( "if ( this." + field.getName() + " != null )" );

                sc.add( "{" );

                sc.indent();

                sc.add( "this." + field.getName() + ".break" + modelAssociation.getModelClass().getName() +
                    "Association( this );" );

                sc.unindent();

                sc.add( "}" );

                sc.add( "" );
            }

            String interfaceCast = "";

            if ( StringUtils.isNotEmpty( javaAssociationMetadata.getInterfaceName() )
                 && modelAssociation.isOneMultiplicity() )
            {
                interfaceCast = "(" + field.getType().getName() + ") ";

                createClassCastAssertion( sc, modelAssociation, "set" );
            }

            sc.add( "this." + field.getName() + " = " + interfaceCast + field.getName() + ";" );

            if ( isOneMultiplicity && javaAssociationMetadata.isBidi() )
            {
                sc.add( "" );

                sc.add( "if ( " + field.getName() + " != null )" );

                sc.add( "{" );

                sc.indent();

                sc.add( "this." + field.getName() + ".create" + modelAssociation.getModelClass().getName() +
                    "Association( this );" );

                sc.unindent();

                sc.add( "}" );
            }
        }
        else
        {
            sc.add( "this." + field.getName() + " = " + field.getName() + ";" );
        }

        return setter;
    }

    private void createClassCastAssertion( JSourceCode sc, ModelAssociation modelAssociation, String crudModifier )
        throws ModelloException
    {
        JavaAssociationMetadata javaAssociationMetadata =
            (JavaAssociationMetadata) modelAssociation.getAssociationMetadata( JavaAssociationMetadata.ID );

        if ( StringUtils.isEmpty( javaAssociationMetadata.getInterfaceName() ) )
        {
            return; // java.useInterface feature not used, no class cast assertion needed
        }

        String propertyName = capitalise( modelAssociation.getName() );

        JField field = createField( modelAssociation );
        String fieldName = field.getName();
        JType type = field.getType();

        if ( modelAssociation.isOneMultiplicity() )
        {
            type = new JClass( javaAssociationMetadata.getInterfaceName() );
        }
        else
        {
            fieldName = uncapitalise( modelAssociation.getTo() );
            type = new JClass( modelAssociation.getTo() );
        }

        String instanceName = type.getName();

        // Add sane class cast exception message
        // When will sun ever fix this?

        sc.add( "if ( !( " + fieldName + " instanceof " + instanceName + " ) )" );

        sc.add( "{" );

        sc.indent();

        sc.add( "throw new ClassCastException( \"" + modelAssociation.getModelClass().getName() + "." + crudModifier
            + propertyName + "(" + fieldName + ") parameter must be instanceof \" + " + instanceName
            + ".class.getName() );" );

        sc.unindent();

        sc.add( "}" );
    }

    private void createAssociation( JClass jClass, ModelAssociation modelAssociation, JSourceCode jConstructorSource )
        throws ModelloException
    {
        JavaFieldMetadata javaFieldMetadata = (JavaFieldMetadata) modelAssociation.getMetadata( JavaFieldMetadata.ID );

        JavaAssociationMetadata javaAssociationMetadata =
            (JavaAssociationMetadata) modelAssociation.getAssociationMetadata( JavaAssociationMetadata.ID );

        if ( !JavaAssociationMetadata.INIT_TYPES.contains( javaAssociationMetadata.getInitializationMode() ) )
        {
            throw new ModelloException( "The Java Modello Generator cannot use '"
                + javaAssociationMetadata.getInitializationMode() + "' as a <association java.init=\""
                + javaAssociationMetadata.getInitializationMode() + "\"> "
                + "value, the only the following are acceptable " + JavaAssociationMetadata.INIT_TYPES );
        }

        if ( modelAssociation.isManyMultiplicity() )
        {
            JType type;
            if ( modelAssociation.isGenericType() )
            {
                type = new JCollectionType( modelAssociation.getType(), new JClass( modelAssociation.getTo() ),
                                            useJava5 );
            }
            else
            {
                type = new JClass( modelAssociation.getType() );
            }

            JField jField = new JField( type, modelAssociation.getName() );

            if ( !isEmpty( modelAssociation.getComment() ) )
            {
                jField.setComment( modelAssociation.getComment() );
            }

            if ( StringUtils.equals( javaAssociationMetadata.getInitializationMode(),
                                     JavaAssociationMetadata.FIELD_INIT ) )
            {
                jField.setInitString( getDefaultValue ( modelAssociation ) );
            }

            if ( StringUtils.equals( javaAssociationMetadata.getInitializationMode(),
                                     JavaAssociationMetadata.CONSTRUCTOR_INIT ) )
            {
                jConstructorSource.add( "this." + jField.getName() + " = " + getDefaultValue ( modelAssociation ) + ";" );
            }

            jClass.addField( jField );

            if ( javaFieldMetadata.isGetter() )
            {
                String propertyName = capitalise( jField.getName() );

                JMethod getter = new JMethod( "get" + propertyName, jField.getType(), null );

                JSourceCode sc = getter.getSourceCode();

                if ( StringUtils.equals( javaAssociationMetadata.getInitializationMode(),
                                         JavaAssociationMetadata.LAZY_INIT ) )
                {
                    sc.add( "if ( this." + jField.getName() + " == null )" );

                    sc.add( "{" );

                    sc.indent();

                    sc.add( "this." + jField.getName() + " = " + getDefaultValue ( modelAssociation ) + ";" );

                    sc.unindent();

                    sc.add( "}" );

                    sc.add( "" );
                }

                sc.add( "return this." + jField.getName() + ";" );

                jClass.addMethod( getter );
            }

            if ( javaFieldMetadata.isSetter() )
            {
                jClass.addMethod( createSetter( jField, modelAssociation ) );
            }

            if ( javaAssociationMetadata.isAdder() )
            {
                createAdder( modelAssociation, jClass );
            }
        }
        else
        {
            createField( jClass, modelAssociation );
        }

        if ( isBidirectionalAssociation( modelAssociation ) )
        {
            if ( javaAssociationMetadata.isBidi() )
            {
                createCreateAssociation( jClass, modelAssociation );
            }

            if ( javaAssociationMetadata.isBidi() )
            {
                createBreakAssociation( jClass, modelAssociation );
            }
        }
    }

    private void createCreateAssociation( JClass jClass, ModelAssociation modelAssociation )
    {
        JMethod createMethod = new JMethod( "create" + modelAssociation.getTo() + "Association" );

        JavaAssociationMetadata javaAssociationMetadata =
            (JavaAssociationMetadata) modelAssociation.getAssociationMetadata( JavaAssociationMetadata.ID );

        createMethod.addParameter(
            new JParameter( new JClass( modelAssociation.getTo() ), uncapitalise( modelAssociation.getTo() ) ) );

        // TODO: remove after tested
//            createMethod.addException( new JClass( "Exception" ) );

        JSourceCode sc = createMethod.getSourceCode();

        if ( modelAssociation.isOneMultiplicity() )
        {
            if ( javaAssociationMetadata.isBidi() )
            {
                sc.add( "if ( this." + modelAssociation.getName() + " != null )" );

                sc.add( "{" );

                sc.indent();

                sc.add(
                    "break" + modelAssociation.getTo() + "Association( this." + modelAssociation.getName() + " );" );

                sc.unindent();

                sc.add( "}" );

                sc.add( "" );
            }

            sc.add( "this." + modelAssociation.getName() + " = " + uncapitalise( modelAssociation.getTo() ) + ";" );
        }
        else
        {
            jClass.addImport( "java.util.Collection" );

            sc.add( "Collection " + modelAssociation.getName() + " = get" + capitalise( modelAssociation.getName() )
                    + "();" );

            sc.add( "" );

            sc.add( "if ( " + modelAssociation.getName() + ".contains( "
                    + uncapitalise( modelAssociation.getTo() ) + " ) )" );

            sc.add( "{" );

            sc.indent();

            sc.add( "throw new IllegalStateException( \"" + uncapitalise( modelAssociation.getTo() )
                    + " is already assigned.\" );" );

            sc.unindent();

            sc.add( "}" );

            sc.add( "" );

            sc.add( modelAssociation.getName() + ".add( " + uncapitalise( modelAssociation.getTo() ) + " );" );
        }

        jClass.addMethod( createMethod );
    }

    private void createBreakAssociation( JClass jClass, ModelAssociation modelAssociation )
    {
        JSourceCode sc;
        JMethod breakMethod = new JMethod( "break" + modelAssociation.getTo() + "Association" );

        breakMethod.addParameter(
            new JParameter( new JClass( modelAssociation.getTo() ), uncapitalise( modelAssociation.getTo() ) ) );

        // TODO: remove after tested
//            breakMethod.addException( new JClass( "Exception" ) );

        sc = breakMethod.getSourceCode();

        if ( modelAssociation.isOneMultiplicity() )
        {
            sc.add(
                "if ( this." + modelAssociation.getName() + " != " + uncapitalise( modelAssociation.getTo() ) + " )" );

            sc.add( "{" );

            sc.indent();

            sc.add( "throw new IllegalStateException( \"" + uncapitalise( modelAssociation.getTo() )
                    + " isn't associated.\" );" );

            sc.unindent();

            sc.add( "}" );

            sc.add( "" );

            sc.add( "this." + modelAssociation.getName() + " = null;" );
        }
        else
        {
            sc.add( "if ( ! get" + capitalise( modelAssociation.getName() ) + "().contains( "
                    + uncapitalise( modelAssociation.getTo() ) + " ) )" );

            sc.add( "{" );

            sc.indent();

            sc.add( "throw new IllegalStateException( \"" + uncapitalise( modelAssociation.getTo() )
                    + " isn't associated.\" );" );

            sc.unindent();

            sc.add( "}" );

            sc.add( "" );

            sc.add( "get" + capitalise( modelAssociation.getName() ) + "().remove( "
                    + uncapitalise( modelAssociation.getTo() ) + " );" );
        }

        jClass.addMethod( breakMethod );
    }

    private void createAdder( ModelAssociation modelAssociation, JClass jClass )
        throws ModelloException
    {
        String fieldName = modelAssociation.getName();

        JavaAssociationMetadata javaAssociationMetadata =
            (JavaAssociationMetadata) modelAssociation.getAssociationMetadata( JavaAssociationMetadata.ID );

        String parameterName = uncapitalise( modelAssociation.getTo() );
        String implementationParameterName = parameterName;

        boolean bidirectionalAssociation = isBidirectionalAssociation( modelAssociation );

        JType addType;

        if ( StringUtils.isNotEmpty( javaAssociationMetadata.getInterfaceName() ) )
        {
            addType = new JClass( javaAssociationMetadata.getInterfaceName() );
            implementationParameterName = "( (" + modelAssociation.getTo() + ") " + parameterName + " )";
        }
        else if ( modelAssociation.getToClass() != null )
        {
            addType = new JClass( modelAssociation.getToClass().getName() );
        }
        else
        {
            addType = new JClass( "String" );
        }

        if ( modelAssociation.getType().equals( ModelDefault.PROPERTIES )
             || modelAssociation.getType().equals( ModelDefault.MAP ) )
        {
            JMethod adder = new JMethod( "add" + capitalise( singular( fieldName ) ) );

            if ( modelAssociation.getType().equals( ModelDefault.MAP ) )
            {
                adder.addParameter( new JParameter( new JClass( "Object" ), "key" ) );
            }
            else
            {
                adder.addParameter( new JParameter( new JClass( "String" ), "key" ) );
            }

            adder.addParameter( new JParameter( new JClass( modelAssociation.getTo() ), "value" ) );

            adder.getSourceCode().add( "get" + capitalise( fieldName ) + "().put( key, value );" );

            jClass.addMethod( adder );
        }
        else
        {
            JMethod adder = new JMethod( "add" + singular( capitalise( fieldName ) ) );

            adder.addParameter( new JParameter( addType, parameterName ) );

            createClassCastAssertion( adder.getSourceCode(), modelAssociation, "add" );

            adder.getSourceCode().add(
                "get" + capitalise( fieldName ) + "().add( " + implementationParameterName + " );" );

            if ( bidirectionalAssociation && javaAssociationMetadata.isBidi() )
            {
                // TODO: remove after tested
//                adder.addException( new JClass( "Exception" ) );

                adder.getSourceCode().add( implementationParameterName + ".create"
                    + modelAssociation.getModelClass().getName() + "Association( this );" );
            }

            jClass.addMethod( adder );

            JMethod remover = new JMethod( "remove" + singular( capitalise( fieldName ) ) );

            remover.addParameter( new JParameter( addType, parameterName ) );

            createClassCastAssertion( remover.getSourceCode(), modelAssociation, "remove" );

            if ( bidirectionalAssociation && javaAssociationMetadata.isBidi() )
            {
                // TODO: remove after tested
//                remover.addException( new JClass( "Exception" ) );

                remover.getSourceCode().add(
                    parameterName + ".break" + modelAssociation.getModelClass().getName() + "Association( this );" );
            }

            remover.getSourceCode().add(
                "get" + capitalise( fieldName ) + "().remove( " + implementationParameterName + " );" );

            jClass.addMethod( remover );
        }
    }

    private boolean isBidirectionalAssociation( ModelAssociation association )
    {
        Model model = association.getModelClass().getModel();

        if ( !isClassInModel( association.getTo(), model ) )
        {
            return false;
        }

        ModelClass toClass = association.getToClass();

        for ( Iterator j = toClass.getFields( getGeneratedVersion() ).iterator(); j.hasNext(); )
        {
            ModelField modelField = (ModelField) j.next();

            if ( !( modelField instanceof ModelAssociation ) )
            {
                continue;
            }

            ModelAssociation modelAssociation = (ModelAssociation) modelField;

            if ( !isClassInModel( modelAssociation.getTo(), model ) )
            {
                continue;
            }

            ModelClass totoClass = modelAssociation.getToClass();

            if ( association.getModelClass().equals( totoClass ) )
            {
                return true;
            }
        }

        return false;
    }

    private JType getDesiredType( ModelField modelField, boolean useTo )
        throws ModelloException
    {
        JField field = createField( modelField );
        JType type = field.getType();

        if ( modelField instanceof ModelAssociation )
        {
            ModelAssociation modelAssociation = (ModelAssociation) modelField;
            JavaAssociationMetadata javaAssociationMetadata = (JavaAssociationMetadata) modelAssociation
                .getAssociationMetadata( JavaAssociationMetadata.ID );

            if ( StringUtils.isNotEmpty( javaAssociationMetadata.getInterfaceName() )
                 && ! modelAssociation.isManyMultiplicity() )
            {
                type = new JClass( javaAssociationMetadata.getInterfaceName() );
            }
            else if ( modelAssociation.isManyMultiplicity() && modelAssociation.isGenericType() )
            {
                type = new JCollectionType( modelAssociation.getType(), new JClass( modelAssociation.getTo() ),
                                            useJava5 );
            }
            else if ( useTo )
            {
                type = new JClass( modelAssociation.getTo() );
            }
        }

        return type;
    }
}
