package org.example.meetingroomreservation.room.domain.port.in;

/**
 * Datos necesarios para crear una sala. El estado no viaja: toda sala nace activa.
 */
public record CreateRoomCommand(String name, int capacity) {
}
