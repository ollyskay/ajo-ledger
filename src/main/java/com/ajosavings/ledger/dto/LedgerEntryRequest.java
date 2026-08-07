package com.ajosavings.ledger.dto;

import java.util.UUID;

/**
 * One leg of a transaction to be posted. amountKobo is signed: positive is a
 * credit to the account, negative is a debit.
 */
public record LedgerEntryRequest(UUID accountId, long amountKobo, String description) {
}
