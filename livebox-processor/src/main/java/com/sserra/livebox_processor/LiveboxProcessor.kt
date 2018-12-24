package com.sserra.livebox_processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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
    private lateinit var assetFetcherType: ClassName

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

        assetFetcherType = if (hasJacksonSerializer())
            ClassName("com.sserra.livebox_jackson", "assetFetcher")
        else
            ClassName("com.sserra.livebox_gson", "assetFetcher")
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

        warning("Bindings: %s", bindings)

        makeItHappen(bindings)

        return true
    }

    private fun parseAssetsAnnotation(typeElement: TypeElement, bindings: MutableList<AnnotatedElement>) {
        val assetElement = AssetsElement(typeElement)
        if (assetElement.folder.isEmpty()) {
            error("Folder path is empty or null for @Asset annotation in: %s", typeElement)
        }
        if (assetElement.mapsTo == null) {
            error("Maps to is null for @Asset annotation in: %s", typeElement)
        }
        bindings.add(assetElement)
    }

    private fun makeItHappen(bindings: MutableList<AnnotatedElement>) {
        bindings.forEach {
            warning("Generate class for: $it")

            val file = FileSpec.builder(getPackage(it.element as TypeElement), KAPT_FILENAME)

            // Check if output file exists, if not create
            val outfile = File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME], KAPT_DIR)
            if (!outfile.exists()) {
                outfile.mkdirs()
            }

            val classSpec = TypeSpec.classBuilder((it as AssetsElement).className)

            val files = File(assetsDir, it.folder).listFiles()
            if (files.isNullOrEmpty()) {
                error("No files found for path: ${it.folder}")
            }

            files.forEach { f ->
                // Build a bindAsset property for each file found in folder
                val propName = buildPropName(f)

                val propSpec = PropertySpec.builder(propName, String::class)
                        .addModifiers(KModifier.PUBLIC)
                        .delegate("%T(%S)", bindAssetType, it.folder + "/" + f.name)
                        .build()
                classSpec.addProperty(propSpec)
            }

            generateFetchersMap(it, files, classSpec)

            file.addFunction(generateFetchesFun(it))
            file.addType(classSpec.build())

            // Write to file
            file.build().writeTo(outfile)
        }
    }

    private fun generateFetchesFun(element: AssetsElement): FunSpec {
        val fetcherParameterType = fetcherType.parameterizedBy(element.mapsTo!!.asTypeName())
        val param = ParameterSpec.builder("status", statusType).build()
        return FunSpec.builder(element.folder + "Fetchers")
                .addParameter(param)
                .returns(fetcherParameterType)
                .addStatement("return ${element.className}().${element.folder}FetchersMap[${param.name}]!!")
                .build()
    }

    private fun generateFetchersMap(element: AssetsElement, files: Array<File>, classSpec: TypeSpec.Builder) {
        val fetcherParameterType = fetcherType.parameterizedBy(element.mapsTo!!.asTypeName())
        val mapType = Map::class.asClassName().parameterizedBy(statusType, fetcherParameterType)

        val fetchersCodeBlock = CodeBlock.builder()
        fetchersCodeBlock.addStatement("%T {", byLazy)
        fetchersCodeBlock.addStatement("mapOf<%T,%T>(", statusType, fetcherParameterType)

        files.forEachIndexed { index, file ->
            val statusName = file.name.split(".").first().split("_").last().capitalize()
            val statusType = mElements.getTypeElement("com.creations.runtime.state.Status.$statusName")
            if (statusType != null) {
                fetchersCodeBlock
                        .add("%T to %T(%S)", statusType, assetFetcherType, element.folder + "/" + file.name)
                if (index < files.size - 1) {
                    fetchersCodeBlock.addStatement(",")
                }
            }
            warning("Status type: $statusType")
        }
        fetchersCodeBlock.addStatement(")").addStatement("}")

        // Build fetchers map
        val fetchersPropName = element.folder + "FetchersMap"
        val fetchersPropSpec = PropertySpec.builder(fetchersPropName, mapType, KModifier.PUBLIC)
                .delegate(fetchersCodeBlock.build())
                .build()
        classSpec.addProperty(fetchersPropSpec)
    }

    /**
     * Build prop name from file name.
     * First convert remove file extension, then convert to camel case and append "response" suffix.
     */
    private fun buildPropName(f: File) = toCamelCase(f.name.split(".").first()) + BIND_ASSET_SUFFIX

    private fun getPackage(element: TypeElement): String =
            element.qualifiedName.toString().substring(0, element.qualifiedName.toString().lastIndexOf("."))

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
        const val KAPT_DIR = "livebox"
        const val KAPT_FILENAME = "AssetsSettings"
        const val BIND_ASSET_SUFFIX = "Response"

        val bindAssetType = ClassName("com.creations.livebox.util.io", "bindAsset")
        val byLazy = ClassName("kotlin", "lazy")

        val statusType = ClassName("com.creations.runtime.state", "Status")
        val fetcherType = ClassName("com.creations.livebox.datasources.fetcher", "Fetcher")
    }
}

private fun toCamelCase(snakeCase: String): String {
    val nameBuilder = StringBuilder(snakeCase.length)
    var capitalizeNextChar = false

    for (c in snakeCase.toCharArray()) {
        if (c == '_') {
            capitalizeNextChar = true
            continue
        }
        if (capitalizeNextChar) {
            nameBuilder.append(Character.toUpperCase(c))
        } else {
            nameBuilder.append(c)
        }
        capitalizeNextChar = false
    }
    return nameBuilder.toString()
}