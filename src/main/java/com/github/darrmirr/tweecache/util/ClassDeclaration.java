package com.github.darrmirr.tweecache.util;

import org.codehaus.janino.util.ClassFile;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * {@link ClassDeclaration} contains parts of class definition. Its main goal is build definition step by step.
 */
public class ClassDeclaration {
    private String packageName;
    private String className;
    private String modifier;
    private List<String> importDeclarations = new LinkedList<>();
    private List<String> fieldDeclarations = new LinkedList<>();

    public ClassDeclaration(String packageName, String className) {
        this(packageName, className, "public");
    }

    public ClassDeclaration(String packageName, String className, String modifier) {
        this.packageName = packageName;
        this.className = toCapitalFirstLetter(className);
        this.modifier = modifier;
    }

    public void addImport(Class<?> importClass) {
        Optional.ofNullable(importClass)
                .map(Class::getName)
                .map(className ->
                        "import " + className + ";")
                .ifPresent(importDeclarations::add);
    }

    public void addField(String fieldDeclaration) {
        Optional.ofNullable(fieldDeclaration)
                .ifPresent(fieldDeclarations::add);
    }

    public String getDefinition() {
        return new StringBuilder()
                .append("package").append(" ")
                .append(packageName).append("; ")
                .append(imports()).append(" ")
                .append(modifier).append(" ")
                .append("class").append(" ")
                .append(className).append(" ")
                .append("{").append(" ")
                .append(fields()).append(" ")
                .append("}")
                .toString();
    }

    public String getClassSimpleName() {
        return className;
    }

    public String getClassName() {
        return packageName + "." + className;
    }

    public String getJavaFilePath() {
        return ClassFile.getSourceResourceName(getClassName());
    }

    public String getClassFilePath() {
        return ClassFile.getClassFileResourceName(getClassName());
    }

    private String imports() {
        return String.join(" ", importDeclarations);
    }

    private String fields() {
        return String.join(" ", fieldDeclarations);
    }

    private String toCapitalFirstLetter(String string) {
        String firstCapital = string.substring(0, 1).toUpperCase();
        return firstCapital + string.substring(1);
    }

    @Override
    public String toString() {
        return getDefinition();
    }
}
