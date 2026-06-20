package com.netkar.domain.model.allocation;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.SettlementLine;
import java.util.List;

/**
 * Domain policy (NOT a port): pure, in-memory split of a package-level fee
 * across the package's lines. Sign-free and I/O-free.
 */
public interface AllocationStrategy {
    List<Money> allocate(Money total, List<SettlementLine> lines);
}
