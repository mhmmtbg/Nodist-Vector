# Google Play'e yükleme rehberi

Depoda hazır olanlar ve senin yapman gerekenler. Sıra önemlidir: önce imza anahtarı, sonra AdMob,
sonra Play Console.

## Hazır olanlar

| Ne | Nerede |
| --- | --- |
| Mağaza paketi (AAB) | Her derlemenin [Releases](../../../releases) sayfasında `nodist-N.aab` |
| Başlık, kısa ve uzun açıklama (TR, EN) | `fastlane/metadata/android/<dil>/*.txt` |
| Simge 512×512 | `fastlane/metadata/android/<dil>/images/icon.png` |
| Öne çıkan görsel 1024×500 | `…/images/featureGraphic.png` |
| Telefon ekran görüntüleri 1080×1920 (5 adet) | `…/images/phoneScreenshots/` |
| 7" tablet 1200×1920 ve 10" tablet 2560×1600 (5'er adet) | `…/images/sevenInchScreenshots/`, `tenInchScreenshots/` |
| Gizlilik politikası (TR, EN) | `docs/privacy-policy.md` |

Görselleri yeniden üretmek için: `./gradlew :desktop:storeScreenshots` ve ardından
`python3 store/make_listing.py`. Ekran görüntüleri uygulamanın gerçek ekranlarıdır (Android ile ortak
arayüz kodu, masaüstü çizicisiyle görüntüye çizilir); yazı tipi telefondakinden biraz farklıdır.

## 1. Ad, simge ve paket kimliği

- Ad: **Nodist: Vektör** (mağaza başlığı "Nodist: Vektör" / "Nodist: Vector").
- Paket kimliği: **`com.mhmmtbg.nodist`**. Mağazaya ilk yüklemeden sonra bir daha değiştirilemez.
- Simge: lacivert zemin üzerinde düğümleri ve tutamaçlarıyla bir Bezier eğrisi.

Ad değişirse dokunulacak yerler: `app/src/main/res/values/strings.xml` (`app_name`),
`fastlane/metadata/android/*/title.txt`, `store/make_listing.py` (`APP_NAME`, sonra betiği yeniden çalıştır),
`desktop/build.gradle.kts` (`packageName`) ve pencere başlığı (`Main.kt`).

Açıklamada "Illustrator (.ai) dosyalarını açar" demek, ürünün neyle uyumlu olduğunu anlatan bir
kullanımdır ve açıklamanın sonunda Adobe ile bağlantı olmadığı yazılıdır. Başlıkta ve simgede bu ad geçmez.

## 2. İmza anahtarı

Play, hata ayıklama anahtarıyla imzalanmış paketi kabul etmez. Bir kez anahtar oluştur ve sakla:

```sh
keytool -genkeypair -v -keystore upload.keystore -alias upload -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 upload.keystore   # çıktıyı SIGNING_KEYSTORE_BASE64 olarak kaydet
```

Depo ayarları → Secrets and variables → Actions altında şunları tanımla: `SIGNING_KEYSTORE_BASE64`,
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` (`upload`), `SIGNING_KEY_PASSWORD`. Sonraki derlemede
APK ve AAB bu anahtarla imzalanır.

Dikkat: anahtar değişince telefonundaki mevcut kurulumun üzerine güncelleme yapılamaz. Önce
Belgelerim'deki çalışmalarını "Farklı kaydet" ile dışarı al, uygulamayı kaldır, yenisini kur.

## 3. AdMob

1. https://admob.google.com → uygulama ekle (Android) → bir **Banner** reklam birimi oluştur.
2. Depo gizli değişkenleri: `ADMOB_APP_ID` (`ca-app-pub-…~…`) ve `ADMOB_BANNER_ID` (`ca-app-pub-…/…`).
   Tanımlı değilken uygulama Google'ın deneme reklamlarını gösterir; bunlar gelir üretmez.
3. AdMob → Gizlilik ve mesajlaşma: Avrupa için onay mesajını (GDPR) oluştur ve yayımla. Uygulama onay
   penceresini bu ayara göre gösterir.
4. Kendi reklamlarına tıklama; hesap kapatılır. Denerken deneme kimlikleriyle derlenmiş sürümü kullan.

Reklamlar şurada görünür: düzenleyicide üst çubuğun altında ve Belgelerim ekranının altında. Hata
ayıklama derlemesinde (otomatik testlerin kullandığı) reklam kapalıdır.

## 4. Play Console

1. Uygulamayı oluştur (ücretsiz, uygulama). Paket adı: `com.mhmmtbg.nodist`.
2. AAB'yi önce **dahili test** kanalına yükle. Satın alma ürünü ancak bir paket yüklendikten sonra tanımlanabilir.
3. Para kazanma → Ürünler → Uygulama içi ürünler → ürün oluştur:
   - Ürün kimliği: **`coffee`** (uygulama bu kimliği arar; farklıysa `app/build.gradle.kts` içindeki `COFFEE_PRODUCT_ID`'yi değiştir)
   - Ad: "Geliştiriciye kahve ısmarla", fiyat: **₺99,99**, durumu: etkin
   - Tek seferlik ve tüketilmeyen bir üründür: bir hesap bir kez alır, yeni cihazda kendiliğinden geri gelir.
4. Mağaza girişi: metinleri ve görselleri `fastlane/metadata/android/` altından yükle (Türkçe ve İngilizce).
5. Uygulama içeriği:
   - Gizlilik politikası adresi: `docs/privacy-policy.md` dosyasının GitHub adresi (önce içindeki iletişim adresini doldur)
   - Reklamlar: **Evet, reklam içeriyor**
   - Reklam kimliği: **Evet**, amaç: reklamcılık
   - Veri güvenliği: uygulama kendisi veri toplamaz; AdMob nedeniyle "Cihaz ya da diğer kimlikler", "Yaklaşık konum",
     "Uygulama etkileşimleri" ve "Tanılama" verilerinin reklam amacıyla toplandığını ve paylaşıldığını bildir
     (Google'ın güncel AdMob veri güvenliği yönergesine göre doldur)
   - Hedef kitle: 13 yaş ve üzeri (çocuklara yönelik değil)
   - İçerik derecelendirmesi anketini doldur
6. Satın almayı dene: Play Console → Ayarlar → Lisans testi bölümüne kendi hesabını ekle, uygulamayı
   dahili test bağlantısından kur, "Geliştiriciye kahve ısmarla"yı seç. Ödeme deneme kartıyla yapılır;
   ardından reklamların kalktığını gör.

Yeni açılmış kişisel geliştirici hesaplarında Google, üretime çıkmadan önce kapalı test (en az 12
test kullanıcısı, 14 gün) şartı arar; hesabının durumunu Play Console'da görürsün.

## 5. Otomatik yükleme (GitHub → Play)

İlk paketi Play Console'dan elle yüklemen gerekir (Google, API ile ilk yüklemeye izin vermez). Sonraki
her derleme kendiliğinden yüklenebilir:

1. Google Cloud Console → bir proje seç → **Google Play Android Developer API**'yi etkinleştir.
2. IAM → Hizmet hesapları → hizmet hesabı oluştur → Anahtarlar → **JSON** anahtarı indir.
3. Play Console → Kullanıcılar ve izinler → Yeni kullanıcı davet et → hizmet hesabının e-posta adresini ekle;
   bu uygulama için "Sürümleri test kanallarına yayınla" (ve istersen "Üretime yayınla") iznini ver.
4. Depo gizli değişkeni: **`PLAY_SERVICE_ACCOUNT_JSON`** = indirdiğin JSON dosyasının tüm içeriği.

Bundan sonra `main` dalına giden her derleme, testler geçerse AAB'yi **dahili test** kanalına taslak olarak
yükler (iş akışındaki `play` işi). İsteğe bağlı depo değişkenleri (Settings → Secrets and variables →
Actions → Variables):

| Değişken | Varsayılan | Ne zaman değiştirilir |
| --- | --- | --- |
| `PLAY_TRACK` | `internal` | Kapalı test için `alpha`, açık test için `beta`, üretim için `production` |
| `PLAY_RELEASE_STATUS` | `draft` | Uygulama ilk incelemeden geçip yayımlandıktan sonra `completed` (sürüm kendiliğinden yayına girer) |

Gizli değişken tanımlı değilse ya da paket kendi anahtarınla imzalanmamışsa yükleme sessizce atlanır.

## Gizli değişkenlerin tamamı

| Ad | Ne için | Zorunlu mu |
| --- | --- | --- |
| `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` | Mağaza imzası | Evet |
| `ADMOB_APP_ID`, `ADMOB_BANNER_ID` | Gerçek reklamlar | Evet (yoksa deneme reklamı gösterilir, gelir üretmez) |
| `PLAY_SERVICE_ACCOUNT_JSON` | Otomatik yükleme | Hayır (AAB elle de yüklenebilir) |

## Henüz denenmemiş olanlar

Bunlar ancak senin hesaplarınla denenebilir; kod yazıldı ve derleniyor ama gerçek mağaza ortamında çalıştırılmadı:

- Satın alma akışı (ürün sorgusu, ödeme, onay, yeni cihazda geri yükleme)
- Gerçek reklam birimiyle reklam gösterimi ve Avrupa onay penceresi
- GitHub'dan Play'e otomatik yükleme (hizmet hesabı tanımlanınca ilk çalışmada görülür)
