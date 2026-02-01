package com.archiveat.server.domain.newsletter.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "domains")
public class Domain {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Youtube, Naver News, Branch ...

    public Domain(String name){
        this.name = name;
    }
}