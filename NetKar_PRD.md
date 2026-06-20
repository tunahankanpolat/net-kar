# NetKar — Ürün Gereksinim Dokümanı (PRD)

> **Not:** "NetKar" bir çalışma adıdır; nihai marka adıyla değiştirilecek.
> **Doküman tipi:** Ürün tanımı + MVP kapsamı odaklı PRD
> **Versiyon:** 0.3 (API şema araştırması + KDV motoru + mimari kararlar tamamlandı)
> **Hedef:** Tek kişilik (solo) geliştirici tarafından geliştirilecek, abonelik (SaaS) modelli web uygulaması

---

## 1. Ürün Özeti

NetKar, Türkiye'deki pazaryeri satıcılarının (öncelikle Trendyol) satışlarından **gerçekte ne kadar kâr ettiğini ürün bazında otomatik olarak gösteren** bir web panelidir. Satıcı mağazasını API anahtarıyla bir kez bağlar, ürünlerinin alış maliyetini bir kez girer; sistem komisyon, kargo, iade ve vergi kalemlerini hesaba katarak her ürün için net kârı, kâr marjını ve zarar ettiren ürünlerin listesini sürekli ve otomatik olarak çıkarır.

Tek cümleyle: **Satıcının bugün Excel'de saatler harcayıp yine de eksik/yanlış yaptığı net kâr hesabını, otomatik ve sürekli hale getiren araç.**

---

## 2. Problem

Pazaryeri satıcısı, platformun ona ödediği "hakediş" tutarını görür ama cebine gerçekte ne kaldığını bilmez. Çünkü hakedişin içinden hâlâ ürünün **alış maliyeti**, **iade masrafları**, **reklam gideri** ve **vergi** çıkacaktır — ve platform, satıcının alış maliyetini bilmediği için bu hesabı yapısal olarak yapamaz. Sonuç: satıcı yüksek ciro yaparken bazı ürünlerden farkında olmadan **zarar eder**.

**Bu problem kimde ve ne sıklıkta yaşanıyor?**
Her ölçekteki pazaryeri satıcısında, sürekli olarak. Trendyol'un tek bir kampanyada 400 binin üzerinde satıcıyla çalıştığı düşünülürse, etkilenen kitle çok büyüktür. Satıcılar bu hesabı bugün dört kötü yöntemden biriyle yapıyor:

1. **Excel ile** (en yaygın): Panelden satış raporu, cari hesap ekstresi ve kargo faturalarını ayrı ayrı indirip, kendi maliyet listesiyle DÜŞEYARA'larla eşleştirmeye çalışıyorlar. Kesinti kalemleri farklı raporlarda ve farklı zamanlarda oluştuğu için bu eşleştirme hiçbir zaman tam "bitmiyor".
2. **Hiç yapmıyorlar**: Cironun pozitif olmasına bakıp "kazanıyorum herhalde" diyorlar; ürün bazında kim kazandırıyor kim kaybettiriyor, bilmiyorlar.
3. **Mali müşavir**: Ay sonunda toplam kâr/zararı söylüyor ama ürün bazında değil ve gecikmeli — fiyat kararı vermeye yaramıyor.
4. **Şişkin entegratörler** (Sentos vb.): Operasyonu büyümüş satıcılar için; küçük satıcıya pahalı ve karmaşık.

**Çözülmemesinin maliyeti:**
Satıcı, zarar eden ürünleri tespit edemediği için ayda binlerce TL'lik "görünmez kanama" yaşıyor; yanlış ürünün stoğunu artırıp doğru ürünü ihmal ediyor; kampanyalara körlemesine girip zarar ediyor.

**Talebin kanıtı:** Piyasada bu acının yarım çözümlerine bile para ödeniyor — manuel rapor yükleten Excel-tabanlı araçlar aylık ~500 TL'ye satılıyor; ücretli Excel şablonlarına müşteri desteği veriliyor; ve hatta API'den kendi verisini çeken yazılımcı satıcılar bile bu hesabı kendileri için kodlamaya çalışıyor. Global ölçekte aynı model (Amazon için Sellerboard, Shopify için TrueProfit) kanıtlanmış, kârlı bir kategoridir.

---

## 3. Hedef Kullanıcı

**Birincil persona — "Çok ürünlü küçük/orta satıcı":**
Trendyol'da onlarca-yüzlerce farklı ürün satan, ayda birkaç yüz ile birkaç bin sipariş alan satıcı. Tek ürünün hesabını kafadan yapabiliyor ama 200 ürünün kârlılığını aynı anda takip edemiyor. Teknik bilgisi sınırlı; "API anahtarını kopyala-yapıştır" yapabilir ama kod yazamaz. Excel'le boğuşuyor ve bundan bıkmış.

