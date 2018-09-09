package com.creations.livebox;


import com.creations.livebox.serializers.LiveboxGsonSerializer;
import com.creations.livebox.serializers.Serializer;
import com.creations.livebox.util.Bag;
import com.google.gson.reflect.TypeToken;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okio.BufferedSource;
import okio.Okio;

/**
 * Unit tests for {@link LiveboxGsonSerializer}
 */
public class LiveboxGsonSerializerTest {

    @Test
    public void serializeDeserializeBag() {

        // Setup
        final List<String> values = new ArrayList<>();
        values.add("one");
        values.add("two");
        values.add("three");
        final Bag<String> bag = new Bag<>("100", values);

        // Exercise
        final Serializer<Bag<String>> serializer = LiveboxGsonSerializer.create(Bag.class);
        final Bag newBag = serializer.deserialize(serializer.serialize(bag));

        // Verify
        Assert.assertEquals(bag, newBag);

    }

    @Test
    public void deserializeBigJson() throws IOException {

        // Setup
        InputStream is = getClass().getClassLoader().getResourceAsStream("bigJson.json");
        BufferedSource source = Okio.buffer(Okio.source(is));

        // Exercise
        TypeToken<List<Bag<String>>> bagType = new TypeToken<List<Bag<String>>>() {
        };
        final Serializer<List<Bag<String>>> serializer = LiveboxGsonSerializer.create(bagType);
        final List<Bag<String>> bags = serializer.deserialize(source);

        final BufferedSource serializeSource = serializer.serialize(bags);

        Assert.assertTrue(serializeSource.readByteArray().length > 0);
        Assert.assertEquals(600, bags.size());
    }

}