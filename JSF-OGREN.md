# JSF + PrimeFaces Hızlı Başlangıç

Angular'dan gelirken JSF kafa karıştırıcı olabilir. Bu doküman projeni anlamaya yetecek
kadar JSF/PrimeFaces temelini veriyor.

## 1. Temel Fark: SPA vs Sunucu-Taraflı Render

### Angular (bildiğin dünya)
- Tarayıcıda çalışan JavaScript uygulaması
- Component → TypeScript sınıfı + HTML template + CSS
- HTTP ile REST API çağırır
- State client-side (memory'de)
- Değişiklikte virtual DOM diff ile yalnızca değişen kısım güncellenir

### JSF (Jakarta Faces)
- **Sunucu-taraflı** render — HTML server'da oluşturulup tarayıcıya gönderilir
- **XHTML dosyası** = HTML gibi ama JSF tag'leri ile (`<h:form>`, `<p:dataTable>`)
- **Managed Bean** = Java sınıfı, XHTML'in "controller"ı gibi
- State server-side (session'da tutuluyor)
- Her form submit aslında bir request — PrimeFaces AJAX ile partial update yapar

Kısaca: Angular'da **component class + HTML** varsa, JSF'te de **managed bean + XHTML** var.
Fark: Senkronizasyon REST üzerinden değil, doğrudan Java-HTML bağıyla oluyor.

---

## 2. Temel Yapı Taşları

### 2a) Managed Bean

Bu projede bir bean şöyle görünüyor:

```java
@Named              // CDI - XHTML'den bu bean'e "tableListBean" adıyla erişim
@ViewScoped         // Bu bean'in yaşam süresi: sayfa açık kaldığı sürece
public class TableListBean implements Serializable {

    @Inject
    private DynamicCreateTableService service;

    private List<String> tables = new ArrayList<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        tables = service.listDynamicTables();
    }

    public void deleteTable(String name) {
        service.dropTable(name);
        refresh();
    }

    public List<String> getTables() { return tables; }
}
```

### 2b) XHTML Sayfa

```xhtml
<h:form>
    <p:dataTable value="#{tableListBean.tables}" var="t">
        <p:column headerText="Tablo Adı">
            <h:outputText value="#{t}"/>
        </p:column>
        <p:column>
            <p:commandButton value="Sil"
                             action="#{tableListBean.deleteTable(t)}"
                             update="@form"/>
        </p:column>
    </p:dataTable>
</h:form>
```

Açıklama:
- `#{tableListBean.tables}` → Expression Language (EL). Bean'in `getTables()` metodunu çağırır.
- `var="t"` → Her satırdaki item'ı `t` adıyla gezer (Angular'daki `*ngFor="let t of tables"` gibi).
- `action="#{tableListBean.deleteTable(t)}"` → Butona basılınca bean metodu çağrılır.
- `update="@form"` → Sadece mevcut form'u yeniden render et (partial AJAX update).

---

## 3. Scope'lar (Çok Önemli!)

Bean'in "ne kadar süre yaşayacağı":

| Scope | Süre | Ne zaman kullan |
|-------|------|-----------------|
| `@RequestScoped` | Tek bir HTTP request | Basit form submit |
| `@ViewScoped` | **Sayfa açık kaldığı sürece** | CRUD sayfaları (en çok bunu kullanıyoruz) |
| `@SessionScoped` | Kullanıcının session'ı | Login bilgisi, shopping cart |
| `@ApplicationScoped` | Server açık kaldığı sürece (singleton) | Stateless servisler, converter |

Bu projede bean'lerimiz çoğunlukla `@ViewScoped` çünkü her sayfa kendi state'ini tutuyor
(liste, form değerleri, edit modu vb.).

---

## 4. Expression Language (EL) — `#{...}`

EL, Angular'ın interpolation'ına (`{{...}}`) benzer ama daha güçlü:

```xhtml
<!-- Property okuma -->
#{bean.name}                 → bean.getName()

<!-- Koleksiyon eleman -->
#{bean.items[0]}             → bean.getItems().get(0)
#{row['columnName']}         → row.get("columnName")

<!-- Metot çağırma (parametreli) -->
#{bean.save()}
#{bean.delete(item.id)}

<!-- Koşul -->
#{bean.editing ? 'Düzenle' : 'Ekle'}

<!-- Boolean -->
#{empty bean.items}          → liste boş mu
#{bean.count > 0}
```

---

## 5. Temel Tag'ler (Bu Projede Kullanılanlar)

### JSF Core (`h:`, `f:`, `ui:`)

| Tag | Açıklama |
|-----|----------|
| `<h:form>` | HTML form (Angular formGroup gibi) |
| `<h:inputText>` | Text input |
| `<h:outputText>` | Düz text |
| `<h:commandButton>` | Form submit button (bean metodu çağırır) |
| `<h:link outcome="/foo.xhtml">` | Başka sayfaya link |
| `<h:panelGroup>` | Div gibi, grup oluşturur |
| `<f:selectItem>` / `<f:selectItems>` | Dropdown option |
| `<f:param>` | URL parametresi |
| `<ui:composition template="...">` | Layout template uygula |
| `<ui:define name="...">` | Template slot'unu doldur |
| `<ui:insert name="...">` | Template'te slot tanımla |
| `<ui:repeat var="x" value="#{list}">` | Loop |

### PrimeFaces (`p:`) — daha güçlü alternatifler

| Tag | Açıklama |
|-----|----------|
| `<p:inputText>` | Daha güzel input |
| `<p:commandButton>` | AJAX destekli button |
| `<p:dataTable>` | Tablo (pagination, filtering, sorting) |
| `<p:column>` | Sabit kolon |
| `<p:columns>` | **Dinamik kolon** — çok kritik! |
| `<p:selectOneMenu>` | Dropdown |
| `<p:selectManyListbox>` | Multi-select |
| `<p:dialog>` | Modal |
| `<p:confirmDialog>` | Onay dialog'u |
| `<p:growl>` | Toast mesajı |
| `<p:inputNumber>` | Sayı input'u (para için ideal) |

---

## 6. Sayfa Akışı — En Önemli Kısım

Aşağıdaki akışı iyi anlarsan gerisi kolay:

1. Kullanıcı `http://.../tables/list.xhtml` açar
2. WildFly FacesServlet bu URL'yi yakalar
3. Sayfa oluşturulacak → JSF hangi bean'lere ihtiyaç var bakar (EL'deki `#{tableListBean...}`)
4. Bean yoksa yaratır, `@PostConstruct init()` çalışır
5. XHTML render edilir → tarayıcıya HTML gider
6. Kullanıcı "Sil" butonuna basar → AJAX request
7. Bean metodu (`deleteTable`) çalışır
8. `update="@form"` yazdığımız yerler yeniden render edilir → DOM güncellenir

