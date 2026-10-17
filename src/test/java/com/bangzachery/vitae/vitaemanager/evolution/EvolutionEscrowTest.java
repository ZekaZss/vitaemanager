package com.bangzachery.vitae.vitaemanager.evolution;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStoreTest.*;

class EvolutionEscrowTest {
    @Test void preparedCursorCommitsOnlyAfterMatchingWithdrawalReceipt() {
        UUID id=UUID.randomUUID(),receipt=UUID.randomUUID();
        Deposit d=new Deposit(id,OWNER,"api",altar().table(),Map.of(),new Transfer(receipt,ITEM,Map.of(0,ITEM)));
        State pending=initial().deposit(d);assertTrue(pending.deposits().get(OWNER).items().isEmpty());
        State committed=pending.settle(OWNER,id,receipt,true);assertEquals(Map.of(0,ITEM),committed.deposits().get(OWNER).items());
        assertNull(committed.deposits().get(OWNER).transfer());assertFalse(pending.settle(OWNER,id,receipt,false).deposits().containsKey(OWNER));
        assertThrows(IllegalArgumentException.class,()->committed.settle(OWNER,id,receipt,true));
        assertThrows(IllegalArgumentException.class,()->pending.settle(OWNER,id,UUID.randomUUID(),true));
    }
    @Test void closingBeforeWithdrawalKeepsAllPreviousDeposits() {
        UUID id=UUID.randomUUID(),receipt=UUID.randomUUID();
        Deposit d=new Deposit(id,OWNER,"api",altar().table(),Map.of(0,ITEM),new Transfer(receipt,ITEM,Map.of(0,ITEM,8,ITEM)));
        State next=initial().deposit(d).settle(OWNER,id,receipt,false);
        assertEquals(Map.of(0,ITEM),next.deposits().get(OWNER).items());assertNull(next.deposits().get(OWNER).transfer());
    }
    @Test void oneAltarCannotContainTwoOwnersOrBeDeletedWhileHoldingItems() {
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM),null);
        State state=initial().deposit(d);
        assertThrows(IllegalArgumentException.class,()->state.deposit(new Deposit(UUID.randomUUID(),UUID.randomUUID(),"api",altar().table(),Map.of(0,ITEM),null)));
        assertThrows(IllegalArgumentException.class,()->state.remove("api"));assertEquals(Map.of(0,ITEM),state.deposits().get(OWNER).items());
    }
    @Test void escrowMovesIntoRitualExactlyOnceWithoutChangingInputs() {
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM,8,"Ag=="),null);State state=initial().deposit(d);
        Ticket t=new Ticket(UUID.randomUUID(),OWNER,"api",altar().table(),Phase.PREPARED,List.of(ITEM,"Ag=="),"Aw==");
        State begun=state.beginDeposit(t,d.id());assertTrue(begun.deposits().isEmpty());assertEquals(t,begun.tickets().get(OWNER));
        assertThrows(IllegalArgumentException.class,()->begun.beginDeposit(t,d.id()));
        Ticket wrong=new Ticket(t.id(),OWNER,"api",altar().table(),Phase.PREPARED,List.of("BA=="),"Aw==");
        assertThrows(IllegalArgumentException.class,()->state.beginDeposit(wrong,d.id()));
    }
    @Test void unsettledTransferCannotStartAndDifferentReceiptCannotRemoveItems() {
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM),new Transfer(UUID.randomUUID(),ITEM,Map.of(0,ITEM,8,ITEM)));
        State state=initial().deposit(d);Ticket t=new Ticket(UUID.randomUUID(),OWNER,"api",altar().table(),Phase.PREPARED,List.of(ITEM),ITEM);
        assertThrows(IllegalArgumentException.class,()->state.beginDeposit(t,d.id()));assertThrows(IllegalArgumentException.class,()->state.removeDeposit(OWNER,UUID.randomUUID()));
    }
    @Test void committedOrRefundedTicketCannotPayAgain() {
        Ticket t=new Ticket(UUID.randomUUID(),OWNER,"api",altar().table(),Phase.PREPARED,List.of(ITEM),ITEM);
        State active=initial().prepare(t).phase(OWNER,t.id(),Phase.ACTIVE);
        assertThrows(IllegalArgumentException.class,()->active.phase(OWNER,t.id(),Phase.DELIVERED));
        State done=active.phase(OWNER,t.id(),Phase.COMMITTING).phase(OWNER,t.id(),Phase.DELIVERED);
        assertThrows(IllegalArgumentException.class,()->done.phase(OWNER,t.id(),Phase.REFUNDING));assertThrows(IllegalArgumentException.class,()->done.phase(OWNER,t.id(),Phase.DELIVERED));
    }
}
