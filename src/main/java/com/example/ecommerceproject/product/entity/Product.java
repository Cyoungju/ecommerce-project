package com.example.ecommerceproject.product.entity;

import com.example.ecommerceproject.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 1000)
    private String content;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus productStatus; // 기본값 AVAILABLE

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;


    @Builder
    public Product(Long id, String name, String content, Long price, int stock, ProductStatus productStatus) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.price = price;
        this.stock = stock;
        this.productStatus = productStatus;
    }
}
