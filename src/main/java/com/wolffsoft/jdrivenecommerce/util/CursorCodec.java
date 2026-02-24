package com.wolffsoft.jdrivenecommerce.util;

import co.elastic.clients.elasticsearch._types.FieldValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CursorCodec {

    private final ObjectMapper objectMapper;

    public String encodeCursorFromSort(List<FieldValue> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) {
            throw new IllegalArgumentException("Cannot encode cursor from empty sort values");
        }

        try {
            List<Object> raw = sortValues.stream()
                    .map(this::fieldValueToPrimitive)
                    .toList();

            byte[] json = objectMapper.writeValueAsBytes(raw);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to encode cursor", ex);
        }
    }

    public List<FieldValue> decodeCursorToSearchAfter(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return List.of();
        }

        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            JsonNode node = objectMapper.readTree(json);

            if (!node.isArray()) {
                throw new IllegalArgumentException("Invalid cursor: expected a JSON array");
            }

            List<FieldValue> out = new ArrayList<>(node.size());
            for (JsonNode el : node) {
                out.add(jsonNodeToFieldValue(el));
            }
            return out;

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid cursor", ex);
        }
    }

    private Object fieldValueToPrimitive(FieldValue fieldValue) {
        if (fieldValue == null) return null;

        return switch (fieldValue._kind()) {
            case String -> fieldValue.stringValue();
            case Long -> fieldValue.longValue();
            case Double -> fieldValue.doubleValue();
            case Boolean -> fieldValue.booleanValue();
            case Null -> null;
            default -> throw new IllegalArgumentException(String.format("Unsupported sort cursor value type [%s]",
                    fieldValue._kind()));
        };
    }

    private FieldValue jsonNodeToFieldValue(JsonNode el) {
        return switch (el.getNodeType()) {
            case NULL -> FieldValue.NULL;
            case STRING -> FieldValue.of(el.asText());
            case BOOLEAN -> FieldValue.of(el.asBoolean());
            case NUMBER -> {
                if (el.isIntegralNumber()) yield FieldValue.of(el.asLong());
                if (el.isFloatingPointNumber()) yield FieldValue.of(el.asDouble());
                throw new IllegalArgumentException("Invalid cursor: unsupported numeric element " + el.asText());
                throw new IllegalArgumentException(String.format("Invalid cursor: unsupported numeric element %s",
                        jsonNode.asText()));
            }
            default -> throw new IllegalArgumentException(String.format("Invalid cursor: unsupported element type [%s]",
                    jsonNode.getNodeType()));
        };
    }
}
