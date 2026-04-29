# Muhasebe Backend (Jakarta EE 10)

Dinamik muhasebe yazılımı — kullanıcılar kod yazmadan tablolar oluşturabilir, ilişkilendirebilir,
view'ler üretebilir ve tablolar arası otomatik işlemler (stored procedure) tanımlayabilir.

## Teknoloji Yığını

- **Java 17**
- **Jakarta EE 10** (EJB, CDI, JPA)
- **EclipseLink** (JPA provider)
- **Jakarta Faces (JSF) + PrimeFaces** (frontend)
- **PostgreSQL** (veritabanı)
- **Maven** (WAR paketleme)
- **WildFly 31** (uygulama sunucusu)
- **glassfish

## Kurulum Adımları

### Gereksinimler
- Java 17 (JDK) — `java -version` ile kontrol et
- Maven 3.8+ — `mvn -version`
- Docker + Docker Compose — `docker --version`, `docker compose version`

### Hızlı Başlangıç (Docker ile)

Projeyi tek komutla çalıştırmanın en kolay yolu:

```bash
# 1. WAR dosyasını oluştur
mvn clean package

# 2. PostgreSQL + WildFly'ı başlat
docker compose up -d --build

# 3. Tarayıcıdan aç
# http://localhost:8080/muhasebe-backend/
```

İlk çalıştırmada WildFly image'ı build edilir (~2-3 dk), sonraki çalıştırmalar hızlıdır.

### Log'ları İzle

```bash
# Tüm servisler
docker compose logs -f

# Sadece WildFly
docker compose logs -f wildfly

# Sadece PostgreSQL
docker compose logs -f postgres
```

### Kod Değişikliği Sonrası Yeniden Deploy

```bash
# WAR'ı yeniden build et
mvn clean package

# Sadece WildFly container'ını yeniden build edip başlat
docker compose up -d --build wildfly
```

### Servisleri Durdur

```bash
docker compose down              # container'ları durdur, volume'ları sakla
docker compose down -v           # volume'ları da sil (DB sıfırlanır!)
```

### Veritabanına Erişim

**psql ile içeriden:**
```bash
docker compose exec postgres psql -U postgres -d muhasebedb
```

**DBeaver/pgAdmin gibi araçlardan:**
- Host: `localhost`
- Port: `5432`
- DB: `muhasebedb`
- User: `postgres`
- Password: `postgres`



### Docker Olmadan Çalıştırma (Manuel Kurulum)

Docker kullanmak istemezsen:

1. PostgreSQL'i manuel kur, `createdb -U postgres muhasebedb`, `psql -d muhasebedb -f db/init.sql`
2. WildFly 31 indir, `standalone/configuration/standalone.xml` içine datasource ekle:
   ```xml
   <datasource jndi-name="java:/MuhasebeDS" pool-name="MuhasebeDS" enabled="true">
       <connection-url>jdbc:postgresql://localhost:5432/muhasebedb</connection-url>
       <driver>postgresql</driver>
       <security><user-name>postgres</user-name><password>postgres</password></security>
   </datasource>
   <driver name="postgresql" module="org.postgresql">
       <driver-class>org.postgresql.Driver</driver-class>
   </driver>
   ```
3. PostgreSQL driver'ını `modules/org/postgresql/main/` altına jar + module.xml olarak ekle
4. `mvn clean package`, `target/muhasebe-backend.war` dosyasını `standalone/deployments/` altına kopyala
5. `bin/standalone.sh` ile başlat



---

## Dizin Yapısı

```
muhasebe-backend/
├── pom.xml                          # Maven konfigürasyonu
├── README.md                        # Bu dosya
├── JSF-OGREN.md                     # JSF/PrimeFaces tanıtımı (senin için)
├── docker-compose.yml               # PostgreSQL + WildFly orkestrasyonu
├── Dockerfile                       # WildFly 31 image'ı (EclipseLink + PG driver)
├── .dockerignore
├── db/
│   └── init.sql                     # Şema oluşturma scripti
└── src/main/
    ├── java/com/muhasebe/
    │   ├── base/                    # Ortak sınıflar (BaseEntity, BaseRepository...)
    │   ├── tablemap/                # İlişki metadata modülü
    │   ├── category/                # Örnek: Kategori CRUD
    │   ├── product/                 # Örnek: Ürün CRUD
    │   ├── dynamic/
    │   │   ├── validator/           # SQL identifier validator (güvenlik)
    │   │   ├── ddl/                 # Tablo/kolon oluşturma/silme
    │   │   ├── dml/                 # Veri insert/update/delete
    │   │   ├── join/                # View oluşturma + filtreleme
    │   │   ├── query/               # Basit dinamik sorgu
    │   │   └── procedure/           # Stored procedure yönetimi
    │   └── web/                     # JSF managed bean'ler + converter
    ├── resources/META-INF/
    │   └── persistence.xml          # EclipseLink konfigürasyonu
    └── webapp/
        ├── index.xhtml              # Ana sayfa
        ├── WEB-INF/
        │   ├── web.xml
        │   ├── beans.xml            # CDI
        │   └── faces-config.xml
        ├── pages/                   # XHTML sayfaları
        │   ├── templates/layout.xhtml
        │   ├── tables/
        │   ├── views/
        │   ├── procedures/
        │   ├── categories/
        │   └── products/
        └── resources/css/app.css
```

