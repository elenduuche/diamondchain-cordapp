package com.idt;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import static com.idt.DiamondChainContract.DC_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.security.PublicKey;
import java.util.List;

/**
 * Define your flow here.
 */
public class DiamondCreateFlow {
    /**
     * You can add a constructor to each FlowLogic subclass to pass objects into the flow.
     */
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        /**
         * Define the fields to be used to initialise the DiamondAssetState
         */
        private final UniqueIdentifier linearId;
        private final DiamondType diamondType; //compulsory
        private final String description; //compulsory
        private final double carats;
        private final double cost;
        private final float percent;
        private final Party approver;
        /**
         * The progress tracker provides checkpoints indicating the progress of the flow to observers.
         */
        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new DiamondAsset.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        /**
         * Define constructor to pass initialize DiamondAssetState object for the flow
         */
        public Initiator(String externalId, String description, double carats, double cost, float percent, Party approver){
            this.linearId = UniqueIdentifier.Companion.fromString(externalId);
            this.diamondType = DiamondType.SIGHT;
            this.description = description;
            this.carats=carats;
            this.cost=cost;
            this.percent=percent;
            this.approver=approver;
        }

        @Suspendable
        @Override public SignedTransaction call() throws FlowException {
            //We retrieve notary identity from the network map.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            //Stage 1 - Generating the transaction
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            //We create a transaction builder.
            final TransactionBuilder txBuilder = new TransactionBuilder();
            txBuilder.setNotary(notary);
            //We create the transaction components
            DiamondAssetState diamondAssetState = new DiamondAssetState(linearId, diamondType, getOurIdentity(),
                    description, carats, cost, percent, approver);
            StateAndContract outputStateAndContract = new StateAndContract(diamondAssetState, DC_CONTRACT_ID);
            List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), approver.getOwningKey());
            final Command<DiamondChainContract.Commands.Create> createCmd = new Command<DiamondChainContract.Commands.Create>(
                    new DiamondChainContract.Commands.Create(), requiredSigners);
            //We add items to builder
            txBuilder.withItems(outputStateAndContract, createCmd);
            //Stage 2 - Verify transaction
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            //Verifying the transaction
            txBuilder.verify(getServiceHub());

            //Stage 3 - Signing transaction
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            //Signing the transaction
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            //Stage 4 - Gathering Signatures
            progressTracker.setCurrentStep(GATHERING_SIGS);
            //Creating a session with the other party
            FlowSession approverPartySession = initiateFlow(approver);

            //Obtaining Counterparty's signature
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    ImmutableList.of(approverPartySession), CollectSignaturesFlow.tracker()));
            //Stage 5 - Finalising Transaction
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            //Finalising the transaction
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession approverPartySession;

        public Responder(FlowSession approverPartySession) {
            this.approverPartySession = approverPartySession;
        }

        /**
         * Define the acceptor's flow logic here.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException{
            class SignTxFlow extends SignTransactionFlow{
                private SignTxFlow(FlowSession approverPartySession, ProgressTracker progressTracker){
                    super(approverPartySession, progressTracker);
                }
                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require->{
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be a DiamondAssetState", output instanceof DiamondAssetState);
                        DiamondAssetState diamondAsset = (DiamondAssetState) output;
                        require.using("Approval status must be PENDING", diamondAsset.getApprovalStatus()==ApprovalStatus.PENDING);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(approverPartySession, SignTransactionFlow.Companion.tracker()));
        }
    }
}
