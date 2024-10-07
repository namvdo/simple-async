package annotation;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("at.cpx.common.annotation.Immutable")
@AutoService(Processor.class)
public class ImmutableProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (final Element element : roundEnv.getElementsAnnotatedWith(Immutable.class)) {
            if (element.getKind() == ElementKind.FIELD) {
                checkFieldImmutability(element);
            } else if (element.getKind() == ElementKind.CLASS) {
                checkClassImmutability(element);
            }

        }
        return true;
    }

    private void checkClassImmutability(Element element) {
        for (final Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD &&
                    !enclosedElement.getModifiers().contains(Modifier.FINAL)
            ) {
                processingEnv.getMessager()
                        .printMessage(Diagnostic.Kind.ERROR,
                                "All fields in class annotated with @Immutable must be final", enclosedElement);
            }


            if (enclosedElement.getKind() == ElementKind.METHOD && isSetterMethod(enclosedElement) && enclosedElement.getModifiers().contains(Modifier.PUBLIC)) {
                    processingEnv.getMessager()
                            .printMessage(Diagnostic.Kind.ERROR,
                                    "There must be no public setter methods in the class annotated with @Immutable",
                                    enclosedElement);
                }

        }
    }

    private boolean isSetterMethod(Element enclosedElement) {
        ExecutableElement method = (ExecutableElement) enclosedElement;
        TypeMirror returnType = method.getReturnType();
        boolean isVoid = returnType.getKind() == TypeKind.VOID;
        String methodName = method.getSimpleName().toString();
        return methodName.contains("set")
                && isVoid
                && method.getParameters().size() == 1;
    }

    private void checkFieldImmutability(Element element) {
        if (!element.getModifiers().contains(Modifier.FINAL)) {
           processingEnv.getMessager()
                   .printMessage(Diagnostic.Kind.ERROR, "Field annotated with @Immutable must be final", element);
        }
    }


}
