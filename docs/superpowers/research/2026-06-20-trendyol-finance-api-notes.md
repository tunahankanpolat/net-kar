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

## 2b. Real response schema — ✅ CONFIRMED from the OpenAPI (`/reference/*.md`)

**Important correction to the PRD/core-domain assumption.** `getSettlements` and `getOtherFinancials` return the SAME **flat** `FinancialTransaction` rows — **NOT** a nested `order → lines[]` structure — and there is **NO `vatRate`, `vatAmount`, or `lineGrossAmount` field**.

Shared `content[]` fields (settlements & otherfinancials), `?` = nullable:
`id, transactionDate, barcode?, transactionType, receiptId?, description?, debt, credit, paymentPeriod?, commissionRate?, commissionAmount?, commissionInvoiceSerialNumber?, sellerRevenue?, orderNumber?, paymentOrderId?, paymentDate?, sellerId, storeId?, storeName?, storeAddress?, country, orderDate?, affiliate, shipmentPackageId?`
**Absent: `vatRate`, `vatAmount`, `lineGrossAmount`.**

What this means:
- The finance endpoints are a **ledger of transaction rows** (a Sale row, a Commission adjustment row, …) keyed by `barcode` + `orderNumber` + `shipmentPackageId`. Money/direction is in `debt`/`credit`; `commissionRate` + `commissionAmount` + `sellerRevenue` ride on the row.
- **`commissionRate` IS present and per-row (variable by category)** — confirms commission is not a fixed %. With `commissionRate` + the amounts in one real row, the commission base and KDV-inclusivity can be **reverse-engineered from a single sandbox record**.
- **Sale gross (KDV dahil) and product VAT rate do NOT come from here** — they come from order/shipment data (`getShipmentPackages`) + Products API `vatRate`. So the normalizer must **join 4–5 sources** by `shipmentPackageId`/`orderNumber`/`barcode`: Orders (sale gross, qty, vatRate) + Settlements (commission, realized amounts, returns) + OtherFinancials (service fee, stopaj) + Cargo (shipping) + Products (vatRate).
- **No VAT field anywhere** in the finance responses → VAT is **always derived from a rate**; only the inclusive/exclusive convention changes the formula (sandbox-only).

This does NOT invalidate the domain model — `SettlementPackage`/`SettlementLine` remain a fine *target*. But the normalizer is a real **multi-source join + ledger-folding** job, not a field rename, and the core spec's "Settlements `lines[]` barcode-based `lineGrossAmount`/`vatRate`" claim (PRD §12) was an assumption, now corrected.

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
**✅ CONFIRMED (OpenAPI):** cargo items have exactly 5 fields — `shipmentPackageType, parcelUniqueId, orderNumber, amount, desi` — and **NO VAT field**. **⚠️ SANDBOX:** whether `amount` is KDV-inclusive vs exclusive (VAT derived from a 20% rate either way). `desi` present → enables the P1 desi-based allocation later.

---

## 5. Still need a sandbox (⚠️ OPEN)

1. **KDV-inclusivity of `commissionAmount` and cargo `amount`** — the only formula-affecting unknown (no VAT field exists, so we derive). Resolvable from **one real row** via `commissionRate` (does `commissionAmount` ≈ base × rate, and does it carry the 20%?). Core engine currently assumes commission exclusive (`× 0.20`) but cargo/service inclusive (`splitVat`) — reconcile once known.
2. **`debt`/`credit` semantics per `transactionType`** — which field+sign means what, and which amount on a Sale row is the gross sale vs `sellerRevenue` (net). Needs a few real rows.
3. **Finance schemas: ✅ now CONFIRMED** (flat `FinancialTransaction`; cargo = 5 fields — see §2b/§4). Remaining: confirm the **order/`getShipmentPackages`** line fields (`lineGrossAmount`/`vatRate`/qty/`barcode`) that supply the sale side of the join.
4. **Service/rate limits for finance endpoints** — limits page only exposed product-service limits. Note "order package retrieval limits change 2026-06-08" (TBD); design the client with backoff + pagination regardless.

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
- **Multi-source join (not a rename)**: the finance endpoints are a flat ledger with no sale gross / vatRate, so the normalizer stitches **Orders/`getShipmentPackages` (sale gross, qty, vatRate) + Settlements (commission, realized amounts, returns) + OtherFinancials (service fee, stopaj) + Cargo (shipping) + Products (vatRate)** — keyed on `shipmentPackageId` (now on settlements/otherfinancials), falling back to `orderNumber`/`barcode`/`parcelUniqueId`.
- **Fee VAT-splitting belongs in the normalizer** (it owns the marketplace's KDV convention), and should "read-explicit-else-derive" — but note there is NO VAT field, so it always derives from a rate; isolate the inclusive/exclusive choice to one place.
- **Validate against 2 sources** (PRD §12): after Trendyol, paper-map Hepsiburada Finans to the same normalized model to test the abstraction.
- **Two concrete sandbox questions to answer first** (cheap, hours): commission VAT-inclusivity and cargo VAT-inclusivity — both feed the KDV engine's accuracy and need mali müşavir sign-off.
