package cl.duoc.pedidos360.security;

import jakarta.persistence.*;

/** Pins an explicitly approved email to the immutable Microsoft directory object. */
@Entity
@Table(name = "admin_identity_bindings")
public class AdminIdentityBinding {
    @Id
    private String email;
    @Column(nullable = false, unique = true)
    private String objectId;
    protected AdminIdentityBinding() {}
    public AdminIdentityBinding(String email, String objectId) { this.email = email; this.objectId = objectId; }
    public String getObjectId() { return objectId; }
}
