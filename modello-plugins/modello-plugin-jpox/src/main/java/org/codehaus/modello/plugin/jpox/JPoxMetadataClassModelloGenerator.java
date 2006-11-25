package org.codehaus.modello.plugin.jpox;

/*
 * Copyright (c) 2006, Codehaus.org
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

import org.apache.velocity.context.Context;
import org.codehaus.modello.ModelloException;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.plugin.store.AbstractVelocityModelloGenerator;
import org.codehaus.modello.plugin.store.metadata.StoreClassMetadata;
import org.codehaus.modello.plugin.store.metadata.StoreFieldMetadata;

import java.util.Properties;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id: JPoxStoreModelloGenerator.java 699 2006-11-23 03:37:55Z brett $
 */
public class JPoxMetadataClassModelloGenerator
    extends AbstractVelocityModelloGenerator
{
    public void generate( Model model, Properties parameters )
        throws ModelloException
    {
        initialize( model, parameters );

        // Initialize the Velocity context

        Context context = makeStubVelocityContext( model, getGeneratedVersion() );

        context.put( "storeClassMetadataId", StoreClassMetadata.ID );

        context.put( "storeFieldMetadataId", StoreFieldMetadata.ID );

        // Generate a ModelloMetadata class for storing model information in the database
        String packageName = model.getDefaultPackageName( isPackageWithVersion(), getGeneratedVersion() );

        String className = model.getName() + "ModelloMetadata";
        writeClass( "/org/codehaus/modello/plugin/jpox/templates/ModelloMetadata.java.vm", getOutputDirectory(),
                    packageName, className, context );
    }
}
