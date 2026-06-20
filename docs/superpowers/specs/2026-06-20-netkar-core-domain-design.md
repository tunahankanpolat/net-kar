# NetKar — Alt-Proje 1: Çekirdek Hesaplama Domain'i (Tasarım / Spec)

> **Durum:** Taslak — kullanıcı onayı bekliyor
> **Tarih:** 2026-06-20
> **Bağlam:** [NetKar_PRD.md](../../../NetKar_PRD.md) (v0.3)
> **Kapsam:** P0 MVP'nin ilk alt-projesi — saf domain çekirdeği (Kâr/KDV hesaplama motoru). Trendyol bağlantısı, kalıcılık (DB), REST/UI, abonelik **bu spec'in dışındadır** ve sonraki alt-projelerdir.

---

## 1. Neden bu alt-proje, neden önce bu?

Tüm P0 MVP'si birden çok bağımsız alt-sistemden oluşur (Trendyol connector, hesaplama motoru, kalıcılık + panel API, frontend, abonelik/ödeme, onboarding). Hepsini tek tasarım→plan→implementasyon döngüsüne sığdırmak çok büyük. Bu yüzden parçalıyoruz ve **en zor + en çok değer üreten + DDD'yi en iyi öğreten** parçadan başlıyoruz: **saf hesaplama domain'i**.

Bu parça API anahtarı veya DB olmadan, sahte/örnek normalize veriyle tamamen test edilebilir. En karmaşık iş mantığını (kâr + KDV) erkenden çözer ve sonraki tüm alt-projelerin oturacağı port/arayüz sınırlarını netleştirir.

### Alt-proje sırası (yol haritası)

1. **Çekirdek hesaplama domain'i** ← *bu spec*
2. Trendyol connector + normalizasyon (ham `debt`/`credit` + `transactionType` → normalize model)
3. Kalıcılık + ürün bazlı panel okuma API'si
4. Frontend dashboard (hazır UI kit ile)
5. Hesap/auth + abonelik/ödeme (iyzico, 14 gün deneme)
6. Onboarding + alış maliyeti girişi (tekli + toplu Excel)

---

## 2. Kapsam (acımasızca sınırlı)

