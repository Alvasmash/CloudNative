package cl.duoc.pedidos360.security;

import org.springframework.data.jpa.repository.JpaRepository;
public interface AdminIdentityRepository extends JpaRepository<AdminIdentityBinding, String> {}
