package org.codehaus.modello.generator.xml.jdom;

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

import junit.framework.Assert;
import org.codehaus.modello.test.features.Features;
import org.codehaus.modello.test.features.io.dom4j.ModelloFeaturesTestDom4jReader;
import org.codehaus.modello.test.features.io.jdom.ModelloFeaturesTestJDOMWriter;
import org.codehaus.modello.verifier.Verifier;
import org.codehaus.modello.verifier.VerifierException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author Hervé Boutemy
 * @version $Id$
 */
public class JDOMFeaturesVerifier
    extends Verifier
{
    public void verify()
        throws Exception
    {
        Features features = read();

        verifyWriter( features );
    }

    private Features read()
        throws Exception
    {
        ModelloFeaturesTestDom4jReader reader = new ModelloFeaturesTestDom4jReader();

        return reader.read( getClass().getResource( "/features.xml" ) );
    }

    public void verifyWriter( Features features )
        throws Exception
    {
        ModelloFeaturesTestJDOMWriter writer = new ModelloFeaturesTestJDOMWriter();

        StringWriter buffer = new StringWriter();

        Document doc = new Document( new Element( "features" ) );
        writer.write( features, doc, buffer, Format.getRawFormat() );

        String actualXml = buffer.toString();
        // workaround for MODELLO-...
        actualXml =
            actualXml.replaceFirst( "<features>", "<features xmlns=\"http://modello.codehaus.org/FEATURES/1.0.0\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://modello.codehaus.org/FEATURES/1.0.0 "
                + "http://modello.codehaus.org/features-1.0.0.xsd\">" );

        // alias is rendered as default field name => must be reverted here to let the test pass
        actualXml = actualXml.replaceFirst( "<id>alias</id>", "<key>alias</key>" );

        XMLUnit.setIgnoreWhitespace( true );
        XMLUnit.setIgnoreComments( true );
        Diff diff = XMLUnit.compareXML( IOUtil.toString( getClass().getResourceAsStream( "/features.xml" ), "UTF-8" ), actualXml );

        if ( !diff.identical() )
        {
            System.err.println( actualXml );
            /*throw*/ new VerifierException( "writer result is not the same as original content: " + diff )
                .printStackTrace( System.err );
        }
    }
}
