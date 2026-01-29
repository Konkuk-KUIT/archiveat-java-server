package com.archiveat.server.domain.explore.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name; // 경제, IT ...

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Topic> topics = new ArrayList<>(); // 초기화를 통해 NullPointerException 방지
}