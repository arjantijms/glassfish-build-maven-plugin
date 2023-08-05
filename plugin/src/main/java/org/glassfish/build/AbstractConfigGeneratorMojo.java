/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.glassfish.build.hk2.config.generator.ConfigInjectorGenerator;

/**
 * @author jwells
 *
 * Abstract Mojo for config generator
 */
public abstract class AbstractConfigGeneratorMojo extends AbstractMojo {
    protected final static String GENERATED_SOURCES = "generated-sources/hk2-config-generator/src";
    protected final static String MAIN_NAME = "main";
    protected final static String TEST_NAME = "test";
    protected final static String JAVA_NAME = "java";

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Parameter
    private boolean verbose;

    @Parameter(property = "supportedProjectTypes", defaultValue = "jar,glassfish-jar")
    private String supportedProjectTypes;

    @Parameter(property="includes", defaultValue = "**/*.java")
    private String includes;

    @Parameter(property="excludes", defaultValue = "")
    private String excludes;

    protected abstract File getSourceDirectory();
    protected abstract File getGeneratedDirectory();
    protected abstract File getOutputDirectory();
    protected abstract void addCompileSourceRoot(String path);

    private void internalExecute() throws Exception {
        List<String> projectTypes = Arrays.asList(supportedProjectTypes.split(","));
        if(!projectTypes.contains(project.getPackaging())
                || !getSourceDirectory().exists()
                || !getSourceDirectory().isDirectory()){
            return;
        }
        getLog().info(getGeneratedDirectory().getAbsolutePath());
        if (!getGeneratedDirectory().exists()) {
            if (!getGeneratedDirectory().mkdirs()) {
                getLog().info("Could not create output directory " +
                        getOutputDirectory().getAbsolutePath());
                return;
            }
        }
        if (!getGeneratedDirectory().exists()) {
            getLog().info("Exiting hk2-config-generator because could not find generated directory " +
                  getGeneratedDirectory().getAbsolutePath());
            return;
        }
        String outputPath = getGeneratedDirectory().getAbsolutePath();

        // prepare command line arguments
        List<String> options = new ArrayList<>();
        options.add("-proc:only");
        options.add("-s");
        options.add(outputPath);
        options.add("-d");
        options.add(outputPath);
        options.add("-cp");
        options.add(getBuildClasspath());
        List<String> classNames = new ArrayList<>();
        classNames.addAll(FileUtils.getFileNames(getSourceDirectory(), includes, excludes,true));

        if(classNames.isEmpty()){
            getLog().info("No source file");
            return;
        }

        if(verbose){
            getLog().info("");
            getLog().info("-- AnnotationProcessing Command Line --");
            getLog().info("");
            getLog().info(options.toString());
            getLog().info(classNames.toString());
            getLog().info("");
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(classNames);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
        task.setProcessors(Collections.singleton(new ConfigInjectorGenerator()));

        boolean compilationResult = task.call();
        if(verbose) {
            getLog().info("Result: " + (compilationResult ? "OK" : "!!! failed !!!"));
        }

        // make the generated source directory visible for compilation
        addCompileSourceRoot(outputPath);
        if (getLog().isInfoEnabled()) {
            getLog().info("Source directory: " + outputPath + " added.");
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            internalExecute();
        } catch (Exception th) {
            if (th instanceof MojoExecutionException) {
                throw (MojoExecutionException) th;
            }
            if (th instanceof MojoFailureException) {
                throw (MojoFailureException) th;
            }

            Throwable cause = th;
            int lcv = 0;
            while (cause != null) {
                getLog().error("Exception from hk2-config-generator[" + lcv++ + "]=" + cause.getMessage());
                cause.printStackTrace();

                cause = cause.getCause();
            }

            throw new MojoExecutionException(th.getMessage(), th);
        }
    }

    private String getBuildClasspath() {
        StringBuilder sb = new StringBuilder();

        sb.append(project.getBuild().getOutputDirectory());
        sb.append(File.pathSeparator);

        if (!getOutputDirectory().getAbsolutePath().equals(
                project.getBuild().getOutputDirectory())) {

            sb.append(getOutputDirectory().getAbsolutePath());
            sb.append(File.pathSeparator);
        }

        List<Artifact> artList = new ArrayList<>(project.getArtifacts());
        Iterator<Artifact> i = artList.iterator();

        if (i.hasNext()) {
            sb.append(i.next().getFile().getPath());

            while (i.hasNext()) {
                sb.append(File.pathSeparator);
                sb.append(i.next().getFile().getPath());
            }
        }

        String classpath = sb.toString();
        if(verbose){
            getLog().info("");
            getLog().info("-- Classpath --");
            getLog().info("");
            getLog().info(classpath);
            getLog().info("");
        }
        return classpath;
    }
}
