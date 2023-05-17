/*
 * Copyright (c) 2022-2022 Balanced.network.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icon.xcall.lib.annotation;

import com.squareup.javapoet.*;
import foundation.icon.annotation_processor.AbstractProcessor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;

public class XCallProcessor extends AbstractProcessor {
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> s = new HashSet<>();
        s.add(XCall.class.getCanonicalName());
        return s;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<String, List<Element>> classMethods = new HashMap<>();
        Map<String, TypeElement> classes = new HashMap<>();

        boolean claimed = false;
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
            if (annotationElements.isEmpty()) {
                continue;
            }

            claimed = true;
            for (Element element : annotationElements) {
                TypeElement typeElement = (TypeElement) element.getEnclosingElement();
                String elementClassName = ClassName.get(typeElement).toString();
                List<Element> methods = classMethods.getOrDefault(elementClassName, new ArrayList<Element>());
                methods.add(element);
                classMethods.put(elementClassName, methods);
                classes.put(elementClassName, typeElement);
            }
        }

        classes.forEach((className, classType) -> {
            if (classMethods.get(className).size() == 0) {
                return;
            }

            classType.getInterfaces().forEach(type -> {
                List<Element> superList = classMethods.get(ClassName.get(type).toString());
                if (superList == null || superList.size() == 0) {
                    return;
                }
                List<Element> concatedList = Stream.concat(classMethods.get(className).stream(), superList.stream()).collect(Collectors.toList());
                classMethods.put(className, concatedList);

            });

            if (!classType.getSuperclass().getKind().equals(TypeKind.NONE)) {
                List<Element> superList = classMethods.get(ClassName.get(classType.getSuperclass()).toString());
                if (superList == null || superList.size() == 0) {
                    return;
                }

                List<Element> concatedList = Stream.concat(classMethods.get(className).stream(), superList.stream()).collect(Collectors.toList());
                classMethods.put(className, concatedList);
            }
        });

        classMethods.forEach((className, methodElements) -> {
            generateProcessorClass(processingEnv.getFiler(), ClassName.bestGuess(className), methodElements);
            generateMessageClass(processingEnv.getFiler(), ClassName.bestGuess(className), methodElements);
        });

        return claimed;
    }

    private void generateProcessorClass(Filer filer, ClassName elementClassName, List<? extends Element> elements) {
        Element element =  elements.iterator().next();
        XCall ann = element.getAnnotation(XCall.class);
        ClassName className = ClassName.get(elementClassName.packageName(), elementClassName.simpleName() + ann.suffix());

        TypeSpec typeSpec = processorTypeSpec(elementClassName, className, elements);
        JavaFile javaFile = JavaFile.builder(className.packageName(), typeSpec).build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            messager.warningMessage("create javaFile error : %s", e.getMessage());
        }
    }

    private void generateMessageClass(Filer filer, ClassName elementClassName, List<? extends Element> elements) {
        String messagesPackage  = this.getClass().getPackageName().replaceFirst("annotation", "messages");
        ClassName className = ClassName.get(messagesPackage, elementClassName.simpleName() + "Messages");

        TypeSpec typeSpec = messagesTypeSpec(className, elements);
        JavaFile javaFile = JavaFile.builder(className.packageName(), typeSpec).build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            messager.warningMessage("create javaFile error : %s", e.getMessage());
        }
    }

    private TypeSpec processorTypeSpec(ClassName elementClassName, ClassName className, List<? extends Element> elements) {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        MethodSpec.Builder handleMethod = MethodSpec.methodBuilder("process")
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.STATIC)
            .addParameter(elementClassName, "score")
            .addParameter(ParameterSpec.builder(String.class, "from").build())
            .addParameter(ParameterSpec.builder(byte[].class, "data").build())
            .addStatement("$T reader = $T.newByteArrayObjectReader(\"RLPn\", data)", ObjectReader.class, Context.class)
            .addStatement("reader.beginList()")
            .addStatement("String method = reader.readString()")
            .beginControlFlow("switch (method)");

        for (Element element : elements) {
            Name methodName = element.getSimpleName();
            ExecutableElement executableElement = (ExecutableElement) element;

            List<? extends VariableElement> parameters = executableElement.getParameters();
            VariableElement fromParam = parameters.get(0);
            if(!fromParam.toString().equals("from") || !fromParam.asType().toString().endsWith(".String")) {
                throw new RuntimeException("First parameter in a XCall must be the from parameter, (String from)");
            }

            handleMethod.addCode("case $S: \n", methodName);
            handleMethod.addCode("$> score." + methodName + "(from");

            for (int i = 1; i < parameters.size(); i++) {
                TypeMirror type = parameters.get(i).asType();
                String parser = getParseMethod(type.toString());
                handleMethod.addCode(", reader.$L()", parser);
            }

            handleMethod.addCode(");\n$<");
            handleMethod.addStatement("$>break$<");

        }

        handleMethod.addCode("default: \n");
        handleMethod.addStatement("$>$T.revert()$<", Context.class);
        handleMethod.endControlFlow();
        builder.addMethod(handleMethod.build());

        return builder.build();
    }

    private TypeSpec messagesTypeSpec(ClassName className,  List<? extends Element> elements) {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        for (Element element : elements) {
            Name methodName = element.getSimpleName();
            ExecutableElement executableElement = (ExecutableElement) element;
            List<? extends VariableElement> parameters = executableElement.getParameters();

            MethodSpec.Builder createMethod = MethodSpec.methodBuilder(methodName.toString())
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(byte[].class)
                .addStatement("$T writer = $T.newByteArrayObjectWriter(\"RLPn\")", ByteArrayObjectWriter.class, Context.class)
                .addStatement("writer.beginList($L)", parameters.size())
                .addStatement("writer.write($S)", methodName.toString());

            for (int i = 1; i < parameters.size(); i++) {
                TypeName type = TypeName.get(parameters.get(i).asType());
                Name name = parameters.get(i).getSimpleName();

                createMethod.addParameter(type, name.toString());
                createMethod.addStatement("writer.write($L)", name.toString());
            }

            createMethod.addStatement("writer.end()");
            createMethod.addStatement("return writer.toByteArray()");
            builder.addMethod(createMethod.build());
        }

        return builder.build();
    }

    private String getParseMethod(String paramType) {
        paramType = paramType.substring(paramType.lastIndexOf(".") + 1);
        switch(paramType) {
            case "String":
                return "readString";
            case "Address":
                return "readAddress";
            case "BigInteger":
                return "readBigInteger";
            case "Boolean":
                return "readBoolean";
            case "byte[]":
                return "readByteArray";
            default:
                throw new RuntimeException("XCall annotations does not support parameter type " + paramType);
        }
    }

    private String getSerializeMethod(String paramType) {
        paramType = paramType.substring(paramType.lastIndexOf(".") + 1);
        switch(paramType) {
            case "String":
                return "serializeString";
            case "String[]":
                return "serializeStringArray";
            case "Address":
                return "serializeAddress";
            case "Address[]":
                return "serializeAddressArray";
            case "BigInteger":
                return "serializeBigInteger";
            case "BigInteger[]":
                return "serializeBigIntegerArray";
            case "Boolean":
                return "serializeBoolean";
            case "Boolean[]":
                return "serializeBooleanArray";
            case "byte[]":
                return "serializeBytes";
            default:
                throw new RuntimeException("XCall annotations does not support parameter type " + paramType);
        }
    }
}