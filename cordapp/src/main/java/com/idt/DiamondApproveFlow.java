package com.idt;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.QueryCriteriaUtils;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.List;

import static com.idt.DiamondChainContract.DC_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class DiamondApproveFlow {
    /**
     * You can add a constructor to each FlowLogic subclass to pass objects into the flow.
     */
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        /**
         * Define the fields to be used to initialise the DiamondAssetState
         */
        private final UniqueIdentifier id;
        private final boolean approve;
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
        public Initiator(String externalId, boolean approve){
            //this.externalId = externalId;
            this.id = UniqueIdentifier.Companion.fromString(externalId);
            this.approve = approve;
        }

        @Suspendable
        @Override public SignedTransaction call() throws FlowException {
            //var criteria = new QueryCriteria.LinearStateQueryCriteria()
            QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(ImmutableList.of(getOurIdentity()), ImmutableList.of(id), Vault.StateStatus.UNCONSUMED, ImmutableSet.of(DiamondAssetState.class));
            List<StateAndRef<DiamondAssetState>> stateAndRefs = getServiceHub().getVaultService().queryBy(DiamondAssetState.class, criteria).getStates();
            if(stateAndRefs.size()>1){
                throw new FlowException("External ID returned more than 1 result.");
            }
            //We retrieve notary identity from the network map.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            //Stage 1 - Generating the transaction
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            //We create a transaction builder.
            final TransactionBuilder txBuilder = new TransactionBuilder();
            txBuilder.setNotary(notary);
            //We create the transaction components
            DiamondAssetState diamondAssetState = (DiamondAssetState)stateAndRefs.get(0).getState().getData();
            if(approve){
                diamondAssetState.setApprovalStatus(ApprovalStatus.APPROVED);
            }else{
                diamondAssetState.setApprovalStatus(ApprovalStatus.DECLINED);
            }
            StateAndContract outputStateAndContract = new StateAndContract(diamondAssetState, DC_CONTRACT_ID);
            List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), diamondAssetState.getSource().getOwningKey());
            final Command<DiamondChainContract.Commands.Approve> approveCmd = new Command<DiamondChainContract.Commands.Approve>(
                    new DiamondChainContract.Commands.Approve(), requiredSigners);
            //We add items to builder
            txBuilder.withItems(stateAndRefs.get(0), outputStateAndContract, approveCmd);
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
            FlowSession sourcePartySession = initiateFlow(diamondAssetState.getSource());

            //Obtaining Counterparty's signature
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    ImmutableList.of(sourcePartySession), CollectSignaturesFlow.tracker()));
            //Stage 5 - Finalising Transaction
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            //Finalising the transaction
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(DiamondApproveFlow.Initiator.class)
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
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession sourcePartySession, ProgressTracker progressTracker){
                    super(sourcePartySession, progressTracker);
                }
                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require->{
                        require.using("The inputdata must be of size 1", stx.getTx().getInputs().size()==1);
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
