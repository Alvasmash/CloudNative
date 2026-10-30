package cl.duoc.pedidos360.accounts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer_accounts")
public class Account {
    @Id public String id = java.util.UUID.randomUUID().toString();
    @Column(nullable = false, unique = true, length = 400) public String identityKey;
    @Column(nullable = false, length = 254) public String email;
    @Column(nullable = false, length = 120) public String nombre;
    @Column(nullable = false) public String proveedor;
    @Column(nullable = false) public boolean verificado;
    @Column(nullable = false) public boolean administrador;
    @Column(nullable = false) public Instant fechaRegistro = Instant.now();
    public Instant ultimoAcceso;
    @JsonIgnore public String passwordHash;
    @JsonIgnore public String verificationHash;
    @JsonIgnore public Instant verificationExpires;
}
