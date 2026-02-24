package com.wolffsoft.jdrivenecommerce.outbox;

import org.apache.avro.specific.SpecificRecord;

import java.util.Map;

public final class OutboxEventTypeRegistry {

    private final Map<String, Class<? extends SpecificRecord>> typeToClass;
    private final Map<String, String> typeToTopic;

    public OutboxEventTypeRegistry(
            Map<String, Class<? extends SpecificRecord>> typeToClass,
            Map<String, String> typeToTopic) {
        this.typeToClass = Map.copyOf(typeToClass);
        this.typeToTopic = Map.copyOf(typeToTopic);
    }

    public Class<? extends SpecificRecord> eventClass(String eventType) {
        Class<? extends SpecificRecord> clazz = typeToClass.get(eventType);
        if (clazz == null) {
            throw new IllegalStateException(String.format("Unknown eventType [%s]" , eventType));
        }
        return clazz;
    }

    public String topic(String eventType) {
        String topic = typeToTopic.get(eventType);
        if (topic == null) {
            throw new IllegalStateException(String.format("No topic mapping for eventType [%s]",
                    eventType));
        }
        return topic;
    }
}
