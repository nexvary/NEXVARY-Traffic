package com.nexvary.trafficlab.core;

import java.util.*;

/** Infers presentation-friendly controls for structured fields in the authorized local lab. */
public final class SchemaAwareEditor {
    public enum Control { STEPPER, TOGGLE, CHOICE, TEXT, RAW }
    public record FieldHint(String key, StructuredBodyEditor.Kind kind, Control control, List<String> choices, String explanation) {}

    private SchemaAwareEditor() {}

    public static List<FieldHint> infer(List<StructuredBodyEditor.Field> fields) {
        if (fields == null || fields.isEmpty()) return List.of();
        List<FieldHint> out = new ArrayList<>();
        for (StructuredBodyEditor.Field f : fields) {
            String k = f.key().toLowerCase(Locale.ROOT);
            Control c;
            List<String> choices = List.of();
            String why;
            if (f.kind() == StructuredBodyEditor.Kind.NUMBER) {
                c = Control.STEPPER;
                why = "Numeric field: use +/- controls and quick values.";
            } else if (f.kind() == StructuredBodyEditor.Kind.BOOLEAN) {
                c = Control.TOGGLE;
                why = "Boolean field: use a toggle instead of typing.";
            } else if (f.kind() == StructuredBodyEditor.Kind.STRING && (k.equals("role") || k.equals("status") || k.equals("item") || k.equals("type"))) {
                c = Control.CHOICE;
                choices = choicesFor(k, f.value());
                why = "Known categorical field: use safe lecture presets.";
            } else if (f.kind() == StructuredBodyEditor.Kind.STRING) {
                c = Control.TEXT;
                why = "Free text field: edit directly.";
            } else {
                c = Control.RAW;
                why = "Nested/raw value: preserve exact syntax.";
            }
            out.add(new FieldHint(f.key(), f.kind(), c, choices, why));
        }
        return List.copyOf(out);
    }

    private static List<String> choicesFor(String key, String current) {
        LinkedHashSet<String> x = new LinkedHashSet<>();
        if (current != null && !current.isBlank()) x.add(current);
        switch (key) {
            case "item" -> { x.add("TE70"); x.add("demo"); x.add("test"); }
            case "role" -> { x.add("viewer"); x.add("editor"); x.add("demo"); }
            case "status" -> { x.add("draft"); x.add("active"); x.add("demo"); }
            case "type" -> { x.add("standard"); x.add("demo"); x.add("test"); }
            default -> {}
        }
        return List.copyOf(x);
    }
}
