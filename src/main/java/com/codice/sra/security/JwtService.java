package com.codice.sra.security;

import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.DocenteRepository;
import com.codice.sra.repositories.EmpleadoRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    private final EmpleadoRepository empleadoRepository;
    private final DocenteRepository docenteRepository;

    public JwtService(EmpleadoRepository empleadoRepository, DocenteRepository docenteRepository) {
        this.empleadoRepository = empleadoRepository;
        this.docenteRepository = docenteRepository;
    }

    public String generateToken(Usuario usuario) {
        return generateToken(usuario, null);
    }

    public String generateToken(Usuario usuario, Long idSesion) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("idUsuario", usuario.getIdUsuario());

        String rol = usuario.getRol().getRol();
        extraClaims.put("rol", rol);

        if (idSesion != null) {
            extraClaims.put("idSesion", idSesion);
        }

        if ("EMPLEADO".equalsIgnoreCase(rol) ||
                "FINANZAS".equalsIgnoreCase(rol) ||
                "ADMINISTRADOR".equalsIgnoreCase(rol)
        ) {

            empleadoRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                    .ifPresent(empleado -> {
                        if (empleado.getSede() != null) {
                            extraClaims.put("idSede", empleado.getSede().getIdSede());
                            extraClaims.put("nombreSede", empleado.getSede().getNombreSede());
                        }
                        if (empleado.getCargo() != null) {
                            extraClaims.put("idCargo", empleado.getCargo().getIdCargo());
                            extraClaims.put("nombreCargo", empleado.getCargo().getCargo());
                        }
                    });
        } else if ("DOCENTE".equalsIgnoreCase(rol)) {
            docenteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                    .ifPresent(docente -> {
                        if (docente.getSede() != null) {
                            extraClaims.put("idSede", docente.getSede().getIdSede());
                        }
                    });
        }

        return generateToken(extraClaims, usuario.getCorreoInstitucional());
    }

    public String generateToken(Map<String, Object> extraClaims, String username) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Long extractIdUsuario(String token) {
        return extractClaim(token, claims -> claims.get("idUsuario", Long.class));
    }

    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    public Long extractIdSesion(String token) {
        return extractClaim(token, claims -> claims.get("idSesion", Long.class));
    }

    public Long extractIdSede(String token) {
        return extractClaim(token, claims -> claims.get("idSede", Long.class));
    }

    public Long extractIdCargo(String token) {
        return extractClaim(token, claims -> claims.get("idCargo", Long.class));
    }

    public boolean isTokenValid(String token, String username) {
        try {
            final String tokenUsername = extractUsername(token);
            return (tokenUsername.equals(username)) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}