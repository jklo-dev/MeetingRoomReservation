package org.example.meetingroomreservation;

import org.springframework.boot.SpringApplication;

public class TestMeetingRoomReservationApplication {

    public static void main(String[] args) {
        SpringApplication.from(MeetingRoomReservationApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
