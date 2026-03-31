package com.ued.distributedsystem.repository;

import com.ued.distributedsystem.model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookingRepository extends MongoRepository<Booking, String> {
}