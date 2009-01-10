package org.codehaus.modello.generator.xml.xpp3;

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

import org.codehaus.modello.AbstractModelloJavaGeneratorTest;
import org.codehaus.modello.ModelloParameterConstants;
import org.codehaus.modello.core.ModelloCore;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.model.Version;
import org.codehaus.modello.plugins.xml.metadata.XmlFieldMetadata;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class Xpp3GeneratorTest
    extends AbstractModelloJavaGeneratorTest
{
    public Xpp3GeneratorTest()
    {
        super( "xpp3" );
    }

    public void testXpp3Generator()
        throws Throwable
    {
        ModelloCore modello = (ModelloCore) lookup( ModelloCore.ROLE );

        Model model = modello.loadModel( ReaderFactory.newXmlReader( getTestFile( "src/test/resources/maven.mdo" ) ) );

        // check some elements read from the model
        List classesList = model.getClasses( new Version( "4.0.0" ) );

        assertEquals( 27, classesList.size() );

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

        // now generate sources and test them
        File generatedSources = new File( getTestPath( "target/xpp3/sources" ) );

        File classes = new File( getTestPath( "target/xpp3/classes" ) );

        FileUtils.deleteDirectory( generatedSources );

        FileUtils.deleteDirectory( classes );

        generatedSources.mkdirs();

        classes.mkdirs();

        Properties parameters = new Properties();
        parameters.setProperty( ModelloParameterConstants.OUTPUT_DIRECTORY, generatedSources.getAbsolutePath() );
        parameters.setProperty( ModelloParameterConstants.VERSION, "4.0.0" );
        parameters.setProperty( ModelloParameterConstants.PACKAGE_WITH_VERSION, Boolean.toString( false ) );

        modello.generate( model, "java", parameters );
        modello.generate( model, "xpp3-writer", parameters );
        modello.generate( model, "xpp3-reader", parameters );

        addDependency( "xmlunit", "xmlunit", "1.2" );
        compile( generatedSources, classes );

        // TODO: see why without this, version system property is set to "2.4.1" value after verify
        System.setProperty( "version", getModelloVersion() );

        verify( "org.codehaus.modello.generator.xml.xpp3.Xpp3Verifier", "xpp3" );
    }
}
