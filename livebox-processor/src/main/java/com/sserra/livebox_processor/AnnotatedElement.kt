package com.sserra.livebox_processor

import com.sserra.annotations.Assets
import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror

data class AssetsElement(override val element: Element) : AnnotatedElement(element) {

    val className: String
    val folder: String = element.getAnnotation(Assets::class.java).folder
    val mapsTo: TypeMirror?
        get() {
            try {
                element.getAnnotation(Assets::class.java).mapsTo
            } catch (e: MirroredTypeException) {
                return e.typeMirror
            }
            return null
        }

    init {
        element.getAnnotation(Assets::class.java).name.apply {
            className = if (isEmpty()) {
                folder.toLowerCase().capitalize() + "Assets"
            } else {
                this
            }
        }
    }

    override fun toString(): String {
        return "AssetsElement(element=$element, className='$className', folder='$folder', mapsTo='$mapsTo')"
    }
}

open class AnnotatedElement(open val element: Element) {
    val name: Name
        get() = element.simpleName

    val enclosingElement: TypeElement
        get() = element.enclosingElement as TypeElement
}
