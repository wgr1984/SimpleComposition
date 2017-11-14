package de.wr.annotationprocessor.processor

import com.github.javaparser.JavaParser.parseClassOrInterfaceType
import com.github.javaparser.ast.ArrayCreationLevel
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.ArrayType
import de.wr.libsimplecomposition.Include
import de.wr.libsimplecomposition.ObjectWrapper
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import java.io.BufferedWriter
import java.io.IOException
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.SourceVersion.latestSupported
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeKind
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.collections.HashSet
import com.github.javaparser.ast.Modifier as AstModifier
import com.github.javaparser.ast.type.Type as AstType

class SimpleCompositionProcessor : AbstractProcessor() {

    private val methodsForClass = Hashtable<TypeElement, List<ExecutableElement>>()

    private lateinit var objectType: String
    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer
    private lateinit var messager: Messager

    override fun getSupportedSourceVersion(): SourceVersion {
        return latestSupported()
    }

    override fun getSupportedAnnotationTypes() = supportedAnnotations

    @Synchronized override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils
        elementUtils = processingEnv.elementUtils
        filer = processingEnv.filer
        messager = processingEnv.messager
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val clazzes = supportedAnnotationsClasses.toObservable()
            .flatMap {
                roundEnv.getElementsAnnotatedWith(it).toObservable()
            }.flatMap { element ->
                    try {
                        element.getAnnotation(Include::class.java).value
                    } catch (e: MirroredTypesException) {
                        return@flatMap e.typeMirrors.toObservable().cast(DeclaredType::class.java).map { it.asElement() }
                    }
                    Observable.just(element)
            }.toList().blockingGet()

        generateMethodsForClazz(clazzes)

        return true
    }

    private fun generateMethodsForClazz(clazzes: List<Element>) {

        clazzes.forEach {

            val clazzElement = it as TypeElement

            info(clazzElement, "Composition found %s", clazzElement.simpleName.toString() )

            try {
                createInterface("I${clazzElement.simpleName.toString()}", clazzElement, false)

                createInterface("${clazzElement.simpleName.toString()}Composition", clazzElement, true)
            } catch (e: IOException) {
                System.err.println(objectType + " :" + e + e.message)
                error(clazzElement, "Error: %s %n", e)
            }
        }
    }

    private fun createInterface(fileName: String, clazzElement: TypeElement, defaultOne: Boolean) {
        val source = processingEnv.filer.createSourceFile(fileName)

        val writer = BufferedWriter(source.openWriter())

        val cu = CompilationUnit();
        // set the package
        cu.setPackageDeclaration(getPackageName(clazzElement));

        val defaultInterface = cu.addInterface(fileName, AstModifier.PUBLIC)

        val methods = clazzElement.enclosedElements
                .filter { it is ExecutableElement }
                .map { it as ExecutableElement }

        var compElement: Expression? = null

        if (defaultOne) {
            defaultInterface.tryAddImportToParentCompilationUnit(ObjectWrapper::class.java)
            defaultInterface.addExtendedType("${getPackageName(clazzElement)}.I${clazzElement.simpleName.toString()}")

            val constructors = methods.filter {
                it.modifiers.contains(Modifier.PUBLIC) && !it.modifiers.contains(Modifier.STATIC) && it.kind == ElementKind.CONSTRUCTOR
            }

            info(clazzElement, "Class %s constructors found %s", clazzElement, constructors)

            compElement = constructors.minBy { it.parameters.size }?.let {
                val fieldName = "comp${clazzElement.simpleName.toString()}"

                info(clazzElement, "Class %s create field %s %s", clazzElement, clazzElement.simpleName.toString(), fieldName)

                val type = parseClassOrInterfaceType(clazzElement.simpleName.toString())

                when (it.parameters.size) {
                    0 -> {
                        defaultInterface.addField(type, fieldName).setVariable(0,
                                VariableDeclarator(type, fieldName, ObjectCreationExpr().setType(type))
                        )
                        NameExpr(fieldName)
                    }
                    else -> {
                        val objectWrapper = parseClassOrInterfaceType("ObjectWrapper<${clazzElement.simpleName.toString()}>")
                        defaultInterface.addField(objectWrapper, fieldName).setVariable(0,
                                VariableDeclarator(objectWrapper, fieldName, ObjectCreationExpr().setType(objectWrapper))
                        )
                        MethodCallExpr(NameExpr(fieldName), "get")
                    }
                }
            }
        }

        methods.filter { it.modifiers.contains(Modifier.PUBLIC) && !it.modifiers.contains(Modifier.STATIC) && it.kind != ElementKind.CONSTRUCTOR }
                .forEach { method ->

                    info(method, "Class %s contains public method %s %s", clazzElement, method.returnType, method.toString())

                    val methodName = method.simpleName.toString()

                    val interfaceMethod = defaultInterface.addMethod(methodName).setModifiers(if (defaultOne) {
                        EnumSet.of(AstModifier.DEFAULT)
                    } else {
                        EnumSet.noneOf(AstModifier::class.java)
                    }).setType(method.returnType.toString())

                    if (defaultOne) {
                        val defMethodExp = MethodCallExpr(compElement, methodName)

                        method.parameters.forEach {
                            defMethodExp.addArgument(it.simpleName.toString())
                        }

                        interfaceMethod.setBody(when {
                            method.returnType.kind == TypeKind.VOID -> BlockStmt().addStatement(defMethodExp)
                            else -> BlockStmt().addStatement(ReturnStmt().setExpression(defMethodExp))
                        })
                    } else {
                        interfaceMethod.removeBody()
                    }


                    method.parameters.forEach {
                        interfaceMethod.addParameter(it.asType().toString(), it.simpleName.toString())
                    }
                }

        writer.run {
            write(cu.toString())
            flush()
            close()
        }
    }

    private fun getPackageName(typeElement: TypeElement) =
            typeElement.qualifiedName.substring(0, typeElement.qualifiedName.length - typeElement.simpleName.length - 1)

    private fun error(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, *args),
                e)
    }

    private fun info(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(
                Diagnostic.Kind.WARNING,
                String.format(msg, *args),
                e)
    }


    companion object {
        private var supportedAnnotations = HashSet<String>()
        private var supportedAnnotationsClasses = mutableListOf<Class<out Annotation>>()

        init {
            supportedAnnotationsClasses.apply {
                add(Include::class.java)
            }.forEach { supportedAnnotations.add(it.canonicalName) }
        }

        val DEFAULT = false
    }
}