### Dahil (P0, bu alt-proje)
- Maven çok-modüllü iskelet; **Java 21 proje seviyesinde** sabitlenmiş (sistemdeki Java 17'ye dokunmadan).
- Hexagonal modül yapısı: `domain` / `application` / `infrastructure`; bağımlılık kuralı Maven modül sınırlarıyla **derleme zamanında** zorlanır.
- **Pazaryeri-bağımsız normalize işlem modeli** (canonical model) — Trendyol'a özgü hiçbir kavram domain'de yok.
- **Kâr/KDV hesaplama motoru** (saf domain) — SALE (satış) yolu eksiksiz.
- Ürün bazlı roll-up (`ProductProfitability`) + **kırmızı liste** kuralı.
- `RevenueWeightedAllocation` (hasılat bazlı paylaştırma) + kuruş-korumalı `allocate()` (largest-remainder).
- Çalışan demo adaptörü: örnek fixture'ları motordan geçirip per-ürün kâr + kırmızı liste basar.
- Kapsamlı test paketi (golden örnek + kenar durumlar) + **ArchUnit** sınır testleri.
- TR↔EN ubiquitous-language sözlüğü.

### Hariç (sonraki alt-projeler / P1)
- Trendyol API auth, veri çekme, Stream/webhook, normalizasyon adaptörü.
- Kalıcılık / veritabanı.
- REST API, frontend, onboarding ekranları, Excel yükleme.
- Hesap/auth, abonelik/ödeme (iyzico).
- Reklam (Trendyol Ads), marj alarmı, özet panel, trend grafiği (P1).
- **İade/iptal aritmetiğinin tam doğrulaması** (mimari olarak hazırlanır, ama sandbox verisi + mali müşavir ile sonraki alt-projede kesinleşir — bkz. §11).

---

## 3. Kararlar günlüğü (brainstorm + araştırma)

| # | Karar | Gerekçe |
|---|---|---|
| D1 | Önce çekirdek domain, test-first | En zor mantık + en iyi DDD öğrenimi; API/DB'siz test edilebilir |
| D2 | Hexagonal + framework'siz domain | `domain` saf Java; Spring yalnızca `infrastructure`'da; bağımlılık kuralı Maven ile zorlanır |
| D3 | İngilizce kod + Türkçe ubiquitous-language sözlüğü | İş görüşmesi dostu; sözlük disiplini DDD'yi pekiştirir |
| D4 | Aggregate + domain service yaklaşımı | `SettlementPackage` aggregate invariant'ları tutar; `ProfitCalculator` domain service hesaplar |
| D5 | `AllocationStrategy` = **domain policy**, port DEĞİL | Port = dış dünya (I/O) sınırı; `AllocationStrategy` saf aritmetik. "Çok implementasyon" ≠ port; bu sadece Strategy deseni (in-domain polymorphism) |
| D6 | Sign/yön tek noktada (adapter) atanır; domain marketplace tipini görmez; **per-rol işaretli efekt**, blanket flip yok | Trendyol `debt`/`credit` + `transactionType` ledger modeli; çift-ters-çevirme (double-reversal) ve "iade kargosu = kazanç" hatası önlenir |
| D7 | `NetProfit` = KDV hariç ürün bazlı operasyonel kâr; KDV ayrı `EstimatedNetVatBurden` | VAT-kayıtlı satıcıda KDV nakit olarak nötrlenir; KDV ayrı şeffaflık kalemi olarak gösterilir, **resmi beyanname gibi sunulmaz** |
| D8 | `Money` = custom VO `(amount: BigDecimal, currency: Currency=TRY)`, JSR-354 yok | Domain bağımlılıksız kalır; `allocate()`/yuvarlama tam kontrol; tek para birimi MVP için custom yeterli |
| D9 | Yuvarlama: hesapta yüksek precision, sunumda 2 hane HALF_UP; allocation'da largest-remainder + "toplam korunur" invariant | Kuruş kaçağı (33.33×3=99.99) önlenir; Fowler `allocate()` |
| D10 | `ProductCost` = `{ amount, vatInclusive, costVatRate }`; MVP'de `costVatRate` default = `saleVatRate` | Türkiye'de alış/satış KDV oranı **farklılaşabilir** (indirimli oran) ve KDV-dahil maliyette `NetProfit`'i etkiler; alan modellenir ama kullanıcıdan ayrıca istenmez |

Kaynaklar §14'te.

---

## 4. Mimari ve modüller (hexagonal)

```
netkar/                          (parent pom — java.release=21, enforcer [21,) )
├─ netkar-domain/                SAF Java — Spring yok, I/O yok, framework yok
│   model/
│     Money, VatRate, Percentage, ProductRef, Quantity      (value objects)
│     SettlementPackage (aggregate root), SettlementLine     (aggregate)
│     TransactionEffect                                      (normalize yön: SALE/RETURN/CANCEL)
│     allocation/
│       AllocationStrategy (interface)                       (DOMAIN POLICY — port değil)
│       RevenueWeightedAllocation                            (tek MVP implementasyonu)
│     cost/
│       ProductCost, CostBook                                (alış maliyeti snapshot'ı)
│   service/
│     ProfitCalculator                                       (domain service)
│   result/
│     ProfitBreakdown, EstimatedNetVatBurden,
│     ProductProfitability, RedList
├─ netkar-application/           SADECE domain'e bağımlı
│   CalculateProductProfitabilityUseCase
│   (giriş/çıkış DTO'ları, orkestrasyon)
└─ netkar-infrastructure/        Spring Boot kabuğu (şimdilik minimal)
    DemoRunner                                               (örnek JSON fixture → motor → çıktı)
```

**Bağımlılık kuralı (ArchUnit ile zorlanır):**
- `domain` → hiçbir şeye bağımlı değil (ne Spring, ne jakarta, ne `application`/`infrastructure`).
- `application` → yalnızca `domain`.
- `infrastructure` → `application` + `domain`.

Maven modül grafiği bu yönü zaten derleme zamanında zorlar; ArchUnit ek olarak paket-içi import yönünü ve "domain'de Spring/jakarta import yok" kuralını test eder.

---

## 5. Ubiquitous language sözlüğü (TR ↔ EN)

| Türkçe (domain dili) | Kod (İngilizce) | Not |
|---|---|---|
| Hakediş / mutabakat / cari hesap satırı | Settlement / SettlementLine | API'de `debt`/`credit` ledger |
| Sipariş paketi | SettlementPackage | `shipmentPackageId` karşılığı |
| Satış / İade / İptal yönü | TransactionEffect (SALE/RETURN/CANCEL) | Normalize; marketplace tipi değil |
| Komisyon | Commission | Barcode bazında gelir |
| Hizmet bedeli | ServiceFee | Paket bazında (~8,49 + KDV) |
| Kargo / iade kargosu | ShippingFee / ReturnShipping | Ayrı endpoint, join'lenir |
| Kampanya katkı payı / kupon | CampaignContribution | |
| Erken ödeme / vade farkı | EarlyPaymentFee | |
| Cezai kesinti | Penalty | |
| Stopaj | WithholdingTax | KDV hariç tutar üzerinden %1 |
| Alış maliyeti (COGS) | ProductCost / cost of goods sold | Satıcıdan istenen tek zorunlu kalem |
| Satış KDV oranı | saleVatRate | Products API `vatRate` |
| Alış KDV oranı | costVatRate | Satıştan farklı olabilir |
| Net KDV Yükü (ürün bazlı tahmin) | EstimatedNetVatBurden | Şeffaflık kalemi; resmi beyanname değil |
| Net kâr (vergi öncesi işletme kârı) | NetProfit | KDV hariç operasyonel kâr |
| Kâr marjı | Margin (Percentage) | |
| Kırmızı liste | RedList | Net kârı < 0 ürünler |
| Hasılat bazlı paylaştırma | RevenueWeightedAllocation | "~" ile işaretlenir |
| (Gelecek) Dönem/işletme KDV özeti | PeriodVatSummary | Rezerve isim; bu alt-projede yok |

---

## 6. Domain modeli

### 6.1 Value object'ler

- **`Money(amount: BigDecimal, currency: Currency)`**
  - Default `currency = TRY`. Operasyonlar farklı para birimleriyle karışırsa hata.
  - **Negatif değere izin verir** (canonical signed efektler ve negatif KDV yükü = devreden KDV için gerekli). Negatiflik kısıtı `Money`'de değil, ilgili invariant'larda (ör. bir satışın brüt tutarı ≥ 0).
  - Aritmetik dahili **yüksek precision** `BigDecimal` ile; sunum/serileştirme 2 hane **HALF_UP**.
  - `allocate(weights): List<Money>` — **largest-remainder** (Fowler); **invariant: parçaların toplamı == kaynak tutar**.
  - `splitVat(rate): (net, vat)` — `net + vat == kaynak` olacak şekilde kuruş-korumalı ayrıştırma.
- **`VatRate(value: BigDecimal)`** — ör. `0.20`, `0.10`, `0.01`, `0.00`. `[0,1]` doğrulanır.
- **`Percentage`** — marj için; sıfıra bölme korumalı.
- **`ProductRef(barcode: String)`** — ürün kimliği (boş olamaz).
- **`Quantity(value: int)`** — `≥ 1`.
- **`TransactionEffect`** — enum `SALE`, `RETURN`, `CANCEL` (normalize yön; marketplace string'i değil).

### 6.2 Aggregate

- **`SettlementPackage`** (aggregate root)
  - `packageId`, `effect: TransactionEffect`, `lines: List<SettlementLine>` (boş olamaz),
    paket-seviyesi kesintiler: `shippingFee`, `serviceFee`, `penalty`, `earlyPaymentFee` (her biri `Money`, magnitude).
  - **Invariant'lar:** satırlar boş değil; magnitude alanları `≥ 0`; paylaştırma sonrası parçalar paket tutarına eşit.
- **`SettlementLine`** (aggregate içi entity)
  - `productRef`, `quantity`, `lineGrossAmount` (KDV dahil satış, magnitude), `saleVatRate`,
    `commissionAmount` (magnitude), `withholdingTax` (magnitude), `campaignContribution` (magnitude).

> **Not (D6):** SALE yolu için alanlar pozitif magnitude'dur. İade/iptal yönü ve per-rol işaretli efektler **adapter** tarafından belirlenir (§7). Bu alt-projede SALE yolu eksiksiz modellenir; iade örnekleri illüstratiftir ve sandbox doğrulamasına işaretlidir.

### 6.3 Alış maliyeti

- **`ProductCost(amount: Money, vatInclusive: boolean, costVatRate: VatRate)`**
  - `costVatRate` MVP'de default `saleVatRate` (kullanıcıdan ayrıca istenmez; onboarding yalnızca tutar + "KDV dahil mi?" sorar).
  - **Risk (D10):** indirimli oran senaryolarında `costVatRate ≠ saleVatRate` olabilir ve KDV-dahil maliyette `NetProfit`'i etkiler → mali müşavir doğrulaması (§11).
- **`CostBook`** — `Map<ProductRef, ProductCost>` snapshot'ı; saf domain'e parametre olarak geçer (port değil — bu alt-projede I/O yok). Eksik maliyet → `missingCost` flag.

### 6.4 Domain service & policy

- **`ProfitCalculator.calculate(pkg: SettlementPackage, costs: CostBook): PackageProfit`**
  - Her satır için `ProfitBreakdown` üretir; paket-seviyesi kesintileri `AllocationStrategy` ile paylaştırır.
- **`AllocationStrategy`** (domain policy interface) — `allocate(total: Money, lines): Map<line, Money>`; **toplam korunur** invariant'ı.
  - `RevenueWeightedAllocation`: ağırlık = `line.lineGrossAmount / Σ lineGrossAmount`. Paylaştırılan kalemler sonuçta `~` (allocated) flag'i taşır.

### 6.5 Sonuç (result) value object'leri

- **`ProfitBreakdown`** (satır bazında): `productRef`, `revenueNet`, `cogsNet`, `commission`, `serviceFeeShare~`, `shippingShare~`, `withholdingTax`, `campaignContribution`, `penaltyShare~`, `earlyPaymentShare~`, `estimatedNetVatBurden`, `netProfit`, `margin`, flag'ler `{ allocated, missingCost }`.
- **`EstimatedNetVatBurden`** (şeffaflık VO'su): 5 bileşen — `saleVat`, `cogsVat`, `shippingVat`, `commissionVat`, `serviceFeeVat` — ve net sonuç (negatif olabilir = devreden KDV).
- **`ProductProfitability`** (roll-up): bir ürünün dönem içi tüm `ProfitBreakdown`'larının toplamı; `isLoss`.
- **`RedList`**: `netProfit < 0` olan `ProductProfitability`'lerin listesi.

---

## 7. İşaret (sign) ve yön yönetimi — kritik karar (D6)

**Sorun:** Trendyol finans API'si tek bir işaretli tutar vermez; her satırda ayrı **`debt`** ve **`credit`** alanları (biri 0, diğeri **pozitif**) + bir `transactionType` (`Sale`, `Return`, …) bulunur. Bu bir **debit/credit ledger** modelidir.

**Reddedilen yaklaşım:** "Domain RETURN/CANCEL gördüğünde tüm satırı `×(-1)` yapar."
İki nedenle yanlış:
1. **Çift ters-çevirme (double-reversal):** Adapter `debt`/`credit` yönünü zaten yorumlar; domain bir kez daha çevirirse işaret iki kez döner.
2. **İade kargosu hatası:** İade, satışın gelir/komisyon/KDV'sini *tersine çevirir* ama **iade kargosu yeni bir masraftır**. Tüm satırı `×(-1)` yapmak bu masrafı kazanca çevirir → yanlış.

**Benimsenen yaklaşım:**
- **Yön/işaret tek noktada, adapter'da atanır.** Adapter her ledger kalemini, `debt`/`credit` + `transactionType`'a bakarak **rol bazında işaretli bir kâr efektine** çevirir (gelir +, masraf −; iade satış kalemlerini tersine çevirir; iade kargosunu yeni masraf olarak ekler).
- **Domain marketplace `transactionType` string'ini hiç görmez**, blanket sign flip yapmaz; **normalize edilmiş işaretli efektleri toplar.**
- Normalize `TransactionEffect` yalnızca **gösterim/gruplama ve invariant kontrolü** için taşınır (hesapta `×(-1)` çarpanı olarak değil).

**Bu alt-projedeki kapsam (dürüst sınır):**
- **SALE (satış) yolu eksiksiz** spesifiye edilir ve test edilir.
- İade/iptal **mimari olarak hazırdır** (model işaretli efektleri taşıyabilir) ve **bir basit, restock-edilebilir iade fixture'ı** ile netleme gösterilir — ama **tam aritmetik** (restock-edilemeyen ürün maliyeti, iade kargosu, KDV tersine kalemleri) **connector alt-projesinde gerçek sandbox verisi + mali müşavir** ile kesinleşir (§11). Bu bilinçli bir kapsam çizgisidir, sessizce atlanan kalem değil.

---

## 8. Hesaplama (SALE yolu) — formüller ve invariant'lar

Satır başına (tüm tutarlar `Money`, kuruş-korumalı):

```
# Satıştan KDV ayrıştırma (reconcile: saleNet + saleVat == lineGrossAmount)
(saleNet, saleVat) = lineGrossAmount.splitVat(saleVatRate)

# Alış (COGS) — CostBook'tan; yoksa missingCost=true ve cogs=0
cogsNet = vatInclusive ? cost.amount.splitVat(costVatRate).net : cost.amount
cogsVat = vatInclusive ? cost.amount.splitVat(costVatRate).vat : cost.amount * costVatRate

# Komisyon (varsayım: KDV hariç — AÇIK KALEM, sandbox'ta doğrulanacak)
commissionNet = commissionAmount
commissionVat = commissionAmount * 0.20

# Paket kalemleri — hasılat bazlı paylaştırma (largest-remainder), '~' flag
shippingShare       = allocate(pkg.shippingFee)      → (net, vat) split
serviceFeeShare     = allocate(pkg.serviceFee)       → (net, vat) split   # 8,49 + 1,70 bilinen
penaltyShare        = allocate(pkg.penalty)
earlyPaymentShare   = allocate(pkg.earlyPaymentFee)

# Stopaj — normalize modelde verili (API kaynaklı); yoksa saleNet * 0.01
withholdingTax = line.withholdingTax

# --- Şeffaflık: ürün bazlı KDV etkisi (resmi beyanname DEĞİL) ---
EstimatedNetVatBurden = saleVat
                      − cogsVat − shippingShareVat − commissionVat − serviceFeeShareVat

# --- Ekonomik kâr: KDV HARİÇ (VAT-kayıtlı satıcıda KDV nakit olarak nötrlenir) ---
NetProfit = saleNet
          − cogsNet − commissionNet − serviceFeeShareNet − shippingShareNet
          − withholdingTax − campaignContribution − penaltyShare − earlyPaymentShare

Margin = NetProfit / saleNet            # saleNet == 0 ise tanımsız → flag
```

**Önemli:** `NetProfit`, `EstimatedNetVatBurden`'ı **çıkarmaz**. KDV nakit akışında nötrlenir (cebir doğrulandı); KDV ayrı bir şeffaflık kalemidir. İki kez saymayı önlemek için bu ayrım kesindir.

**Invariant'lar (golden testlerde doğrulanır):**
- `saleNet + saleVat == lineGrossAmount`
- Her paylaştırma: `Σ pay == paket tutarı` (kuruş kaçağı yok)
- Eksik maliyet → `missingCost=true`, hesap eksik işaretlenir (sessizce 0 kâr göstermez)
- Paylaştırılmış her kalem `~` (allocated) flag taşır

---

## 9. Test stratejisi (TDD — bu alt-projenin kalbi)

- **Golden örnek** ile başla: tek bir gerçekçi **çok ürünlü paket**, her satır kuruşuna kadar assert edilir. Önce test, sonra implementasyon.
- Kapsanacak senaryolar:
  - Tek ürünlü paket
  - Çok ürünlü paket → hasılat bazlı paylaştırma + `~` flag + toplam korunur
  - KDV-dahil vs KDV-hariç alış maliyeti
  - `costVatRate ≠ saleVatRate` (indirimli oran)
  - Eksik maliyet → `missingCost`
  - Zarar eden ürün → kırmızı liste
  - Kenar KDV oranları: `0.00`, `0.01`, `0.10`, `0.20`
  - Marj `saleNet == 0` koruması
  - Kuruş-kaçağı: `100 TL / 3 ürün` → parçalar `100.00`'a tam eşit
  - İllüstratif restock-edilebilir iade (netleme; "sandbox doğrulaması bekliyor" notlu)
- **ArchUnit:** `domain` paketi `org.springframework`/`jakarta`/`infrastructure` import etmez; bağımlılık yönü korunur; value object'ler immutable.

---

## 10. Java 21'i proje seviyesinde sabitleme (sistem 17'ye dokunmadan)

- `.sdkmanrc` → `java=21.0.2-tem` (dizine `cd` olunca otomatik geçiş; SDKMAN auto-env).
- Parent pom: `<maven.compiler.release>21</maven.compiler.release>`.
- `maven-enforcer-plugin` → `requireJavaVersion [21,)` (yanlış JDK ile derleme başarısız olur).
- (Opsiyonel) Maven toolchains ile JDK 21'i açıkça sabitle.
- macOS sistem `/usr/libexec/java_home` (Zulu 17) **hiç değiştirilmez**; kullanıcı default'u korunur.

---

## 11. Sonraki alt-projelere devredilen açık kalemler (bloklayıcı değil)

| Kalem | Nerede çözülür |
|---|---|
| `commissionAmount` KDV dahil mi hariç mi? | Connector alt-projesi — Trendyol sandbox |
| Kargo (`getCargoInvoiceItems`) KDV dahil/hariç + oran | Connector alt-projesi — sandbox |
| Tam `transactionType` enumerasyonu (docs örneği yalnız `Sale`/`Return` gösterdi) | Connector alt-projesi — sandbox |
| İade/iptal tam aritmetiği (restock, iade kargosu, KDV tersine kalemleri) | Connector alt-projesi + mali müşavir |
| Stopaj kaynağı: API kalemi mi yoksa `saleNet × %1` hesabı mı | Connector alt-projesi — sandbox |
| Tüm KDV mantığı + `costVatRate` doğruluğu | Mali müşavir danışması (PRD §13) |

Bu motor, bu kalemler netleştiğinde golden örneklerle mali müşavire **doğrulatılacak** şekilde test-edilebilir kurulur.

---

## 12. "Bitti" tanımı (kabul kriterleri)

- [ ] `mvn verify` Java 21 ile çalışır; tüm testler + ArchUnit yeşil.
- [ ] `DemoRunner` örnek fixture'lardan doğru per-ürün `ProfitBreakdown` + kırmızı liste basar.
- [ ] Golden çok-ürünlü paket kuruşuna kadar doğru; tüm paylaştırma toplamları korunur.
- [ ] `domain` modülü hiçbir framework/infra import etmez (ArchUnit kanıtlar).
- [ ] TR↔EN sözlük commit'li.

---

## 13. Riskler

| Risk | Önlem |
|---|---|
| İade aritmetiği sandbox verisine bağımlı | SALE yolu eksiksiz; iade mimari olarak hazır + sandbox'a işaretli (§7) |
| KDV mantığı doğruluk iddiası taşıyor | Mali müşavir doğrulaması; her şey golden testlerle doğrulanabilir |
| `costVatRate` default'u gerçekten farklı olabilir | Alan modellendi; P1'de UI override; mali müşavir notu |
| Erken soyutlama (over-engineering) | Yalnızca `AllocationStrategy` policy soyutlandı; gerisi somut |

---

## 14. Kaynaklar

- [Trendyol — Current Account Statement Integration (`debt`/`credit` + `transactionType`)](https://developers.trendyol.com/v3.0/docs/1current-account-statement-integration)
- [Trendyol Developers — Accounting & Finance Integration](https://developers.trendyol.com/int/docs/Accounting%20and%20Finance%20Integration/Account%20Statement%20Integration)
- [Martin Fowler — Money pattern & `allocate()` (largest-remainder)](http://thierryroussel.free.fr/java/books/martinfowler/www.martinfowler.com/isa/money.html)
- [Codecentric — Hexagonal Architecture & DDD: Ports & Adapters](https://www.codecentric.de/en/knowledge-hub/blog/hexagon-schmexagon-1)
- [Adding DDD to Ports & Adapters — improving port interfaces](https://codeartify.substack.com/p/adding-domain-driven-design-to-ports)
- [Baeldung — Java Money & Currency API (JSR-354)](https://www.baeldung.com/java-money-and-currency)
- [Alomaliye — e-Ticarette Komisyon Faturalarında KDV](https://www.alomaliye.com/2025/12/16/e-ticaretteki-komisyon-faturalarinda-kdv/)
- [Güncel KDV Oranları (indirimli oran)](https://kdvhesaplama.org/kdv-oranlari-listesi/)
