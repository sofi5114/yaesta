package pe.upn.yaesta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Jsend<T>(
    String status,
    T data,
    String message
) {

    public static <T> Jsend<T> success(T data) {
        return new Jsend<>("success", data, null);
    }

    public static <T> Jsend<T> fail(T data) {
        return new Jsend<>("fail", data, null);
    }

    public static Jsend<Void> error(String message) {
        return new Jsend<>("error", null, message);
    }
}