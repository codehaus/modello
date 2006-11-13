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

package org.codehaus.modello.maven;

import org.codehaus.modello.maven.AbstractModelloGeneratorMojo;

import java.io.File;


/**
 * Creates a jdom writer from the model that is capable of preserving element ordering
 * and comments. In future it should also preserve whitespace.
 *
 * @goal jdom-writer
 *
 * @phase generate-sources
 *
 * @author <a href="mailto:mkleint@codehaus.org">Milos Kleint</a>
 *
 */
public class ModelloJDOMWriterMojo
    extends AbstractModelloGeneratorMojo
{
    /**
     * The output directory of the generated jdom writer.
     *
     * @parameter expression="${basedir}/target/generated-sources/modello"
     *
     * @required
     */
    private File outputDirectory;

    protected String getGeneratorType()
    {
        return "jdom-writer";
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }
}
