package org.codehaus.modello.generator.xml.stax;

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

import java.util.Properties;

/**
 * @author Hervé Boutemy
 * @version $Id$
 */
public class FeaturesStaxGeneratorTest
    extends AbstractModelloJavaGeneratorTest
{
    public FeaturesStaxGeneratorTest()
    {
        super( "features" );
    }

    public void testJavaGenerator()
        throws Throwable
    {
        ModelloCore modello = (ModelloCore) lookup( ModelloCore.ROLE );

        Properties parameters = new Properties();
        parameters.setProperty( ModelloParameterConstants.OUTPUT_DIRECTORY, getOutputDirectory().getAbsolutePath() );
        parameters.setProperty( ModelloParameterConstants.PACKAGE_WITH_VERSION, Boolean.toString( false ) );
        parameters.setProperty( ModelloParameterConstants.VERSION, "1.0.0" );

        Model model = modello.loadModel( getModelResource( "/features.mdo" ) );

        modello.generate( model, "java", parameters );
        modello.generate( model, "stax-writer", parameters );
        modello.generate( model, "stax-reader", parameters );

        addDependency( "stax", "stax-api", "1.0.1" );
        addDependency( "org.codehaus.woodstox", "wstx-asl", "3.2.0" );
        addDependency( "net.java.dev.stax-utils", "stax-utils", "20060502" );
        addDependency( "xmlunit", "xmlunit", "1.2" );

        compile( getOutputDirectory(), getOutputClasses() );

        // TODO: see why without this, version system property is set to "2.4.1" value after verify
        System.setProperty( "version", getModelloVersion() );

        verify( "org.codehaus.modello.generator.xml.stax.StaxFeaturesVerifier", getName() );
    }
}
