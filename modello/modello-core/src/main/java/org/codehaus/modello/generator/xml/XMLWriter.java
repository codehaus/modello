package org.codehaus.modello.generator.xml;

public interface XMLWriter
{
    void startElement( String name );

    void addAttribute( String key, String value );

    void writeText( String text );

    void endElement();
    
    void addCData( String cdata );
}
