package org.codehaus.modello.plugins.xml;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.plugin.AbstractModelloGenerator;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:hboutemy@codehaus.org">Herv&eacute; Boutemy</a>
 * @version $Id$
 */
public abstract class AbstractXmlGenerator
    extends AbstractModelloGenerator
{
    /**
     * Compute the tagName of a given field. <br>
     * This method return the first child tag name created by this field.
     * This means that for a association with multiplicity * and listStyle to
     * wrapped (which is the default), this method will return the plural tagName,
     * while for a listStyle of flat, it will return the singular tagName.
     * @param field the field we are looking for the tag name.
     * @return the tag name to use
     */
    protected String resolveFieldTagName( ModelField field )
    {
        XmlFieldMetadata metadata = (XmlFieldMetadata) field.getMetadata( XmlFieldMetadata.ID );

        String tagName = uncapitalise( field.getName() );

        if ( metadata != null && StringUtils.isNotEmpty( metadata.getTagName() ) )
        {
            tagName = metadata.getTagName();
        }

        return tagName;
    }

    /**
     * Return the child attribute fields of this class.
     * @param modelClass current class
     * @return the list of attribute fields of this class
     */
    protected List getAttributeFieldsForClass( ModelClass modelClass )
    {
        List attributeFields = new ArrayList();

        while ( modelClass != null )
        {
            List allFields = modelClass.getFields( getGeneratedVersion() );

            for (Iterator allFieldsIt = allFields.iterator(); allFieldsIt.hasNext(); )
            {
                ModelField field = (ModelField) allFieldsIt.next();
                XmlFieldMetadata fieldMetadata = (XmlFieldMetadata) field.getMetadata( XmlFieldMetadata.ID );
                if ( fieldMetadata.isAttribute() )
                {
                    attributeFields.add( field );
                }
            }

            String superClass = modelClass.getSuperClass();
            if ( superClass != null )
            {
                modelClass = getModel().getClass( superClass, getGeneratedVersion() );
            }
            else
            {
                modelClass = null;
            }
        }

        return attributeFields;
    }
}