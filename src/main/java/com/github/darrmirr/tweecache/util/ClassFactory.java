package com.github.darrmirr.tweecache.util;

import org.apache.calcite.adapter.enumerable.EnumerableInterpretable;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.util.ResourceFinderClassLoader;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.MapResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.CompilerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ClassFactory} compiles cache schema classes and loads it using by special ClassLoader.
 *
 * ClassLoader is set to {@link EnumerableInterpretable} in order to make compiled classes visible to Apache Calcite
 */
public class ClassFactory {
    private static final Logger log = LoggerFactory.getLogger(ClassFactory.class);
    public static final ClassFactory INSTANCE = new ClassFactory();
    private final ICompiler compiler = new CompilerFactory().newCompiler();
    private final MapResourceCreator resourceCreator = new MapResourceCreator(new ConcurrentHashMap<>());                           // Store generated .class files in a Map
    private final MapResourceFinder resourceFinder = new MapResourceFinder();                                                       // Store java files content
    private final ClassLoader classLoader = new ResourceFinderClassLoader(resourceFinder, ClassLoader.getSystemClassLoader());      // Set up a class loader that uses the generated classes.

    private ClassFactory() {
        compiler.setClassFileCreator(resourceCreator);
        EnumerableInterpretable.setParentClassLoader(classLoader);
    }

    /**
     * Compile class according to provided {@link ClassDeclaration}
     *
     * @param classDeclaration class declaration to compile
     * @return compiled Class object
     */
    private Result<Class<?>> compileFunction(ClassDeclaration classDeclaration) {
        try {
            log.debug("compile java class : '{}'", classDeclaration);
            synchronized (compiler) {
                compiler.compile(new Resource[] { new StringResource(classDeclaration.getJavaFilePath(),  classDeclaration.getDefinition()) });
            }
            add2classLoader(classDeclaration);
            return Result.ok(classLoader.loadClass(classDeclaration.getClassName()));
        } catch (CompileException | IOException | ClassNotFoundException | ClassFormatError e) {
            log.error("error to compile class : '{}' due to '{}'.", classDeclaration, e.getMessage());
            return Result.error(e);
        }
    }

    /**
     * Compile class according to provided {@link ClassDeclaration}
     *
     * @param classDeclaration class declaration to compile
     * @return compiled Class object
     */
    public Result<Class<?>> compile(ClassDeclaration classDeclaration) {
        return Optional
                .ofNullable(classDeclaration)
                .map(this::compileFunction)
                .orElseGet(() -> Result.error(new NullPointerException("Error to compile due to class declaration is null.")));
    }

    /**
     * Add compiled class to class loader
     *
     * @param classDeclaration class declaration of complied class
     */
    private void add2classLoader(ClassDeclaration classDeclaration) {
        String classFilePath = classDeclaration.getClassFilePath();
        byte[] classFileBytes = resourceCreator.getMap().get(classFilePath);
        resourceFinder.addResource(classFilePath, classFileBytes);
    }
}
