package pe.upn.yaesta.model;

import jakarta.persistence.*;


@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100,
            columnDefinition = "nvarchar(100)")
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "clave_hash", nullable = false, length = 100)
    private String claveHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "acepta_recordatorios", nullable = false)
    private boolean aceptaRecordatorios = false;

    protected Usuario() {
    }

    public Usuario(String nombre, String correo,
                   String claveHash, Rol rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.claveHash = claveHash;
        this.rol = rol;
    }

    public void cambiarRecordatorios(boolean acepta) {
        aceptaRecordatorios = acepta;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isAceptaRecordatorios() {
        return aceptaRecordatorios;
    }
}