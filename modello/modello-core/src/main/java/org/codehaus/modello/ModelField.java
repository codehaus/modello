package org.codehaus.modello;


/**
 *
 *
 * @author <a href="mailto:jason@modello.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class ModelField
    extends BaseElement
{
    String type;

    String specification;

    String defaultValue;

    String typeValidator;

    boolean required;

    public String getType()
    {
        return type;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public String getSpecification()
    {
        return specification;
    }

    public String getTypeValidator()
    {
        return typeValidator;
    }

    public boolean isRequired()
    {
        return required;
    }
}