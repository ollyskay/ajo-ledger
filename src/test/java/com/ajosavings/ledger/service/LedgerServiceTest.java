package com.ajosavings.ledger.service;

import com.ajosavings.ledger.domain.LedgerEntry;
import com.ajosavings.ledger.dto.LedgerEntryRequest;
import com.ajosavings.ledger.exception.UnbalancedTransactionException;
import com.ajosavings.ledger.repository.AccountRepository;
import com.ajosavings.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private AccountRepository accountRepository;

    private LedgerService ledgerService;

    private final UUID memberWallet = UUID.randomUUID();
    private final UUID groupPot = UUID.randomUUID();
    private final UUID fees = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(ledgerEntryRepository, accountRepository);
    }

    @Test
    void postsBalancedTransactionAndReturnsTransactionId() {
        when(accountRepository.existsById(any())).thenReturn(true);

        UUID transactionId = ledgerService.post("contribution", List.of(
                new LedgerEntryRequest(memberWallet, -500_000L, null),
                new LedgerEntryRequest(groupPot, 500_000L, null)
        ));

        assertThat(transactionId).isNotNull();

        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerEntryRepository).saveAll(captor.capture());

        List<LedgerEntry> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(e -> e.getTransactionId().equals(transactionId));
        assertThat(saved.stream().mapToLong(LedgerEntry::getAmountKobo).sum()).isZero();
    }

    @Test
    void postsMultiLegTransactionThatSumsToZero() {
        when(accountRepository.existsById(any())).thenReturn(true);

        UUID transactionId = ledgerService.post("contribution with fee", List.of(
                new LedgerEntryRequest(memberWallet, -500_000L, null),
                new LedgerEntryRequest(groupPot, 480_000L, null),
                new LedgerEntryRequest(fees, 20_000L, null)
        ));

        assertThat(transactionId).isNotNull();
        verify(ledgerEntryRepository).saveAll(any());
    }

    @Test
    void rejectsUnbalancedTransactionAndSavesNothing() {
        when(accountRepository.existsById(any())).thenReturn(true);

        assertThatThrownBy(() -> ledgerService.post("bad", List.of(
                new LedgerEntryRequest(memberWallet, -500_000L, null),
                new LedgerEntryRequest(groupPot, 499_999L, null)
        ))).isInstanceOf(UnbalancedTransactionException.class)
                .hasMessageContaining("sum to zero");

        verify(ledgerEntryRepository, never()).saveAll(any());
    }

    @Test
    void rejectsTransactionWithFewerThanTwoEntries() {
        assertThatThrownBy(() -> ledgerService.post("bad", List.of(
                new LedgerEntryRequest(memberWallet, 500_000L, null)
        ))).isInstanceOf(UnbalancedTransactionException.class)
                .hasMessageContaining("at least two entries");

        verify(ledgerEntryRepository, never()).saveAll(any());
    }

    @Test
    void rejectsTransactionWithNoEntries() {
        assertThatThrownBy(() -> ledgerService.post("bad", List.of()))
                .isInstanceOf(UnbalancedTransactionException.class);

        verify(ledgerEntryRepository, never()).saveAll(any());
    }

    @Test
    void rejectsEntryWithZeroAmount() {
        when(accountRepository.existsById(any())).thenReturn(true);

        assertThatThrownBy(() -> ledgerService.post("bad", List.of(
                new LedgerEntryRequest(memberWallet, 0L, null),
                new LedgerEntryRequest(groupPot, 0L, null)
        ))).isInstanceOf(UnbalancedTransactionException.class)
                .hasMessageContaining("cannot be zero");

        verify(ledgerEntryRepository, never()).saveAll(any());
    }

    @Test
    void rejectsTransactionReferencingUnknownAccount() {
        when(accountRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> ledgerService.post("bad", List.of(
                new LedgerEntryRequest(memberWallet, -500_000L, null),
                new LedgerEntryRequest(groupPot, 500_000L, null)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown account");

        verify(ledgerEntryRepository, never()).saveAll(any());
    }
}
