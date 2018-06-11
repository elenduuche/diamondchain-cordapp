package com.idt;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A DiamondAssetState schema.
 * */

public class DiamondAssetSchemaV1 extends MappedSchema {
    public DiamondAssetSchemaV1(){
        super(DiamondAssetSchema.class, 1, ImmutableList.of(PersistentDiamondAsset.class));

    }
    @Entity
    @Table(name="diamondasset_states")
    public static class PersistentDiamondAsset extends PersistentState{
        public UUID getLinearId() {
            return linearId;
        }

        public DiamondType getDiamondType() {
            return diamondType;
        }

        public String getSource() {
            return source;
        }

        public String getOwner() {
            return owner;
        }

        public double getBoxValue() {
            return boxValue;
        }

        public LocalDateTime getDateSold() {
            return dateSold;
        }

        public String getDescription() {
            return description;
        }

        public int getCreditDuration() {
            return creditDuration;
        }

        public double getCarats() {
            return carats;
        }

        public double getCost() {
            return cost;
        }

        public float getPercent() {
            return percent;
        }

        public double getAmount() {
            return amount;
        }

        public LocalDateTime getDateIssued() {
            return dateIssued;
        }

        public LocalDateTime getLastPurchaseDate() {
            return lastPurchaseDate;
        }

        public String getApprover() {
            return approver;
        }

        public LocalDateTime getApprovalDate() {
            return approvalDate;
        }

        public ApprovalStatus getApprovalStatus() {
            return approvalStatus;
        }

        @Column(name="linear_id") private final UUID linearId; //compulsory
        @Enumerated(EnumType.STRING)
        @Column(name="diamond_type")
        private final DiamondType diamondType; //compulsory
        @Column(name="source") private final String source; //compulsory
        @Column(name="owner") private final String owner;
        @Column (name="box_value") private final double boxValue; //compulsory
        @Column(name="date_sold") private final LocalDateTime dateSold;
        @Column private final String description; //compulsory
        @Column(name="credit_duration") private final int creditDuration;
        @Column private final double carats;
        @Column private final double cost;
        @Column private final float percent;
        @Column private final double amount;
        @Column(name="date_issued") private final LocalDateTime dateIssued; //compulsory
        @Column(name="last_purchase_date") private final LocalDateTime lastPurchaseDate;
        @Column private final String approver;
        @Column(name="approval_date") private final LocalDateTime approvalDate;
        @Enumerated(EnumType.STRING)
        @Column(name="approval_status")
        private final ApprovalStatus approvalStatus;

        public PersistentDiamondAsset(UUID linearId, DiamondType diamondType, String source, String owner, double boxValue,
                                      LocalDateTime dateSold, String description, int creditDuration,
                                      double carats, double cost, float percent, double amount, LocalDateTime dateIssued,
                                      LocalDateTime lastPurchaseDate, String approver,
                                      LocalDateTime approvalDate, ApprovalStatus approvalStatus){
            this.linearId = linearId;
            this.diamondType = diamondType;
            this.source = source;
            this.owner = owner;
            this.boxValue = boxValue;
            this.dateSold = dateSold;
            this.description = description;
            this.creditDuration = creditDuration;
            this.carats = carats;
            this.cost=cost;
            this.percent = percent;
            this.amount =amount;
            this.dateIssued = dateIssued;
            this.lastPurchaseDate = lastPurchaseDate;
            this.approver = approver;
            this.approvalDate = approvalDate;
            this.approvalStatus = approvalStatus;
        }
        // Default constructor required by hibernate.
        public PersistentDiamondAsset(){
            this.linearId = null;
            this.diamondType = null;
            this.source = null;
            this.owner = null;
            this.boxValue = 0;
            this.dateSold = null;
            this.description = null;
            this.creditDuration = 0;
            this.carats = 0;
            this.cost=0;
            this.percent = 0;
            this.amount =0;
            this.dateIssued = null;
            this.lastPurchaseDate = null;
            this.approver = null;
            this.approvalDate = null;
            this.approvalStatus = null;
        }
    }

}