---

## Düzeltilmiş Bug'lar (Önceki Projeye Göre)

Eski Spring Boot projesinden alınan ve bu sürümde giderilen sorunlar:

1. **SQL Injection** — Tüm dinamik identifier'lar artık `SqlIdentifierValidator` üzerinden geçiyor.
   Eski kodda `params.add("'" + value + "'")` gibi string concat kullanılıyordu — kritik açık.
2. **"product" hardcoded** — `DynamicJoinTable`'da `productIndex` mantığı tamamen kaldırıldı.
   İlişki yönü artık `table_map`'ten okunuyor, graf gezimi ile hesaplanıyor.
3. **Many-to-many ara tablo ambiguity** — `table_map`'e `join_table_name` kolonu eklendi.
   Artık `table1_table2` mı yoksa `table2_table1` mi diye tahmin etmiyoruz.
4. **`inputs.get(0)` bug'ı** — `DynamicProcedure`'de her column aynı input'u kullanıyordu.
   Şimdi her operasyon kendi `inputName` alanını belirtiyor.
5. **DECIMAL tip desteği** — Muhasebe için `NUMERIC(19,4)` eklendi. Eski kodda sadece INTEGER vardı.
6. **Cascade FK** — M2M ara tablolarına `ON DELETE CASCADE` eklendi, orphan row'lar birikmeyecek.
7. **Bool unboxing NPE** — `enableAlarm` null kontrolü yapılıyor.
8. **Boş if blokları** — `BaseService`'teki kontroller düzgün exception fırlatıyor.
9. **fetchFilteredDataFromView performansı** — Java filtreleme yerine SQL WHERE kullanılıyor.
10. **View kolon filtreleme** — `product_*` hardcoded prefix'leri kaldırıldı, generic filter var.

---

## Örnek Kullanım Senaryosu (Muhasebe)

1. **Tablolar oluştur:**
   - `banka`: ad (varchar), bakiye (decimal)
   - `borclar`: aciklama (varchar), tutar (decimal), odenen (decimal)
2. **Bir procedure tanımla** (Procedure Oluştur sayfası):
   - Ad: `borc_odeme`
   - Input: `odenen_tutar` (NUMERIC)
   - Operation 1: borclar.odenen, increase, inputName=odenen_tutar
   - Operation 2: banka.bakiye, decrease, inputName=odenen_tutar
3. **Önce veriler ekle:**
   - banka: ad="Ziraat", bakiye=10000
   - borclar: aciklama="Elektrik", tutar=500, odenen=0
4. **Procedure çalıştır:**
   - Ad: borc_odeme
   - Input değerleri: `150.50`
   - Etkilenen ID'ler: `borclar=1,banka=1`
5. **Sonuç:** borclar.odenen=150.50, banka.bakiye=9849.50

---

## Sorun Giderme

**`WFLYCTL0212: Duplicate resource` hatası**
- `standalone.xml`'de aynı datasource birden fazla tanımlı, tekrarı sil.

**`Unable to find class javax.transaction.TransactionManager`**
- JAR'ın içine EclipseLink gireceği için `jakarta.transaction` kullanılmalı.
  `module.xml`'de `javax.transaction.api` yerine gerekirse `jakarta.transaction.api` yaz.

**EclipseLink yerine Hibernate kullanılıyor**
- `persistence.xml`'de `<provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>`
  satırı var mı kontrol et. Ayrıca `<jboss-deployment-structure>` ile Hibernate'i
  dışarıda bırakmak gerekebilir (detaylı bilgi için bana danış).

**Veritabanı bağlantı hatası**
- `standalone.xml`'deki connection-url, user-name, password doğru mu?
- PostgreSQL servisi çalışıyor mu? `pg_isready` ile test et.

**404 hatası**
- URL doğru mu? Context path: `/muhasebe-backend/`
- `target/muhasebe-backend.war` gerçekten deploy edildi mi?

---

## Sonraki Adımlar

Bu ZIP'i deploy ettikten sonra yapacağımız iş:

1. Temel çalıştırma testleri (tablo oluştur, veri ekle, view yap, procedure çalıştır)
2. Bulunan hataları/eksikleri düzeltmek
3. Angular frontend'ini bana verirsen, onun ekranlarını JSF'e taşımak
4. Opsiyonel iyileştirmeler: Alarm özelliği, authentication, daha iyi UI
