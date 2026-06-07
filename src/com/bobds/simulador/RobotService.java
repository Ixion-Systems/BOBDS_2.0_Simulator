package com.bobds.simulador;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class RobotService {

    private final Set<String> unidadesTrabajando =
        Collections.synchronizedSet(new HashSet<>());
    private final Semaphore mutex = new Semaphore(1);
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String BACKEND_URL =
        "http://localhost:8081/api/orders/completada";

    public void procesarOrden(String idUnidad, String orden) {
        // Sección crítica: verificar y registrar que la unidad está ocupada
        try {
            mutex.acquire();
            try {
                if (unidadesTrabajando.contains(idUnidad)) {
                    System.out.println("AVISO: Unidad " + idUnidad + " ocupada. Orden ignorada.");
                    return;
                }
                unidadesTrabajando.add(idUnidad);
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // Ejecutar la orden
        try {
            ejecutarOrden(idUnidad, orden);

            // Avisar al backend que terminó
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_URL + "?idUnidad=" + idUnidad))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[!] Unidad " + idUnidad + 
                " terminó. Backend respondió: " + response.statusCode());

        } catch (Exception e) {
            System.err.println("Error avisando al backend: " + e.getMessage());
        } finally {
            // Liberar la unidad con semáforo
            try {
                mutex.acquire();
                try {
                    unidadesTrabajando.remove(idUnidad);
                    System.out.println("Unidad " + idUnidad + " liberada.");
                } finally {
                    mutex.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void ejecutarOrden(String idUnidad, String orden) {
        Random rand = new Random();
        int duracion = 5 + rand.nextInt(16);
        long inicio = System.currentTimeMillis();

        System.out.println(">>> INICIANDO: Unidad " + idUnidad +
            " (" + orden + ") - Duración: " + duracion + "s");

        while ((System.currentTimeMillis() - inicio) < duracion * 1000) {
            // simula trabajo
        }

        System.out.println(">>> FINALIZADO: Unidad " + idUnidad);
    }
}