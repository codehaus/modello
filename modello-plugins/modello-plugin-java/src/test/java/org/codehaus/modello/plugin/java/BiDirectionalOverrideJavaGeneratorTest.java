package org.codehaus.modello.plugin.java;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.modello.AbstractModelloJavaGeneratorTest;
import org.codehaus.modello.ModelloParameterConstants;
import org.codehaus.modello.core.ModelloCore;
import org.codehaus.modello.model.Model;

import java.util.Properties;

/**
 * BiDirectionalOverrideJavaGeneratorTest
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class BiDirectionalOverrideJavaGeneratorTest
    extends AbstractModelloJavaGeneratorTest
{
    private String modelFile = "src/test/resources/models/bidirectional-override.mdo";

    public BiDirectionalOverrideJavaGeneratorTest()
    {
        super( "bidirectional" );
    }

    public void testJavaGenerator()
        throws Throwable
    {
        ModelloCore modello = (ModelloCore) lookup( ModelloCore.ROLE );

        Properties parameters = new Properties();
        parameters.setProperty( ModelloParameterConstants.OUTPUT_DIRECTORY, getOutputDirectory().getAbsolutePath() );
        parameters.setProperty( ModelloParameterConstants.PACKAGE_WITH_VERSION, Boolean.toString( false ) );
        parameters.setProperty( ModelloParameterConstants.VERSION, "1.0.0" );

        Model model = modello.loadModel( getTestFile( modelFile ) );

        modello.generate( model, "java", parameters );

        compile( getOutputDirectory(), getOutputClasses() );

        verify( "JavaVerifier", getName() );
    }
}
