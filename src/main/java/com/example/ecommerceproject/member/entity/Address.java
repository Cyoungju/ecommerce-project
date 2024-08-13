package com.example.ecommerceproject.member.entity;

import com.example.ecommerceproject.core.utils.BaseTimeEntity;
import com.example.ecommerceproject.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "address")
public class Address extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 300, nullable = false)
    private String addressName;

    @Column(length = 300, nullable = false)
    private String address;

    @Column(length = 300)
    private String detailAdr;

    @Column(length = 100)
    private String phone;

    private boolean defaultAdr;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
}
