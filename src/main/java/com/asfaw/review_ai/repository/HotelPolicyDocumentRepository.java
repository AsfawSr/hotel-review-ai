package com.asfaw.review_ai.repository;

import com.asfaw.review_ai.model.entity.HotelPolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelPolicyDocumentRepository extends JpaRepository<HotelPolicyDocument, Long> {

    List<HotelPolicyDocument> findAllByActiveTrue();
}

