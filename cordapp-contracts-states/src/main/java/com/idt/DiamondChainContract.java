package com.idt;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
//import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * Define your contract here.
 */
public class DiamondChainContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String DC_CONTRACT_ID = "com.idt.DiamondChainContract";
    /**
     * A transaction is considered valid if the verify() function of the contract of each of the transaction's input
     * and output states does not throw an exception.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        //final CommandWithParties<DiamondChainContract.Commands> cmd = requireSingleCommand(tx.getCommands(), DiamondChainContract.Commands.class);
        final CommandWithParties<CommandData> cmd = tx.getCommands().get(0);
        //#1-Verify Create
        if(tx.commandsOfType(DiamondChainContract.Commands.Create.class).get(0)!=null) {
            requireThat(check -> {
                check.using("No inputs should be consumed when issuing the asset", tx.getInputs().isEmpty());
                check.using("There should be one output state of the type DiamondAssetState", tx.getOutputs().size()==1);
                //DiamondAssetState specific constraints
                final DiamondAssetState out = tx.outputsOfType(DiamondAssetState.class).get(0);
                final Party source = out.getSource();
                final Party approver = out.getApprover();
                check.using("Source must not be the same as Approver", source!=approver);
                check.using("Approval status is Pending", out.getApprovalStatus()==ApprovalStatus.PENDING);
                return null;
            });
        }
        //#2-Verify Approve
        else if(tx.commandsOfType(DiamondChainContract.Commands.Approve.class).get(0)!=null){
            requireThat(check -> {
                check.using("Input should be consumed when approving the asset", tx.getInputs().size()==1);
                final DiamondAssetState input = (DiamondAssetState) tx.getInput(0);
                check.using("There should be one output state of the type DiamondAssetState", tx.getOutputs().size()==1);
                //DiamondAssetState specific constraints
                final DiamondAssetState out = tx.outputsOfType(DiamondAssetState.class).get(0);
                final Party source = out.getSource();
                final Party approver = out.getApprover();
                check.using("Source must not be the same as Approver", source!=approver);
                check.using("Approver is the same as the owning party and not null", approver!=null);
                check.using("The previous approval status should be PENDING", input.getApprovalStatus()==ApprovalStatus.PENDING);
                check.using("Approval status is Approved", out.getApprovalStatus()==ApprovalStatus.APPROVED);
                return null;
            });
        }
        //#3-Verify Decline
        else if(tx.commandsOfType(DiamondChainContract.Commands.Decline.class).get(0)!=null){
            requireThat(check -> {
                check.using("Input should be consumed when approving the asset", tx.getInputs().size()==1);
                final DiamondAssetState input = (DiamondAssetState) tx.getInput(0);
                check.using("There should be one output state of the type DiamondAssetState", tx.getOutputs().size()==1);
                //DiamondAssetState specific constraints
                final DiamondAssetState out = tx.outputsOfType(DiamondAssetState.class).get(0);
                final Party source = out.getSource();
                final Party approver = out.getApprover();
                check.using("Source must not be the same as Approver", source!=approver);
                check.using("Approver is the same as the owning party and not null", approver!=null);
                check.using("The previous approval status should be PENDING", input.getApprovalStatus()==ApprovalStatus.PENDING);
                check.using("Approval status is Declined", out.getApprovalStatus()==ApprovalStatus.DECLINED);
                return null;
            });
        }
        //#4-Verify Transfer
        else if(tx.commandsOfType(DiamondChainContract.Commands.Transfer.class).get(0)!=null){
            requireThat(check -> {
                check.using("Input should be consumed when approving the asset", tx.getInputs().size()==1);
                check.using("There should be one output state of the type DiamondAssetState", tx.getOutputs().size()==1);
                //DiamondAssetState specific constraints
                final DiamondAssetState out = tx.outputsOfType(DiamondAssetState.class).get(0);
                final Party newOwner = out.getOwner();
                final DiamondAssetState in=tx.inputsOfType(DiamondAssetState.class).get(0);
                final Party previousOwner = in.getOwner();
                check.using("New owner must not be the same as the previous owner", newOwner!=previousOwner);
                check.using("Approval status is Approved", out.getApprovalStatus()==ApprovalStatus.APPROVED);
                return null;
            });
        }
        //#5-Verify Update
        else if(tx.commandsOfType(DiamondChainContract.Commands.Update.class).get(0)!=null){
            requireThat(check -> {
                check.using("Input should be consumed when approving the asset", tx.getInputs().size()==1);
                check.using("There should be one output state of the type DiamondAssetState", tx.getOutputs().size()==1);
                //DiamondAssetState specific constraints
                final DiamondAssetState out = tx.outputsOfType(DiamondAssetState.class).get(0);
                check.using("Approval status is Approved", out.getApprovalStatus()==ApprovalStatus.APPROVED);
                return null;
            });
        }
        else{
            throw new UnsupportedOperationException("Unrecognised command");
        }
    }

    public interface Commands extends CommandData {
        public class Create implements Commands {
            @Override
            public boolean equals(Object obj){
                return obj instanceof Create;
            }
        }
        public class Approve implements Commands {
            @Override
            public boolean equals(Object obj){
                return obj instanceof Approve;
            }
        }
        public class Decline implements Commands {
            @Override
            public boolean equals(Object obj){
                return obj instanceof Decline;
            }
        }
        public class Transfer implements Commands {
            @Override
            public boolean equals(Object obj){
                return obj instanceof Transfer;
            }
        }
        public class Update implements Commands {
            @Override
            public boolean equals(Object obj){
                return obj instanceof Update;
            }
        }
    }
}