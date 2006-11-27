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

import junit.framework.Assert;
import org.codehaus.modello.test.model.parts.Model;
import org.codehaus.modello.test.model.parts.SingleReference;
import org.codehaus.modello.test.model.parts.Reference;
import org.codehaus.modello.test.model.parts.io.stax.PartsStaxReader;
import org.codehaus.modello.test.model.parts.io.stax.PartsStaxWriter;
import org.codehaus.modello.verifier.Verifier;
import org.codehaus.plexus.util.FileUtils;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import javax.xml.stream.XMLStreamException;

/**
 * @version $Id: Xpp3Verifier.java 675 2006-11-16 10:58:59Z brett $
 */
public class StaxVerifierParts
    extends Verifier
{
    public void verify()
        throws IOException, XMLStreamException
    {
        String path = "src/test/verifiers/stax-parts/parts.xml";

        FileReader reader = new FileReader( path );
        PartsStaxReader modelReader = new PartsStaxReader();

        Model model = modelReader.read( reader );

        Assert.assertNotNull( model.getSingleReference() );
        Assert.assertNotNull( model.getSingleReference().getReference() );
        Assert.assertEquals( "single", model.getSingleReference().getReference().getId() );
        Assert.assertEquals( "Single Reference", model.getSingleReference().getReference().getName() );
        Assert.assertEquals( "single", model.getSecondReference().getReference().getId() );
        Assert.assertEquals( "Single Reference", model.getSecondReference().getReference().getName() );
        Assert.assertEquals( "other", model.getThirdReference().getReference().getId() );
        Assert.assertEquals( "Other Reference", model.getThirdReference().getReference().getName() );
        Assert.assertEquals( "single", model.getDualReference().getFirst().getId() );
        Assert.assertEquals( "Single Reference", model.getDualReference().getFirst().getName() );
        Assert.assertEquals( "other", model.getDualReference().getSecond().getId() );
        Assert.assertEquals( "Other Reference", model.getDualReference().getSecond().getName() );
        Assert.assertEquals( "single", model.getDupeReference().getFirst().getId() );
        Assert.assertEquals( "Single Reference", model.getDupeReference().getFirst().getName() );
        Assert.assertEquals( "single", model.getDupeReference().getSecond().getId() );
        Assert.assertEquals( "Single Reference", model.getDupeReference().getSecond().getName() );
        Assert.assertEquals( "single", ((Reference)model.getReferenceList().getItems().get( 0 )).getId() );
        Assert.assertEquals( "Single Reference", ((Reference)model.getReferenceList().getItems().get( 0 )).getName() );
        Assert.assertEquals( "single", ((Reference)model.getReferenceList().getItems().get( 1 )).getId() );
        Assert.assertEquals( "Single Reference", ((Reference)model.getReferenceList().getItems().get( 1 )).getName() );
        Assert.assertEquals( "other", ((Reference)model.getReferenceList().getItems().get( 2 )).getId() );
        Assert.assertEquals( "Other Reference", ((Reference)model.getReferenceList().getItems().get( 2 )).getName() );
        Assert.assertEquals( "another", ((Reference)model.getReferenceList().getItems().get( 3 )).getId() );
        Assert.assertEquals( "Another Reference", ((Reference)model.getReferenceList().getItems().get( 3 )).getName() );
        Assert.assertEquals( "other", ((SingleReference)model.getSingleReferences().get( 0 )).getReference().getId() );
        Assert.assertEquals( "Other Reference", ((SingleReference)model.getSingleReferences().get( 0 )).getReference().getName() );
        Assert.assertEquals( "single", ((SingleReference)model.getSingleReferences().get( 1 )).getReference().getId() );
        Assert.assertEquals( "Single Reference", ((SingleReference)model.getSingleReferences().get( 1 )).getReference().getName() );
        Assert.assertEquals( "another", ((SingleReference)model.getSingleReferences().get( 2 )).getReference().getId() );
        Assert.assertEquals( "Another Reference", ((SingleReference)model.getSingleReferences().get( 2 )).getReference().getName() );
        Assert.assertEquals( "parent", model.getNestedReference().getId() );
        Assert.assertEquals( model.getNestedReference(), model.getNestedReference().getChildReference().getParentReference() );
        Assert.assertEquals( 3, model.getReferences().size() );

        String expected = FileUtils.fileRead( path );

        PartsStaxWriter modelWriter = new PartsStaxWriter();
        StringWriter w = new StringWriter();
        modelWriter.write( w, model );
        Assert.assertEquals( expected, w.toString() );
    }
}
