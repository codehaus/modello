package org.codehaus.modello.plugin.xdoc;

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

import org.codehaus.modello.AbstractModelloTest;
import org.codehaus.modello.ModelloParameterConstants;
import org.codehaus.modello.core.ModelloCore;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.model.Version;
import org.codehaus.modello.plugins.xml.metadata.XmlFieldMetadata;
import org.codehaus.modello.verifier.VerifierException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class XdocGeneratorTest
    extends AbstractModelloTest

{
    public XdocGeneratorTest()
    {
        super( "xdoc" );
    }

    public void testMavenXdocGenerator()
        throws Throwable
    {
        ModelloCore modello = (ModelloCore) container.lookup( ModelloCore.ROLE );

        Model model = modello.loadModel( ReaderFactory.newXmlReader( getTestFile( "src/test/resources/maven.mdo" ) ) );

        List classesList = model.getClasses( new Version( "4.0.0" ) );

        assertEquals( 26, classesList.size() );

        ModelClass clazz = (ModelClass) classesList.get( 0 );

        assertEquals( "Model", clazz.getName() );

        ModelField extend = clazz.getField( "extend", new Version( "4.0.0" ) );

        assertTrue( extend.hasMetadata( XmlFieldMetadata.ID ) );

        XmlFieldMetadata xml = (XmlFieldMetadata) extend.getMetadata( XmlFieldMetadata.ID );

        assertNotNull( xml );

        assertTrue( xml.isAttribute() );

        assertEquals( "extender", xml.getTagName() );

        ModelField build = clazz.getField( "build", new Version( "4.0.0" ) );

        assertTrue( build.hasMetadata( XmlFieldMetadata.ID ) );

        xml = (XmlFieldMetadata) build.getMetadata( XmlFieldMetadata.ID );

        assertNotNull( xml );

        assertEquals( "builder", xml.getTagName() );

        File generatedSources = getTestFile( "target/generated-site/xdoc" );

        FileUtils.deleteDirectory( generatedSources );

        generatedSources.mkdirs();

        Properties parameters = new Properties();
        parameters.setProperty( ModelloParameterConstants.OUTPUT_DIRECTORY, generatedSources.getAbsolutePath() );
        parameters.setProperty( ModelloParameterConstants.VERSION, "4.0.0" );
        parameters.setProperty( ModelloParameterConstants.PACKAGE_WITH_VERSION, Boolean.toString( false ) );

        modello.generate( model, "xdoc", parameters );

        //addDependency( "modello", "modello-core", "1.0-SNAPSHOT" );

        //verify( "org.codehaus.modello.generator.xml.cdoc.XdocVerifier", "xdoc" );
        checkInternalLinks( new File( generatedSources, "maven.xml" ) );
    }

    public void testFeaturesXdocGenerator()
        throws Throwable
    {
        File generatedSources = getTestFile( "target/generated-site/xdoc" );

        ModelloCore modello = (ModelloCore) lookup( ModelloCore.ROLE );

        Properties parameters = new Properties();
        parameters.setProperty( ModelloParameterConstants.OUTPUT_DIRECTORY, generatedSources.getAbsolutePath() );
        parameters.setProperty( ModelloParameterConstants.PACKAGE_WITH_VERSION, Boolean.toString( false ) );
        parameters.setProperty( ModelloParameterConstants.VERSION, "1.0.0" );

        Model model = modello.loadModel( getModelResource( "/features.mdo" ) );

        modello.generate( model, "xdoc", parameters );

        checkInternalLinks( new File( generatedSources, "features.xml" ) );
    }

    /**
     * Checks internal links in the xdoc content: for every 'a href="#xxx"' link, a 'a name="xxx"' must exist (or there
     * is a problem in the generated content).
     *
     * @param xdoc
     * @throws Exception
     */
    private void checkInternalLinks( File xdoc )
        throws Exception
    {
        String content = FileUtils.fileRead( xdoc, "UTF-8" );

        Set hrefs = new HashSet();
        Pattern p = Pattern.compile( "<a href=\"#(class_[^\"]+)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE );
        Matcher m = p.matcher( content );
        while ( m.find() )
        {
            hrefs.add( m.group( 1 ) );
        }
        Assert.assertTrue( "should find some '<a href=' links", hrefs.size() > 0 );

        Set names = new HashSet();
        p = Pattern.compile( "<a name=\"(class_[^\"]+)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE );
        m = p.matcher( content );
        while ( m.find() )
        {
            names.add( m.group( 1 ) );
        }
        Assert.assertTrue( "should find some '<a name=' anchor definitions", names.size() > 0 );

        hrefs.removeAll( names );
        if ( hrefs.size() > 0 )
        {
            throw new VerifierException( "some internal hrefs in " + xdoc.getName() + " are not defined: " + hrefs );
        }
    }
}
