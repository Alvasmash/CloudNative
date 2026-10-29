package cl.duoc.pedidos360.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cl.duoc.pedidos360.dto.PedidoCreateDTO;
import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.repository.PedidoRepository;

@Service
public class GuestOrderService {
    private final PedidoService orders;
    private final PedidoRepository repository;
    public GuestOrderService(PedidoService orders, PedidoRepository repository) {
        this.orders = orders; this.repository = repository;
    }

    @Transactional
    public Pedido create(PedidoCreateDTO dto, String token) {
        String hash = hash(token);
        Pedido order = orders.crearPedido(dto, null);
        order.setGuestAccessHash(hash);
        return repository.save(order);
    }
    @Transactional(readOnly = true)
    public List<Pedido> list(String token) { return repository.findByGuestAccessHashOrderByFechaCreacionDesc(hash(token)); }
    @Transactional(readOnly = true)
    public Optional<Pedido> find(Long id, String token) { return repository.findByIdAndGuestAccessHash(id, hash(token)); }

    private String hash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Acceso de invitado no válido");
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
