package org.codehaus.modello.model;

/*
 * Copyright (c) 2004, Jason van Zyl
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

import org.codehaus.modello.ModelloRuntimeException;

/**
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class VersionRange
{
    private static final String VERSION_SEPARATOR = "/";
    private Version fromVersion;

    private Version toVersion;

    private String versionRange;

    public VersionRange( String versionRange )
    {
        if ( versionRange.endsWith( "+" ) )
        {
            fromVersion = new Version( versionRange.substring( 0, versionRange.length() - 1 ) );
            toVersion = Version.INFINITE;
        }
        else if ( versionRange.indexOf( VERSION_SEPARATOR ) > 0 && ! versionRange.endsWith( VERSION_SEPARATOR ) )
        {
            int pos = versionRange.indexOf( VERSION_SEPARATOR );

            fromVersion = new Version( versionRange.substring( 0, pos ) );

            toVersion = new Version( versionRange.substring( pos + 1 ) );
        }
        else
        {
            fromVersion = new Version( versionRange );
            toVersion = new Version( versionRange );
        }

        this.versionRange = versionRange;
    }

    public Version getFromVersion()
    {
        return fromVersion;
    }

    public Version getToVersion()
    {
        return toVersion;
    }

    public boolean isToInfinite()
    {
        return toVersion == Version.INFINITE;
    }

    public String toString()
    {
        return versionRange;
    }
}