**İkincil persona — "Büyümekte olan profesyonel satıcı":**
Birden fazla pazaryerinde satış yapan, operasyonu büyüyen satıcı. Şu an entegratör kullanıyor olabilir ama kârlılık tarafı entegratörde zayıf. İleride çoklu pazaryeri desteğiyle hedeflenecek.

**MVP satıcı tipi kısıtı:** Yalnızca **KDV mükellefi, gerçek usulde vergilendirilen** satıcılar (şahıs şirketi veya sermaye şirketi). Vergiden muaf esnaf ve basit usul mükellefler MVP kapsamı dışıdır — bu profiller için stopaj kesilmiyor, KDV yükümlülüğü yok; hesap motoru farklılaşıyor. Onboarding'de vergi durumu tek soruyla alınır; uygun olmayan profil yönlendirilir (P1'de eklenebilir).

**Kapsam dışı kullanıcı (şimdilik):** Kendi web sitesinden (pazaryeri olmadan) satış yapanlar, yalnızca tek-tük ürünü olan hobi satıcıları, vergiden muaf esnaf ve basit usul mükellefler.

---

## 4. Değer Önerisi

Veri zaten platformda var — o yüzden değerimiz "veriye erişim" değil, verinin üstüne koyduğumuz **dört katman**:

| Katman | Ne yapıyor | Neden değerli |
|---|---|---|
| **1. Erişilebilirlik** | Dağınık raporları (hakediş, kargo faturası, iade) tek panelde toplar | Satıcı bu raporları indirip birleştirmekten kurtulur |
| **2. Birleştirme** | Satış/komisyon/kargo/iade verisini, satıcının girdiği alış maliyetiyle **ürün bazında** birleştirir | "X ürününün net kârı 23 TL" bilgisi hiçbir hazır ekranda yoktur — sadece burada üretilir |
| **3. Doğruluk** | Platformun tüm kesintilerini (komisyon, hizmet bedeli, kargo, iade, kampanya katkı payı, erken ödeme, ceza) + vergi etkisini (KDV mahsubu, stopaj) + manuel maliyetleri (alış, reklam, paketleme) eksiksiz hesaba katar | Satıcının Excel'de en çok yanlış yaptığı/atladığı katman; sonuç daha doğru olur. (Tam kalem listesi: Bölüm 6.8) |
| **4. Karar** | Tabloyu göstermez, ne yapılması gerektiğini söyler (kırmızı liste, marj alarmı) | Satıcı veriyi değil **aksiyonu** satın alır; aboneliğin asıl gerekçesi budur |

**Konumlandırma:** Şişkin entegratör (Sentos) gibi *her şeyi* yapan dev paket değil; ücretsiz hesaplayıcılar gibi *manuel* değil. **"Mağazanı bağla, hangi üründen kazanıp hangisinden kaybettiğini gör."** Tek bir soruya net cevap veren odaklı araç.

---

## 5. Rekabet Konumu

| Rakip katmanı | Örnekler | Bizim farkımız |
|---|---|---|
| **Doğrudan rakipler** (kârlılık paneli) | HesapRobotu, Pazarmetrik, Profit.Entegrify | Pazar yeni ve 3-4 oyunculu; kazanan belli değil. Doğruluk + basitlik + fiyatla ayrışılır |
| **Tam kapsamlı entegratörler** | Sentos, Dopigo, StockMount | Farklı müşteriye (büyük operasyon) satıyorlar; bizim kitlemize pahalı/karmaşık |
| **Ürün araştırma araçları** | SatışAnaliz, Emparator | Odakları "ne satayım"; bizimki "kazanıyor muyum". Emparator hesaplama hatalarıyla eleştiriliyor → doğruluk farklılaşma noktası |
| **Manuel Excel araçları** | TicaretHub (~499 TL/ay), ücretli şablonlar | Rapor indirip yükleme gerektiriyorlar; biz API ile **tam otomatik**iz |
| **Ücretsiz hesaplayıcılar** | Sopyo, Olaybuiste, Tetsi | Rakip değil, **pazarın kanıtı** ve SEO ile müşteri kaynağımız |
| **Platformun kendi paneli** | Trendyol Finans ekranı | Alış maliyetini bilmediği için net kâr gösteremez — var olma sebebimiz tam bu boşluk |

**Global emsal:** Sellerboard (Amazon), Türkiye pazaryerlerini desteklemiyor; bu da yerel boşluğun neden açık kaldığını açıklıyor.

---

## 6. Özellikler

### 6.1 Mağaza Bağlama (API entegrasyonu)
Satıcı, Trendyol satıcı panelinden aldığı API kimlik bilgilerini (Satıcı ID, API Key, Secret) sisteme girer. Sistem sipariş, hakediş (settlement), kargo faturası ve iade verisini otomatik çeker. Sürekli senkronizasyon için Stream/webhook altyapısı kullanılır.

