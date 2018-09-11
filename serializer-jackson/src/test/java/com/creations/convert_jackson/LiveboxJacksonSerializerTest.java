package com.creations.convert_jackson;

import com.creations.livebox_common.serializers.Serializer;
import com.creations.livebox_common.util.Bag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okio.BufferedSource;
import okio.Okio;

/**
 * @author Sérgio Serra on 10/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class LiveboxJacksonSerializerTest {

    @Test
    public void serializeDeserializeBag() {

        // Setup
        final List<String> values = new ArrayList<>();
        values.add("one");
        values.add("two");
        values.add("three");
        final Bag<String> bag = new Bag<>("100", values);

        // Exercise
        final Serializer serializer = LiveboxJacksonSerializer.create(new ObjectMapper());
        TypeReference<Bag<String>> bagType = new TypeReference<Bag<String>>() {
        };
        JavaType type = TypeFactory.defaultInstance().constructType(bagType);
        final Bag newBag = serializer.deserialize(serializer.serialize(bag, bagType.getType()), type);

        // Verify
        Assert.assertEquals(bag, newBag);

    }

    @Test
    public void deserializeBigJson() throws IOException {

        // Setup
        InputStream is = getClass().getClassLoader().getResourceAsStream("bigJson.json");
        BufferedSource source = Okio.buffer(Okio.source(is));

        // Exercise
        TypeReference<List<Bag<String>>> bagType = new TypeReference<List<Bag<String>>>() {
        };
        JavaType type = TypeFactory.defaultInstance().constructType(bagType);

        final Serializer serializer = LiveboxJacksonSerializer.create(new ObjectMapper());
        final List<Bag<String>> bags = serializer.deserialize(source, type);

        final BufferedSource serializeSource = serializer.serialize(bags, type);

        Assert.assertTrue(serializeSource.readByteArray().length > 0);
        Assert.assertEquals(600, bags.size());
    }

}
