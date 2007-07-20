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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.codehaus.modello.verifier.VerifierException;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.javac.JavacCompiler;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public abstract class AbstractModelloGeneratorTest
    extends AbstractModelloTest
{
    private List dependencies = new ArrayList();

    private String name;

    private List urls = new ArrayList();

    private ArtifactRepository repository;

    private ArtifactFactory artifactFactory;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private MavenSettingsBuilder settingsBuilder;

    private ArtifactRepositoryLayout repositoryLayout;

    private List classPathElements = new ArrayList();

    protected AbstractModelloGeneratorTest( String name )
    {
        this.name = name;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( getGeneratedSources() );

        assertTrue( getGeneratedSources().mkdirs() );

        repositoryLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, "default" );

        settingsBuilder = (MavenSettingsBuilder) lookup( MavenSettingsBuilder.ROLE );

        Settings settings = settingsBuilder.buildSettings();

        String localRepo = settings.getLocalRepository();

        artifactRepositoryFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        String url = "file://" + localRepo;

        repository = artifactRepositoryFactory.createArtifactRepository( "local", url, repositoryLayout, null, null );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
    }

    protected File getGeneratedSources()
    {
        return getTestFile( "target/" + getName() );
    }

    public void addDependency( String groupId, String artifactId, String version )
        throws MalformedURLException
    {
        File dependencyFile = getDepedencyFile( groupId, artifactId, version );

        dependencies.add( dependencyFile );

        addClassPathFile( dependencyFile );
    }

    protected File getDepedencyFile( String groupId, String artifactId, String version )
    {
        Artifact artifact =
            artifactFactory.createArtifact( groupId, artifactId, version, Artifact.SCOPE_COMPILE, "jar" );

        File dependencyFile = new File( repository.getBasedir(), repository.pathOf( artifact ) );

        assertTrue( "Cant find dependency: " + dependencyFile.getAbsolutePath(), dependencyFile.isFile() );
        return dependencyFile;
    }

    public String getName()
    {
        return name;
    }

    public List getClasspath()
    {
        return dependencies;
    }

    protected String getModelloVersion()
        throws IOException
    {
        Properties properties = new Properties( System.getProperties() );

        if ( properties.getProperty( "version" ) == null )
        {
            InputStream is = getResourceAsStream( "/META-INF/maven/org.codehaus.modello/modello-test/pom.properties" );

            if ( is != null )
            {
                properties.load( is );
            }
        }

        return properties.getProperty( "version" );
    }

    protected void compile( File generatedSources, File destinationDirectory )
        throws IOException, CompilerException
    {
        addDependency( "junit", "junit", "3.8.1" );

        addDependency( "org.codehaus.plexus", "plexus-utils", "1.4.3" );

        addDependency( "org.codehaus.modello", "modello-test", getModelloVersion() );

        String[] classPathElements = new String[dependencies.size() + 2];

        classPathElements[0] = getTestPath( "target/classes" );

        classPathElements[1] = getTestPath( "target/test-classes" );

        for ( int i = 0; i < dependencies.size(); i++ )
        {
            classPathElements[i + 2] = ( (File) dependencies.get( i ) ).getAbsolutePath();
        }

        String[] sourceDirectories =
            new String[]{getTestPath( "src/test/verifiers/" + getName() ), generatedSources.getAbsolutePath()};

        Compiler compiler = new JavacCompiler();

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.setClasspathEntries( Arrays.asList( classPathElements ) );
        configuration.setSourceLocations( Arrays.asList( sourceDirectories ) );
        configuration.setOutputLocation( destinationDirectory.getAbsolutePath() );
        configuration.setDebug( true );

        List messages = compiler.compile( configuration );

        for ( Iterator it = messages.iterator(); it.hasNext(); )
        {
            CompilerError message = (CompilerError) it.next();

            System.out.println( message.getFile() + "[" + message.getStartLine() + "," + message.getStartColumn() +
                "]: " + message.getMessage() );
        }

        assertEquals( "There was compilation errors.", 0, messages.size() );
    }

    protected void verify( String className, String testName )
        throws MalformedURLException
    {
        addClassPathFile( getTestFile( "target/" + getName() + "/classes" ) );

        addClassPathFile( getTestFile( "target/classes" ) );

        addClassPathFile( getTestFile( "target/test-classes" ) );

        ClassLoader oldCCL = Thread.currentThread().getContextClassLoader();
        URLClassLoader classLoader = URLClassLoader.newInstance( (URL[]) urls.toArray( new URL[urls.size()] ), null );

        Thread.currentThread().setContextClassLoader( classLoader );

        try
        {
            Class clazz = classLoader.loadClass( className );

            Method verify = clazz.getMethod( "verify", new Class[0] );

            try
            {
                verify.invoke( clazz.newInstance(), new Object[0] );
            }
            catch ( InvocationTargetException ex )
            {
                throw ex.getCause();
            }
        }
        catch ( Throwable throwable )
        {
            throw new VerifierException( "Error verifying modello tests: " + throwable.getMessage(), throwable );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldCCL );
        }
    }

    protected void addClassPathFile( File file )
        throws MalformedURLException
    {
        assertTrue( "File doesn't exists: " + file.getAbsolutePath(), file.exists() );

        urls.add( file.toURL() );

        classPathElements.add( file.getAbsolutePath() );
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

    protected List getClassPathElements()
    {
        return classPathElements;
    }
}