### 6.2 Alış Maliyeti Yönetimi
Satıcıdan istenen tek **zorunlu** veri budur (P0). KDV oranı, komisyon oranı veya vergi parametreleri için satıcıya hiçbir soru sorulmaz; bunlar API'den otomatik gelir (bkz. Bölüm 6.8). (Reklam, paketleme gibi diğer manuel maliyetler P1'de opsiyonel olarak eklenir.)

Ürün başına alış maliyeti tek tek veya toplu Excel yüklemesiyle girilir. Onboarding'de "KDV dahil mi hariç mi?" bir kez seçilir; sistem buna göre alış KDV'sini ayrıştırır. (İleride: tarih bazlı değişen maliyet / FIFO desteği.)

### 6.3 Kâr Hesaplama Motoru
Ürünün kalbi. Her satış için tüm kalemler düşülerek **net kâr** bulunur. Platform kaynaklı kalemler (komisyon, hizmet bedeli, kargo, iade kargosu, kampanya katkı payı, erken ödeme/vade farkı, cezai kesintiler) tahmin edilmez; API'den **gerçekleşen** tutarlar okunur (bu, ücretsiz hesaplayıcılardan daha doğru olmasını sağlar). Tam kalem dökümü ve hangi verinin nereden geldiği için bkz. **Bölüm 6.8**.

### 6.4 Ürün Bazlı Kârlılık Paneli
Ana ekran. Her ürün için: satış adedi, ciro, toplam kesintiler, net kâr ve kâr marjı (%). Kâra/marja göre sıralanabilir, filtrelenebilir.

### 6.5 Kırmızı Liste
Zarar ettiren ürünlerin ayrı, vurgulu listesi. Satıcının ilk "aha" anını yaşadığı ve anında aksiyon aldığı ekran.

### 6.6 Marj Alarmı (bildirim)
Bir ürünün marjı belirli eşiğin altına düştüğünde (kargo zammı, artan iade vb.) satıcıya bildirim. Satıcı panele bakmasa bile ürün onu korur.

### 6.7 Özet Gösterge Paneli
Dönemsel toplam: net kâr, en kârlı/en zararlı ürünler, trend grafiği.

### 6.8 Kâr Kalemleri ve Vergi Katmanı (tam referans)

Türkiye'de bir pazaryeri satışının net kârına giren kalemler aşağıdadır. Tasarım açısından kritik ayrım, her kalemin **verisinin nereden geldiği**dir: çoğu platform kesintisi ve vergi parametresi API'den otomatik gelir; satıcıdan yalnızca alış maliyeti istenir.

---

#### Grup 1 — API'den otomatik (gerçekleşen tutarlar, tahmin değil)

