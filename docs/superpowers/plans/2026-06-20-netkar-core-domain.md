# NetKar Core Calculation Domain — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the marketplace-agnostic profit/VAT calculation engine for NetKar as a pure, framework-free DDD domain, fully test-driven, with no API or database.

**Architecture:** Maven multi-module hexagonal project. `netkar-domain` is pure Java (no Spring, no I/O) holding value objects, the `SettlementPackage` aggregate, the `ProfitCalculator` domain service, and a `RevenueWeightedAllocation` domain policy. `netkar-application` orchestrates a use-case over the domain. `netkar-infrastructure` is a thin Spring Boot shell with a demo runner and the ArchUnit boundary tests. The dependency rule (domain → nothing) is enforced by both Maven module wiring and ArchUnit.

**Tech Stack:** Java 21, Maven, JUnit 5, AssertJ, ArchUnit 1.4.2, Spring Boot 4.1.0 (infrastructure only — chosen over 3.5.x because 3.x OSS support ends 2026-06-30; Boot 4 GA since Nov 2025, Java 17+ baseline, trivial usage here).

## Global Constraints

- **Java 21** everywhere; `maven.compiler.release=21`; `maven-enforcer-plugin` requires `[21,)`. The host system JDK (Zulu 17) must NOT be changed — `.sdkmanrc` pins `java=21.0.2-tem` at the project level.
- **`netkar-domain` is dependency-free at runtime:** no `org.springframework`, no `jakarta`, no `com.netkar.application`/`com.netkar.infrastructure` imports. Enforced by ArchUnit (Task 14).
- **Ubiquitous language:** code identifiers in **English**; the TR↔EN glossary lives in the design spec (`docs/superpowers/specs/2026-06-20-netkar-core-domain-design.md` §5).
- **Money discipline:** `Money` carries `(BigDecimal amount, Currency currency)`, default `TRY`. Negatives allowed at the `Money` level; sign constraints live in higher invariants. Internal arithmetic keeps full precision; presentation/minor-unit rounding is **2 decimals, HALF_UP**. Allocation uses the **largest-remainder** method and MUST preserve the source total exactly.
- **`NetProfit` is KDV-hariç** (pre-tax operating profit) and **does NOT subtract `EstimatedNetVatBurden`** — VAT is surfaced only as a separate transparency figure (verified to net to zero in cash terms).
- **Scope:** SALE path only. `RETURN`/`CANCEL` arithmetic is deferred to the connector sub-project; `ProfitCalculator` throws `UnsupportedOperationException` for non-SALE effects.
- **Base package:** `com.netkar`. **Maven coordinates:** `com.netkar:netkar:0.1.0-SNAPSHOT` (parent, packaging `pom`).
- **Commit** after every task (frequent commits).

---

## File Structure

```
netkar/
├─ pom.xml                                  parent (modules, BOMs, enforcer, java 21)
├─ .sdkmanrc                                java=21.0.2-tem
├─ netkar-domain/
│   ├─ pom.xml                              pure: only junit + assertj (test)
│   └─ src/{main,test}/java/com/netkar/domain/
│       model/Money.java                    Money value object
│       model/VatRate.java                  VAT rate value object
│       model/VatSplit.java                 (net, vat) pair
│       model/Percentage.java               ratio value object (margin)
│       model/ProductRef.java               barcode identity
│       model/Quantity.java                 positive quantity
│       model/TransactionEffect.java        enum SALE/RETURN/CANCEL
│       model/SettlementLine.java           aggregate-internal entity
│       model/SettlementPackage.java        aggregate root + invariants
│       model/allocation/AllocationStrategy.java       domain policy interface
│       model/allocation/RevenueWeightedAllocation.java
│       model/cost/ProductCost.java         COGS + vatInclusive + costVatRate
│       model/cost/CostBook.java            ProductRef -> ProductCost snapshot
│       service/ProfitCalculator.java       SALE-path domain service
│       result/EstimatedNetVatBurden.java   5-component VAT transparency
│       result/ProfitBreakdown.java         per-line result
│       result/PackageProfit.java           per-package result
│       result/ProductProfitability.java    per-product roll-up
│       result/RedList.java                 loss-making products
├─ netkar-application/
│   ├─ pom.xml                              depends on netkar-domain
│   └─ src/{main,test}/java/com/netkar/application/
│       CalculateProductProfitabilityUseCase.java
└─ netkar-infrastructure/
    ├─ pom.xml                              depends on netkar-application + spring-boot
    └─ src/{main,test}/java/com/netkar/infrastructure/
        NetKarDemoApplication.java          @SpringBootApplication
        DemoRunner.java                     CommandLineRunner: sample -> engine -> print
        SampleData.java                     in-code sample packages + costs
        architecture/HexagonalBoundaryTest.java   ArchUnit rules (test)
```

---

### Task 1: Project scaffold (Maven multi-module, Java 21, build green)

**Files:**
- Create: `pom.xml`, `.sdkmanrc`
- Create: `netkar-domain/pom.xml`, `netkar-application/pom.xml`, `netkar-infrastructure/pom.xml`
- Test: `netkar-domain/src/test/java/com/netkar/domain/BuildSmokeTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: a buildable reactor; module coordinates `com.netkar:netkar-domain|netkar-application|netkar-infrastructure:0.1.0-SNAPSHOT`.

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/BuildSmokeTest.java`
```java
package com.netkar.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BuildSmokeTest {
    @Test
    void runs_on_java_21_or_newer() {
        assertThat(Runtime.version().feature()).isGreaterThanOrEqualTo(21);
    }
}
```

- [ ] **Step 2: Create the parent pom**

`pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.netkar</groupId>
    <artifactId>netkar</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>netkar-domain</module>
        <module>netkar-application</module>
        <module>netkar-infrastructure</module>
    </modules>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.release>21</maven.compiler.release>
        <spring-boot.version>4.1.0</spring-boot.version>
        <archunit.version>1.4.2</archunit.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.tngtech.archunit</groupId>
                <artifactId>archunit-junit5</artifactId>
                <version>${archunit.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-enforcer-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <id>enforce-java-21</id>
                        <goals><goal>enforce</goal></goals>
                        <configuration>
                            <rules>
                                <requireJavaVersion><version>[21,)</version></requireJavaVersion>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create the three module poms and `.sdkmanrc`**

`.sdkmanrc`
```
java=21.0.2-tem
```

`netkar-domain/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.netkar</groupId>
        <artifactId>netkar</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>
    <artifactId>netkar-domain</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

`netkar-application/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.netkar</groupId>
        <artifactId>netkar</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>
    <artifactId>netkar-application</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.netkar</groupId>
            <artifactId>netkar-domain</artifactId>
            <version>0.1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

`netkar-infrastructure/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.netkar</groupId>
        <artifactId>netkar</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>
    <artifactId>netkar-infrastructure</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.netkar</groupId>
            <artifactId>netkar-application</artifactId>
            <version>0.1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Run the test to verify it passes on Java 21**

Run: `mvn -q test`
Expected: BUILD SUCCESS; `BuildSmokeTest` passes. (If enforcer fails, the active JDK is < 21 — run `sdk use java 21.0.2-tem` first.)

- [ ] **Step 5: Commit**

```bash
git add pom.xml .sdkmanrc netkar-domain netkar-application netkar-infrastructure
git commit -m "build: scaffold Java 21 hexagonal Maven multi-module"
```

---

### Task 2: `Money` value object (construction, arithmetic, equality, rounding)

