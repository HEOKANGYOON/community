package com.kangyoon.community.domain.board.entity;

import com.kangyoon.community.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseEntity {

    private Board(String name, String description, BoardCategory boardCategory) {
        this.name = name;
        this.description = description;
        this.boardCategory = boardCategory;
    }

    public static Board create(String name, String description, BoardCategory boardCategory) {
        new Board(name, description, boardCategory);
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String description;
    @ManyToOne @JoinColumn(name = "category_id", updatable = false) BoardCategory boardCategory;
    private LocalDateTime deletedAt;


    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateCategory(BoardCategory boardCategory) {
        this.boardCategory = boardCategory;
    }

}
