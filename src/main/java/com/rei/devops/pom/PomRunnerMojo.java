package com.rei.devops.pom;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

@Mojo(name="run", requiresProject=false)
public class PomRunnerMojo extends AbstractMojo {
    private static final Set<String> MOJO_PARAMS = ImmutableSet.of("g", "a", "v", "goals");
    
    @Component
    RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}")
    RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remotePluginRepositories}")
    List<RemoteRepository> remoteRepos;

    @Parameter(property = "g")
    String groupId;

    @Parameter(property = "a")
    String artifactId;
    
    @Parameter(property = "v")
    String version;
    
    @Parameter(property = "goals")
    String goals;
    
    @Component(hint="invoker")
    MavenExecutor executor;
    
    @Parameter(defaultValue="${basedir}")
    File basedir;
    
    @Parameter(property="maven.home", readonly=true, required=true)
    File mavenHome;
    
    @Parameter(property="java.home", readonly=true)
    protected File javaHome;
    
    @Component
    protected Settings settings;

    @Parameter(defaultValue="${maven.repo.local}")
   protected File localRepoDirectory;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(groupId + ":" + artifactId + ":pom:" + version));
        request.setRepositories(remoteRepos);
        try {
            ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);
            FileUtils.copyFile(result.getArtifact().getFile(), new File("pom.xml"));
            try {
                executor.executeGoals(basedir, goals, getReleaseEnvironment(), false, getAdditionalArguments(), new ReleaseResult());
            } catch (MavenExecutorException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } catch (Exception e) {
                throw new MojoFailureException("failed invoking pom", e);
            }            
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("failed to download pom", e);
        } catch (IOException e) {
            throw new MojoFailureException("failed to copy pom from repository", e);
        }
    }
    
    String getAdditionalArguments() throws Exception { 
        Map<String, String> userProperties = getUserSuppliedSystemProperties();
        return "\"-D" + Joiner.on("\" \"-D").withKeyValueSeparator("=").join(userProperties) + "\"";
    }

    Map<String, String> getUserSuppliedSystemProperties() throws Exception {
        Commandline cl = new Commandline();
        cl.setExecutable(new File(javaHome, "bin/java").getCanonicalPath());
        cl.addSystemEnvironment();
        cl.createArg().setValue("-cp");
        cl.createArg().setValue(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath());
        cl.createArg().setValue(SystemPropertiesOutputter.class.getName());
        
        final Set<String> builtinProperties = new HashSet<String>();
        
        StreamConsumer lineConsumer = new StreamConsumer() {
            @Override
            public void consumeLine(String line) {
                builtinProperties.add(line);
            }};
        
        CommandLineUtils.executeCommandLine(cl, lineConsumer, lineConsumer);
        
        return Maps.filterKeys(Maps.fromProperties(System.getProperties()), and(not(in(MOJO_PARAMS)), not(in(builtinProperties))));        
    }
    
    private ReleaseEnvironment getReleaseEnvironment() {
        return new DefaultReleaseEnvironment().setSettings( settings )
                .setJavaHome( javaHome )
                .setMavenHome( mavenHome )
                .setLocalRepositoryDirectory( localRepoDirectory );
    }
    
}
