/* ==========================================================================
 * Copyright 2005 Mevenide Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * =========================================================================
 */
package org.codehaus.modello.plugin.jdom;

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelDefault;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.plugin.java.JavaFieldMetadata;
import org.codehaus.modello.plugin.java.javasource.JClass;
import org.codehaus.modello.plugin.java.javasource.JConstructor;
import org.codehaus.modello.plugin.java.javasource.JField;
import org.codehaus.modello.plugin.java.javasource.JMethod;
import org.codehaus.modello.plugin.java.javasource.JParameter;
import org.codehaus.modello.plugin.java.javasource.JSourceCode;
import org.codehaus.modello.plugin.java.javasource.JSourceWriter;
import org.codehaus.modello.plugin.java.javasource.JType;
import org.codehaus.modello.plugin.model.ModelClassMetadata;
import org.codehaus.modello.plugins.xml.XmlFieldMetadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author mkleint@codehaus.org
 */
public class JDOMWriterGenerator
    extends AbstractJDOMGenerator
{

    public void generate( Model model, Properties parameters )
        throws ModelloException
    {
        initialize( model, parameters );
        try
        {
            generateJDOMWriter();
        }
        catch ( IOException ex )
        {
            throw new ModelloException( "Exception while generating JDOM Writer.", ex );
        }
    }

    private void generateJDOMWriter()
        throws ModelloException, IOException
    {
        Model objectModel = getModel();

        String packageName;
        if ( isPackageWithVersion() )
        {
            packageName = objectModel.getDefaultPackageName( true, getGeneratedVersion() );
        }
        else
        {
            packageName = objectModel.getDefaultPackageName( false, null );
        }
        packageName = packageName + ".io.jdom";

        String marshallerName = getFileName( "JDOMWriter" );

        JSourceWriter sourceWriter = newJSourceWriter( packageName, marshallerName );

        JClass jClass = new JClass( marshallerName );
        // -------------------------------------------------------------
        // imports now
        // -------------------------------------------------------------
        jClass.setPackageName( packageName );
        jClass.addImport( "java.io.OutputStream" );
        jClass.addImport( "java.io.OutputStreamWriter" );
        jClass.addImport( "java.io.Writer" );
        jClass.addImport( "java.util.ArrayList" );
        jClass.addImport( "java.util.Iterator" );
        jClass.addImport( "java.util.List" );
        jClass.addImport( "java.util.Properties" );
        jClass.addImport( "java.util.Map" );
        jClass.addImport( "java.util.Collection" );
        jClass.addImport( "java.util.ListIterator" );
        jClass.addImport( "org.jdom.DefaultJDOMFactory" );
        jClass.addImport( "org.jdom.Content" );
        jClass.addImport( "org.jdom.Element" );
        jClass.addImport( "org.jdom.Document" );
        jClass.addImport( "org.jdom.Text" );
        jClass.addImport( "org.jdom.output.Format" );
        jClass.addImport( "org.jdom.output.XMLOutputter" );
        jClass.addImport( "org.codehaus.plexus.util.xml.Xpp3Dom" );

//        jClass.addImport( "" );
        addModelImports( jClass, null );

        jClass.addField( new JField( new JClass( "DefaultJDOMFactory" ), "factory" ) );
        jClass.addField( new JField( new JClass( "String" ), "lineSeparator" ) );

        createConter( jClass );

        // constructor --
        JConstructor constructor = jClass.createConstructor();
        JSourceCode constCode = constructor.getSourceCode();
        constCode.add( "factory = new DefaultJDOMFactory();" );
        constCode.add( "lineSeparator = \"\\n\";" );

        String root = objectModel.getRoot( getGeneratedVersion() );
        ModelClass rootClass = objectModel.getClass( root, getGeneratedVersion() );
        ModelClassMetadata metadata = (ModelClassMetadata) rootClass.getMetadata( ModelClassMetadata.ID );

        String rootElement;
        if ( metadata == null || metadata.getTagName() == null )
        {
            rootElement = uncapitalise( root );
        }
        else
        {
            rootElement = metadata.getTagName();
        }

        // the public global write method..
        jClass.addMethod( generateWriteModel( root, rootElement ) );
        jClass.addMethod( generateWriteModel2( root, rootElement ) );
        jClass.addMethod( generateWriteModel3( root, rootElement ) );
        // the private utility classes;
        jClass.addMethods( generateUtilityMethods() );

        writeAllClasses( objectModel, jClass, rootClass );
        jClass.print( sourceWriter );
        sourceWriter.close();
    }

    private void createConter( final JClass jClass )
        throws IllegalArgumentException
    {
        //inner counter class
        JClass counter = jClass.createInnerClass( "Counter" );
        counter.getModifiers().setStatic( true );
        JField fld = new JField( new JType( "int" ), "currentIndex" );
        fld.setInitString( "0" );
        counter.addField( fld );
        fld = new JField( new JType( "int" ), "level" );
        counter.addField( fld );

        JConstructor constr =
            counter.createConstructor( new JParameter[]{new JParameter( new JType( "int" ), "depthLevel" )} );
        constr.getSourceCode().append( "level = depthLevel;" );

        JMethod inc = new JMethod( "increaseCount" );
        inc.getSourceCode().add( "currentIndex = currentIndex + 1;" );
        counter.addMethod( inc );
        JMethod getter = new JMethod( new JType( "int" ), "getCurrentIndex" );
        getter.getSourceCode().add( "return currentIndex;" );
        counter.addMethod( getter );
        getter = new JMethod( new JType( "int" ), "getDepth" );
        getter.getSourceCode().add( "return level;" );
        counter.addMethod( getter );
    }

    private JMethod generateWriteModel( String root, String rootElement )
    {
        JMethod marshall = new JMethod( null, "write" );

        marshall.addParameter( new JParameter( new JClass( root ), rootElement ) );
        marshall.addParameter( new JParameter( new JClass( "Document" ), "document" ) );
        marshall.addParameter( new JParameter( new JClass( "OutputStream" ), "stream" ) );
        marshall.addException( new JClass( "java.io.IOException" ) );

        marshall.getJDocComment().appendComment( "\n@deprecated" );

        JSourceCode sc = marshall.getSourceCode();
        sc.add( "update" + root + "(" + rootElement + ", \"" + rootElement
            + "\", new Counter(0), document.getRootElement());" );
        sc.add( "XMLOutputter outputter = new XMLOutputter();" );
        sc.add( "outputter.setFormat(Format.getPrettyFormat()" );
        sc.add( ".setIndent(\"    \")" );
        sc.add( ".setLineSeparator(System.getProperty(\"line.separator\")));" );
        sc.add( "outputter.output(document, stream);" );

        return marshall;

    }

    private JMethod generateWriteModel2( String root, String rootElement )
    {
        JMethod marshall = new JMethod( null, "write" );

        marshall.addParameter( new JParameter( new JClass( root ), rootElement ) );
        marshall.addParameter( new JParameter( new JClass( "Document" ), "document" ) );
        marshall.addParameter( new JParameter( new JClass( "OutputStreamWriter" ), "writer" ) );
        marshall.addException( new JClass( "java.io.IOException" ) );

        JSourceCode sc = marshall.getSourceCode();
        sc.add( "Format format = Format.getRawFormat()" );
        sc.add( ".setEncoding(writer.getEncoding())" );
        sc.add( ".setLineSeparator(System.getProperty(\"line.separator\"));" );
        sc.add( "write(" + rootElement + ", document, writer, format);" );
        return marshall;

    }

    private JMethod generateWriteModel3( String root, String rootElement )
    {
        JMethod marshall = new JMethod( null, "write" );

        marshall.addParameter( new JParameter( new JClass( root ), rootElement ) );
        marshall.addParameter( new JParameter( new JClass( "Document" ), "document" ) );
        marshall.addParameter( new JParameter( new JClass( "Writer" ), "writer" ) );
        marshall.addParameter( new JParameter( new JClass( "Format" ), "jdomFormat" ) );
        marshall.addException( new JClass( "java.io.IOException" ) );

        JSourceCode sc = marshall.getSourceCode();
        sc.add( "update" + root + "(" + rootElement + ", \"" + rootElement
            + "\", new Counter(0), document.getRootElement());" );
        sc.add( "XMLOutputter outputter = new XMLOutputter();" );
        sc.add( "outputter.setFormat(jdomFormat);" );
        sc.add( "outputter.output(document, writer);" );

        return marshall;

    }

    private JMethod[] generateUtilityMethods()
    {
        JMethod findRSElement = new JMethod( new JClass( "Element" ), "findAndReplaceSimpleElement" );
        findRSElement.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        findRSElement.addParameter( new JParameter( new JClass( "Element" ), "parent" ) );
        findRSElement.addParameter( new JParameter( new JClass( "String" ), "name" ) );
        findRSElement.addParameter( new JParameter( new JClass( "String" ), "text" ) );
        findRSElement.addParameter( new JParameter( new JClass( "String" ), "defaultValue" ) );

        findRSElement.getModifiers().makeProtected();
        JSourceCode sc = findRSElement.getSourceCode();
        sc.add( "if (defaultValue != null && text != null && defaultValue.equals(text)) {" );
        sc.indent();
        sc.add( "Element element =  parent.getChild(name, parent.getNamespace());" );
        sc.add( "// if exist and is default value or if doesn't exist.. just keep the way it is.." );
        sc.add( "if ((element != null && defaultValue.equals(element.getText())) || element == null) {" );
        sc.addIndented( "return element;" );
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );

        sc.add( "boolean shouldExist = text != null && text.trim().length() > 0;" );
        sc.add( "Element element = updateElement(counter, parent, name, shouldExist);" );
        sc.add( "if (shouldExist) {" );
        sc.addIndented( "element.setText(text);" );
        sc.add( "}" );
        sc.add( "return element;" );

        JMethod updateElement = new JMethod( new JClass( "Element" ), "updateElement" );
        updateElement.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        updateElement.addParameter( new JParameter( new JClass( "Element" ), "parent" ) );
        updateElement.addParameter( new JParameter( new JClass( "String" ), "name" ) );
        updateElement.addParameter( new JParameter( new JType( "boolean" ), "shouldExist" ) );
        updateElement.getModifiers().makeProtected();
        sc = updateElement.getSourceCode();
        sc.add( "Element element =  parent.getChild(name, parent.getNamespace());" );
        sc.add( "if (element != null && shouldExist) {" );
        sc.addIndented( "counter.increaseCount();" );
        sc.add( "}" );
        sc.add( "if (element == null && shouldExist) {" );
        sc.indent();
        sc.add( "element = factory.element(name, parent.getNamespace());" );
        sc.add( "insertAtPreferredLocation(parent, element, counter);" );
        sc.add( "counter.increaseCount();" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "if (!shouldExist && element != null) {" );
        sc.indent();
        sc.add( "int index = parent.indexOf(element);" );
        sc.add( "if (index > 0) {" );
        sc.indent();
        sc.add( "Content previous = parent.getContent(index - 1);" );
        sc.add( "if (previous instanceof Text) {" );
        sc.indent();
        sc.add( "Text txt = (Text)previous;" );
        sc.add( "if (txt.getTextTrim().length() == 0) {" );
        sc.addIndented( "parent.removeContent(txt);" );
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "parent.removeContent(element);" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "return element;" );

        JMethod insAtPref = new JMethod( null, "insertAtPreferredLocation" );
        insAtPref.addParameter( new JParameter( new JClass( "Element" ), "parent" ) );
        insAtPref.addParameter( new JParameter( new JClass( "Element" ), "child" ) );
        insAtPref.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        insAtPref.getModifiers().makeProtected();
        sc = insAtPref.getSourceCode();
        sc.add( "int contentIndex = 0;" );
        sc.add( "int elementCounter = 0;" );
        sc.add( "Iterator it = parent.getContent().iterator();" );
        sc.add( "Text lastText = null;" );
        sc.add( "int offset = 0;" );
        sc.add( "while (it.hasNext() && elementCounter <= counter.getCurrentIndex()) {" );
        sc.indent();
        sc.add( "Object next = it.next();" );
        sc.add( "offset = offset + 1;" );
        sc.add( "if (next instanceof Element) {" );
        sc.indent();
        sc.add( "elementCounter = elementCounter + 1;" );
        sc.add( "contentIndex = contentIndex + offset;" );
        sc.add( "offset = 0;" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "if (next instanceof Text && it.hasNext()) {" );
        sc.addIndented( "lastText = (Text)next;" );
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
//        sc.add("if (lastText == null) {");
//        sc.indent();
//        sc.add("int index = parent.getParentElement().indexOf(parent);");
//        sc.add("if (index > 0) {");
//        sc.indent();
//        sc.add("Content cont = parent.getParentElement().getContent( index  - 1);");
//        sc.add("if (cont instanceof Text) {");
//        sc.addIndented("lastText = (Text)cont;");
//        sc.add("}");
//        sc.unindent();
//        sc.add("}");
//        sc.unindent();
//        sc.add("}");

        sc.add( "if (lastText != null && lastText.getTextTrim().length() == 0) {" );
        sc.addIndented( "lastText = (Text)lastText.clone();" );
        sc.add( "} else {" );
        sc.indent();
        sc.add( "String starter = lineSeparator;" );
        sc.add( "for (int i = 0; i < counter.getDepth(); i++) {" );
        sc.indent();
        sc.add( "starter = starter + \"    \"; //TODO make settable?" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "lastText = factory.text(starter);" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "if (parent.getContentSize() == 0) {" );
        sc.indent();
        sc.add( "Text finalText = (Text)lastText.clone();" );
        sc.add(
            "finalText.setText(finalText.getText().substring(0, finalText.getText().length() - \"    \".length()));" );
        sc.add( "parent.addContent(contentIndex, finalText);" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "parent.addContent(contentIndex, child);" );
        sc.add( "parent.addContent(contentIndex, lastText);" );

        JMethod findRSProps = new JMethod( new JClass( "Element" ), "findAndReplaceProperties" );
        findRSProps.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        findRSProps.addParameter( new JParameter( new JClass( "Element" ), "parent" ) );
        findRSProps.addParameter( new JParameter( new JClass( "String" ), "name" ) );
        findRSProps.addParameter( new JParameter( new JClass( "Map" ), "props" ) );
        findRSProps.getModifiers().makeProtected();
        sc = findRSProps.getSourceCode();
        sc.add( "boolean shouldExist = props != null && ! props.isEmpty();" );
        sc.add( "Element element = updateElement(counter, parent, name, shouldExist);" );
        sc.add( "if (shouldExist) {" );
        sc.indent();
        sc.add( "Iterator it = props.keySet().iterator();" );
        sc.add( "Counter innerCounter = new Counter(counter.getDepth() + 1);" );
        sc.add( "while (it.hasNext()) {" );
        sc.indent();
        sc.add( "String key = (String) it.next();" );
        sc.add( "findAndReplaceSimpleElement(innerCounter, element, key, (String)props.get(key), null);" );
        sc.add( "}" );
        sc.unindent();
        sc.add( "ArrayList lst = new ArrayList(props.keySet());" );
        sc.add( "it = element.getChildren().iterator();" );
        sc.add( "while (it.hasNext()) {" );
        sc.indent();
        sc.add( "Element elem = (Element) it.next();" );
        sc.add( "String key = elem.getName();" );
        sc.add( "if (!lst.contains(key)) {" );
        sc.addIndented( "it.remove();" );
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "return element;" );

        JMethod findRSLists = new JMethod( new JClass( "Element" ), "findAndReplaceSimpleLists" );
        findRSLists.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        findRSLists.addParameter( new JParameter( new JClass( "Element" ), "parent" ) );
        findRSLists.addParameter( new JParameter( new JClass( "java.util.Collection" ), "list" ) );
        findRSLists.addParameter( new JParameter( new JClass( "String" ), "parentName" ) );
        findRSLists.addParameter( new JParameter( new JClass( "String" ), "childName" ) );
        findRSLists.getModifiers().makeProtected();
        sc = findRSLists.getSourceCode();
        sc.add( "boolean shouldExist = list != null && list.size() > 0;" );
        sc.add( "Element element = updateElement(counter, parent, parentName, shouldExist);" );
        sc.add( "if (shouldExist) {" );
        sc.indent();
        sc.add( "Iterator it = list.iterator();" );
        sc.add( "Iterator elIt = element.getChildren(childName, element.getNamespace()).iterator();" );
        sc.add( "if (! elIt.hasNext()) elIt = null;" );
        sc.add( "Counter innerCount = new Counter(counter.getDepth() + 1);" );
        sc.add( "while (it.hasNext()) {" );
        sc.indent();
        sc.add( "String value = (String) it.next();" );
        sc.add( "Element el;" );
        sc.add( "if (elIt != null && elIt.hasNext()) {" );
        sc.indent();
        sc.add( "el = (Element) elIt.next();" );
        sc.add( "if (! elIt.hasNext()) elIt = null;" );
        sc.unindent();
        sc.add( "} else {" );
        sc.indent();
        sc.add( "el = factory.element(childName, element.getNamespace());" );
        sc.add( "insertAtPreferredLocation(element, el, innerCount);" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "el.setText(value);" );
        sc.add( "innerCount.increaseCount();" );

        sc.unindent();
        sc.add( "}" );
        sc.add( "if (elIt != null) {" );
        sc.indent();
        sc.add( "while (elIt.hasNext()) {" );
        sc.indent();
        sc.add( "elIt.next();" );
        sc.add( "elIt.remove();" );
        sc.unindent();
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "return element;" );

        JMethod findRSDom = new JMethod( new JClass( "Element" ), "findAndReplaceXpp3DOM" );
        findRSDom.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        findRSDom.addParameter( new JParameter( new JClass( "Element" ), "parent" ) );
        findRSDom.addParameter( new JParameter( new JClass( "String" ), "name" ) );
        findRSDom.addParameter( new JParameter( new JClass( "Xpp3Dom" ), "dom" ) );
        findRSDom.getModifiers().makeProtected();
        sc = findRSDom.getSourceCode();
        sc.add( "boolean shouldExist = dom != null && (dom.getChildCount() > 0 || dom.getValue() != null);" );
        sc.add( "Element element = updateElement(counter, parent, name, shouldExist);" );
        sc.add( "if (shouldExist) {" );
        sc.addIndented( "replaceXpp3DOM(element, dom, new Counter(counter.getDepth() + 1));" );
        sc.add( "}" );
        sc.add( "return element;" );

        JMethod findRSDom2 = new JMethod( "replaceXpp3DOM" );
        findRSDom2.addParameter( new JParameter( new JClass( "Element" ), "parent" ) );
        findRSDom2.addParameter( new JParameter( new JClass( "Xpp3Dom" ), "parentDom" ) );
        findRSDom2.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        findRSDom2.getModifiers().makeProtected();
        sc = findRSDom2.getSourceCode();
        sc.add( "if (parentDom.getChildCount() > 0) {" );
        sc.indent();
        sc.add( "Xpp3Dom[] childs = parentDom.getChildren();" );
        sc.add( "Collection domChilds = new ArrayList();" );
        sc.add( "for (int i = 0; i < childs.length; i++) {" );
        sc.addIndented( "domChilds.add(childs[i]);" );
        sc.add( "}" );
        sc.add( "ListIterator it = parent.getChildren().listIterator();" );
        sc.add( "while (it.hasNext()) {" );
        sc.indent();
        sc.add( "Element elem = (Element) it.next();" );
        sc.add( "Iterator it2 = domChilds.iterator();" );
        sc.add( "Xpp3Dom corrDom = null;" );
        sc.add( "while (it2.hasNext()) {" );
        sc.indent();
        sc.add( "Xpp3Dom dm = (Xpp3Dom)it2.next();" );
        sc.add( "if (dm.getName().equals(elem.getName())) {" );
        sc.indent();
        sc.add( "corrDom = dm;" );
        sc.add( "break;" );
        sc.unindent();
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "if (corrDom != null) {" );
        sc.indent();
        sc.add( "domChilds.remove(corrDom);" );
        sc.add( "replaceXpp3DOM(elem, corrDom, new Counter(counter.getDepth() + 1));" );
        sc.add( "counter.increaseCount();" );
        sc.unindent();
        sc.add( "} else {" );
        sc.addIndented( "it.remove();" );
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "Iterator it2 = domChilds.iterator();" );
        sc.add( "while (it2.hasNext()) {" );
        sc.indent();
        sc.add( "Xpp3Dom dm = (Xpp3Dom) it2.next();" );
        sc.add( "Element elem = factory.element(dm.getName(), parent.getNamespace());" );
        sc.add( "insertAtPreferredLocation(parent, elem, counter);" );
        sc.add( "counter.increaseCount();" );
        sc.add( "replaceXpp3DOM(elem, dm, new Counter(counter.getDepth() + 1));" );
        sc.unindent();
        sc.add( "}" );
        sc.unindent();
        sc.add( "} else if (parentDom.getValue() != null) {" );
        sc.addIndented( "parent.setText(parentDom.getValue());" );
        sc.add( "}" );

        return new JMethod[]{findRSElement, updateElement, insAtPref, findRSProps, findRSLists, findRSDom, findRSDom2};
    }

    private void writeAllClasses( Model objectModel, JClass jClass, ModelClass rootClass )
    {
        ArrayList alwaysExistingElements = new ArrayList();
        alwaysExistingElements.add( rootClass );
        for ( Iterator i = objectModel.getClasses( getGeneratedVersion() ).iterator(); i.hasNext(); )
        {
            ModelClass clazz = (ModelClass) i.next();
            updateClass( clazz, jClass, alwaysExistingElements );
        }
    }

    private void updateClass( ModelClass clazz, JClass jClass, ArrayList alwaysExisting )
    {
        String className = clazz.getName();
        String uncapClassName = uncapitalise( className );
        String clazzTagName = null;
        if ( clazz.hasMetadata( XmlFieldMetadata.ID ) )
        {
            XmlFieldMetadata clazzMetadata = (XmlFieldMetadata) clazz.getMetadata( XmlFieldMetadata.ID );
            clazzTagName = clazzMetadata.getTagName();
        }
        JMethod marshall = new JMethod( null, "update" + className );
        marshall.addParameter( new JParameter( new JClass( className ), "value" ) );
        marshall.addParameter( new JParameter( new JClass( "String" ), "xmlTag" ) );
        marshall.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        marshall.addParameter( new JParameter( new JClass( "Element" ), "element" ) );
        marshall.getModifiers().makeProtected();
        if ( clazzTagName == null )
        {
            clazzTagName = uncapClassName;
        }
        JSourceCode sc = marshall.getSourceCode();
        if ( alwaysExisting.contains( clazz ) )
        {
            sc.add( "Element root = element;" );
        }
        else
        {
            sc.add( "boolean shouldExist = value != null;" );
            sc.add( "Element root = updateElement(counter, element, xmlTag, shouldExist);" );
            sc.add( "if (shouldExist) {" );
            sc.indent();
        }
        sc.add( "Counter innerCount = new Counter(counter.getDepth() + 1);" );

        for ( Iterator i = clazz.getAllFields( getGeneratedVersion(), true ).iterator(); i.hasNext(); )
        {
            ModelField field = (ModelField) i.next();
            XmlFieldMetadata fieldMetadata = (XmlFieldMetadata) field.getMetadata( XmlFieldMetadata.ID );
            JavaFieldMetadata javaFieldMetadata = (JavaFieldMetadata) field.getMetadata( JavaFieldMetadata.ID );
            String fieldTagName = fieldMetadata.getTagName();
            if ( fieldTagName == null )
            {
                fieldTagName = field.getName();
            }
            String singularTagName = fieldMetadata.getAssociationTagName();
            if ( singularTagName == null )
            {
                singularTagName = singular( fieldTagName );
            }
            boolean wrappedList = XmlFieldMetadata.LIST_STYLE_WRAPPED.equals( fieldMetadata.getListStyle() );
            String type = field.getType();
            String value = "value." + getPrefix( javaFieldMetadata ) + capitalise( field.getName() ) + "()";
            if ( fieldMetadata.isAttribute() )
            {
                continue;
            }
            if ( field instanceof ModelAssociation )
            {
                ModelAssociation association = (ModelAssociation) field;
                String associationName = association.getName();
                ModelClass toClass = association.getToClass();
                if ( ModelAssociation.ONE_MULTIPLICITY.equals( association.getMultiplicity() ) )
                {
                    sc.add( "update" + capitalise( field.getType() ) + "( " + value + ", \"" + fieldTagName
                        + "\", innerCount, root);" );
                }
                else
                {
                    //MANY_MULTIPLICITY
//                    
//                    type = association.getType();
//                    String toType = association.getTo();
//                    
                    if ( ModelDefault.LIST.equals( type ) || ModelDefault.SET.equals( type ) )
                    {
//                        type = association.getType();
                        String toType = association.getTo();
                        if ( toClass != null )
                        {
                            if (wrappedList)
                            {
                                sc.add( "iterate" + capitalise( toType ) + "(innerCount, root, " + value + ",\""
                                    + field.getName() + "\",\"" + singular( fieldTagName ) + "\");" );
                                createIterateMethod( field.getName(), toClass, singular( fieldTagName ), jClass );
                            }
                            else
                            {
                                //assume flat..
                                sc.add( "iterate2" + capitalise( toType ) + "(innerCount, root, " + value + ",\"" + singular( fieldTagName ) + "\");" );
                                createIterateMethod2( field.getName(), toClass, singular( fieldTagName ), jClass );
                            }
                            alwaysExisting.add( toClass );
                        }
                        else
                        {
                            //list of strings?
                            sc.add( "findAndReplaceSimpleLists(innerCount, root, " + value + ", \"" + fieldTagName
                                + "\", \"" + singular( fieldTagName ) + "\");" );
                        }
                    }
                    else
                    {
                        //Map or Properties
                        sc.add(
                            "findAndReplaceProperties(innerCount, root,  \"" + fieldTagName + "\", " + value + ");" );
                    }
                }
            }
            else
            {
                if ( "DOM".equals( field.getType() ) )
                {
                    sc.add(
                        "findAndReplaceXpp3DOM(innerCount, root, \"" + fieldTagName + "\", (Xpp3Dom)" + value + ");" );
                }
                else
                {
                    sc.add( "findAndReplaceSimpleElement(innerCount, root,  \"" + fieldTagName + "\", "
                        + getValueChecker( type, value, field ) + getValue( type, value ) + ", "
                        + ( field.getDefaultValue() != null ? ( "\"" + field.getDefaultValue() + "\"" ) : "null" )
                        + ");" );
                }
            }
        }
        if ( !alwaysExisting.contains( clazz ) )
        {
            sc.unindent();
            sc.add( "}" );
        }
        jClass.addMethod( marshall );
    }

    private String getValue( String type, String initialValue )
    {
        String textValue = initialValue;

        if ( !"String".equals( type ) )
        {
            textValue = "String.valueOf( " + textValue + " )";
        }
        return textValue;
    }

    private String getValueChecker( String type, String value, ModelField field )
    {
        if ( "boolean".equals( type ) || "double".equals( type ) || "float".equals( type ) || "int".equals( type )
            || "long".equals( type ) || "short".equals( type ) )
        {
            return "" + value + " == " + field.getDefaultValue() + " ? null : ";
        }
        else if ( "char".equals( type ) )
        {
            return "" + value + " == '" + field.getDefaultValue() + "' ? null : ";
        }
        else if ( ModelDefault.LIST.equals( type ) || ModelDefault.SET.equals( type )
            || ModelDefault.MAP.equals( type ) || ModelDefault.PROPERTIES.equals( type ) )
        {
            return "" + value + " == null || " + value + ".size() == 0 ? null : ";
//        } else if ( "String".equals( type ) && field.getDefaultValue() != null ) {
//            return "" + value + " == null || " + value + ".equals( \"" + field.getDefaultValue() + "\" ) ? null : ";
        }
        else
        {
            return "";
        }
    }

    private void createIterateMethod( String field, ModelClass toClass, String childFieldTagName, JClass jClass )
    {
        if ( jClass.getMethod( "iterate" + capitalise( toClass.getName() ), 0 ) != null )
        {
//            System.out.println("method iterate" + capitalise(field) + " already exists");
            return;
        }
        JMethod toReturn = new JMethod( null, "iterate" + capitalise( toClass.getName() ) );
        toReturn.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        toReturn.addParameter( new JParameter( new JClass( "Element" ), "parent" ) );
        toReturn.addParameter( new JParameter( new JClass( "java.util.Collection" ), "list" ) );
        toReturn.addParameter( new JParameter( new JClass( "java.lang.String" ), "parentTag" ) );
        toReturn.addParameter( new JParameter( new JClass( "java.lang.String" ), "childTag" ) );
        toReturn.getModifiers().makeProtected();
        JSourceCode sc = toReturn.getSourceCode();
        sc.add( "boolean shouldExist = list != null && list.size() > 0;" );
        sc.add( "Element element = updateElement(counter, parent, parentTag, shouldExist);" );
        sc.add( "if (shouldExist) {" );
        sc.indent();
        sc.add( "Iterator it = list.iterator();" );
        sc.add( "Iterator elIt = element.getChildren(childTag, element.getNamespace()).iterator();" );
        sc.add( "if (!elIt.hasNext()) elIt = null;" );
        sc.add( "Counter innerCount = new Counter(counter.getDepth() + 1);" );
        sc.add( "while (it.hasNext()) {" );
        sc.indent();
        sc.add( toClass.getName() + " value = (" + toClass.getName() + ") it.next();" );
        sc.add( "Element el;" );
        sc.add( "if (elIt != null && elIt.hasNext()) {" );
        sc.indent();
        sc.add( "el = (Element) elIt.next();" );
        sc.add( "if (! elIt.hasNext()) elIt = null;" );
        sc.unindent();
        sc.add( "} else {" );
        sc.indent();
        sc.add( "el = factory.element(childTag, element.getNamespace());" );
        sc.add( "insertAtPreferredLocation(element, el, innerCount);" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "update" + toClass.getName() + "(value, childTag, innerCount, el);" );
        sc.add( "innerCount.increaseCount();" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "if (elIt != null) {" );
        sc.indent();
        sc.add( "while (elIt.hasNext()) {" );
        sc.indent();
        sc.add( "elIt.next();" );
        sc.add( "elIt.remove();" );
        sc.unindent();
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );

        jClass.addMethod( toReturn );
    }
    
    private void createIterateMethod2( String field, ModelClass toClass, String childFieldTagName, JClass jClass )
    {
        if ( jClass.getMethod( "iterate2" + capitalise( toClass.getName() ), 0 ) != null )
        {
//            System.out.println("method iterate" + capitalise(field) + " already exists");
            return;
        }
        JMethod toReturn = new JMethod( null, "iterate2" + capitalise( toClass.getName() ) );
        toReturn.addParameter( new JParameter( new JClass( "Counter" ), "counter" ) );
        toReturn.addParameter( new JParameter( new JClass( "Element" ), "parent" ) );
        toReturn.addParameter( new JParameter( new JClass( "java.util.Collection" ), "list" ) );
        toReturn.addParameter( new JParameter( new JClass( "java.lang.String" ), "childTag" ) );
        toReturn.getModifiers().makeProtected();
        JSourceCode sc = toReturn.getSourceCode();
        sc.add( "Iterator it = list.iterator();" );
        sc.add( "Iterator elIt = parent.getChildren(childTag, parent.getNamespace()).iterator();" );
        sc.add( "if (!elIt.hasNext()) elIt = null;" );
        sc.add( "Counter innerCount = new Counter(counter.getDepth() + 1);" );
        sc.add( "while (it.hasNext()) {" );
        sc.indent();
        sc.add( toClass.getName() + " value = (" + toClass.getName() + ") it.next();" );
        sc.add( "Element el;" );
        sc.add( "if (elIt != null && elIt.hasNext()) {" );
        sc.indent();
        sc.add( "el = (Element) elIt.next();" );
        sc.add( "if (! elIt.hasNext()) elIt = null;" );
        sc.unindent();
        sc.add( "} else {" );
        sc.indent();
        sc.add( "el = factory.element(childTag, parent.getNamespace());" );
        sc.add( "insertAtPreferredLocation(parent, el, innerCount);" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "update" + toClass.getName() + "(value, childTag, innerCount, el);" );
        sc.add( "innerCount.increaseCount();" );
        sc.unindent();
        sc.add( "}" );
        sc.add( "if (elIt != null) {" );
        sc.indent();
        sc.add( "while (elIt.hasNext()) {" );
        sc.indent();
        sc.add( "elIt.next();" );
        sc.add( "elIt.remove();" );
        sc.unindent();
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );

        jClass.addMethod( toReturn );
    }
    

}
