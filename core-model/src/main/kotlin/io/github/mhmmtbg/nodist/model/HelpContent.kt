package io.github.mhmmtbg.nodist.model

/**
 * Uygulama içi yardım: kullanım kılavuzu ve kısayol tablosu. Metinler hem Türkçe hem İngilizce tutulur; gösterimde
 * geçerli dile göre seçilir. [keys] doluysa madde bir kısayoldur (tuş birleşimi); aksi halde kılavuz maddesidir.
 */
class HelpItem(
    val section: String,
    val title: String,
    val body: String,
    val keys: String = "",
) {
    val isShortcut: Boolean get() = keys.isNotEmpty()
}

object HelpContent {
    private class Raw(val section: Pair<String, String>, val title: Pair<String, String>, val body: Pair<String, String>, val keys: String = "")

    private val raw = ArrayList<Raw>()

    private fun guide(section: Pair<String, String>, title: Pair<String, String>, body: Pair<String, String>) {
        raw += Raw(section, title, body)
    }

    private fun key(section: Pair<String, String>, keys: String, action: Pair<String, String>) {
        raw += Raw(section, action, action, keys)
    }

    private val start = "Başlarken" to "Getting started"
    private val tools = "Araçlar" to "Tools"
    private val edit = "Seçim ve düzenleme" to "Selecting and editing"
    private val paths = "Yollar ve şekiller" to "Paths and shapes"
    private val arrange = "Dönüştürme ve hizalama" to "Transform and align"
    private val boards = "Çalışma yüzeyleri" to "Artboards"
    private val helpers = "Cetvel, kılavuz, ızgara" to "Rulers, guides, grid"
    private val look = "Görünüm ve efektler" to "Appearance and effects"
    private val colour = "Renk ve örnekler" to "Colour and swatches"
    private val layers = "Katmanlar" to "Layers"
    private val files = "Dosyalar" to "Files"
    private val shortcuts = "Klavye kısayolları (Windows)" to "Keyboard shortcuts (Windows)"

