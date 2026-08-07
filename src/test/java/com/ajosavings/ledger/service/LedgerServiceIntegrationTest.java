package com.ajosavings.ledger.service;

import com.ajosavings.ledger.domain.Account;
import com.ajosavings.ledger.domain.AccountType;
import com.ajosavings.ledger.domain.LedgerEntry;
import com.ajosavings.ledger.dto.LedgerEntryRequest;
import com.ajosavings.ledger.exception.UnbalancedTransactionException;
import com.ajosavings.ledger.repository.AccountRepository;
import com.ajosavings.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises LedgerService against a real Postgres instance (Testcontainers),
 * not H2 - the sum-zero invariant, rollback behaviour, and concurrent
 * inserts all need to be proven against the actual database engine.
 */
@SpringBootTest
@Testcontainers
class LedgerServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private Account memberWallet;
    private Account groupPot;

    @BeforeEach
    void setUp() {
        memberWallet = accountRepository.save(Account.open(AccountType.MEMBER_WALLET, UUID.randomUUID()));
        groupPot = accountRepository.save(Account.open(AccountType.GROUP_POT, UUID.randomUUID()));
    }

    @AfterEach
    void tearDown() {
        ledgerEntryRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void postedBalancedTransactionPersistsAndIsQueryable() {
        UUID transactionId = ledgerService.post("contribution", List.of(
                new LedgerEntryRequest(memberWallet.getId(), -500_000L, null),
                new LedgerEntryRequest(groupPot.getId(), 500_000L, null)
        ));

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transactionId);
        assertThat(entries).hasSize(2);
        assertThat(ledgerEntryRepository.sumAllAmountKobo()).isZero();
        assertThat(ledgerEntryRepository.sumAmountKoboByAccountId(groupPot.getId())).isEqualTo(500_000L);
        assertThat(ledgerEntryRepository.sumAmountKoboByAccountId(memberWallet.getId())).isEqualTo(-500_000L);
    }

    @Test
    void unbalancedTransactionIsRolledBackCompletely() {
        assertThatThrownBy(() -> ledgerService.post("bad", List.of(
                new LedgerEntryRequest(memberWallet.getId(), -500_000L, null),
                new LedgerEntryRequest(groupPot.getId(), 499_999L, null)
        ))).isInstanceOf(UnbalancedTransactionException.class);

        assertThat(ledgerEntryRepository.count()).isZero();
    }

    /**
     * Milestone 1 definition of done: post 1,000 random transactions and
     * assert the sum of all entries is exactly zero.
     */
    @Test
    void oneThousandRandomBalancedTransactionsSumToZero() {
        List<UUID> accountIds = new ArrayList<>(List.of(memberWallet.getId(), groupPot.getId()));
        for (int i = 0; i < 8; i++) {
            accountIds.add(accountRepository.save(
                    Account.open(AccountType.MEMBER_WALLET, UUID.randomUUID())).getId());
        }

        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            ledgerService.post("tx-" + i, randomBalancedEntries(random, accountIds));
        }

        assertThat(ledgerEntryRepository.count()).isGreaterThanOrEqualTo(2000L);
        assertThat(ledgerEntryRepository.sumAllAmountKobo()).isZero();
    }

    @Test
    void concurrentPostsToTheSameAccountsAllPersistWithoutCorruptingTheSum() throws InterruptedException {
        int threadCount = 8;
        int postsPerThread = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            pool.submit(() -> {
                try {
                    for (int i = 0; i < postsPerThread; i++) {
                        ledgerService.post("concurrent contribution", List.of(
                                new LedgerEntryRequest(memberWallet.getId(), -1_000L, null),
                                new LedgerEntryRequest(groupPot.getId(), 1_000L, null)
                        ));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(ledgerEntryRepository.count()).isEqualTo((long) threadCount * postsPerThread * 2);
        assertThat(ledgerEntryRepository.sumAllAmountKobo()).isZero();
    }

    private List<LedgerEntryRequest> randomBalancedEntries(Random random, List<UUID> accountIds) {
        long total = 1_000L + random.nextInt(1_000_000);
        int creditLegs = 1 + random.nextInt(3);
        long[] credits = composeInto(random, total, creditLegs);

        List<LedgerEntryRequest> entries = new ArrayList<>();
        entries.add(new LedgerEntryRequest(pick(random, accountIds), -total, null));
        for (long credit : credits) {
            entries.add(new LedgerEntryRequest(pick(random, accountIds), credit, null));
        }
        return entries;
    }

    /** Splits total into `parts` positive longs that sum to exactly total. */
    private long[] composeInto(Random random, long total, int parts) {
        if (parts == 1) {
            return new long[] {total};
        }
        TreeSet<Long> cuts = new TreeSet<>();
        while (cuts.size() < parts - 1) {
            cuts.add(1L + random.nextInt((int) (total - 1)));
        }
        long[] result = new long[parts];
        long previous = 0;
        int idx = 0;
        for (long cut : cuts) {
            result[idx++] = cut - previous;
            previous = cut;
        }
        result[idx] = total - previous;
        return result;
    }

    private UUID pick(Random random, List<UUID> accountIds) {
        return accountIds.get(random.nextInt(accountIds.size()));
    }
}