**Angular'daki `subscribe()` yok, manuel state yönetimi yok.** Sen bean'de `tables` listesini
değiştirirsen, güncel değer otomatik sayfaya yansıyor (çünkü sunucu taraflı).

---

## 7. AJAX Update (Çok Kritik!)

PrimeFaces'te bir butona basınca **tüm sayfa yeniden yüklenmez**, sadece belirttiğin kısım update olur:

```xhtml
<p:commandButton value="Ekle"
                 action="#{bean.add}"
                 update="dataTable messages"
                 process="@form"/>
```

- `action` → hangi bean metodu çağrılacak
- `update` → request bittikten sonra hangi component yeniden render edilsin (boşlukla ayrılmış ID'ler)
- `process` → form submit'te hangi input'lar gönderilsin (`@form` = tüm form)

Özel ID'ler:
- `@form` → en yakın form
- `@this` → sadece butonun kendisi
- `@all` → tüm sayfa

**Tipik pattern:** Butona basınca tabloyu güncelle → `update=":formId:tableId"`
(`:` ile başlayanlar mutlak ID, `:` siz olanlar göreceli.)

---

## 8. Dinamik Tablo — Bu Projede Anahtar Kısım

Kullanıcı runtime'da tablo oluşturduğu için kolonları önceden bilmiyoruz. Çözüm:

```xhtml
<p:dataTable value="#{tableDataBean.rows}" var="row">
    <p:columns value="#{tableDataBean.columns}" var="col">
        <f:facet name="header">#{col}</f:facet>
        #{row[col]}
    </p:columns>
</p:dataTable>
```

- `rows` = `List<Map<String, Object>>` (bean'de)
- `columns` = `List<String>` (kolon isimleri)
- `row[col]` = Map lookup (EL otomatik `row.get(col)` yapıyor)

Angular'da aynı şeyi `*ngFor` ile yapacaksın, benzer mantık — ama burada server-side çalışıyor.

---

## 9. Layout Template Kullanımı

Proje `pages/templates/layout.xhtml` adında bir layout tanımlıyor. Her sayfa onu kullanıyor:

**Template (`layout.xhtml`):**
```xhtml
<h:head><title><ui:insert name="title">Default</ui:insert></title></h:head>
<h:body>
    <div class="topbar">Menü</div>
    <div class="container">
        <ui:insert name="content">Buraya içerik gelir</ui:insert>
    </div>
</h:body>
```

**Kullanan sayfa (`index.xhtml`):**
```xhtml
<ui:composition template="/pages/templates/layout.xhtml">
    <ui:define name="title">Ana Sayfa</ui:define>
    <ui:define name="content">
        <h1>Merhaba!</h1>
    </ui:define>
</ui:composition>
```

Angular'daki `<router-outlet>` + layout component mantığına benzer.

---

## 10. Hızlı Referans Kartı

| Ne yapmak istiyorsun? | Nasıl yapılır |
|------------------------|---------------|
| Tablo göster | `<p:dataTable value="..." var="x">` |
| Dinamik kolonlar | `<p:columns value="..." var="col">` |
| Satıra tıklayınca metot çağır | `<p:commandButton action="#{bean.edit(x)}" update="..."/>` |
| Başka sayfaya link | `<h:link outcome="/pages/foo.xhtml" value="Git"/>` |
| URL parametresiyle link | `<h:link outcome="..."><f:param name="id" value="#{x.id}"/></h:link>` |
| URL parametresi oku (bean'de) | `FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id")` |
| Modal aç | `<p:dialog widgetVar="dlg">...</p:dialog>` + butondan `oncomplete="PF('dlg').show()"` |
| Toast mesaj | `FacesUtil.info("Kaydedildi")` + sayfada `<p:growl id="growl"/>` + butonda `update=":growl"` |
| Onay al | `<p:confirmDialog global="true">...</p:confirmDialog>` + butonda `<p:confirm .../>` |
| Formu temizle | Bean'de yeni object ata + `update="form"` |
| Dropdown | `<p:selectOneMenu><f:selectItems .../></p:selectOneMenu>` |

---

## 11. Sık Yapılan Hatalar

1. **Bean değişikliği sayfada görünmüyor** → `update="..."` yazmayı unuttun veya yanlış ID yazdın.
2. **`#{bean.xyz}` not found** → Bean `@Named` eksik, veya import paketi yanlış.
3. **`view expired`** → ViewScoped bean session'dan atıldı (çok uzun inaktif kalındı).
4. **Form submit sonrası sayfa yenileniyor** → `ajax="false"` olmalıydı ya da `update` yoktu.
5. **Dropdown değeri bean'e dönmüyor** → Converter eksik (entity kullanıyorsan `EntityConverter`).
6. **Sayfa bozuk görünüyor** → XHTML valid değil (unclosed tag, yanlış namespace vs).

---

## 12. Projende Nerede Ne Var?

- Sayfa yapısı: `src/main/webapp/pages/templates/layout.xhtml`
- Ana sayfa: `src/main/webapp/index.xhtml`
- Tablo sayfaları: `src/main/webapp/pages/tables/`
- View sayfaları: `src/main/webapp/pages/views/`
- Procedure sayfaları: `src/main/webapp/pages/procedures/`
- Bean'ler: `src/main/java/com/muhasebe/web/bean/`
- CSS: `src/main/webapp/resources/css/app.css`

---

## 13. Önerilen Öğrenme Sırası

1. Bu dokümanı bir kez oku
2. `index.xhtml` + `TableListBean.java` + `tables/list.xhtml` üçlüsünü birlikte incele
3. Deploy et, çalıştır, tarayıcıda dev tools aç — AJAX request'leri izle
4. Küçük bir değişiklik yap (örn. yeni bir alan ekle), çalıştır, sonucu gör
5. Resmi doküman: <https://primefaces.org/showcase/> (canlı örnekler)

Takıldığın yerde kodu bana yolla, birlikte debug ederiz.
