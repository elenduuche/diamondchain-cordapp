package com.idt;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;
//import java.util.UUID;

/**
 * Define your state object here.
 */
public class DiamondAssetState implements LinearState, QueryableState {
    private final UniqueIdentifier linearId; //compulsory
    private final DiamondType diamondType; //compulsory
    private final Party source; //compulsory
    private final double boxValue; //compulsory
    private final LocalDateTime dateSold;
    private final String description; //compulsory
    private final int creditDuration;
    private final double carats;
    private final double cost;
    private final float percent;
    private final double amount;

    public void setOwner(Party owner) {
        this.owner = owner;
    }

    private Party owner;
    private final LocalDateTime dateIssued; //compulsory
    private final LocalDateTime lastPurchaseDate;
    private final Party approver;
    private final LocalDateTime approvalDate;

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    private ApprovalStatus approvalStatus;

    public DiamondType getDiamondType() {
        return diamondType;
    }

    public Party getSource() {
        return source;
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

    public Party getOwner() {
        return owner;
    }

    public LocalDateTime getDateIssued() {
        return dateIssued;
    }

    public LocalDateTime getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public Party getApprover() {
        return approver;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }



    public DiamondAssetState(UniqueIdentifier linearId, DiamondType diamondType, Party source, String description, double carats, double cost, float percent, Party approver) {
        this.linearId = linearId;
        this.diamondType = diamondType;
        this.source = source;
        this.boxValue = cost + percent;
        this.amount = boxValue;
        this.description = description;
        this.carats = carats;
        this.cost = cost;
        this.percent = percent;
        this.approver = approver;
        this.dateSold = null;
        this.creditDuration = 0;
        this.owner = source;
        this.dateIssued = LocalDateTime.now();
        this.lastPurchaseDate = null;
        this.approvalDate = null;
        this.approvalStatus = ApprovalStatus.PENDING;
    }

    /** The public keys of the involved parties. */
    @Override public List<AbstractParty> getParticipants() {
        return ImmutableList.of(source, owner, approver);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new DiamondAssetSchemaV1());
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if(schema instanceof DiamondAssetSchemaV1){
            return new DiamondAssetSchemaV1.PersistentDiamondAsset(
                    this.linearId.getId(),
                    this.diamondType,
                    this.source.getName().toString(),
                    this.owner.getName().toString(),
                    this.boxValue,
                    this.dateSold,
                    this.description,
                    this.creditDuration,
                    this.carats,
                    this.cost,
                    this.percent,
                    this.amount,
                    this.dateIssued,
                    this.lastPurchaseDate,
                    this.approver.getName().toString(),
                    this.approvalDate,
                    this.approvalStatus);
        }
        else{
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }
    @Override
    public String toString(){
        return String.format("DiamondAssetState(Type=%s, linearId=%s, source=%s, approver=%s, carats=%s, cost=% issuance date=%", diamondType, linearId, source, approver, carats, cost, dateIssued);
    }
}