    init {
        guide(start, "Gezinme" to "Navigating", "İki parmakla kaydırıp yakınlaştırın. Windows'ta fare tekerleği yakınlaştırır; sağ ya da orta tuşla sürüklemek kaydırır. Ctrl+0 belgeyi ekrana sığdırır." to "Pan and zoom with two fingers. On Windows the mouse wheel zooms and dragging with the right or middle button pans. Ctrl+0 fits the document to the screen.")
        guide(start, "Geri al ve yinele" to "Undo and redo", "Üst çubuktaki oklar son işlemleri geri alır ve yineler. Her işlem tek adımdır; sürükleme bitince kaydedilir." to "The arrows in the top bar undo and redo your last actions. Each action is one step and is recorded when the drag ends.")
        guide(start, "Belgelerim ve sekmeler" to "My documents and tabs", "Yeni belge açmak eskisini silmez; açık belgeler sekmelerde durur (en çok 8). Sekmeyi kapatmak belgeyi silmez, son hali Belgelerim'de kalır." to "Opening a new document never deletes the old one. Open documents live in tabs (up to 8). Closing a tab keeps the document in My documents.")
        guide(start, "Dil" to "Language", "Dosya menüsündeki Dil satırı Türkçe ve İngilizce arasında geçiş yapar." to "The Language row in the File menu switches between Turkish and English.")

        guide(tools, "Seçim (V)" to "Selection (V)", "Nesneleri taşıyın, köşe tutamaçlarından ölçekleyin, döndürün. Boş alanda sürükleyince çıkan kutu, dokunduğu bütün nesneleri seçer. Seçimde alt çubuk hizalama, şekil işlemleri, dönüştürme ve dışa aktarma eylemlerini gösterir." to "Move, scale from the corner handles and rotate objects. Dragging on empty canvas draws a box that selects everything it touches. With a selection the bottom bar shows align, shape, transform and export actions.")
        guide(tools, "Doğrudan seçim (A)" to "Direct selection (A)", "Düğümleri ve tutamaçları sürükleyin. Yola dokunarak düğüm ekleyin; seçili düğümü silin ya da köşe/yumuşak arasında çevirin." to "Drag anchors and handles. Tap a path to add an anchor; delete the selected anchor or switch it between corner and smooth.")
        guide(tools, "Kalem (P) ve kurşun kalem (N)" to "Pen (P) and pencil (N)", "Kalemle noktalara dokunarak ya da sürükleyerek Bezier yolu çizin; Enter (ya da bitir) yolu tamamlar, Esc iptal eder. Kurşun kalem serbest çizimi düzgün bir eğriye çevirir." to "With the pen, tap or drag to place Bezier points; Enter (or Finish) completes the path and Esc cancels. The pencil fits a smooth curve to your freehand stroke.")
        guide(tools, "Silgi (E)" to "Eraser (E)", "Yalnızca etkin katmanda ve yalnızca fırçanın değdiği yeri siler; diğer katmanlara dokunmaz. Metin ve görseller silgiyle silinmez." to "Erases only on the active layer and only where the brush touches; other layers are untouched. Text and images are not erased by the eraser.")
        guide(tools, "Makas (C)" to "Scissors (C)", "Araç çubuğundaki Kesme araçları düğmesinin menüsündedir. Bir yola dokunun: yol o noktadan kesilir. Açık yol ikiye ayrılır; kapalı yol o noktada açılır." to "In the Cutting tools button of the tool bar. Tap a path to cut it at that point. An open path is split in two; a closed path is opened at the point.")
        guide(tools, "Bıçak (K)" to "Knife (K)", "Kesme araçları menüsündedir. Tuvalde bir çizgi sürükleyin: etkin katmandaki dolgulu, kapalı şekiller bu çizgi boyunca iki ayrı şekle bölünür. Çizgi hiçbir şekli kesmezse bir uyarı çıkar." to "In the Cutting tools menu. Drag a line across the canvas: filled, closed shapes on the active layer are split in two along that line. If the line cuts nothing a notice appears.")
        guide(tools, "Şekiller (M, Q, L, Y, U)" to "Shapes (M, Q, L, Y, U)", "Bütün şekil araçları araç çubuğundaki tek Şekiller düğmesindedir; köşesindeki küçük üçgen menüsü olduğunu gösterir. Dokununca dikdörtgen (M), yuvarlak dikdörtgen (Q), elips (L), çokgen (Y), yıldız (U) ve çizgi simgeleriyle listelenir; düğme son seçilen şeklin simgesini gösterir. Sürükleyerek çizilir. Çokgende Kenar ±, yıldızda Uç ± ve İç ± düğmeleri bir sonraki şekli ayarlar." to "All shape tools are in the single Shapes button of the tool bar; the small triangle in its corner shows it has a menu. Tapping it lists rectangle (M), rounded rectangle (Q), ellipse (L), polygon (Y), star (U) and line with their icons; the button shows the icon of the last shape used. Shapes are drawn by dragging. Sides ±, Points ± and Inner ± adjust the next polygon or star.")
        guide(tools, "Çizgi, metin, damlalık, el" to "Line, text, eyedropper, hand", "Çizgi aracı düz çizgi çeker. Metin (T) dokunulan yere yazı ekler. Damlalık (I) dokunulan nesnenin görünümünü alır. El (H) yalnızca tuvali kaydırır." to "The line tool draws straight lines. Text (T) adds text where you tap. The eyedropper (I) picks up the look of the tapped object. Hand (H) only pans the canvas.")

        guide(edit, "Seçim, kopyalama, sıralama" to "Selecting, copying, ordering", "Ctrl+A hepsini seçer. Çoğalt, kopyala/kes/yapıştır (sekmeler arasında da), öne/arkaya alma, grupla ve grubu çöz eylemleri alt çubuktadır." to "Ctrl+A selects everything. Duplicate, copy/cut/paste (also between tabs), bring forward/send back, group and ungroup are in the bottom bar.")
        guide(edit, "Aynısını seç" to "Select same", "Seçili nesneyle aynı dolguya, konturuna, kontur kalınlığına, opaklığa ya da türe sahip tüm nesneleri seçer (alt çubuk: Aynısını seç…)." to "Selects every object with the same fill, stroke, stroke width, opacity or type as the selection (bottom bar: Select same…).")
        guide(edit, "Kırpma maskesi" to "Clipping mask", "İki ya da daha çok nesne seçin; en üstteki yol maske olur, diğerleri onun içine kırpılır. Maskeyi bırak kırpmayı kaldırır." to "Select two or more objects; the topmost path becomes the mask and the rest are clipped to it. Release mask removes the clipping.")

        guide(paths, "Pathfinder: birleştir, çıkar, kesiştir, dışla" to "Pathfinder: unite, minus front, intersect, exclude", "En az iki yol seçin ve Şekil menüsünden bir işlem seçin. Metin için önce Yola çevir, grup için Grubu çöz kullanın." to "Select at least two paths and pick an operation from the Shape menu. Convert text to outlines and ungroup groups first.")
        guide(paths, "Böl" to "Divide", "Yol menüsünde, en az iki yol seçiliyken: örtüşen şekiller sınırlarına göre ayrı parçalara bölünür; her parça üstteki sahibinin görünümünü alır." to "In the Path menu with two or more paths selected: overlapping shapes are split along their boundaries into separate pieces; each piece takes the look of its topmost owner.")
        guide(paths, "Bileşik yol" to "Compound path", "Yol menüsünden Bileşik yap: seçili yollar tek yol olur, içteki şekiller delik açar. Bileşiği çöz her parçayı yeniden ayrı yol yapar." to "Path menu → Make compound: the selected paths become one path and inner shapes punch holes. Release compound turns each piece back into its own path.")
        guide(paths, "Yolları birleştir" to "Join paths", "Tek bir açık yol seçiliyse kapatılır; birkaç açık yol seçiliyse uçları en yakın uca bağlanıp tek yol olur." to "A single open path is closed; several open paths are chained end to end by their nearest ends.")
        guide(paths, "Kontur ana hattı" to "Outline stroke", "Konturlu yolu, konturun biçimindeki dolu bir şekle çevirir (uç, köşe ve kesikli çizgi dahil). Yolda dolgu da varsa dolgu ve kontur bir grupta ayrılır." to "Turns a stroked path into a filled shape with the outline of the stroke (including caps, joins and dashes). If the path also has a fill, fill and stroke are split into a group.")
        guide(paths, "Yolu ötele" to "Offset path", "Yol menüsündeki Yolu ötele… kapalı yolu belirtilen mesafe kadar kalıcı olarak büyütür; negatif değer küçültür, köşeler yuvarlak olur. Sonradan değiştirilebilen hali Görünüm panelindeki canlı Yolu ötele efektidir." to "Offset path… in the Path menu permanently grows a closed path by the given distance; a negative value shrinks it and corners become round. The adjustable version is the live Offset path effect in the Appearance panel.")

        guide(arrange, "Hizalama ve dağıtma" to "Align and distribute", "Hizala menüsünde altı hizalama, yatay/dikey dağıtma ve \"Yatay/Dikey boşluk…\" (belirli boşluk) vardır. Sondaki Hedef düğmesi hizalamanın neye göre yapılacağını seçer: seçime, çalışma yüzeyine ya da anahtar nesneye (seçimin en üstteki nesnesi)." to "The Align menu has six alignments, horizontal/vertical distribute and Horizontal/Vertical spacing… (fixed gap). The Target chip at the end chooses what to align to: the selection, the artboard or the key object (the topmost selected object).")
        guide(arrange, "Dönüşüm araçları" to "Transform tools", "Dönüşüm araçları…: döndür, ölçekle, yansıt ya da yatır. Başvuru noktasını (dokuz noktadan biri), değeri ve \"kopya\" seçeneğini belirleyin. Tekrar dönüştür son dönüşümü yeniden uygular; kopyayla birlikte kullanınca düzenli bir dizi oluşur." to "Transform tools…: rotate, scale, reflect or shear. Choose the reference point (one of nine), the value and the Copy option. Transform again repeats the last transform; with Copy it builds a regular series.")
        guide(arrange, "Sayısal dönüştürme" to "Numeric transform", "Dönüştür eylemi seçimin konumunu, boyutunu ve açısını sayıyla girmenizi sağlar." to "Transform lets you type the selection's position, size and angle.")

        guide(boards, "Çalışma yüzeyi yönetimi" to "Managing artboards", "Dosya → Çalışma yüzeyleri…: yenisini ekleyin, çoğaltın (içeriğiyle), silin, yeniden adlandırın, konum ve boyutunu sayıyla verin, sanata sığdırın, yüzeye yakınlaşın ya da yalnızca o yüzeyi dışa aktarın. Yeni yüzeyler yan yana, 60 birim boşlukla dizilir." to "File → Artboards…: add, duplicate (with content), delete, rename, set position and size numerically, fit to artwork, zoom to an artboard or export just that artboard. New artboards are laid out side by side with a 60 unit gap.")

        guide(helpers, "Cetveller" to "Rulers", "Dosya → Cetveller tuvalin üstüne ve soluna ölçekli cetvel koyar. Cetvelden tuvale sürüklemek kılavuz çizgisi oluşturur." to "File → Rulers adds scaled rulers along the top and left. Dragging from a ruler onto the canvas creates a guide.")
        guide(helpers, "Kılavuzlar" to "Guides", "Kılavuza yapışılır. Sürükleyerek taşıyın; cetvele ya da tuvalin dışına bırakırsanız silinir. Dosya menüsünden gizleyin, kilitleyin, temizleyin ya da seçili nesnenin kenarlarından kılavuz üretin." to "Objects snap to guides. Drag to move them; drop one onto the ruler or off the canvas to delete it. Hide, lock or clear them from the File menu, or make guides from the selected object's edges.")
        guide(helpers, "Izgara" to "Grid", "Dosya → Izgara ızgarayı gösterir, Izgaraya yakala nesneleri ızgara çizgilerine yapıştırır, Izgara aralığı… aralığı değiştirir. Izgara ayarı değil, kılavuzlar .nodist dosyasında saklanır." to "File → Grid shows the grid, Snap to grid snaps objects to grid lines, Grid spacing… changes the spacing. Guides are stored in the .nodist file.")

        guide(look, "Görünüm paneli ve canlı efektler" to "Appearance panel and live effects", "Nesne seçiliyken alt çubuktaki Görünüm… eylemi paneli açar: karışım modu, nesnenin efekt listesi ve Efekt ekle bölümü. Efektler canlıdır: nesnenin kendisi değişmez, efekt üstüne uygulanır. Listedeki efekte dokunarak değerlerini değiştirin, çöp kutusuyla kaldırın. Canlı efektler .nodist dosyasında düzenlenebilir kalır; .ai, PDF, SVG ve PNG'ye görünümleriyle (düz vektör) yazılır. Görünümü genişlet, efektleri kalıcı vektöre çevirir." to "With an object selected, the Appearance… action in the bottom bar opens the panel: blend mode, the object's effect list and an Add effect section. Effects are live: the object itself stays unchanged and the effect is applied on top. Tap an effect in the list to change its values, or remove it with the bin. Live effects stay editable in .nodist files; they are written to .ai, PDF, SVG and PNG as their appearance (plain vector). Expand appearance turns the effects into permanent vector artwork.")
        guide(look, "Karışım modu" to "Blend mode", "Seçili nesnenin altındakilerle nasıl karışacağını belirler: Çarpma, Ekran, Bindirme, Koyulaştır, Açıklaştır, Fark ve diğerleri. Android 9 ve öncesinde yalnızca beş temel mod çizilir." to "Sets how the selected object mixes with what lies beneath: Multiply, Screen, Overlay, Darken, Lighten, Difference and more. On Android 9 and older only five basic modes are drawn.")
        guide(look, "Köşe yuvarlama" to "Round corners", "Düz köşeli bir yol (dikdörtgen, çokgen, yıldız, kalemle çizilmiş köşeler) seçiliyken alt çubuğun başındaki Köşe − ve Köşe + düğmeleri köşe yarıçapını değiştirir; yarıçap sıfırlanınca efekt kalkar. Kesin değer Görünüm panelinden girilir. Yuvarlak dikdörtgen aracı da şekli bu canlı efektle çizer, böylece köşeler sonradan ayarlanabilir." to "With a path with straight corners selected (rectangle, polygon, star, pen corners), the Corner − and Corner + buttons at the start of the bottom bar change the corner radius; at zero the effect is removed. Type an exact value in the Appearance panel. The rounded rectangle tool also draws with this live effect, so the corners can be adjusted afterwards.")
        guide(look, "Gölge" to "Drop shadow", "Seçili yol, metin ya da grubun arkasına gölge koyar. Yatay/dikey kayma, bulanıklık ve saydamlık girilir; rengi efekti düzenlerken değiştirilir. Bulanıklık, kademeli genişleyen yarı saydam kopyalarla taklit edilir." to "Puts a shadow behind the selected path, text or group. You enter the horizontal/vertical offset, blur and opacity; the colour is changed when editing the effect. The blur is imitated with gradually widening translucent copies.")
        guide(look, "Dış kontur" to "Outer stroke", "Nesnenin arkasına, seçili kontur renginde ve girilen kalınlıkta ikinci bir kontur ekler (iki renkli çerçeve gibi)." to "Adds a second stroke behind the object in the current stroke colour and the thickness you enter (like a two-colour frame).")
        guide(look, "Değişken genişlik" to "Variable width", "Açık bir yolun kalınlığını baştan sona değiştirir (sivri uçlu fırça darbesi gibi): baştaki ve sondaki kalınlık girilir. Yol düzenlenebilir kalır." to "Changes the width of an open path from start to end (like a tapered brush stroke): you enter the width at the start and at the end. The path stays editable.")
        guide(look, "Ok uçları" to "Arrowheads", "Konturlu açık yolun başına, sonuna ya da iki ucuna üçgen ok başı ekler. Boy, kontur kalınlığının katı olarak girilir (varsayılan 4)." to "Adds a triangular arrowhead to the start, the end or both ends of a stroked open path. The length is entered as a multiple of the stroke width (4 by default).")
        guide(look, "Kontur hizası" to "Align stroke", "Kapalı yolun konturunu şeklin içine ya da dışına hizalar." to "Aligns a closed path's stroke to the inside or outside of the shape.")
        guide(look, "Desen" to "Pattern", "En az iki nesne seçin: en üstteki kapalı yol doldurulur, diğerleri karo olur. Karolar yan yana döşenir ve şekle kırpılır. Karo efektin içine alınır (tuvalden kalkar); boşluk değerleri sonradan değiştirilebilir." to "Select at least two objects: the topmost closed path is filled and the others become the tile. Tiles are laid side by side and clipped to the shape. The tile is taken into the effect (it leaves the canvas); the gap values can be changed later.")
        guide(look, "Fırça" to "Brush", "Bir kapalı şekil ve bir açık yol seçin: şekil yolun boyuna esner ve yola oturur (sanat fırçası). Şekil fırçanın içine alınır; yolu doğrudan seçimle düzenlediğinizde fırça da onu izler. Kalınlık çarpanı şeklin yola dik boyunu ölçekler." to "Select one closed shape and one open path: the shape is stretched along the path (art brush). The shape is taken into the brush; when you edit the path with direct selection the brush follows it. The thickness factor scales the shape across the path.")
        guide(look, "Yol üzerine yazı" to "Text on path", "Bir metin ve bir açık yol seçin: metin yolun boyunca dizilir ve düzenlenebilir kalır (Metni düzenle). Yol metnin parçası olur ve metinle birlikte taşınır; başlangıç uzaklığı efektten değiştirilir." to "Select one text and one open path: the text is set along the path and stays editable (Edit text). The path becomes part of the text and moves with it; the start offset is changed in the effect.")
        guide(look, "Ağ gradyan" to "Mesh gradient", "Seçili kapalı yolu satır ve sütun sayısı kadar hücreye böler; hücreler dört köşe renginden çift doğrusal ara değerle boyanır ve şekle kırpılır. Köşe renkleri belgedeki ilk dört renk örneğidir (sırayla sol üst, sağ üst, sol alt, sağ alt); örnek yoksa mevcut dolgu ve kontur renkleri kullanılır. Noktaları düzenlenebilen gerçek bir ağ değildir." to "Splits the selected closed path into the given number of rows and columns; cells are coloured by bilinear interpolation of four corner colours and clipped to the shape. The corner colours are the first four swatches of the document (top-left, top-right, bottom-left, bottom-right); without swatches the current fill and stroke colours are used. It is not a true mesh with editable points.")
        guide(look, "Opaklık maskesi" to "Opacity mask", "İki nesne seçin: en üstteki, doğrusal gradyanlı şekil maske olur; beyaz görünür, siyah gizler. Maskenin gradyanı efekte alınır (şekil tuvalden kalkar). Görünüm, gradyan yönünde şeritlerle üretilir; şerit sayısı efektten değiştirilir ve şeritler arasında çok ince çizgiler görülebilir." to "Select two objects: the topmost shape with a linear gradient becomes the mask; white shows, black hides. The mask's gradient is taken into the effect (the shape leaves the canvas). The look is produced with bands along the gradient; the number of bands can be changed in the effect, and faint seams can show between bands.")
        guide(look, "Ana hat görünümü" to "Outline view", "Dosya menüsündeki Ana hat görünümü dolguları gizler ve yalnızca yolları ince çizgilerle gösterir. Belge değişmez; yalnızca gösterim farklıdır." to "Outline view in the File menu hides fills and shows only the paths as thin lines. The document is unchanged; only the display differs.")

        guide(colour, "Dolgu ve kontur" to "Fill and stroke", "Alt çubuktaki boya satırı dolgu ve konturu düzenler: düz renk, doğrusal/dairesel gradyan, opaklık; kontur kalınlığı, uç, köşe ve kesikli çizgi." to "The paint row in the bottom bar edits fill and stroke: solid colour, linear/radial gradient, opacity; stroke width, cap, join and dashes.")
        guide(colour, "Renk seçici" to "Colour picker", "RGB ve CMYK modlarında renk tekerleği; #RRGGBB, RGB ve CMYK değerleri her zaman görünür." to "A colour wheel in RGB and CMYK modes; the #RRGGBB, RGB and CMYK values are always shown.")
        guide(colour, "Renk örnekleri (swatches)" to "Swatches", "Örnekler düğmesi belgenin renk örnekleri panelini açar. Seçili rengi ya da belgedeki tüm renkleri ekleyin. Bir örneğin rengini değiştirmek, belgede o rengi kullanan her yeri de değiştirir. Örneğe dokunmak seçili nesneye uygular." to "The Swatches chip opens the document's swatch panel. Add the current colour or all document colours. Changing a swatch's colour also changes every place in the document that uses it. Tap a swatch to apply it to the selection.")

        guide(layers, "Katman paneli" to "Layers panel", "Katman ekleyin, silin, adlandırın, sıralayın, gizleyin, kilitleyin. Her katmanın içindeki nesne ağacı panelden gezilir; Katmanda göster seçili nesneyi listede bulur." to "Add, delete, rename, reorder, hide and lock layers. Each layer's object tree can be browsed; Show in layer finds the selected object in the list.")
        guide(layers, "Renklere göre gruplama, çoklu seçim" to "Group by colour, multi-select", "Paletteki düğme listeyi belgedeki renklere çevirir; satıra dokunmak o renkteki nesneleri seçer. Çoklu seçim düğmesi birkaç katman ve nesneyi birlikte seçer." to "The palette button turns the list into the document's colours; tapping a row selects the objects of that colour. The multi-select button selects several layers and objects together.")

        guide(files, "Açma ve kaydetme" to "Opening and saving", "Açılabilen biçimler: .nodist, .ai (PDF uyumlu), .pdf, .svg. Kaydet kendi biçimi olan .nodist ile yazar; katmanlar, düzenlenebilir metin, kılavuzlar, örnekler ve yüzey adları kayıpsız saklanır." to "You can open .nodist, .ai (PDF compatible), .pdf and .svg. Save writes the native .nodist format and keeps layers, editable text, guides, swatches and artboard names intact.")
        guide(files, "Dışa aktarma" to "Exporting", "Belgeyi .ai, PDF, SVG ya da PNG olarak dışa aktarın. Seçili nesneleri ya da bir katmanı ayrı dışa aktarmak için Dışa aktar eylemini kullanın." to "Export the document as .ai, PDF, SVG or PNG. Use Export to write just the selected objects or a single layer.")

        val s = shortcuts
        key(s, "Ctrl+Z" , "Geri al" to "Undo")
        key(s, "Ctrl+Shift+Z / Ctrl+Y", "Yinele" to "Redo")
        key(s, "Ctrl+S", "Kaydet" to "Save")
        key(s, "Ctrl+O", "Aç" to "Open")
        key(s, "Ctrl+Shift+I", "Dışarıdan ekle" to "Import into the document")
        key(s, "Ctrl+A", "Hepsini seç" to "Select all")
        key(s, "Ctrl+C / Ctrl+X / Ctrl+V", "Kopyala / kes / yapıştır" to "Copy / cut / paste")
        key(s, "Ctrl+D", "Çoğalt" to "Duplicate")
        key(s, "Ctrl+G", "Grupla" to "Group")
        key(s, "Ctrl+Shift+G", "Grubu çöz" to "Ungroup")
        key(s, "Ctrl+8", "Bileşik yol yap" to "Make compound path")
        key(s, "Ctrl+Alt+8", "Bileşik yolu çöz" to "Release compound path")
        key(s, "Ctrl+J", "Yolları birleştir" to "Join paths")
        key(s, "Ctrl+R", "Cetvelleri göster/gizle" to "Show/hide rulers")
        key(s, "Ctrl+0", "Ekrana sığdır" to "Fit to screen")
        key(s, "Ctrl+Tab / Ctrl+Shift+Tab", "Sonraki / önceki sekme" to "Next / previous tab")
        key(s, "Ctrl+W", "Sekmeyi kapat" to "Close tab")
        key(s, "Delete / Backspace", "Seçimi sil" to "Delete selection")
        key(s, "Esc", "Kalemde yolu iptal et" to "Cancel the pen path")
        key(s, "Enter", "Kalemde yolu bitir" to "Finish the pen path")
        key(s, "F1", "Yardım" to "Help")
        key(s, "V", "Seçim aracı" to "Selection tool")
        key(s, "A", "Doğrudan seçim aracı" to "Direct selection tool")
        key(s, "P", "Kalem" to "Pen")
        key(s, "N", "Kurşun kalem" to "Pencil")
        key(s, "E", "Silgi" to "Eraser")
        key(s, "C", "Makas" to "Scissors")
        key(s, "K", "Bıçak" to "Knife")
        key(s, "M", "Dikdörtgen" to "Rectangle")
        key(s, "Q", "Yuvarlak dikdörtgen" to "Rounded rectangle")
        key(s, "L", "Elips" to "Ellipse")
        key(s, "Y", "Çokgen" to "Polygon")
        key(s, "U", "Yıldız" to "Star")
        key(s, "T", "Metin" to "Text")
        key(s, "I", "Damlalık" to "Eyedropper")
        key(s, "H", "El (kaydır)" to "Hand (pan)")
    }