| Kalem | Veri kaynağı | Granülerlik | Not |
|---|---|---|---|
| Komisyon | Settlements → `commissionAmount` | **Ürün (barcode) bazında** | KDV hariç tutar; Settlements response'ında her ürün satırı kendi commission kalemini taşır |
| Hizmet bedeli | OtherFinancials → DeductionInvoices | Paket bazında | Komisyondan ayrı sabit kesinti (~%3,49 + KDV); sipariş/paket başına |
| Kargo + iade kargosu | `getCargoInvoiceItems` (ayrı endpoint) | Paket bazında | `shipmentPackageId` üzerinden Settlements ile join'lenir |
| Stopaj | Settlements → transaction type | Ürün bazında | KDV hariç tutar üzerinden %1; platform kesip devlete öder (01.01.2025'ten beri). **Anlık maliyet olarak işlenir; yıl sonu gelir vergisi mahsubu kapsam dışı** |
| Kampanya katkı payı / kupon | Settlements → transaction type | Ürün bazında | Satıcının ortak finanse ettiği indirimler |
| Erken ödeme / vade farkı | OtherFinancials | Paket/dönem bazında | Satıcı parasını erken alırsa finansman maliyeti |
| Cezai kesintiler | Settlements → transaction type | Paket bazında | Termin aşımı, tedarik edememe vb. |
| İade KDV düzeltmesi | Settlements → Cancel/Return transaction types | Ürün bazında | İade gerçekleştiğinde hem satış KDV'si hem komisyon KDV'si tersine döner; bu transaction type'lar doğru okunursa otomatik işlenir |

---

#### Grup 2 — Kural bazlı / hesaplanan (ürün başına KDV katmanı)

**Bu katman ürünün asıl değer farkıdır:** Trendyol satıcının alış maliyetini bilmediği için bu hesabı yapamaz. KDV oranı ve komisyon tutarı API'den otomatik geldiği için satıcıya hiçbir vergi sorusu sorulmaz.

**Net KDV Yükü formülü (ürün başına):**

```
Net KDV Yükü = Satıştan Oluşan KDV
             − Alıştan Oluşan KDV
             − Kargodan Oluşan KDV        (paylaştırılmış)
             − Komisyondan Oluşan KDV
             − Hizmet Bedelinden Oluşan KDV (paylaştırılmış)
             [− Reklamdan Oluşan KDV]      (P1)
```

**5 KDV türünün hesaplanması:**

| KDV Türü | Yön | Veri kaynağı | Hesaplama |
|---|---|---|---|
| **Satıştan Oluşan KDV** | Output ↑ (devlete borç) | `lines[].lineGrossAmount` + `lines[].vatRate` | `lineGrossAmount / (1 + vatRate) × vatRate` |
| **Alıştan Oluşan KDV** | Input ↓ (indirilecek) | Satıcı girişi (alış maliyeti) + `vatRate` | `alışMaliyeti / (1 + vatRate) × vatRate` |
| **Kargodan Oluşan KDV** | Input ↓ (indirilecek) | `getCargoInvoiceItems` | Fatura tutarından türetilir; %20 standart oran (doğrulanacak) |
| **Komisyondan Oluşan KDV** | Input ↓ (indirilecek) | Settlements `commissionAmount` | `commissionAmount × 0.20` |
| **Hizmet Bedelinden Oluşan KDV** | Input ↓ (indirilecek) | OtherFinancials | `8,49 TL × 0.20 = 1,70 TL / sipariş` (sabit) |
| **Reklamdan Oluşan KDV** (P1) | Input ↓ (indirilecek) | Trendyol Ads API / ayrı fatura | Manuel/yarı-otomatik giriş |

**KDV oranı nereden geliyor?** Products API → `vatRate` field'ı — satıcının bu bilgiyi girmesi gerekmez, ürün Trendyol'a listelenirken zaten belirlenen oran API'den otomatik okunur.

**Önemli kapsam kararları:**
- **v1'de "vergi öncesi işletme kârı" gösterilir:** Gelir/kurumlar vergisi ürün bazında değil, işletmenin yıllık toplam kârı üzerinden hesaplanır. Sellerboard dahil tüm global araçlar bu çizgiyi tutar. v1 bu çizgide kalır; bilinçli karar, sessizce atlanan kalem değil.
- **Stopaj yıl sonu mahsubu kapsam dışı:** Stopaj anlık maliyet olarak işlenir (hakediş azalıyor, bunu hesaba katıyoruz). Yıl sonu gelir vergisi mahsubu ise işletme düzeyinde bir işlem; ürün bazında modellenemez.
- **KDV hesabı "ürün başına tahmin"dir:** KDV beyannamesi aslında tüm işlemlerin aylık toplamı üzerinden verilir. Çok ürünlü portföyde "devreden KDV" gibi durumlar toplamı etkiler. Panel bunu "ürün başına tahmin" olarak konumlandırır — yine de rakiplerin tamamından daha doğru bir hesaptır.

---

#### Grup 3 — Manuel (yalnızca satıcının bilebileceği)

| Kalem | Not |
|---|---|
| **Alış maliyeti (COGS)** | **P0 — satıcıdan istenen tek zorunlu kalem.** Tekli veya toplu Excel; "KDV dahil mi hariç mi?" bir kez seçilir |
| Reklam (Trendyol Ads) | P1. Toplam tutar API'den gelebilir ama ürün bazında atfı zor; manuel/yarı-otomatik giriş |
| Paketleme/ambalaj | P1. Koli, streç, dolgu — küçük ama gerçek, ürün başına |
| İadenin gerçek maliyeti | P1. Sadece iade kargosu değil; açılmış/hasarlı dönen satılamayan ürün |

---

#### Çoklu ürün / aynı pakette farklı ürünler

Müşteri aynı sepette aynı mağazadan farklı ürünler aldığında tek bir kargo faturası ve tek bir hizmet bedeli oluşur. Komisyon ve KDV ise Settlements'ta ürün (barcode) bazında ayrışmış gelir — bunlar için paylaştırma gerekmez.

**Paket bazında gelen kalemler (kargo + hizmet bedeli) için paylaştırma yöntemi:**

MVP: **Hasılat bazlı (revenue-weighted) paylaştırma**

```
Ürünün ağırlığı = ürünün satış fiyatı / paketin toplam satış fiyatı
Ürüne düşen kargo = toplam kargo × ağırlık
Ürüne düşen hizmet bedeli = toplam hizmet bedeli × ağırlık
```

Panelde paylaştırılmış kalemler "~" işaretiyle gösterilir: "Tahmini kargo payı: ~60 TL (hasılat bazlı)." Bu yöntem Sellerboard dahil tüm global araçların standardı; satıcı da bu hesabı daha iyi yapamaz.

P1: Desi bazlı paylaştırma (kargo maliyeti özünde ağırlık/hacim kaynaklı olduğu için daha hassas; satıcıdan ürün desisi girişi gerektirir).

**Paket bölme durumu:** Satıcı paketi bölerse her parça kendi `shipmentPackageId`'sini ve ayrı bir kargo faturasını alır — bu durumda paylaştırma gerekmez, her alt paket kendi kargo maliyetiyle eşleştirilir.

---

**Vergi yapısı onboarding:** Satıcının vergi durumu (şahıs şirketi / sermaye şirketi) onboarding'de tek soruyla alınır. KDV oranı ve komisyon tutarı için satıcıya hiçbir soru sorulmaz — API'den otomatik gelir. Tüm KDV hesap mantığı mali müşavire doğrulatılmalı (doğruluk garantisi + "mali müşavir onaylı hesaplama" pazarlama kozu).

### 6.9 (Gelecek) Kampanya Kârlılık Simülasyonu
"Bu indirimle kampanyaya girersem ürün başına ne kazanırım/kaybederim?" — kampanyaya girmeden önce öngörü.

### 6.10 (Gelecek) Çoklu Pazaryeri
Hepsiburada (öncelikli — belgeli Finans modülü var), ardından N11, Pazarama, ÇiçekSepeti vb. Aynı kâr motoru, farklı veri kaynağı. Mimari buna baştan hazırlanır (bkz. Bölüm 12).

---

## 7. MVP Kapsamı (Önceliklendirme)

> **İlke:** P0 listesi acımasızca dar tutulur. "Bunu çıkarsak ürün temel problemi hâlâ çözüyor mu?" Cevap hayırsa P0'dır.

### Must-Have / P0 — bunlar olmadan ürün yayınlanamaz
- **Trendyol API ile mağaza bağlama** ve otomatik veri çekme (sipariş + hakediş + kargo + iade)
- **Alış maliyeti girişi** (tekli + toplu Excel yükleme)
- **Kâr hesaplama motoru** — API'den gelen tüm gerçekleşen platform kesintileriyle: komisyon, hizmet bedeli, kargo, iade kargosu, kampanya katkı payı, erken ödeme, ceza (bkz. Bölüm 6.8 Grup 1)
- **Ürün bazlı kârlılık paneli** (net kâr + marj) — *vergi öncesi işletme kârı* düzeyinde
- **Kırmızı liste** (zarar eden ürünler)
- **KDV hesaplama motoru** — Net KDV Yükü (satış − alış − kargo − komisyon − hizmet bedeli KDV'leri); KDV oranı Products API `vatRate`'ten otomatik (satıcı girmez/onaylamaz). Bkz. Bölüm 6.8 Grup 2
- **Çoklu ürün paketinde hasılat bazlı paylaştırma** (kargo + hizmet bedeli için)
- **Abonelik + ödeme** (iyzico, 14 gün ücretsiz deneme)

### Should-Have / P1 — hızlı takip (lansman sonrası ilk eklemeler)
- **Reklam (Trendyol Ads) maliyeti** — manuel/yarı-otomatik giriş ile kâra dahil (önemli kalem; doğruluğu ciddi artırır)
- Marj alarmı / bildirimler
- Özet gösterge paneli + trend grafiği
- Tam KDV/stopaj mahsup derinliği
- Paketleme ve iadenin gerçek (satılamayan ürün) maliyeti girişi
- **Excel yükleme (B planı):** API bağlamak istemeyen/güvenmeyen satıcı için panel raporu yükleme yolu
- Tarih aralığı filtreleme
- Tarih bazlı değişen alış maliyeti / FIFO
- **İkinci pazaryeri — Hepsiburada** (belgeli Finans modülü; mimari hazır olduğu için hızlı takip hedefi)

### Could-Have / P2 — gelecek (şimdilik yapılmıyor ama mimari buna izin vermeli)
- Ek pazaryerleri (N11, Pazarama, ÇiçekSepeti, Amazon TR)
- İleriye dönük fiyatlama simülatörü ("kaça satmalıyım")
- Kampanya kârlılık simülasyonu
- İşletme düzeyinde gelir/kurumlar vergisi tahmini (ürün bazlı değil, opsiyonel görünüm)

### Won't-Have (bu sürümde) — bilinçli olarak kapsam dışı
- Stok yönetimi, ürün yükleme, kargo etiketi (entegratörün işi)
- E-fatura kesme / muhasebe (Paraşüt'ün işi)
- Mobil uygulama (web öncelikli)
- Çoklu kullanıcı / ekip yetkilendirme

---

## 8. Kullanıcı Hikâyeleri

**Onboarding**
- Bir satıcı olarak, mağazamı API anahtarımla bağlamak istiyorum ki satış verim otomatik aksın ve hiçbir şeyi elle girmeyeyim.
- Bir satıcı olarak, ürünlerimin alış maliyetini toplu Excel ile yüklemek istiyorum ki 200 ürünü tek tek girmek zorunda kalmayayım.

**Günlük kullanım**
- Bir satıcı olarak, hangi ürünlerimin zarar ettirdiğini tek bakışta görmek istiyorum ki fiyatını düzeltip ya da ürünü kaldırıp kanamayı durdurayım.
- Bir satıcı olarak, bir ürünün marjı düştüğünde uyarılmak istiyorum ki panele her gün bakmadan da haberim olsun.
- Bir satıcı olarak, geçen ay gerçekte ne kazandığımı ürün bazında görmek istiyorum ki Excel'de hakediş mutabakatı yapmakla saatler harcamayayım.

**Sınır/hata durumları**
- Bir satıcı olarak, alış maliyetini girmediğim ürünleri ayırt edebilmek istiyorum ki hesabın eksik olduğunu bileyim.
- Bir satıcı olarak, API bağlantım koptuğunda net bir uyarı görmek istiyorum ki verinin güncel olmadığını anlayayım.

---

## 9. Kabul Kriterleri (P0 için örnek)

**Mağaza bağlama**
- [ ] Satıcı API kimlik bilgilerini girip bağlantıyı test edebilir
- [ ] Bağlantı başarılıysa son X günün siparişi otomatik çekilir
- [ ] Hatalı kimlik bilgisinde anlaşılır hata mesajı gösterilir
- [ ] Bağlantı koparsa satıcı bilgilendirilir

**Kâr hesaplama + panel**
- [ ] Her ürün için net kâr ve marj hesaplanır (*vergi öncesi işletme kârı* düzeyinde)
- [ ] Komisyon/kargo/iade API'den gelen gerçekleşen tutarlardan alınır (tahmin değil)
- [ ] Net KDV Yükü 5 kalemden doğru hesaplanır (satış − alış − kargo − komisyon − hizmet bedeli)
- [ ] KDV oranı Products API `vatRate`'ten otomatik okunur; satıcıya sorulmaz
- [ ] Çok ürünlü siparişte kargo ve hizmet bedeli hasılat bazlı paylaştırılır ve "~" ile işaretlenir
- [ ] Alış maliyeti girilmemiş ürün "eksik" olarak işaretlenir
- [ ] Zarar eden ürünler kırmızı listede ayrı gösterilir
- [ ] Panel kâra/marja göre sıralanabilir

**Abonelik**
- [ ] 14 günlük ücretsiz deneme başlatılabilir
- [ ] Deneme bitiminde iyzico ile ücretli aboneliğe geçilir
- [ ] Ödeme alınamazsa erişim kısıtlanır ve satıcı bilgilendirilir

---

## 10. Başarı Metrikleri

**Öncül göstergeler (hızlı değişir):**
- Deneme kaydı sayısı
- **Aktivasyon — API bağlama oranı:** kayıt olanların kaçı mağazasını bağlıyor
- **Aktivasyon — maliyet girme oranı:** asıl kritik adım; kaçı alış maliyetini giriyor
- İlk içgörüye ulaşma süresi (kayıttan kırmızı listeyi görmeye kadar)
- Deneme → ücretli dönüşüm oranı

**Ardıl göstergeler (zamanla gelişir):**
- Aylık tekrarlayan gelir (MRR)
- Aylık iptal (churn) oranı
- 3 aylık kullanıcı tutma (retention)
- (İleride) çoklu pazaryeri ile genişleme geliri

**Hedefler (hipotez — görüşmelerle doğrulanacak):**
Bunlar şu an varsayımdır, gerçek değerler satıcı görüşmeleri ve ilk kohortla netleşecektir. Örnek bir başarı eşiği: ilk 3 ayda ~50 ücretli abone; deneme→ücretli dönüşümde ~%20; aylık churn <%5.

---

## 11. Fiyatlandırma (hipotez)

Aylık abonelik. Çıpalar: manuel-yükleme yapan yerli rakip ~499 TL/ay; global emsal Sellerboard ~15 USD/ay'dan başlıyor. Bu veriler **karar değil hipotezdir**; nihai fiyat satıcı görüşmelerindeki "ayda kaç TL öderdin" sorusuyla test edilmeli. Olası yapı: tek mağaza için sabit aylık ücret + ileride çoklu pazaryeri/çoklu mağaza için üst paket.

---

## 12. Teknik Notlar ve Mimari Kararlar

- **API tabanı doğrulandı:** Trendyol'un açık developer portalında Settlements (satış/komisyon/iade/provizyon), Other Financials (hakediş/faturalar) ve Cargo Invoice servisleri mevcut; test ortamı ve webhook desteği var. Erişim self-servis: satıcı kendi panelinden API bilgisini alıp veriyor.

- **Settlements API şeması (doğrulandı):** Response'ta her sipariş paketi içinde `lines[]` array'i var; her satır kendi `barcode`, `lineGrossAmount` ve `vatRate` field'larını taşır. `commissionAmount` ve `commissionRate` **ürün (barcode) bazında** gelir — farklı kategorilerdeki ürünlerin farklı komisyon oranları olduğu için bu beklenen ve doğru davranış. Yani komisyon ve satıştan KDV için paket içi paylaştırma gerekmez; her ürün kendi verisini taşır.

- **KDV oranı — kullanıcıdan alınmaz:** Products API → `vatRate` field'ı ürün başına KDV oranını taşır. Satıcı bu bilgiyi Trendyol'a ürünü listelerken zaten vermiştir; sistem buradan okur. Satıcıya KDV oranı, komisyon oranı veya vergi parametresi için hiçbir soru sorulmaz.

- **Kargo verisi — ayrı endpoint ve join:** Kargo maliyeti Settlements'ta değil, `getCargoInvoiceItems` endpoint'inde ayrı durur. `shipmentPackageId` üzerinden Settlements verisine join'lenir. Kargo KDV'si bu fatura verisinden türetilir (şema ve KDV dahil/hariç ayrımı doğrulanacak — bkz. Bölüm 13).

- **Hizmet bedeli:** OtherFinancials → DeductionInvoices; paket/sipariş bazında sabit tutar (~8,49 TL + %20 KDV = ~10,19 TL). KDV payı: 1,70 TL/sipariş.

- **Çoklu ürün paketinde paylaştırma:** Kargo ve hizmet bedeli paket bazında geldiği için çok ürünlü siparişlerde ürünlere dağıtılması gerekir. MVP'de **hasılat bazlı paylaştırma** (ürünün satış tutarı / paketin toplam satış tutarı). Panelde paylaştırılmış kalemler "~" işaretiyle gösterilir. P1'de desi bazlı paylaştırma (daha hassas; satıcıdan ürün desisi girişi gerektirir).

- **Paket bölme:** Satıcı paketi bölerse her parça kendi `shipmentPackageId`'sini ve ayrı kargo faturasını alır — bu durumda paylaştırma gerekmez, her alt paket kendi kargo maliyetiyle doğrudan eşleştirilir.

- **Ölçek:** `getShipmentPackages` 10.000 kayıtla sınırlı; yüksek hacimde Stream endpoint + webhook kullanılmalı. Webhook her yeni siparişte Trendyol'dan sisteme otomatik bildirim atar — sürekli polling gerekmez. Mimari baştan Stream + webhook üzerine kurulmalı.

- **Connector deseni — "2'ye göre tasarla, 1'i yayınla" (kritik):** Her pazaryeri için ayrı **connector/adaptör**, ortada **pazaryeri-bağımsız tek bir kâr motoru** ve normalize edilmiş ortak bir **işlem (transaction) modeli.** Trendyol verisi doğrudan işlenmez; önce standart formata çevrilir.
  - **Tasarım girdisi olarak 2 kaynak:** Validasyon fazında Hepsiburada Finans API'sine **sığ bir spike** atılır (dokümanı oku, örnek veriyi normalize modele kâğıt üstünde eşle — tam entegrasyon değil). Soyutlama iki gerçek kaynağa karşı doğrulanır; tek kaynakta gizli kalan varsayımlar sınanır.
  - **MVP yalnızca Trendyol'u yayınlar.** Mimari N pazaryerine hazırdır; Hepsiburada ilk hızlı takip (P1) olur.

- **Stack:** Backend ağırlıklı (entegrasyon + hesaplama + zamanlanmış senkronizasyon); geliştiricinin mevcut backend deneyimine oturur. Frontend tek sayfalık tablo/dashboard ağırlıklı, karmaşık değil.

---

## 13. Riskler ve Açık Sorular

| Soru / Risk | Kim cevaplamalı | Bloklayıcı mı? |
|---|---|---|
| ~~Settlements servisi ürün bazlı kâr çıkaracak granülerlikte kalem veriyor mu?~~ | ~~Mühendislik~~ | **✅ ÇÖZÜLDÜ** — `lines[]` array'i barcode bazında `lineGrossAmount`, `vatRate`, `commissionAmount` taşıyor |
| `getCargoInvoiceItems` şemasında KDV dahil/hariç ayrımı net mi; kargo KDV'si direkt okunabiliyor mu? | Mühendislik (şema incelemesi) | **Evet** — kargo KDV'si indirilecek KDV hesabının parçası; yoksa geri hesaplama gerekir |
| `commissionAmount` KDV dahil mi hariç mi? | Mühendislik (API test ortamı) | Hayır ama komisyon KDV hesabını doğrudan etkiler (büyük olasılıkla hariç) |
| KDV ve stopaj hesap mantığının (Net KDV Yükü formülü dahil) tam doğruluğu | Mali müşavir (1-2 saatlik danışma) | Hayır (P1'de derinleşebilir) ama "doğruluk" iddiası için şart |
| Ürün bazlı kâr "vergi öncesi işletme kârı" olarak gösterilsin mi? | **✅ KARAR VERİLDİ** — Evet; gelir/kurumlar vergisi işletme düzeyinde bırakılır | — |
| Reklam (Trendyol Ads) maliyeti ürün bazında ne kadar atfedilebiliyor? | Mühendislik (Ads API incelemesi) | Hayır (P1) |
| Satıcılar API anahtarını bağlar mı, yoksa Excel yükleme mi tercih eder? (güven) | Satıcı görüşmeleri / UX | Hayır ama B planını (Excel yükleme) ne kadar öne alacağımızı belirler |
| Doğru fiyat noktası nedir? | Satıcı görüşmeleri | Hayır |
| Production için rate limit / IP yetkilendirme detayları | Mühendislik (dokümandan teyit) | Hayır (tasarım girdisi) |
| Hepsiburada Finans modülü ürün bazlı kâra yetiyor mu? | Mühendislik (faz 2'de) | Hayır (faz 2) |
| Platform riski: Trendyol bu özelliği kendi paneline eklerse? | Strateji | Hayır — çoklu pazaryeri desteği doğal sigorta |

---

## 14. Yol Haritası / Zaman Çizelgesi

> Geliştirici haftada ~10-20 saat ayırıyor. Aşağıdaki efor tahminleri buna göre takvime yayılmalı.

**Faz 0 — Validasyon (1-2 hafta):**
10-15 satıcıyla görüşme ("hakediş mutabakatını nasıl yapıyorsun, kaç saat sürüyor, ayda kaç TL öderdin"); Settlements/OtherFinancials şemalarının derin incelemesi; **Hepsiburada Finans API'sine sığ spike** (soyutlamayı 2 kaynağa karşı doğrulamak için — tam entegrasyon değil); mali müşavir danışması. Yeşil ışık kriteri: satıcılar Excel acısını doğruluyor ve ödeme niyeti gösteriyor.

**Faz 1 — MVP (yaklaşık 150-200 saatlik iş):**
Yalnızca Trendyol, P0 özellikleri. İlk günden ücretli + 14 gün deneme. İlk 10 müşteri, görüşme yapılan satıcılardan. *(Haftada 15 saat tempoda kabaca 2.5-3.5 ay.)*

**Faz 2 — Hızlı takipler (P1):**
Reklam maliyeti girişi, marj alarmı, özet panel, vergi derinliği, paketleme/iade maliyeti, Excel yükleme B planı, **ve ikinci pazaryeri (Hepsiburada)** — mimari hazır olduğu için bu faza sığar.

**Faz 3 — Genişleme (P2):**
Ek pazaryerleri (N11, Pazarama, ÇiçekSepeti, Amazon TR), kampanya/fiyat simülatörü, işletme düzeyinde vergi tahmini, mobil.

---

## 15. Özet

Pazar var (400 bin+ satıcı), talep kanıtlı (Excel acısı + ödeme yapılan yarım çözümler + yazılımcı satıcıların kendi çözümünü yazması), model global ölçekte kanıtlı (Sellerboard), veri erişilebilir ve şema doğrulandı (Trendyol Settlements `lines[]` barcode bazında komisyon + KDV okunuyor; kargo `getCargoInvoiceItems`'dan join'leniyor; KDV oranı Products API `vatRate`'ten otomatik geliyor) ve büyüme yolu açık (tüm pazaryerlerinin API'si mevcut).

Teknik belirsizliklerden büyük kısmı kapandı. Kalan iki somut teknik soru: `getCargoInvoiceItems` şemasında KDV ayrımı net mi ve `commissionAmount` KDV dahil mi hariç mi — bunlar test ortamında saatler içinde cevaplanabilir, bloklayıcı değil.

Tek kritik doğrulama halkası **gerçek satıcı görüşmeleri** ve ardından **mali müşavir danışması**dır. MVP dar tutulduğunda solo bir geliştirici için ulaşılabilir kapsamdadır.
