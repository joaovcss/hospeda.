package com.hotelaria.mappers;

import com.hotelaria.dtos.request.RoomRequest;
import com.hotelaria.dtos.response.RoomResponse;
import com.hotelaria.models.Room;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

    public Room toEntity(RoomRequest request) {
        return new Room(
                normalizeNumber(request.number()),
                request.floor(),
                request.capacity(),
                request.category(),
                request.dailyRate(),
                request.description());
    }

    public void updateEntity(Room room, RoomRequest request) {
        room.setNumber(normalizeNumber(request.number()));
        room.setFloor(request.floor());
        room.setCapacity(request.capacity());
        room.setCategory(request.category());
        room.setDailyRate(request.dailyRate());
        room.setDescription(request.description());
    }

    public RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getNumber(),
                room.getFloor(),
                room.getCapacity(),
                room.getCategory(),
                room.getDailyRate(),
                room.getStatus(),
                room.getDescription());
    }

    public String normalizeNumber(String number) {
        return number.trim().toUpperCase();
    }
}
