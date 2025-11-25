package com.controlmedicamentos.myapplication.models;

import java.util.ArrayList;
import java.util.List;

public class Usuario {
    private String id;
    private String nombre;
    private String email;
    private String telefono;
    private int edad;
    private List<String> medicamentosIds;
    private String role; // "paciente"

    // Constructor
    public Usuario() {
        this.medicamentosIds = new ArrayList<>();
    }

    // Constructor con parámetros
    public Usuario(String id, String nombre, String email, String telefono, int edad) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.edad = edad;
        this.medicamentosIds = new ArrayList<>();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public List<String> getMedicamentosIds() {
        return medicamentosIds;
    }

    public void setMedicamentosIds(List<String> medicamentosIds) {
        this.medicamentosIds = medicamentosIds;
    }

    // Métodos útiles
    public void agregarMedicamento(String medicamentoId) {
        if (!medicamentosIds.contains(medicamentoId)) {
            medicamentosIds.add(medicamentoId);
        }
    }

    public void eliminarMedicamento(String medicamentoId) {
        medicamentosIds.remove(medicamentoId);
    }

    public boolean tieneMedicamento(String medicamentoId) {
        return medicamentosIds.contains(medicamentoId);
    }

    public int getCantidadMedicamentos() {
        return medicamentosIds.size();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Métodos útiles para roles
    public boolean esPaciente() {
        return "paciente".equals(role);
    }
}
