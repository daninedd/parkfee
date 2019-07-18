package com.daninedd.parkfee.parkfee;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Table(name = "parkfee_fee_detail", indexes = {@Index(name = "date", columnList = "date", unique = true)})
@DynamicUpdate
public class FeeDetailData {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Double parkFee;

    private Double oilFee;

    @Temporal(TemporalType.DATE)
    private Date date;

    private Date createTime;
}