    /** Tüm maddeler, seçilen dilde. */
    fun items(english: Boolean): List<HelpItem> = raw.map { r ->
        val pick = { p: Pair<String, String> -> if (english) p.second else p.first }
        HelpItem(pick(r.section), pick(r.title), if (r.keys.isNotEmpty()) "" else pick(r.body), r.keys)
    }

    /** Aramada büyük/küçük harf, Türkçe harfler (ı, İ, ş, ğ, ü, ö, ç) ve aksanlar yok sayılır. */
    fun fold(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(
                when (ch) {
                    'İ', 'I', 'ı' -> 'i'
                    'Ş', 'ş' -> 's'
                    'Ğ', 'ğ' -> 'g'
                    'Ü', 'ü' -> 'u'
                    'Ö', 'ö' -> 'o'
                    'Ç', 'ç' -> 'c'
                    else -> ch.lowercaseChar()
                },
            )
        }
        return sb.toString()
    }

    /**
     * [query] içindeki her sözcüğü (boşlukla ayrılmış) içeren maddeler; başlıkta bulunanlar önce gelir. Boş aramada hepsi döner.
     * Arama hem seçilen dildeki hem de diğer dildeki metinde yapılır, böylece iki dilde de bulunur.
     */
    fun search(query: String, english: Boolean): List<HelpItem> {
        val own = items(english)
        val words = fold(query).split(' ', '\t').filter { it.isNotEmpty() }
        if (words.isEmpty()) return own
        val other = items(!english)
        val scored = ArrayList<Pair<Int, HelpItem>>()
        for ((i, it) in own.withIndex()) {
            val o = other[i]
            val head = fold(it.title + " " + it.keys + " " + o.title)
            val all = fold(it.section + " " + it.title + " " + it.body + " " + it.keys + " " + o.section + " " + o.title + " " + o.body)
            if (words.all { w -> w in all }) scored += (if (words.all { w -> w in head }) 0 else 1) to it
        }
        return scored.sortedBy { it.first }.map { it.second }
    }
}
