package org.codehaus.modello.plugins.xml;

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

import org.codehaus.modello.metadata.AssociationMetadata;
import org.codehaus.modello.model.ModelValidationException;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class XmlAssociationMetadata
    implements AssociationMetadata
{
    public final static String ID = XmlAssociationMetadata.class.getName();

    public static final String EXPLODE_MODE = "explode";

    public static final String INLINE_MODE = "inline";

    private String mapStyle = INLINE_MODE;

    /**
     * @return Returns the map style.
     */
    public String getMapStyle()
    {
        return mapStyle;
    }

    /**
     * @param mapStyle The map style (inline or explode).
     */
    public void setMapStyle( String mapStyle )
    {
        if ( mapStyle == null )
        {
            this.mapStyle = INLINE_MODE;
        }
        else
        {
            this.mapStyle = mapStyle;
        }
    }
}
