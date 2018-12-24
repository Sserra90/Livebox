package com.sserra.livebox_processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.sserra.annotations.Assets
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class LiveboxProcessor : AbstractProcessor() {

    private val assetsDir = File("app/src/main/assets")
    private val JACKSON = "com.sserra.livebox_jackson"
    private val GSON = "com.sserra.livebox_gson"

    private lateinit var mMessager: Messager
    private lateinit var mFiler: Filer
    private lateinit var mTypeUtils: Types
    private lateinit var mElements: Elements
    private lateinit var assetFetcher: String

    private fun error(msg: String, vararg args: Any) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, *args))
    }

    private fun warning(msg: String, vararg args: Any) {
        mMessager.printMessage(Diagnostic.Kind.WARNING, String.format(msg, *args))
    }

    @Synchronized
    override fun init(env: ProcessingEnvironment) {
        super.init(env)
        mMessager = env.messager
        mFiler = env.filer
        mTypeUtils = env.typeUtils
        mElements = env.elementUtils

        assetFetcher = if (hasJacksonSerializer()) JACKSON else GSON
    }

    private fun hasJacksonSerializer() = mElements.getPackageElement(JACKSON) != null

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(Assets::class.java.canonicalName)

    override fun process(set: Set<TypeElement>, env: RoundEnvironment): Boolean {
        warning("Process")

        val bindings: MutableList<AnnotatedElement> = mutableListOf()

        // Parse @Assets annotated elements
        ElementFilter.typesIn(env.getElementsAnnotatedWith(Assets::class.java)).forEach {
            parseAssetsAnnotation(it, bindings)
        }

        assetsDir.listFiles().forEach {
            if (it.isDirectory) {
                warning("Folder: %s", it)
                it.listFiles().forEach { f ->
                    warning("-- File: %s", f)
                }
            }
            warning("File: %s", it)
        }

        //warning("File: %s", fileObject.openInputStream())
        warning("Bindings: %s", bindings)

        makeItHappen(bindings)

        return true
    }

    private fun parseAssetsAnnotation(typeElement: TypeElement, bindings: MutableList<AnnotatedElement>) {
        val assetElement = AssetsElement(typeElement)
        if (assetElement.folder.isEmpty()) {
            error("Folder path is empty or null for @Asset annotation in: %s", typeElement)
        }
        bindings.add(assetElement)
    }

    private fun makeItHappen(bindings: MutableList<AnnotatedElement>) {
        bindings.forEach {
            warning("Generate class for: $it")

            val file = FileSpec.builder(getPackage(it.element as TypeElement), KAPT_FILENAME)

            val classSpec = createClass((it as AssetsElement).className)
            file.addType(classSpec)

            val outfile = File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME], KAPT_DIR)
            if (!outfile.exists()) {
                outfile.mkdirs()
            }

            file.build().writeTo(outfile)
        }
    }

    private fun createClass(className: String): TypeSpec {
        return TypeSpec.classBuilder(className)
                .build()
    }

    private fun getPackage(element: TypeElement): String =
            element.qualifiedName.toString().substring(0, element.qualifiedName.toString().lastIndexOf("."))

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
        const val KAPT_DIR = "livebox"
        const val KAPT_FILENAME = "AssetsSettings"
    }
}