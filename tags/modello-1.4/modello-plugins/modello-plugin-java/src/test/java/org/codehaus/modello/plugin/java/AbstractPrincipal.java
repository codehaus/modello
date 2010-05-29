package org.codehaus.modello.plugin.java;

/**
 * Test Abstract Class used by {@link JavaGeneratorTest#testBiDirectionalJavaGenerator()} to ensure that
 * externally referenced classes can be used in the &lt;superClass&gt; element.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractPrincipal
{
    private int principal;

    public int getPrincipal()
    {
        return principal;
    }

    public void setPrincipal( int principal )
    {
        this.principal = principal;
    }
}