**Files:**
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/Money.java`
- Test: `netkar-domain/src/test/java/com/netkar/domain/model/MoneyTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `Money.of(BigDecimal amount, Currency currency)`, `Money.tryOf(String amount)`, `Money.zeroTry()`
  - `BigDecimal amount()`, `Currency currency()`
  - `Money add(Money)`, `Money subtract(Money)`, `Money multiply(BigDecimal factor)`, `Money negate()`
  - `Money roundedToMinorUnit()` (scale 2, HALF_UP)
  - `boolean isZero()`, `boolean isNegative()`, `boolean isPositive()`, `int signum()`
  - value equality by `currency` + `amount.compareTo`

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/model/MoneyTest.java`
```java
package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void defaults_to_try() {
        assertThat(Money.tryOf("10.00").currency()).isEqualTo(Currency.getInstance("TRY"));
    }

    @Test
    void equality_ignores_trailing_zero_scale() {
        assertThat(Money.tryOf("10.0")).isEqualTo(Money.tryOf("10.00"));
        assertThat(Money.tryOf("10.0")).hasSameHashCodeAs(Money.tryOf("10.00"));
    }

    @Test
    void adds_and_subtracts() {
        assertThat(Money.tryOf("10.00").add(Money.tryOf("2.50"))).isEqualTo(Money.tryOf("12.50"));
        assertThat(Money.tryOf("10.00").subtract(Money.tryOf("2.50"))).isEqualTo(Money.tryOf("7.50"));
    }

    @Test
    void multiplies_keeping_precision() {
        assertThat(Money.tryOf("10.00").multiply(new BigDecimal("0.20")))
            .isEqualTo(Money.tryOf("2.00"));
    }

    @Test
    void allows_negative_and_reports_sign() {
        assertThat(Money.tryOf("-1.00").isNegative()).isTrue();
        assertThat(Money.zeroTry().isZero()).isTrue();
        assertThat(Money.tryOf("1.00").isPositive()).isTrue();
    }

    @Test
    void rounds_to_minor_unit_half_up() {
        assertThat(Money.tryOf("1.005").roundedToMinorUnit()).isEqualTo(Money.tryOf("1.01"));
    }

    @Test
    void rejects_mixed_currency_arithmetic() {
        Money usd = Money.of(new BigDecimal("1.00"), Currency.getInstance("USD"));
        assertThatThrownBy(() -> Money.tryOf("1.00").add(usd))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-domain test`
Expected: FAIL — `Money` does not exist (compilation error).

- [ ] **Step 3: Write the implementation**

`netkar-domain/src/main/java/com/netkar/domain/model/Money.java`
```java
package com.netkar.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public final class Money {

    private static final Currency TRY = Currency.getInstance("TRY");
    private static final int MINOR_UNIT_SCALE = 2;

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount, "amount");
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money tryOf(String amount) {
        return new Money(new BigDecimal(amount), TRY);
    }

    public static Money zeroTry() {
        return new Money(BigDecimal.ZERO, TRY);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    public Money roundedToMinorUnit() {
        return new Money(amount.setScale(MINOR_UNIT_SCALE, RoundingMode.HALF_UP), currency);
    }

    public int signum() {
        return amount.signum();
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return currency.equals(money.currency) && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return roundedToMinorUnit().amount + " " + currency.getCurrencyCode();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/model/Money.java \
        netkar-domain/src/test/java/com/netkar/domain/model/MoneyTest.java
git commit -m "feat(domain): Money value object with currency, arithmetic, rounding"
```

---

### Task 3: `VatRate`, `VatSplit`, and `Money.splitVat`

**Files:**
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/VatRate.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/VatSplit.java`
- Modify: `netkar-domain/src/main/java/com/netkar/domain/model/Money.java` (add `splitVat`)
- Test: `netkar-domain/src/test/java/com/netkar/domain/model/VatSplitTest.java`

**Interfaces:**
- Consumes: `Money` (Task 2).
- Produces:
  - `VatRate.of(String)`, `VatRate.value()` (BigDecimal), `VatRate.onePlus()`, constant `VatRate.STANDARD` (0.20)
  - `VatSplit` record `(Money net, Money vat)`
  - `Money.splitVat(VatRate)` → `VatSplit` with invariant `net.add(vat).equals(gross.roundedToMinorUnit())`

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/model/VatSplitTest.java`
```java
package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VatSplitTest {

    @Test
    void splits_gross_into_net_and_vat_at_20_percent() {
        VatSplit split = Money.tryOf("120.00").splitVat(VatRate.of("0.20"));
        assertThat(split.net()).isEqualTo(Money.tryOf("100.00"));
        assertThat(split.vat()).isEqualTo(Money.tryOf("20.00"));
    }

    @Test
    void net_plus_vat_reconciles_to_gross_to_the_kurus() {
        Money gross = Money.tryOf("19.99");
        VatSplit split = gross.splitVat(VatRate.of("0.10"));
        assertThat(split.net().add(split.vat())).isEqualTo(gross);
    }

    @Test
    void zero_rate_yields_zero_vat() {
        VatSplit split = Money.tryOf("50.00").splitVat(VatRate.of("0.00"));
        assertThat(split.net()).isEqualTo(Money.tryOf("50.00"));
        assertThat(split.vat()).isEqualTo(Money.zeroTry());
    }

    @Test
    void rejects_rate_outside_zero_to_one() {
        assertThatThrownBy(() -> VatRate.of("1.50")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VatRate.of("-0.10")).isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-domain test`
Expected: FAIL — `VatRate`/`VatSplit`/`splitVat` not defined.

- [ ] **Step 3: Write the implementations**

`netkar-domain/src/main/java/com/netkar/domain/model/VatRate.java`
```java
package com.netkar.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class VatRate {

    public static final VatRate STANDARD = VatRate.of("0.20");

    private final BigDecimal value;

    private VatRate(BigDecimal value) {
        this.value = Objects.requireNonNull(value, "value");
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("VAT rate must be within [0,1]: " + value);
        }
    }

    public static VatRate of(String value) {
        return new VatRate(new BigDecimal(value));
    }

    public BigDecimal value() {
        return value;
    }

    public BigDecimal onePlus() {
        return value.add(BigDecimal.ONE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VatRate vatRate)) return false;
        return value.compareTo(vatRate.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
```

`netkar-domain/src/main/java/com/netkar/domain/model/VatSplit.java`
```java
package com.netkar.domain.model;

public record VatSplit(Money net, Money vat) {
}
```

Add to `Money.java` (after `multiply`):
```java
    public VatSplit splitVat(VatRate rate) {
        Money gross = roundedToMinorUnit();
        java.math.BigDecimal vatAmount = gross.amount
            .multiply(rate.value())
            .divide(rate.onePlus(), 2, java.math.RoundingMode.HALF_UP);
        Money vat = new Money(vatAmount, currency);
        Money net = gross.subtract(vat);
        return new VatSplit(net, vat);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/model/VatRate.java \
        netkar-domain/src/main/java/com/netkar/domain/model/VatSplit.java \
        netkar-domain/src/main/java/com/netkar/domain/model/Money.java \
        netkar-domain/src/test/java/com/netkar/domain/model/VatSplitTest.java
git commit -m "feat(domain): VatRate and kurus-preserving Money.splitVat"
```

---

### Task 4: `Money.allocate` (largest-remainder, total-preserving)

**Files:**
- Modify: `netkar-domain/src/main/java/com/netkar/domain/model/Money.java` (add `allocate`)
- Test: `netkar-domain/src/test/java/com/netkar/domain/model/MoneyAllocateTest.java`

**Interfaces:**
- Consumes: `Money` (Task 2).
- Produces: `List<Money> Money.allocate(List<BigDecimal> weights)` — parts in input order, each at minor-unit scale, summing exactly to `this.roundedToMinorUnit()`.

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/model/MoneyAllocateTest.java`
```java
package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoneyAllocateTest {

    private static final BigDecimal ONE = BigDecimal.ONE;

    @Test
    void distributes_remainder_so_parts_sum_to_total() {
        List<Money> parts = Money.tryOf("100.00").allocate(List.of(ONE, ONE, ONE));
        assertThat(parts).containsExactly(
            Money.tryOf("33.34"), Money.tryOf("33.33"), Money.tryOf("33.33"));
        assertThat(parts.stream().reduce(Money.zeroTry(), Money::add)).isEqualTo(Money.tryOf("100.00"));
    }

    @Test
    void allocates_proportional_to_weights() {
        List<Money> parts = Money.tryOf("60.00")
            .allocate(List.of(new BigDecimal("30"), new BigDecimal("10")));
        assertThat(parts).containsExactly(Money.tryOf("45.00"), Money.tryOf("15.00"));
    }

    @Test
    void zero_weight_sum_falls_back_to_equal_split() {
        List<Money> parts = Money.tryOf("10.00")
            .allocate(List.of(BigDecimal.ZERO, BigDecimal.ZERO));
        assertThat(parts).containsExactly(Money.tryOf("5.00"), Money.tryOf("5.00"));
        assertThat(parts.stream().reduce(Money.zeroTry(), Money::add)).isEqualTo(Money.tryOf("10.00"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-domain test`
Expected: FAIL — `allocate` not defined.

- [ ] **Step 3: Write the implementation**

Add to `Money.java` (and ensure imports `java.util.*`):
```java
    public java.util.List<Money> allocate(java.util.List<java.math.BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("weights must not be empty");
        }
        int n = weights.size();
        java.math.BigDecimal weightSum = weights.stream()
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        boolean equalSplit = weightSum.signum() == 0;

        long totalCents = amount.setScale(2, java.math.RoundingMode.HALF_UP)
            .movePointRight(2).longValueExact();

        long[] cents = new long[n];
        java.math.BigDecimal[] remainders = new java.math.BigDecimal[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            java.math.BigDecimal share = equalSplit
                ? java.math.BigDecimal.valueOf(totalCents)
                    .divide(java.math.BigDecimal.valueOf(n), 10, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.valueOf(totalCents).multiply(weights.get(i))
                    .divide(weightSum, 10, java.math.RoundingMode.HALF_UP);
            long floor = share.setScale(0, java.math.RoundingMode.FLOOR).longValueExact();
            cents[i] = floor;
            remainders[i] = share.subtract(java.math.BigDecimal.valueOf(floor));
            allocated += floor;
        }

        long leftover = totalCents - allocated;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> {
            int cmp = remainders[b].compareTo(remainders[a]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });
        for (int k = 0; k < leftover; k++) cents[order[k]] += 1;

        java.util.List<Money> result = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(new Money(java.math.BigDecimal.valueOf(cents[i], 2), currency));
        }
        return result;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/model/Money.java \
        netkar-domain/src/test/java/com/netkar/domain/model/MoneyAllocateTest.java
git commit -m "feat(domain): total-preserving Money.allocate (largest-remainder)"
```

---

### Task 5: Small value objects — `ProductRef`, `Quantity`, `Percentage`, `TransactionEffect`

**Files:**
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/ProductRef.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/Quantity.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/Percentage.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/TransactionEffect.java`
- Test: `netkar-domain/src/test/java/com/netkar/domain/model/SmallValueObjectsTest.java`

**Interfaces:**
- Consumes: `Money` (Task 2).
- Produces:
  - `ProductRef.of(String barcode)`, `String barcode()`
  - `Quantity.of(int value)`, `int value()`
  - `Percentage.ratio(Money numerator, Money denominator)`, `BigDecimal value()` (4-scale ratio)
  - enum `TransactionEffect { SALE, RETURN, CANCEL }`

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/model/SmallValueObjectsTest.java`
```java
package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SmallValueObjectsTest {

    @Test
    void product_ref_rejects_blank() {
        assertThatThrownBy(() -> ProductRef.of("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(ProductRef.of("BARCODE-1").barcode()).isEqualTo("BARCODE-1");
    }

    @Test
    void quantity_must_be_positive() {
        assertThatThrownBy(() -> Quantity.of(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Quantity.of(3).value()).isEqualTo(3);
    }

    @Test
    void percentage_is_ratio_of_two_money_values() {
        Percentage margin = Percentage.ratio(Money.tryOf("23.00"), Money.tryOf("100.00"));
        assertThat(margin.value()).isEqualByComparingTo(new BigDecimal("0.2300"));
    }

    @Test
    void percentage_rejects_zero_denominator() {
        assertThatThrownBy(() -> Percentage.ratio(Money.tryOf("1.00"), Money.zeroTry()))
            .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void transaction_effect_has_three_values() {
        assertThat(TransactionEffect.values())
            .containsExactly(TransactionEffect.SALE, TransactionEffect.RETURN, TransactionEffect.CANCEL);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-domain test`
Expected: FAIL — types not defined.

- [ ] **Step 3: Write the implementations**

`ProductRef.java`
```java
package com.netkar.domain.model;

import java.util.Objects;

public record ProductRef(String barcode) {
    public ProductRef {
        Objects.requireNonNull(barcode, "barcode");
        if (barcode.isBlank()) {
            throw new IllegalArgumentException("barcode must not be blank");
        }
    }

    public static ProductRef of(String barcode) {
        return new ProductRef(barcode);
    }
}
```

`Quantity.java`
```java
package com.netkar.domain.model;

public record Quantity(int value) {
    public Quantity {
        if (value < 1) {
            throw new IllegalArgumentException("quantity must be >= 1: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }
}
```

`Percentage.java`
```java
package com.netkar.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Percentage(BigDecimal value) {

    public static Percentage ratio(Money numerator, Money denominator) {
        if (denominator.isZero()) {
            throw new ArithmeticException("denominator must not be zero");
        }
        return new Percentage(
            numerator.amount().divide(denominator.amount(), 4, RoundingMode.HALF_UP));
    }
}
```

`TransactionEffect.java`
```java
package com.netkar.domain.model;

public enum TransactionEffect {
    SALE,
    RETURN,
    CANCEL
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/model/ProductRef.java \
        netkar-domain/src/main/java/com/netkar/domain/model/Quantity.java \
        netkar-domain/src/main/java/com/netkar/domain/model/Percentage.java \
        netkar-domain/src/main/java/com/netkar/domain/model/TransactionEffect.java \
        netkar-domain/src/test/java/com/netkar/domain/model/SmallValueObjectsTest.java
git commit -m "feat(domain): ProductRef, Quantity, Percentage, TransactionEffect"
```

---

### Task 6: `ProductCost` and `CostBook`

**Files:**
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/cost/ProductCost.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/cost/CostBook.java`
- Test: `netkar-domain/src/test/java/com/netkar/domain/model/cost/ProductCostTest.java`

**Interfaces:**
- Consumes: `Money`, `VatRate`, `VatSplit`, `ProductRef`.
- Produces:
  - `new ProductCost(Money amount, boolean vatInclusive, VatRate costVatRate)`; `Money netAmount()`, `Money vatAmount()`
  - `CostBook.of(Map<ProductRef, ProductCost>)`, `Optional<ProductCost> find(ProductRef)`

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/model/cost/ProductCostTest.java`
```java
package com.netkar.domain.model.cost;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.VatRate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductCostTest {

    @Test
    void vat_inclusive_cost_is_split_into_net_and_vat() {
        ProductCost cost = new ProductCost(Money.tryOf("120.00"), true, VatRate.of("0.20"));
        assertThat(cost.netAmount()).isEqualTo(Money.tryOf("100.00"));
        assertThat(cost.vatAmount()).isEqualTo(Money.tryOf("20.00"));
    }

    @Test
    void vat_exclusive_cost_keeps_amount_as_net_and_adds_vat() {
        ProductCost cost = new ProductCost(Money.tryOf("100.00"), false, VatRate.of("0.20"));
        assertThat(cost.netAmount()).isEqualTo(Money.tryOf("100.00"));
        assertThat(cost.vatAmount()).isEqualTo(Money.tryOf("20.00"));
    }

    @Test
    void cost_book_finds_or_returns_empty() {
        ProductRef ref = ProductRef.of("A");
        CostBook book = CostBook.of(Map.of(ref, new ProductCost(Money.tryOf("10.00"), false, VatRate.STANDARD)));
        assertThat(book.find(ref)).isPresent();
        assertThat(book.find(ProductRef.of("MISSING"))).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-domain test`
Expected: FAIL — types not defined.

- [ ] **Step 3: Write the implementations**

`ProductCost.java`
```java
package com.netkar.domain.model.cost;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.VatRate;
import java.util.Objects;

public record ProductCost(Money amount, boolean vatInclusive, VatRate costVatRate) {

    public ProductCost {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(costVatRate, "costVatRate");
    }

    public Money netAmount() {
        return vatInclusive ? amount.splitVat(costVatRate).net() : amount.roundedToMinorUnit();
    }

    public Money vatAmount() {
        return vatInclusive
            ? amount.splitVat(costVatRate).vat()
            : amount.multiply(costVatRate.value()).roundedToMinorUnit();
    }
}
```

`CostBook.java`
```java
package com.netkar.domain.model.cost;

import com.netkar.domain.model.ProductRef;
import java.util.Map;
import java.util.Optional;

public final class CostBook {

    private final Map<ProductRef, ProductCost> costs;

    private CostBook(Map<ProductRef, ProductCost> costs) {
        this.costs = Map.copyOf(costs);
    }

    public static CostBook of(Map<ProductRef, ProductCost> costs) {
        return new CostBook(costs);
    }

    public Optional<ProductCost> find(ProductRef productRef) {
        return Optional.ofNullable(costs.get(productRef));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/model/cost \
        netkar-domain/src/test/java/com/netkar/domain/model/cost
git commit -m "feat(domain): ProductCost (net/vat) and CostBook snapshot"
```

---

### Task 7: `SettlementLine` and `SettlementPackage` aggregate

**Files:**
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/SettlementLine.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/SettlementPackage.java`
- Test: `netkar-domain/src/test/java/com/netkar/domain/model/SettlementPackageTest.java`

**Interfaces:**
- Consumes: `ProductRef`, `Quantity`, `Money`, `VatRate`, `TransactionEffect`.
- Produces:
  - `new SettlementLine(ProductRef productRef, Quantity quantity, Money lineGrossAmount, VatRate saleVatRate, Money commissionAmount, Money withholdingTax, Money campaignContribution)`
  - `new SettlementPackage(String packageId, TransactionEffect effect, List<SettlementLine> lines, Money shippingFee, Money serviceFee, Money penalty, Money earlyPaymentFee)`
  - accessors `packageId()`, `effect()`, `lines()`, `shippingFee()`, `serviceFee()`, `penalty()`, `earlyPaymentFee()`

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/model/SettlementPackageTest.java`
```java
package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SettlementPackageTest {

    private static SettlementLine line() {
        return new SettlementLine(
            ProductRef.of("A"), Quantity.of(1), Money.tryOf("100.00"), VatRate.STANDARD,
            Money.tryOf("15.00"), Money.tryOf("0.80"), Money.zeroTry());
    }

    @Test
    void builds_a_valid_sale_package() {
        SettlementPackage pkg = new SettlementPackage(
            "PKG-1", TransactionEffect.SALE, List.of(line()),
            Money.tryOf("30.00"), Money.tryOf("8.49"), Money.zeroTry(), Money.zeroTry());
        assertThat(pkg.lines()).hasSize(1);
        assertThat(pkg.effect()).isEqualTo(TransactionEffect.SALE);
    }

    @Test
    void rejects_empty_lines() {
        assertThatThrownBy(() -> new SettlementPackage(
            "PKG-1", TransactionEffect.SALE, List.of(),
            Money.zeroTry(), Money.zeroTry(), Money.zeroTry(), Money.zeroTry()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_negative_package_fee() {
        assertThatThrownBy(() -> new SettlementPackage(
            "PKG-1", TransactionEffect.SALE, List.of(line()),
            Money.tryOf("-1.00"), Money.zeroTry(), Money.zeroTry(), Money.zeroTry()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-domain test`
Expected: FAIL — types not defined.

- [ ] **Step 3: Write the implementations**

`SettlementLine.java`
```java
package com.netkar.domain.model;

import java.util.Objects;

public record SettlementLine(
    ProductRef productRef,
    Quantity quantity,
    Money lineGrossAmount,
    VatRate saleVatRate,
    Money commissionAmount,
    Money withholdingTax,
    Money campaignContribution) {

    public SettlementLine {
        Objects.requireNonNull(productRef, "productRef");
        Objects.requireNonNull(quantity, "quantity");
        requireNonNegative(lineGrossAmount, "lineGrossAmount");
        Objects.requireNonNull(saleVatRate, "saleVatRate");
        requireNonNegative(commissionAmount, "commissionAmount");
        requireNonNegative(withholdingTax, "withholdingTax");
        requireNonNegative(campaignContribution, "campaignContribution");
    }

    private static void requireNonNegative(Money money, String field) {
        Objects.requireNonNull(money, field);
        if (money.isNegative()) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }
}
```

`SettlementPackage.java`
```java
package com.netkar.domain.model;

import java.util.List;
import java.util.Objects;

public final class SettlementPackage {

    private final String packageId;
    private final TransactionEffect effect;
    private final List<SettlementLine> lines;
    private final Money shippingFee;
    private final Money serviceFee;
    private final Money penalty;
    private final Money earlyPaymentFee;

    public SettlementPackage(
        String packageId,
        TransactionEffect effect,
        List<SettlementLine> lines,
        Money shippingFee,
        Money serviceFee,
        Money penalty,
        Money earlyPaymentFee) {
        this.packageId = Objects.requireNonNull(packageId, "packageId");
        this.effect = Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("a settlement package must have at least one line");
        }
        this.lines = List.copyOf(lines);
        this.shippingFee = requireNonNegative(shippingFee, "shippingFee");
        this.serviceFee = requireNonNegative(serviceFee, "serviceFee");
        this.penalty = requireNonNegative(penalty, "penalty");
        this.earlyPaymentFee = requireNonNegative(earlyPaymentFee, "earlyPaymentFee");
    }

    private static Money requireNonNegative(Money money, String field) {
        Objects.requireNonNull(money, field);
        if (money.isNegative()) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return money;
    }

    public String packageId() { return packageId; }
    public TransactionEffect effect() { return effect; }
    public List<SettlementLine> lines() { return lines; }
    public Money shippingFee() { return shippingFee; }
    public Money serviceFee() { return serviceFee; }
    public Money penalty() { return penalty; }
    public Money earlyPaymentFee() { return earlyPaymentFee; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/model/SettlementLine.java \
        netkar-domain/src/main/java/com/netkar/domain/model/SettlementPackage.java \
        netkar-domain/src/test/java/com/netkar/domain/model/SettlementPackageTest.java
git commit -m "feat(domain): SettlementLine and SettlementPackage aggregate with invariants"
```

---

### Task 8: `AllocationStrategy` policy and `RevenueWeightedAllocation`

**Files:**
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/allocation/AllocationStrategy.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/model/allocation/RevenueWeightedAllocation.java`
- Test: `netkar-domain/src/test/java/com/netkar/domain/model/allocation/RevenueWeightedAllocationTest.java`

**Interfaces:**
- Consumes: `Money`, `SettlementLine`.
- Produces: `List<Money> AllocationStrategy.allocate(Money total, List<SettlementLine> lines)` — aligned to input order, sum-preserving. `RevenueWeightedAllocation` weights by `lineGrossAmount`.

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/model/allocation/RevenueWeightedAllocationTest.java`
```java
package com.netkar.domain.model.allocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.Quantity;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.VatRate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RevenueWeightedAllocationTest {

    private static SettlementLine line(String ref, String gross) {
        return new SettlementLine(ProductRef.of(ref), Quantity.of(1), Money.tryOf(gross),
            VatRate.STANDARD, Money.zeroTry(), Money.zeroTry(), Money.zeroTry());
    }

    @Test
    void splits_total_by_revenue_weight_preserving_sum() {
        AllocationStrategy strategy = new RevenueWeightedAllocation();
        List<SettlementLine> lines = List.of(line("A", "75.00"), line("B", "25.00"));

        List<Money> shares = strategy.allocate(Money.tryOf("40.00"), lines);

        assertThat(shares).containsExactly(Money.tryOf("30.00"), Money.tryOf("10.00"));
        assertThat(shares.stream().reduce(Money.zeroTry(), Money::add)).isEqualTo(Money.tryOf("40.00"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-domain test`
Expected: FAIL — types not defined.

- [ ] **Step 3: Write the implementations**

`AllocationStrategy.java`
```java
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
```

`RevenueWeightedAllocation.java`
```java
package com.netkar.domain.model.allocation;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.SettlementLine;
import java.math.BigDecimal;
import java.util.List;

public final class RevenueWeightedAllocation implements AllocationStrategy {

    @Override
    public List<Money> allocate(Money total, List<SettlementLine> lines) {
        List<BigDecimal> weights = lines.stream()
            .map(line -> line.lineGrossAmount().amount())
            .toList();
        return total.allocate(weights);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/model/allocation \
        netkar-domain/src/test/java/com/netkar/domain/model/allocation
git commit -m "feat(domain): AllocationStrategy policy + RevenueWeightedAllocation"
```

---

### Task 9: Result value objects — `EstimatedNetVatBurden`, `ProfitBreakdown`, `PackageProfit`

**Files:**
- Create: `netkar-domain/src/main/java/com/netkar/domain/result/EstimatedNetVatBurden.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/result/ProfitBreakdown.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/result/PackageProfit.java`
- Test: `netkar-domain/src/test/java/com/netkar/domain/result/EstimatedNetVatBurdenTest.java`

**Interfaces:**
- Consumes: `Money`, `ProductRef`, `Percentage`.
- Produces:
  - `EstimatedNetVatBurden.of(Money saleVat, Money cogsVat, Money shippingVat, Money commissionVat, Money serviceFeeVat)`; accessor `Money net()` + component accessors.
  - `ProfitBreakdown` record with fields: `productRef, revenueNet, cogsNet, commission, serviceFeeShare, shippingShare, withholdingTax, campaignContribution, penaltyShare, earlyPaymentShare, estimatedNetVatBurden, netProfit, margin (Optional<Percentage>), allocated (boolean), missingCost (boolean)`.
  - `PackageProfit` record `(String packageId, List<ProfitBreakdown> lines)`.

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/result/EstimatedNetVatBurdenTest.java`
```java
package com.netkar.domain.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import org.junit.jupiter.api.Test;

class EstimatedNetVatBurdenTest {

    @Test
    void net_is_output_vat_minus_all_input_vats() {
        EstimatedNetVatBurden vat = EstimatedNetVatBurden.of(
            Money.tryOf("20.00"),  // saleVat (output)
            Money.tryOf("10.00"),  // cogsVat
            Money.tryOf("2.00"),   // shippingVat
            Money.tryOf("3.00"),   // commissionVat
            Money.tryOf("1.70"));  // serviceFeeVat
        assertThat(vat.net()).isEqualTo(Money.tryOf("3.30"));
    }

    @Test
    void net_can_be_negative_meaning_carry_forward_vat_credit() {
        EstimatedNetVatBurden vat = EstimatedNetVatBurden.of(
            Money.tryOf("1.00"), Money.tryOf("2.00"), Money.zeroTry(),
            Money.zeroTry(), Money.zeroTry());
        assertThat(vat.net().isNegative()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-domain test`
Expected: FAIL — types not defined.

- [ ] **Step 3: Write the implementations**

`EstimatedNetVatBurden.java`
```java
package com.netkar.domain.result;

import com.netkar.domain.model.Money;

public record EstimatedNetVatBurden(
    Money saleVat,
    Money cogsVat,
    Money shippingVat,
    Money commissionVat,
    Money serviceFeeVat,
    Money net) {

    public static EstimatedNetVatBurden of(
        Money saleVat, Money cogsVat, Money shippingVat, Money commissionVat, Money serviceFeeVat) {
        Money net = saleVat
            .subtract(cogsVat)
            .subtract(shippingVat)
            .subtract(commissionVat)
            .subtract(serviceFeeVat);
        return new EstimatedNetVatBurden(
            saleVat, cogsVat, shippingVat, commissionVat, serviceFeeVat, net);
    }
}
```

`ProfitBreakdown.java`
```java
package com.netkar.domain.result;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.Percentage;
import com.netkar.domain.model.ProductRef;
import java.util.Optional;

public record ProfitBreakdown(
    ProductRef productRef,
    Money revenueNet,
    Money cogsNet,
    Money commission,
    Money serviceFeeShare,
    Money shippingShare,
    Money withholdingTax,
    Money campaignContribution,
    Money penaltyShare,
    Money earlyPaymentShare,
    EstimatedNetVatBurden estimatedNetVatBurden,
    Money netProfit,
    Optional<Percentage> margin,
    boolean allocated,
    boolean missingCost) {

    public boolean isLoss() {
        return netProfit.isNegative();
    }
}
```

`PackageProfit.java`
```java
package com.netkar.domain.result;

import java.util.List;

public record PackageProfit(String packageId, List<ProfitBreakdown> lines) {
    public PackageProfit(String packageId, List<ProfitBreakdown> lines) {
        this.packageId = packageId;
        this.lines = List.copyOf(lines);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/result \
        netkar-domain/src/test/java/com/netkar/domain/result
git commit -m "feat(domain): result VOs EstimatedNetVatBurden, ProfitBreakdown, PackageProfit"
```

---

### Task 10: `ProfitCalculator` — single SALE line + RETURN guard

**Files:**
- Create: `netkar-domain/src/main/java/com/netkar/domain/service/ProfitCalculator.java`
- Test: `netkar-domain/src/test/java/com/netkar/domain/service/ProfitCalculatorSingleLineTest.java`

**Interfaces:**
- Consumes: `SettlementPackage`, `CostBook`, `AllocationStrategy`, all model + result VOs.
- Produces: `new ProfitCalculator(AllocationStrategy strategy)`; `PackageProfit calculate(SettlementPackage pkg, CostBook costs)`. Throws `UnsupportedOperationException` when `pkg.effect() != SALE`.

**Worked single-line example (golden):** gross 120.00 @20% → net 100.00, saleVat 20.00. Cost 60.00 KDV-hariç @20% → cogsNet 60.00, cogsVat 12.00. commission 15.00 → commissionVat 3.00. shippingFee 12.00 (whole line) → split @20% → shippingNet 10.00, shippingVat 2.00. serviceFee 8.49 → split @20% → serviceNet 7.07, serviceVat 1.42. withholdingTax 1.00, campaign 0, penalty 0, early 0.
- `EstimatedNetVatBurden.net` = 20.00 − 12.00 − 2.00 − 3.00 − 1.42 = **1.58**
- `netProfit` = 100.00 − 60.00 − 15.00 − 7.07 − 10.00 − 1.00 − 0 − 0 − 0 = **6.93**

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/service/ProfitCalculatorSingleLineTest.java`
```java
package com.netkar.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.Quantity;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.TransactionEffect;
import com.netkar.domain.model.VatRate;
import com.netkar.domain.model.allocation.RevenueWeightedAllocation;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.model.cost.ProductCost;
import com.netkar.domain.result.PackageProfit;
import com.netkar.domain.result.ProfitBreakdown;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfitCalculatorSingleLineTest {

    private final ProfitCalculator calculator = new ProfitCalculator(new RevenueWeightedAllocation());

    private SettlementPackage salePackage() {
        SettlementLine line = new SettlementLine(
            ProductRef.of("A"), Quantity.of(1), Money.tryOf("120.00"), VatRate.of("0.20"),
            Money.tryOf("15.00"), Money.tryOf("1.00"), Money.zeroTry());
        return new SettlementPackage("PKG-1", TransactionEffect.SALE, List.of(line),
            Money.tryOf("12.00"), Money.tryOf("8.49"), Money.zeroTry(), Money.zeroTry());
    }

    private CostBook costs() {
        return CostBook.of(Map.of(
            ProductRef.of("A"), new ProductCost(Money.tryOf("60.00"), false, VatRate.of("0.20"))));
    }

    @Test
    void computes_net_profit_and_vat_burden_for_one_line() {
        PackageProfit result = calculator.calculate(salePackage(), costs());
        ProfitBreakdown b = result.lines().get(0);

        assertThat(b.revenueNet()).isEqualTo(Money.tryOf("100.00"));
        assertThat(b.cogsNet()).isEqualTo(Money.tryOf("60.00"));
        assertThat(b.commission()).isEqualTo(Money.tryOf("15.00"));
        assertThat(b.shippingShare()).isEqualTo(Money.tryOf("10.00"));
        assertThat(b.serviceFeeShare()).isEqualTo(Money.tryOf("7.07"));
        assertThat(b.estimatedNetVatBurden().net()).isEqualTo(Money.tryOf("1.58"));
        assertThat(b.netProfit()).isEqualTo(Money.tryOf("6.93"));
        assertThat(b.missingCost()).isFalse();
        assertThat(b.margin()).isPresent();
    }

    @Test
    void rejects_non_sale_effect_as_deferred() {
        SettlementLine line = new SettlementLine(
            ProductRef.of("A"), Quantity.of(1), Money.tryOf("120.00"), VatRate.of("0.20"),
            Money.tryOf("15.00"), Money.zeroTry(), Money.zeroTry());
        SettlementPackage returnPkg = new SettlementPackage("PKG-2", TransactionEffect.RETURN,
            List.of(line), Money.zeroTry(), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());

        assertThatThrownBy(() -> calculator.calculate(returnPkg, costs()))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("RETURN");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-domain test`
Expected: FAIL — `ProfitCalculator` not defined.

- [ ] **Step 3: Write the implementation**

`netkar-domain/src/main/java/com/netkar/domain/service/ProfitCalculator.java`
```java
package com.netkar.domain.service;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.Percentage;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.TransactionEffect;
import com.netkar.domain.model.VatRate;
import com.netkar.domain.model.VatSplit;
import com.netkar.domain.model.allocation.AllocationStrategy;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.model.cost.ProductCost;
import com.netkar.domain.result.EstimatedNetVatBurden;
import com.netkar.domain.result.PackageProfit;
import com.netkar.domain.result.ProfitBreakdown;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProfitCalculator {

    private final AllocationStrategy allocationStrategy;

    public ProfitCalculator(AllocationStrategy allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public PackageProfit calculate(SettlementPackage pkg, CostBook costs) {
        if (pkg.effect() != TransactionEffect.SALE) {
            throw new UnsupportedOperationException(
                "Only SALE is supported; " + pkg.effect()
                    + " arithmetic is deferred to the connector sub-project");
        }

        List<SettlementLine> lines = pkg.lines();
        List<Money> shipping = allocationStrategy.allocate(pkg.shippingFee(), lines);
        List<Money> service = allocationStrategy.allocate(pkg.serviceFee(), lines);
        List<Money> penalty = allocationStrategy.allocate(pkg.penalty(), lines);
        List<Money> early = allocationStrategy.allocate(pkg.earlyPaymentFee(), lines);

        boolean allocated = lines.size() > 1;
        List<ProfitBreakdown> breakdowns = new ArrayList<>(lines.size());

        for (int i = 0; i < lines.size(); i++) {
            SettlementLine line = lines.get(i);

            VatSplit sale = line.lineGrossAmount().splitVat(line.saleVatRate());

            Optional<ProductCost> cost = costs.find(line.productRef());
            boolean missingCost = cost.isEmpty();
            Money cogsNet = cost.map(ProductCost::netAmount).orElse(Money.zeroTry());
            Money cogsVat = cost.map(ProductCost::vatAmount).orElse(Money.zeroTry());

            Money commissionNet = line.commissionAmount().roundedToMinorUnit();
            Money commissionVat = line.commissionAmount()
                .multiply(VatRate.STANDARD.value()).roundedToMinorUnit();

            VatSplit serviceSplit = service.get(i).splitVat(VatRate.STANDARD);
            VatSplit shippingSplit = shipping.get(i).splitVat(VatRate.STANDARD);

            EstimatedNetVatBurden vat = EstimatedNetVatBurden.of(
                sale.vat(), cogsVat, shippingSplit.vat(), commissionVat, serviceSplit.vat());

            Money netProfit = sale.net()
                .subtract(cogsNet)
                .subtract(commissionNet)
                .subtract(serviceSplit.net())
                .subtract(shippingSplit.net())
                .subtract(line.withholdingTax().roundedToMinorUnit())
                .subtract(line.campaignContribution().roundedToMinorUnit())
                .subtract(penalty.get(i))
                .subtract(early.get(i));

            Optional<Percentage> margin = sale.net().isZero()
                ? Optional.empty()
                : Optional.of(Percentage.ratio(netProfit, sale.net()));

            breakdowns.add(new ProfitBreakdown(
                line.productRef(), sale.net(), cogsNet, commissionNet,
                serviceSplit.net(), shippingSplit.net(),
                line.withholdingTax().roundedToMinorUnit(),
                line.campaignContribution().roundedToMinorUnit(),
                penalty.get(i), early.get(i), vat, netProfit, margin, allocated, missingCost));
        }

        return new PackageProfit(pkg.packageId(), breakdowns);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS (both single-line golden and RETURN guard).

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/service/ProfitCalculator.java \
        netkar-domain/src/test/java/com/netkar/domain/service/ProfitCalculatorSingleLineTest.java
git commit -m "feat(domain): ProfitCalculator SALE single-line + RETURN deferral guard"
```

---

### Task 11: `ProfitCalculator` — multi-line package-fee allocation (golden multi-product)

**Files:**
- Test: `netkar-domain/src/test/java/com/netkar/domain/service/ProfitCalculatorAllocationTest.java`

**Interfaces:**
- Consumes: `ProfitCalculator` (Task 10). No production code change expected — this proves allocation behavior already implemented; if the assertions fail, fix `ProfitCalculator`.

**Worked multi-product example (golden):** package effect SALE, two lines.
Line A gross 75.00 @20% (net 62.50, saleVat 12.50); Line B gross 25.00 @20% (net 20.83, saleVat 4.17). shippingFee 40.00, serviceFee 0, penalty 0, early 0. Weights 75:25 → shippingShare A 30.00, B 10.00 (sum 40.00). Shipping split @20%: A net 25.00/vat 5.00; B net 8.33/vat 1.67. No cost in book → missingCost true, cogs 0.
- A netProfit = 62.50 − 0 − 0 − 0 − 25.00 = **37.50**; B netProfit = 20.83 − 8.33 = **12.50**
- shipping shares sum to 40.00 (allocation invariant).

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/service/ProfitCalculatorAllocationTest.java`
```java
package com.netkar.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.Quantity;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.TransactionEffect;
import com.netkar.domain.model.VatRate;
import com.netkar.domain.model.allocation.RevenueWeightedAllocation;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.result.PackageProfit;
import com.netkar.domain.result.ProfitBreakdown;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfitCalculatorAllocationTest {

    private final ProfitCalculator calculator = new ProfitCalculator(new RevenueWeightedAllocation());

    private SettlementLine line(String ref, String gross) {
        return new SettlementLine(ProductRef.of(ref), Quantity.of(1), Money.tryOf(gross),
            VatRate.of("0.20"), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());
    }

    @Test
    void allocates_shipping_revenue_weighted_and_preserves_total() {
        SettlementPackage pkg = new SettlementPackage("PKG-1", TransactionEffect.SALE,
            List.of(line("A", "75.00"), line("B", "25.00")),
            Money.tryOf("40.00"), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());

        PackageProfit result = calculator.calculate(pkg, CostBook.of(Map.of()));
        ProfitBreakdown a = result.lines().get(0);
        ProfitBreakdown b = result.lines().get(1);

        assertThat(a.shippingShare()).isEqualTo(Money.tryOf("25.00")); // net part of 30.00 gross share
        assertThat(b.shippingShare()).isEqualTo(Money.tryOf("8.33"));
        assertThat(a.netProfit()).isEqualTo(Money.tryOf("37.50"));
        assertThat(b.netProfit()).isEqualTo(Money.tryOf("12.50"));
        assertThat(a.allocated()).isTrue();
        assertThat(a.missingCost()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails or passes**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS if Task 10 implemented allocation correctly. If FAIL, fix `ProfitCalculator` allocation/loop until green (do not change the test's golden numbers).

- [ ] **Step 3: (Only if Step 2 failed) Fix the implementation**

Re-read `ProfitCalculator.calculate` allocation loop; ensure package fees are allocated via `allocationStrategy` and split with `VatRate.STANDARD`. Re-run until green.

- [ ] **Step 4: Confirm green**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/test/java/com/netkar/domain/service/ProfitCalculatorAllocationTest.java
git commit -m "test(domain): golden multi-product allocation invariant for ProfitCalculator"
```

---

### Task 12: `ProfitCalculator` — edge cases (missing cost, zero VAT, KDV-dahil cost, costVatRate≠saleVatRate, margin guard)

**Files:**
- Test: `netkar-domain/src/test/java/com/netkar/domain/service/ProfitCalculatorEdgeCasesTest.java`

**Interfaces:**
- Consumes: `ProfitCalculator` (Task 10). No production change expected; if an assertion fails, fix `ProfitCalculator`.

- [ ] **Step 1: Write the failing test**

`netkar-domain/src/test/java/com/netkar/domain/service/ProfitCalculatorEdgeCasesTest.java`
```java
package com.netkar.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.Quantity;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.TransactionEffect;
import com.netkar.domain.model.VatRate;
import com.netkar.domain.model.allocation.RevenueWeightedAllocation;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.model.cost.ProductCost;
import com.netkar.domain.result.ProfitBreakdown;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfitCalculatorEdgeCasesTest {

    private final ProfitCalculator calculator = new ProfitCalculator(new RevenueWeightedAllocation());

    private SettlementPackage singleLine(String gross, String vatRate, Money commission) {
        SettlementLine line = new SettlementLine(ProductRef.of("A"), Quantity.of(1),
            Money.tryOf(gross), VatRate.of(vatRate), commission, Money.zeroTry(), Money.zeroTry());
        return new SettlementPackage("PKG", TransactionEffect.SALE, List.of(line),
            Money.zeroTry(), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());
    }

    @Test
    void missing_cost_flags_line_and_treats_cogs_as_zero() {
        ProfitBreakdown b = calculator.calculate(
            singleLine("100.00", "0.20", Money.zeroTry()), CostBook.of(Map.of())).lines().get(0);
        assertThat(b.missingCost()).isTrue();
        assertThat(b.cogsNet()).isEqualTo(Money.zeroTry());
    }

    @Test
    void zero_vat_rate_yields_zero_sale_vat() {
        ProfitBreakdown b = calculator.calculate(
            singleLine("100.00", "0.00", Money.zeroTry()), CostBook.of(Map.of())).lines().get(0);
        assertThat(b.revenueNet()).isEqualTo(Money.tryOf("100.00"));
        assertThat(b.estimatedNetVatBurden().saleVat()).isEqualTo(Money.zeroTry());
    }

    @Test
    void vat_inclusive_cost_with_different_cost_vat_rate_affects_net_profit() {
        // sale gross 200.00 @20% -> net 166.67 ; cost 110.00 KDV-dahil @10% -> cogsNet 100.00
        CostBook costs = CostBook.of(Map.of(
            ProductRef.of("A"), new ProductCost(Money.tryOf("110.00"), true, VatRate.of("0.10"))));
        ProfitBreakdown b = calculator.calculate(
            singleLine("200.00", "0.20", Money.zeroTry()), costs).lines().get(0);
        assertThat(b.cogsNet()).isEqualTo(Money.tryOf("100.00"));
        assertThat(b.netProfit()).isEqualTo(Money.tryOf("66.67")); // 166.67 - 100.00
    }

    @Test
    void loss_making_line_is_marked_as_loss() {
        CostBook costs = CostBook.of(Map.of(
            ProductRef.of("A"), new ProductCost(Money.tryOf("90.00"), false, VatRate.of("0.20"))));
        ProfitBreakdown b = calculator.calculate(
            singleLine("100.00", "0.20", Money.tryOf("20.00")), costs).lines().get(0);
        // net 83.33 - cogs 90.00 - commission 20.00 = -26.67
        assertThat(b.netProfit().isNegative()).isTrue();
        assertThat(b.isLoss()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify outcome**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS if Task 10 logic is correct. If FAIL, fix `ProfitCalculator` (do not weaken the golden numbers).

- [ ] **Step 3: (Only if Step 2 failed) Fix the implementation**

Adjust `ProfitCalculator` until all edge cases are green.

- [ ] **Step 4: Confirm green**

Run: `mvn -q -pl netkar-domain test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/test/java/com/netkar/domain/service/ProfitCalculatorEdgeCasesTest.java
git commit -m "test(domain): ProfitCalculator edge cases (missing cost, vat, cost vat rate, loss)"
```

---

### Task 13: Roll-up (`ProductProfitability`, `RedList`) + application use-case

**Files:**
- Create: `netkar-domain/src/main/java/com/netkar/domain/result/ProductProfitability.java`
- Create: `netkar-domain/src/main/java/com/netkar/domain/result/RedList.java`
- Create: `netkar-application/src/main/java/com/netkar/application/CalculateProductProfitabilityUseCase.java`
- Test: `netkar-application/src/test/java/com/netkar/application/CalculateProductProfitabilityUseCaseTest.java`

**Interfaces:**
- Consumes: `ProfitCalculator`, `PackageProfit`, `ProfitBreakdown`, `ProductRef`, `Money`, `SettlementPackage`, `CostBook`.
- Produces:
  - `ProductProfitability.from(ProductRef ref, List<ProfitBreakdown> breakdowns)`; fields `productRef, lineCount, totalRevenueNet, totalNetProfit, totalEstimatedVatBurden, anyMissingCost`; `boolean isLoss()`.
  - `RedList.from(Collection<ProductProfitability>)` → loss-making, sorted by `totalNetProfit` ascending; `List<ProductProfitability> items()`.
  - `CalculateProductProfitabilityUseCase(ProfitCalculator calculator)`; `Result calculate(List<SettlementPackage> packages, CostBook costs)`; `Result` record `(List<ProductProfitability> products, RedList redList)`.

- [ ] **Step 1: Write the failing test**

`netkar-application/src/test/java/com/netkar/application/CalculateProductProfitabilityUseCaseTest.java`
```java
package com.netkar.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.Quantity;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.TransactionEffect;
import com.netkar.domain.model.VatRate;
import com.netkar.domain.model.allocation.RevenueWeightedAllocation;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.model.cost.ProductCost;
import com.netkar.domain.service.ProfitCalculator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculateProductProfitabilityUseCaseTest {

    private final CalculateProductProfitabilityUseCase useCase =
        new CalculateProductProfitabilityUseCase(new ProfitCalculator(new RevenueWeightedAllocation()));

    private SettlementPackage pkg(String id, String ref, String gross, Money commission) {
        SettlementLine line = new SettlementLine(ProductRef.of(ref), Quantity.of(1),
            Money.tryOf(gross), VatRate.of("0.20"), commission, Money.zeroTry(), Money.zeroTry());
        return new SettlementPackage(id, TransactionEffect.SALE, List.of(line),
            Money.zeroTry(), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());
    }

    @Test
    void aggregates_per_product_across_packages_and_builds_red_list() {
        CostBook costs = CostBook.of(Map.of(
            ProductRef.of("WIN"), new ProductCost(Money.tryOf("10.00"), false, VatRate.of("0.20")),
            ProductRef.of("LOSE"), new ProductCost(Money.tryOf("90.00"), false, VatRate.of("0.20"))));

        var result = useCase.calculate(List.of(
            pkg("P1", "WIN", "100.00", Money.zeroTry()),
            pkg("P2", "WIN", "100.00", Money.zeroTry()),
            pkg("P3", "LOSE", "100.00", Money.tryOf("20.00"))), costs);

        assertThat(result.products()).hasSize(2);
        assertThat(result.redList().items()).hasSize(1);
        assertThat(result.redList().items().get(0).productRef()).isEqualTo(ProductRef.of("LOSE"));
        assertThat(result.redList().items().get(0).isLoss()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-application -am test`
Expected: FAIL — use-case / roll-up types not defined.

- [ ] **Step 3: Write the implementations**

`netkar-domain/src/main/java/com/netkar/domain/result/ProductProfitability.java`
```java
package com.netkar.domain.result;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import java.util.List;

public record ProductProfitability(
    ProductRef productRef,
    int lineCount,
    Money totalRevenueNet,
    Money totalNetProfit,
    Money totalEstimatedVatBurden,
    boolean anyMissingCost) {

    public static ProductProfitability from(ProductRef productRef, List<ProfitBreakdown> breakdowns) {
        Money revenue = Money.zeroTry();
        Money profit = Money.zeroTry();
        Money vat = Money.zeroTry();
        boolean missing = false;
        for (ProfitBreakdown b : breakdowns) {
            revenue = revenue.add(b.revenueNet());
            profit = profit.add(b.netProfit());
            vat = vat.add(b.estimatedNetVatBurden().net());
            missing = missing || b.missingCost();
        }
        return new ProductProfitability(
            productRef, breakdowns.size(), revenue, profit, vat, missing);
    }

    public boolean isLoss() {
        return totalNetProfit.isNegative();
    }
}
```

`netkar-domain/src/main/java/com/netkar/domain/result/RedList.java`
```java
package com.netkar.domain.result;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record RedList(List<ProductProfitability> items) {

    public RedList(List<ProductProfitability> items) {
        this.items = List.copyOf(items);
    }

    public static RedList from(Collection<ProductProfitability> all) {
        List<ProductProfitability> losers = all.stream()
            .filter(ProductProfitability::isLoss)
            .sorted(Comparator.comparing(p -> p.totalNetProfit().amount()))
            .toList();
        return new RedList(losers);
    }
}
```

`netkar-application/src/main/java/com/netkar/application/CalculateProductProfitabilityUseCase.java`
```java
package com.netkar.application;

import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.result.PackageProfit;
import com.netkar.domain.result.ProductProfitability;
import com.netkar.domain.result.ProfitBreakdown;
import com.netkar.domain.result.RedList;
import com.netkar.domain.service.ProfitCalculator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CalculateProductProfitabilityUseCase {

    private final ProfitCalculator calculator;

    public CalculateProductProfitabilityUseCase(ProfitCalculator calculator) {
        this.calculator = calculator;
    }

    public Result calculate(List<SettlementPackage> packages, CostBook costs) {
        Map<ProductRef, List<ProfitBreakdown>> byProduct = new LinkedHashMap<>();
        for (SettlementPackage pkg : packages) {
            PackageProfit profit = calculator.calculate(pkg, costs);
            for (ProfitBreakdown breakdown : profit.lines()) {
                byProduct.computeIfAbsent(breakdown.productRef(), key -> new ArrayList<>())
                    .add(breakdown);
            }
        }

        List<ProductProfitability> products = new ArrayList<>();
        byProduct.forEach((ref, breakdowns) ->
            products.add(ProductProfitability.from(ref, breakdowns)));

        return new Result(List.copyOf(products), RedList.from(products));
    }

    public record Result(List<ProductProfitability> products, RedList redList) {
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl netkar-application -am test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add netkar-domain/src/main/java/com/netkar/domain/result/ProductProfitability.java \
        netkar-domain/src/main/java/com/netkar/domain/result/RedList.java \
        netkar-application/src/main/java/com/netkar/application/CalculateProductProfitabilityUseCase.java \
        netkar-application/src/test/java/com/netkar/application/CalculateProductProfitabilityUseCaseTest.java
git commit -m "feat(application): product roll-up, red list, and calculate use-case"
```

---

### Task 14: Infrastructure demo runner + ArchUnit boundary tests

**Files:**
- Create: `netkar-infrastructure/src/main/java/com/netkar/infrastructure/NetKarDemoApplication.java`
- Create: `netkar-infrastructure/src/main/java/com/netkar/infrastructure/SampleData.java`
- Create: `netkar-infrastructure/src/main/java/com/netkar/infrastructure/DemoRunner.java`
- Create: `netkar-infrastructure/src/main/resources/application.properties`
- Test: `netkar-infrastructure/src/test/java/com/netkar/infrastructure/architecture/HexagonalBoundaryTest.java`

**Interfaces:**
- Consumes: `CalculateProductProfitabilityUseCase`, `ProfitCalculator`, `RevenueWeightedAllocation`, sample-building model types.
- Produces: a runnable Spring Boot app printing per-product profit + red list; ArchUnit rules guarding the dependency rule.

**Note:** JSON fixtures are intentionally NOT used here — JSON→domain mapping belongs to the connector sub-project. The demo builds sample data in code via `SampleData`.

- [ ] **Step 1: Write the failing ArchUnit test**

`netkar-infrastructure/src/test/java/com/netkar/infrastructure/architecture/HexagonalBoundaryTest.java`
```java
package com.netkar.infrastructure.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HexagonalBoundaryTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.netkar");
    }

    @Test
    void domain_does_not_depend_on_spring_or_jakarta() {
        noClasses().that().resideInAPackage("com.netkar.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..")
            .check(classes);
    }

    @Test
    void domain_does_not_depend_on_application_or_infrastructure() {
        noClasses().that().resideInAPackage("com.netkar.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.netkar.application..", "com.netkar.infrastructure..")
            .check(classes);
    }

    @Test
    void application_does_not_depend_on_infrastructure_or_spring() {
        noClasses().that().resideInAPackage("com.netkar.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.netkar.infrastructure..", "org.springframework..")
            .check(classes);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl netkar-infrastructure -am test`
Expected: FAIL — `com.netkar` classes for infrastructure not yet present / app context missing (compilation error on missing main classes).

- [ ] **Step 3: Write the implementations**

`netkar-infrastructure/src/main/resources/application.properties`
```properties
spring.main.web-application-type=none
spring.main.banner-mode=off
```

`netkar-infrastructure/src/main/java/com/netkar/infrastructure/NetKarDemoApplication.java`
```java
package com.netkar.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NetKarDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetKarDemoApplication.class, args);
    }
}
```

`netkar-infrastructure/src/main/java/com/netkar/infrastructure/SampleData.java`
```java
package com.netkar.infrastructure;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.Quantity;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.TransactionEffect;
import com.netkar.domain.model.VatRate;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.model.cost.ProductCost;
import java.util.List;
import java.util.Map;

final class SampleData {

    private SampleData() {
    }

    static List<SettlementPackage> packages() {
        SettlementLine winner = new SettlementLine(
            ProductRef.of("WIN-1"), Quantity.of(1), Money.tryOf("120.00"), VatRate.of("0.20"),
            Money.tryOf("15.00"), Money.tryOf("1.00"), Money.zeroTry());
        SettlementLine loser = new SettlementLine(
            ProductRef.of("LOSE-1"), Quantity.of(1), Money.tryOf("100.00"), VatRate.of("0.20"),
            Money.tryOf("20.00"), Money.tryOf("0.80"), Money.zeroTry());

        return List.of(
            new SettlementPackage("PKG-1", TransactionEffect.SALE, List.of(winner),
                Money.tryOf("12.00"), Money.tryOf("8.49"), Money.zeroTry(), Money.zeroTry()),
            new SettlementPackage("PKG-2", TransactionEffect.SALE, List.of(loser),
                Money.tryOf("18.00"), Money.tryOf("8.49"), Money.zeroTry(), Money.zeroTry()));
    }

    static CostBook costs() {
        return CostBook.of(Map.of(
            ProductRef.of("WIN-1"), new ProductCost(Money.tryOf("60.00"), false, VatRate.of("0.20")),
            ProductRef.of("LOSE-1"), new ProductCost(Money.tryOf("85.00"), false, VatRate.of("0.20"))));
    }
}
```

`netkar-infrastructure/src/main/java/com/netkar/infrastructure/DemoRunner.java`
```java
package com.netkar.infrastructure;

import com.netkar.application.CalculateProductProfitabilityUseCase;
import com.netkar.domain.model.allocation.RevenueWeightedAllocation;
import com.netkar.domain.result.ProductProfitability;
import com.netkar.domain.service.ProfitCalculator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        CalculateProductProfitabilityUseCase useCase =
            new CalculateProductProfitabilityUseCase(new ProfitCalculator(new RevenueWeightedAllocation()));

        var result = useCase.calculate(SampleData.packages(), SampleData.costs());

        System.out.println("=== Ürün bazlı kârlılık ===");
        for (ProductProfitability p : result.products()) {
            System.out.printf("%-8s  netProfit=%s  vatBurden=%s  missingCost=%s%n",
                p.productRef().barcode(), p.totalNetProfit(), p.totalEstimatedVatBurden(),
                p.anyMissingCost());
        }

        System.out.println("=== Kırmızı liste (zarar edenler) ===");
        result.redList().items().forEach(p ->
            System.out.printf("%-8s  netProfit=%s%n", p.productRef().barcode(), p.totalNetProfit()));
    }
}
```

- [ ] **Step 4: Run tests + the demo to verify**

Run: `mvn -q -pl netkar-infrastructure -am test`
Expected: PASS (ArchUnit boundary rules green).

Run: `mvn -q -pl netkar-infrastructure -am spring-boot:run`
Expected: prints two products under "Ürün bazlı kârlılık" and `LOSE-1` under "Kırmızı liste".

- [ ] **Step 5: Commit**

```bash
git add netkar-infrastructure/src
git commit -m "feat(infra): demo runner with sample data + ArchUnit hexagonal boundary tests"
```

---

### Task 15: Full build verification + push

**Files:** none (verification only).

- [ ] **Step 1: Run the full reactor build**

Run: `mvn -q verify`
Expected: BUILD SUCCESS across all three modules; enforcer confirms Java 21; all tests + ArchUnit green.

- [ ] **Step 2: Confirm the acceptance checklist (design spec §12)**

Verify each: build runs on 21; `DemoRunner` prints correct breakdown + red list; golden multi-product allocation preserves totals; `domain` imports no framework (ArchUnit green); glossary committed (already in the spec).

- [ ] **Step 3: Push**

```bash
git push
```
Expected: branch `main` updated on `origin`.

---

## Self-Review

**Spec coverage check (against `2026-06-20-netkar-core-domain-design.md`):**
- §2 Maven multi-module + Java 21 pinning → Task 1. ✅
- §4 hexagonal modules + dependency rule → Tasks 1, 14 (ArchUnit). ✅
- §5 glossary → lives in spec; identifiers follow it. ✅
- §6.1 Money (currency, negatives, rounding, allocate, splitVat) → Tasks 2–4. ✅
- §6.1 VatRate/Percentage/ProductRef/Quantity/TransactionEffect → Tasks 3, 5. ✅
- §6.2 SettlementLine/SettlementPackage + invariants → Task 7. ✅
- §6.3 ProductCost (costVatRate) + CostBook → Task 6. ✅
- §6.4 AllocationStrategy policy + RevenueWeightedAllocation → Task 8. ✅
- §6.5 EstimatedNetVatBurden/ProfitBreakdown/ProductProfitability/RedList → Tasks 9, 13. ✅
- §7 sign handling: SALE only, RETURN/CANCEL throws → Task 10. ✅
- §8 SALE formulas + invariants (splitVat reconcile, allocation sum preserved, missingCost, NetProfit excludes VAT burden) → Tasks 3, 4, 10–12. ✅
- §9 testing (golden, edge cases, ArchUnit) → Tasks 10–12, 14. ✅
- §12 acceptance → Task 15. ✅

**Placeholder scan:** No TBD/TODO; every code step has complete code. ✅
**Type consistency:** `Money.tryOf/zeroTry/of`, `splitVat→VatSplit(net,vat)`, `allocate(List<BigDecimal>)`, `AllocationStrategy.allocate(Money,List<SettlementLine>)→List<Money>`, `ProfitCalculator(AllocationStrategy)`, `PackageProfit(packageId,lines)`, `ProfitBreakdown` fields, `CalculateProductProfitabilityUseCase.Result(products,redList)` — names align across tasks. ✅

**Note on Tasks 11–12:** these add tests against the Task 10 implementation; if green immediately, that confirms correctness (still commit the tests as regression guards).
