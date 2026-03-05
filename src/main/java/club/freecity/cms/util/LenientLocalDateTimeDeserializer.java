package club.freecity.cms.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final List<DateTimeFormatter> FORMATTERS = new ArrayList<>();
    static {
        FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null) return null;
        String v = text.trim();
        if (v.isEmpty()) return null;
        for (DateTimeFormatter f : FORMATTERS) {
            try {
                return LocalDateTime.parse(v, f);
            } catch (Exception ignored) {
            }
        }
        try {
            LocalDate d = LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
            return d.atStartOfDay();
        } catch (Exception ignored) {
        }
        try {
            LocalDate d = LocalDate.parse(v, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            return d.atStartOfDay();
        } catch (Exception ignored) {
        }
        return (LocalDateTime) ctxt.handleWeirdStringValue(LocalDateTime.class, v, "Unsupported date time format");
    }
}

