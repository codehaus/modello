package org.codehaus.mojo.modello;

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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import org.codehaus.modello.core.ModelloCore;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class ModelloJPoxStoreMojoTest
    extends PlexusTestCase
{
    public void testModelloJPoxMojo()
        throws Exception
    {
        ModelloCore modelloCore = (ModelloCore) lookup( ModelloCore.ROLE );

        ModelloJPoxStoreMojo mojo = new ModelloJPoxStoreMojo();

        File outputDirectory = getTestFile( "target/jpox-store" );

        if ( outputDirectory.exists() )
        {
            FileUtils.deleteDirectory( outputDirectory );
        }

        assertTrue( outputDirectory.mkdirs() );

        // ----------------------------------------------------------------------
        // Call the mojo
        // ----------------------------------------------------------------------

        Map parameters = new HashMap();

        parameters.put( "outputDirectory", outputDirectory.getAbsolutePath() );

        parameters.put( "model", getTestPath( "src/test/resources/jpox-model.mdo" ) );

        parameters.put( "version", "1.0.0" );

        parameters.put( "packageWithVersion", Boolean.FALSE.toString() );

        parameters.put( "modelloCore", modelloCore );

        PluginExecutionRequest request = new PluginExecutionRequest( parameters );

        PluginExecutionResponse response = new PluginExecutionResponse();

        mojo.execute( request, response );

        // ----------------------------------------------------------------------
        // Assert
        // ----------------------------------------------------------------------

        File store = new File( outputDirectory, "org/codehaus/mojo/modello/test/jpox/JPoxTestJPoxStore.java" );

        assertTrue( "Could not read the jpox store.", store.canRead() );
    }
}
