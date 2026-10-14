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
        guide(tools, "Makas (C)" to "Scissors (C)", "Bir yola dokunun: yol o noktadan kesilir. Açık yol ikiye ayrılır; kapalı yol o noktada açılır." to "Tap a path to cut it at that point. An open path is split in two; a closed path is opened at the point.")
        guide(tools, "Bıçak (K)" to "Knife (K)", "Tuvalde bir çizgi sürükleyin: etkin katmandaki dolgulu, kapalı şekiller bu çizgi boyunca iki ayrı şekle bölünür. Çizgi hiçbir şekli kesmezse bir uyarı çıkar." to "Drag a line across the canvas: filled, closed shapes on the active layer are split in two along that line. If the line cuts nothing a notice appears.")
        guide(tools, "Şekil araçları (M, Q, L, Y, U)" to "Shape tools (M, Q, L, Y, U)", "Dikdörtgen (M), yuvarlak dikdörtgen (Q), elips (L), çokgen (Y) ve yıldız (U) sürükleyerek çizilir. Dar telefon çubuğunda ek araçlar \"Diğer araçlar\" düğmesinin menüsündedir. Yuvarlak dikdörtgende Köşe ±, çokgende Kenar ±, yıldızda Uç ± ve İç ± düğmeleri bir sonraki şekli ayarlar." to "Rectangle (M), rounded rectangle (Q), ellipse (L), polygon (Y) and star (U) are drawn by dragging. On narrow phone bars the extra tools are in the More tools button. Corner ±, Sides ±, Points ± and Inner ± adjust the next shape.")
        guide(tools, "Çizgi, metin, damlalık, el" to "Line, text, eyedropper, hand", "Çizgi aracı düz çizgi çeker. Metin (T) dokunulan yere yazı ekler. Damlalık (I) dokunulan nesnenin görünümünü alır. El (H) yalnızca tuvali kaydırır." to "The line tool draws straight lines. Text (T) adds text where you tap. The eyedropper (I) picks up the look of the tapped object. Hand (H) only pans the canvas.")

        guide(edit, "Seçim, kopyalama, sıralama" to "Selecting, copying, ordering", "Ctrl+A hepsini seçer. Çoğalt, kopyala/kes/yapıştır (sekmeler arasında da), öne/arkaya alma, grupla ve grubu çöz eylemleri alt çubuktadır." to "Ctrl+A selects everything. Duplicate, copy/cut/paste (also between tabs), bring forward/send back, group and ungroup are in the bottom bar.")
        guide(edit, "Aynısını seç" to "Select same", "Seçili nesneyle aynı dolguya, konturuna, kontur kalınlığına, opaklığa ya da türe sahip tüm nesneleri seçer (alt çubuk: Aynısını seç…)." to "Selects every object with the same fill, stroke, stroke width, opacity or type as the selection (bottom bar: Select same…).")
        guide(edit, "Kırpma maskesi" to "Clipping mask", "İki ya da daha çok nesne seçin; en üstteki yol maske olur, diğerleri onun içine kırpılır. Maskeyi bırak kırpmayı kaldırır." to "Select two or more objects; the topmost path becomes the mask and the rest are clipped to it. Release mask removes the clipping.")

        guide(paths, "Pathfinder: birleştir, çıkar, kesiştir, dışla" to "Pathfinder: unite, minus front, intersect, exclude", "En az iki yol seçin ve Şekil menüsünden bir işlem seçin. Metin için önce Yola çevir, grup için Grubu çöz kullanın." to "Select at least two paths and pick an operation from the Shape menu. Convert text to outlines and ungroup groups first.")
        guide(paths, "Böl" to "Divide", "Yol menüsünde, en az iki yol seçiliyken: örtüşen şekiller sınırlarına göre ayrı parçalara bölünür; her parça üstteki sahibinin görünümünü alır." to "In the Path menu with two or more paths selected: overlapping shapes are split along their boundaries into separate pieces; each piece takes the look of its topmost owner.")
        guide(paths, "Bileşik yol" to "Compound path", "Yol menüsünden Bileşik yap: seçili yollar tek yol olur, içteki şekiller delik açar. Bileşiği çöz her parçayı yeniden ayrı yol yapar." to "Path menu → Make compound: the selected paths become one path and inner shapes punch holes. Release compound turns each piece back into its own path.")
        guide(paths, "Yolları birleştir" to "Join paths", "Tek bir açık yol seçiliyse kapatılır; birkaç açık yol seçiliyse uçları en yakın uca bağlanıp tek yol olur." to "A single open path is closed; several open paths are chained end to end by their nearest ends.")
        guide(paths, "Kontur ana hattı" to "Outline stroke", "Konturlu yolu, konturun biçimindeki dolu bir şekle çevirir (uç, köşe ve kesikli çizgi dahil). Yolda dolgu da varsa dolgu ve kontur bir grupta ayrılır." to "Turns a stroked path into a filled shape with the outline of the stroke (including caps, joins and dashes). If the path also has a fill, fill and stroke are split into a group.")
        guide(paths, "Yolu ötele" to "Offset path", "Kapalı yolu belirtilen mesafe kadar büyütür; negatif değer küçültür. Köşeler yuvarlak olur." to "Grows a closed path by the given distance; a negative value shrinks it. Corners become round.")

        guide(arrange, "Hizalama ve dağıtma" to "Align and distribute", "Hizala menüsünde altı hizalama, yatay/dikey dağıtma ve \"Yatay/Dikey boşluk…\" (belirli boşluk) vardır. Sondaki Hedef düğmesi hizalamanın neye göre yapılacağını seçer: seçime, çalışma yüzeyine ya da anahtar nesneye (seçimin en üstteki nesnesi)." to "The Align menu has six alignments, horizontal/vertical distribute and Horizontal/Vertical spacing… (fixed gap). The Target chip at the end chooses what to align to: the selection, the artboard or the key object (the topmost selected object).")
        guide(arrange, "Dönüşüm araçları" to "Transform tools", "Dönüşüm araçları…: döndür, ölçekle, yansıt ya da yatır. Başvuru noktasını (dokuz noktadan biri), değeri ve \"kopya\" seçeneğini belirleyin. Tekrar dönüştür son dönüşümü yeniden uygular; kopyayla birlikte kullanınca düzenli bir dizi oluşur." to "Transform tools…: rotate, scale, reflect or shear. Choose the reference point (one of nine), the value and the Copy option. Transform again repeats the last transform; with Copy it builds a regular series.")
        guide(arrange, "Sayısal dönüştürme" to "Numeric transform", "Dönüştür eylemi seçimin konumunu, boyutunu ve açısını sayıyla girmenizi sağlar." to "Transform lets you type the selection's position, size and angle.")

        guide(boards, "Çalışma yüzeyi yönetimi" to "Managing artboards", "Dosya → Çalışma yüzeyleri…: yenisini ekleyin, çoğaltın (içeriğiyle), silin, yeniden adlandırın, konum ve boyutunu sayıyla verin, sanata sığdırın, yüzeye yakınlaşın ya da yalnızca o yüzeyi dışa aktarın. Yeni yüzeyler yan yana, 60 birim boşlukla dizilir." to "File → Artboards…: add, duplicate (with content), delete, rename, set position and size numerically, fit to artwork, zoom to an artboard or export just that artboard. New artboards are laid out side by side with a 60 unit gap.")

        guide(helpers, "Cetveller" to "Rulers", "Dosya → Cetveller tuvalin üstüne ve soluna ölçekli cetvel koyar. Cetvelden tuvale sürüklemek kılavuz çizgisi oluşturur." to "File → Rulers adds scaled rulers along the top and left. Dragging from a ruler onto the canvas creates a guide.")
        guide(helpers, "Kılavuzlar" to "Guides", "Kılavuza yapışılır. Sürükleyerek taşıyın; cetvele ya da tuvalin dışına bırakırsanız silinir. Dosya menüsünden gizleyin, kilitleyin, temizleyin ya da seçili nesnenin kenarlarından kılavuz üretin." to "Objects snap to guides. Drag to move them; drop one onto the ruler or off the canvas to delete it. Hide, lock or clear them from the File menu, or make guides from the selected object's edges.")
        guide(helpers, "Izgara" to "Grid", "Dosya → Izgara ızgarayı gösterir, Izgaraya yakala nesneleri ızgara çizgilerine yapıştırır, Izgara aralığı… aralığı değiştirir. Izgara ayarı değil, kılavuzlar .nodist dosyasında saklanır." to "File → Grid shows the grid, Snap to grid snaps objects to grid lines, Grid spacing… changes the spacing. Guides are stored in the .nodist file.")

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
