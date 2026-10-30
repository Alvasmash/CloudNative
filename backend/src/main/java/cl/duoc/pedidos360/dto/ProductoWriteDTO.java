package cl.duoc.pedidos360.dto;

import jakarta.validation.constraints.*;

public record ProductoWriteDTO(
    @NotBlank @Size(max = 120) String nombre,
    @NotBlank @Size(max = 500) String descripcion,
    @NotNull @DecimalMin("1") @DecimalMax("99999999") @Digits(integer = 8, fraction = 0) Double precio,
    @NotBlank @Size(max = 80) String categoria,
    @NotNull @Min(0) @Max(1000000) Integer stock,
    @Pattern(regexp = "(?:images/product-[1-6]\\.jpg)?") String imagenUrl
) {}
