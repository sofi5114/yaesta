package pe.upn.yaesta.dto;
import jakarta.validation.constraints.NotNull;
public record PreferenciasRequest(
    @NotNull(message = "Indica si aceptas recordatorios")
    Boolean aceptaRecordatorios
) {}
