package org.codehaus.modello;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
//import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.javac.JavacCompiler;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public abstract class ModelloGeneratorTest
    extends PlexusTestCase
{
    private List dependencies = new ArrayList();

    private String name;

    private List urls = new ArrayList();

    private ArtifactRepository repository;

    protected ModelloGeneratorTest( String name )
    {
        this.name = name;
    }

    public final void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( getGeneratedSources() );

        assertTrue( getGeneratedSources().mkdirs() );

        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) container.lookup(
            ArtifactRepositoryLayout.ROLE, "default" );

        String localRepo = findLocalRepository();
        
        String url = "file://" + localRepo;

        repository = new ArtifactRepository( "local", url, repositoryLayout );
    }

    private String findLocalRepository() throws IOException, XmlPullParserException
    {
        String settingsPath = System.getProperty( "org.apache.maven.user-settings" );
        
        if ( StringUtils.isEmpty( settingsPath ) )
        {
            settingsPath = System.getProperty( "user.home" ) + "/.m2/settings.xml";
        }
        
        FileReader reader = null;
        try
        {
            reader = new FileReader( settingsPath );
            
            SettingsXpp3Reader settingsReader = new SettingsXpp3Reader();
            
            Settings settings = settingsReader.read( reader );
            
            return settings.getActiveProfile().getLocalRepository();
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    protected File getGeneratedSources()
    {
        return getTestFile( "target/" + getName() );
    }

    public void addDependency( String groupId, String artifactId, String version )
        throws Exception
    {
        DefaultArtifact artifact = new DefaultArtifact( groupId, artifactId, version, Artifact.SCOPE_COMPILE, "jar", null );

        File dependencyFile = new File( repository.getBasedir(), repository.pathOf( artifact ) );

        assertTrue( "Cant find dependency: " + dependencyFile.getAbsolutePath(), dependencyFile.isFile() );

        dependencies.add( dependencyFile );

        addClassPathFile( dependencyFile );
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
        addDependency( "junit", "junit", "3.8.1" );

        addDependency( "plexus", "plexus-utils", "1.0-alpha-3" );

        // TODO: can read my own POM to set this!
        addDependency( "org.codehaus.modello", "modello-test", "1.0-alpha-3-SNAPSHOT" );

        String[] classPathElements = new String[dependencies.size() + 2];

        classPathElements[0] = getTestPath( "target/classes" );

        classPathElements[1] = getTestPath( "target/test-classes" );

        for ( int i = 0; i < dependencies.size(); i++ )
        {
            classPathElements[i + 2] = ( (File) dependencies.get( i ) ).getAbsolutePath();
        }

        String[] sourceDirectories = new String[]{
            getTestPath( "src/test/verifiers/" + getName() ),
            generatedSources.getAbsolutePath()
        };

        Compiler compiler = new JavacCompiler();

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.setClasspathEntries( Arrays.asList( classPathElements ) );
        configuration.setSourceLocations( Arrays.asList( sourceDirectories ) );
        configuration.setOutputLocation( destinationDirectory.getAbsolutePath() );

        List messages = compiler.compile( configuration );

        for ( Iterator it = messages.iterator(); it.hasNext(); )
        {
            CompilerError message = (CompilerError) it.next();

            System.out.println( message.getFile() +
                                "[" + message.getStartLine() + "," + message.getStartColumn() + "]: " +
                                message.getMessage() );
        }

        assertEquals( "There was compilation errors.", 0, messages.size() );
    }

    protected void verify( String className, String testName )
        throws Throwable
    {
        addClassPathFile( getTestFile( "target/" + getName() + "/classes" ) );

        addClassPathFile( getTestFile( "target/classes" ) );

        addClassPathFile( getTestFile( "target/test-classes" ) );

        URLClassLoader classLoader = URLClassLoader.newInstance( (URL[]) urls.toArray( new URL[urls.size()] ),
                                                                 Thread.currentThread().getContextClassLoader() );

        Class clazz = classLoader.loadClass( className );

        Method verify = clazz.getMethod( "verify", new Class[0] );

        if ( false )
        {
            printClasspath( classLoader );
        }

        try
        {
            verify.invoke( clazz.newInstance(), new Object[0] );
        }
        catch ( InvocationTargetException ex )
        {
            throw ex.getCause();
        }
    }

    protected void addClassPathFile( File file )
        throws Exception
    {
        assertTrue( "File doesn't exists: " + file.getAbsolutePath(), file.exists() );

        urls.add( file.toURL() );
    }

    protected void printClasspath( URLClassLoader classLoader )
    {
        URL[] urls = classLoader.getURLs();

        for ( int i = 0; i < urls.length; i++ )
        {
            URL url = urls[i];

            System.out.println( url );
        }
    }

    protected void assertGeneratedFileExists( String filename )
    {
        File file = new File( getGeneratedSources(), filename );

        assertTrue( "Missing generated file: " + file.getAbsolutePath(), file.canRead() );

        assertTrue( "The generated file is empty.", file.length() > 0 );
    }
}
