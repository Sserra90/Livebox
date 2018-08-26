package com.creations.livebox.util;

import org.apache.commons.io.input.ReaderInputStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

import okio.BufferedSource;
import okio.Okio;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class OkioUtils {

    public static ReaderInputStream readerInputStreamUtf8(String source) {
        return new ReaderInputStream(new StringReader(source), Charset.forName("UTF-8"));
    }

    public static BufferedSource bufferedSource(InputStream is) {
        return Okio.buffer(Okio.source(is));
    }

    public static void copy(InputStream is, OutputStream os) {

    }
}
