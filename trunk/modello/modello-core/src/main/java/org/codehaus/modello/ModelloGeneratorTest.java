package org.codehaus.modello;

/*
 * LICENSE
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.javac.IsolatedClassLoader;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public abstract class ModelloGeneratorTest
    extends ModelloTest
{
    private List dependencies = new ArrayList();

    private String name;

    private File mavenRepoLocal;

    private IsolatedClassLoader classLoader = new IsolatedClassLoader();

    protected ModelloGeneratorTest( String name )
    {
        this.name = name;
    }

    public final void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( getTestPath( "target/" + getName() ) );

        mavenRepoLocal = new File( System.getProperty( "user.home" ), ".maven/repository" );

        addDependency( "junit", "junit", "3.8.1" );

        addDependency( "plexus", "plexus", "0.16" );

        addDependency( "classworlds", "classworlds", "1.1-SNAPSHOT" );

        addDependency( "xstream", "xstream", "SNAPSHOT" );

        addDependency( "xpp3", "xpp3", "1.1.3.3" );
    }

    public void addDependency( String groupId, String artifactId, String version )
        throws Exception
    {
        File dependency = new File( mavenRepoLocal, groupId + "/jars/" + artifactId + "-" + version + ".jar" );

        assertTrue( "Cant find dependency: " + dependency, dependency.isFile() );

        dependencies.add( dependency );

        addClassPathFile( dependency );
    }

    public String getName()
    {
        return name;
    }

    public List getClasspath()
    {
        return dependencies;
    }

    protected void compile( File generatedSources, File destinationDirectory )
        throws Exception
    {
        String[] classPathElements = new String[dependencies.size() + 2];

        classPathElements[0] = getTestPath( "target/classes" );

        classPathElements[1] = getTestPath( "target/test-classes" );

        for ( int i = 0; i < dependencies.size(); i++ )
        {
            classPathElements[i + 2] = ((File) dependencies.get( i )).getAbsolutePath();
        }

        String[] sourceDirectories = new String[]{
            getTestPath( "src/test/verifiers/" + getName() ),
            generatedSources.getAbsolutePath()
        };

        Compiler compiler = new JavacCompiler();

        List messages = compiler.compile( classPathElements, sourceDirectories, destinationDirectory.getAbsolutePath() );

        if ( false )
        {
            URL[] urls = classLoader.getURLs();

            for ( int i = 0; i < urls.length; i++ )
            {
                URL url = urls[i];

                System.out.println( url );
            }
        }

        for ( Iterator it = messages.iterator(); it.hasNext(); )
        {
            CompilerError message = (CompilerError) it.next();

            System.out.println( message.getFile() + "[" + message.getStartLine() + "," + message.getStartColumn() + "]: " + message.getMessage() );
        }

        assertEquals( "There was compilation errors.", 0, messages.size() );
    }

    protected void verify( String className, String testName )
        throws Throwable
    {
        // TODO: flip back to getTestFile() when plexus has File getTestFile()
        addClassPathFile( new File( getTestPath( "target/" + getName() + "/classes" ) ) );

        addClassPathFile( new File( getTestPath( "target/classes" ) ) );

        addClassPathFile( new File( getTestPath( "target/test-classes" ) ) );

        Class clazz = classLoader.loadClass( className );

        Method verify = clazz.getMethod( "verify", new Class[0] );

        try
        {
            verify.invoke( clazz.newInstance(), new Object[0] );
        }
        catch( InvocationTargetException ex )
        {
            throw ex.getCause();
        }
    }

    protected ClassLoader getTestClassLoader()
        throws Exception
    {
        return classLoader;
    }

    protected void addClassPathFile( File file )
        throws Exception
    {
        assertTrue( "File doesn't exists: " + file.getAbsolutePath(), file.exists() );

        classLoader.addURL( file.toURL() );
    }
}
