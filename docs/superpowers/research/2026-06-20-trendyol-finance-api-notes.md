# Trendyol Finance API — Research Notes (connector sub-project input)

> **Date:** 2026-06-20 · **Source:** Trendyol developer docs (reference + changelog), mined without live API access.
> **Purpose:** Resolve the open items parked in the core-domain spec §11 as far as the public docs allow, and seed the **connector + normalization** sub-project (alt-proje 2).
> **Caveat:** The ReadMe-style reference pages do NOT expose full response JSON schemas (need "Try It" / OpenAPI / sandbox). Items marked **⚠️ SANDBOX** still require a real test account.

---

## 1. Endpoints & hosts

| Concern | Detail |
|---|---|
| Settlements | `GET .../integration/finance/che/sellers/{sellerId}/settlements` — sales, returns, discounts, coupons, provisions |
| OtherFinancials | `GET .../integration/finance/che/sellers/{sellerId}/otherfinancials` — supplier financing, wire transfers, payment orders (hakediş), invoices, commission/service-fee invoices |
| Cargo invoice items | `GET .../integration/finance/che/sellers/{sellerId}/cargo-invoice/{invoiceSerialNumber}/items` |
| Host migration | Cargo moving `api.trendyol.com` → `api.tgoapis.com` (since 2025-01-09). Confirm the current base host per endpoint when wiring the client. |

Both settlements & otherFinancials are paginated (`page`, `size`) and filter by `startDate`/`endDate` + `transactionType`.

---

## 2. Transaction types (✅ RESOLVED — full enumerations)

**Settlements `transactionType`:**
`Sale, Return, Discount, DiscountCancel, Coupon, CouponCancel, ProvisionPositive, ProvisionNegative, ManualRefund, ManualRefundCancel, TYDiscount, TYDiscountCancel, TYCoupon, TYCouponCancel, SellerRevenuePositive, SellerRevenueNegative, CommissionPositive, CommissionNegative, SellerRevenuePositiveCancel, SellerRevenueNegativeCancel, CommissionPositiveCancel, CommissionNegativeCancel`

**OtherFinancials `transactionType`:**
`Stoppage, CashAdvance, WireTransfer, IncomingTransfer, ReturnInvoice, CommissionAgreementInvoice, PaymentOrder, DeductionInvoices, FinancialItem`

**Implication for the normalizer:** the `Positive/Negative` and `…Cancel` suffixes ARE the ledger direction encoded in the type. This confirms the core-domain decision: **the adapter is the single place that maps (debt/credit + transactionType) → a canonical per-role signed effect**; the domain never re-derives direction. A blanket "RETURN ⇒ ×(−1)" remains wrong (the `Cancel` variants and return-cargo show direction is per-entry, not per-package).

---

## 3. Where each cost line lives (✅ RESOLVED — location/mapping)

| Domain concept | Source |
|---|---|
| Commission | settlements `commissionAmount` (separate from `sellerRevenue`); `CommissionPositive/Negative(+Cancel)` types; reverses on returns |
| Service fee (hizmet bedeli) | otherFinancials → `DeductionInvoices` with `transactionSubType=PlatformServiceFee` (filter added 2026-03-18) |
| Stopaj (withholding) | otherFinancials → `Stoppage` type; **1% since 2025-01-01** |
| Cargo + return cargo | cargo-invoice items; `shipmentPackageType` = "Gönderi Kargo Bedeli" / "İade Kargo Bedeli" |
| Coupons / campaign (TY-funded) | settlements `TYCoupon/TYDiscount(+Cancel)`; invoiced to seller monthly |

---

## 4. Cargo invoice items (✅ fields / ⚠️ VAT)

**Fields seen:** `shipmentPackageType`, `parcelUniqueId`, `orderNumber`, `amount`, `desi`.
**Linkage:** get the `invoiceSerialNumber` from a settlement/otherFinancials record whose `transactionType` is the cargo-invoice type (the record's `Id` becomes `invoiceSerialNumber`); cargo items then carry `orderNumber` / `parcelUniqueId`. Also, **`shipmentPackageId` is now on settlements & otherFinancials** (added 2025-03-21), which eases the cargo↔settlement join the core-domain spec described.
**⚠️ SANDBOX:** cargo `amount` VAT-inclusive vs exclusive, and whether a VAT rate/amount field exists, are NOT documented. `desi` is present → enables the P1 desi-based allocation later.

---

## 5. Still need a sandbox (⚠️ OPEN)

1. **`commissionAmount` VAT-inclusive or exclusive?** Not documented. (Core engine currently assumes exclusive: `commissionVat = commission × 0.20`.)
2. **Cargo `amount` VAT-inclusive/exclusive + rate field?** Not documented.
3. **Exact response JSON schemas** (field names/types, debt/credit field names, `lines[]` shape) — confirm `lineGrossAmount`, `vatRate`, `commissionAmount`, `barcode` field names against a real payload.
4. **Service/rate limits for finance endpoints** — the limits page only exposed product-service limits. Note: "order package retrieval service limits change as of 2026-06-08" (details TBD); design the client with backoff + pagination regardless.

---

## 6. Changelog highlights (finance, 2024–2026)

- **2026-03-18** — `transactionSubType=PlatformServiceFee` filter on otherfinancials.
- **2025-12-30** — settlements/otherfinancials gained `transactionTypes` (plural, multi-select) + `paymentDate` filters.
- **2025-03-21** — `shipmentPackageId` added to settlements AND otherfinancials (helps cargo join).
- **2025-01-01** — 1% e-commerce withholding (stopaj); labor cost (işçilik bedeli) must be reported per line item.
- **2023-07-10** — VAT rates restricted to **10% and 20%** for marketplace requests (older rates error). (Domain `VatRate` still accepts `[0,1]`; keep, but expect 0.10/0.20 in practice.)

---

## 7. Connector design implications (for brainstorming)

- **Normalizer is the anti-corruption layer**: map Trendyol's debt/credit ledger + the ~22/9 transaction types → the existing `SettlementPackage`/`SettlementLine` + canonical signed per-role effects. The domain stays untouched.
- **Join model**: prefer `shipmentPackageId` (now on settlements/otherfinancials) to stitch settlement lines + service fee + cargo; fall back to `orderNumber`/`parcelUniqueId` for cargo items.
- **Validate against 2 sources** (PRD §12): after Trendyol, paper-map Hepsiburada Finans to the same normalized model to test the abstraction.
- **Two concrete sandbox questions to answer first** (cheap, hours): commission VAT-inclusivity and cargo VAT-inclusivity — both feed the KDV engine's accuracy and need mali müşavir sign-off.
