/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme.internal

import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter

fun XmlSerializer.document(docName: String = "UTF-8",
                           xmlStringWriter: StringWriter = StringWriter(),
                           init: XmlSerializer.() -> Unit): String {
    startDocument(docName, true)
    xmlStringWriter.buffer.setLength(0)
    setOutput(xmlStringWriter)
    init()
    endDocument()
    return xmlStringWriter.toString()
}

fun XmlSerializer.element(name: String, init: XmlSerializer.() -> Unit) {
    startTag("", name)
    init()
    endTag("", name)
}

fun XmlSerializer.attribute(name: String, value: String): XmlSerializer {
    return attribute("", name, value)
}