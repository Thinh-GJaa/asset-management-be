package com.concentrix.asset.entity;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailId implements Serializable {
    private Integer transaction;
    private Integer device;
}