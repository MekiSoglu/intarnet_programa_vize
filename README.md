Dinamik muhasebe yönetim sistemi. Kod yazmadan tablolar oluşturup, kategoriler tanımlayıp, ilişkiler kurabilir ve işlemler (procedure) çalıştırabilirsiniz.

## Teknolojiler

- **Java 17**
- **Jakarta EE 10** (EJB, CDI, JPA)
- **JSF + PrimeFaces 13** (UI)
- **EclipseLink 4** (ORM)
- **PostgreSQL 16**
- **WildFly 31** (Uygulama sunucusu)
- **Maven** (WAR packaging)

## Özellikler

- **Dinamik Tablo Yönetimi** — UI üzerinden tablo oluştur, kolon ekle, ilişki tanımla (1-N, N-N)
- **Kategori Hiyerarşisi** — Sınırsız derinlikte kategori ağacı (Araç > Makina > Kepçe)
- **Tablo-Kategori Bağlantısı** — Her tablo bir kategoriye bağlanabilir, Araclar sayfasında ağaç görünümünde listelenir
- **Dinamik View (Cari)** — Birden fazla tabloyu birleştiren view'ler oluştur
- **Dinamik Procedure (İşlem)** — Tablolar arası otomatik işlemler tanımla ve çalıştır (artır, azalt, çarp, böl, set)
- **Cascade Silme** — Kategori silinince tüm alt kategoriler ve bağlı tablolar otomatik silinir
- **SQL Injection Koruması** — Tüm dinamik SQL identifier'ları validate edilir

## Kurulum

### Gereksinimler

- Java 17+
- Maven 3.8+
- Docker + Docker Compose
- WildFly 31.0.1.Final











## Kullanım

### Kategori Oluşturma

1. Sol menüden **Category → Create Category**
2. Root kategori için Parent boş bırakılır (örn: `Araç`)
3. Alt kategori için Parent seçilir (örn: `Makina`, parent: `Araç`)
4. İstenen derinlikte hiyerarşi kurulabilir

### Dinamik Tablo Oluşturma

1. Sol menüden **Create Table**
2. Tablo adı gir (Türkçe karakter otomatik normalize edilir)
3. **Bağlı Kategori** seçilirse tablo o kategorinin altında görünür
4. Kolon ekle (ad + tip: varchar, integer, decimal, date, boolean, text)
5. İstersen FK ilişkisi ekle (1-N veya N-N)
6. **Tabloyu Oluştur**

### Kategori Ağacında Veri Görüntüleme

1. Sol menüden **Arac / Kategoriler**
2. `[+]` ile kategoriyi genişlet
3. Alt kategorileri ve o kategoriye bağlı tabloları gör
4. Mavi `[>] tablo_adi` butonuna tıkla → tablo verisi açılır
5. Yeni kayıt ekle formu alt tarafta çıkar

### Cari (View) Oluşturma

1. **Cari → Cari Oluştur**
2. Birleştirilecek tabloları seç (en az 2)
3. Sistem ilişkileri otomatik tespit eder
4. Cari adı gir → **Cari Oluştur**

### İşlem (Procedure) Tanımlama

1. **İşlem Tanımla**
2. İşlem adı gir
3. Input'lar ekle (örn: `odenen_tutar`)
4. Operasyonlar ekle: hangi tablonun hangi kolonuna ne yapılacak (artır/azalt/çarp/böl/set)
5. **Oluştur**

### İşlem Çalıştırma

1. **İşlemlerim** → işlemi seç
2. Etkilenecek tabloyu yükle, satır seç
3. Input değerlerini gir
4. **İşlemi Çalıştır**



muhasebe-backend/
├── src/main/java/com/muhasebe/
│   ├── base/               # Generic repository + service (CRUD)
│   ├── category/           # Kategori hiyerarşisi (parent-child)
│   ├── dynamic/
│   │   ├── ddl/            # CREATE/DROP TABLE, kategori bağlantısı
│   │   ├── dml/            # INSERT/UPDATE/DELETE/SELECT (dinamik tablolar)
│   │   ├── join/           # View oluşturma + filtreleme
│   │   ├── procedure/      # Procedure tanımlama + çalıştırma
│   │   ├── query/          # Basit dinamik sorgular
│   │   └── validator/      # SQL identifier validator + tip mapper
│   ├── tablemap/           # TableMap, ViewMap, Procedure entity + service
│   └── web/
│       ├── bean/           # JSF managed bean'ler (UI logic)
│       ├── converter/      # JSF entity converter
│       └── util/           # FacesUtil (message, redirect)
├── src/main/webapp/
│   ├── pages/
│   │   ├── templates/      # Sidebar layout
│   │   ├── categories/     # Category CRUD + Araclar ağaç sayfası
│   │   ├── tables/         # Tablo yönetimi + veri sayfaları
│   │   ├── views/          # Cari oluşturma + listeleme
│   │   └── procedures/     # İşlem tanımlama + çalıştırma
│   └── resources/css/      # app.css
├── src/main/resources/
│   └── META-INF/
│       └── persistence.xml # EclipseLink JPA konfigürasyonu
├── db/
│   └── init.sql            # PostgreSQL şema
├── docker/
│   ├── datasource.cli      # WildFly datasource konfigürasyonu
│   └── postgresql-module.xml
├── docker-compose.yml
├── Dockerfile
└── pom.xml
