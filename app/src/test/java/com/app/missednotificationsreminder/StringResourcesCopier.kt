package com.app.missednotificationsreminder

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class StringResourcesCopier {
    internal val kotlinXmlMapper = XmlMapper(JacksonXmlModule().apply {
        setDefaultUseWrapper(false)
    }).registerKotlinModule()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


    internal inline fun <reified T : Any> parseAs(path: String): T {
        val resource = FileInputStream(path)
        return kotlinXmlMapper.readValue(resource, T::class.java)
    }

    @Test
    fun main() {
        println(File(".").absolutePath)
        val resources = parseAs<Resources>("./src/main/res/values-de/strings.xml")
        val translations = parseAs<Resources>("I:/strings.xml")
        resources.strings
                ?.filter { it.translatable != true }
                ?.map { source ->
                    (translations.strings?.firstOrNull { it.name == source.name }
                            ?.value
                            ?: "ABSENT_VALUE: ${source.value}")
                            .let {
                                source.value = it
                                source
                            }
                }
                ?.run {
                    resources.strings = this
                    kotlinXmlMapper.writerWithDefaultPrettyPrinter().writeValue(FileOutputStream("I:/strings2.xml"), resources)
                }

    }

    @JacksonXmlRootElement(localName = "resources")
    internal class Resources {

        @set:JacksonXmlProperty(localName = "string")
        var strings: List<StringElement>? = null
    }

    internal class StringElement {
        @set:JacksonXmlProperty(isAttribute = true)
        var name: String? = null

        @set:JacksonXmlProperty(isAttribute = true)
        var translatable: Boolean? = null

        @set:JacksonXmlText(value = true)
        var value: String? = null
    }
}
