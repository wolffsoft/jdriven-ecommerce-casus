package com.wolffsoft.jdrivenecommerce.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = createMapper();

    // -----------------------------
    // Jackson JSON (for normal DTOs)
    // -----------------------------

    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize object to JSON", ex);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize JSON", ex);
        }
    }

    // -----------------------------
    // Avro JSON (for SpecificRecord)
    // -----------------------------

    public static String toAvroJson(SpecificRecord record) {
        Schema schema = record.getSchema();
        DatumWriter<SpecificRecord> writer = new SpecificDatumWriter<>(schema);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Encoder encoder = EncoderFactory.get().jsonEncoder(schema, out);
            writer.write(record, encoder);
            encoder.flush();
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException(String.format("Failed to Avro-JSON encode record: %s",
                    schema.getFullName()), ex);
        }
    }

    public static <T extends SpecificRecord> T fromAvroJson(String json, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            Schema schema = instance.getSchema();

            DatumReader<T> reader = new SpecificDatumReader<>(schema);
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json);

            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to Avro-JSON decode into: %s", clazz.getName()), e);
        }
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
