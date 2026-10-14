package io.github.mhmmtbg.nodist.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Uygulamanın kendi 24dp simgeleri; büyük simge kitaplığına bağımlılık yok. */
object AppIcons {
    private fun icon(name: String, pathData: String, evenOdd: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(
                pathData = addPathNodes(pathData),
                fill = SolidColor(Color.Black),
                pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
            )
            .build()

    // Araçlar
    val Select by lazy { icon("Select", "M5,3 L5,19 L9.5,14.8 L12.4,21 L14.8,19.9 L11.9,13.8 L18,13.5 Z") }
    val Direct by lazy {
        icon("Direct", "M5,3 L5,19 L9.5,14.8 L12.4,21 L14.8,19.9 L11.9,13.8 L18,13.5 Z M6.8,7.2 L13.6,12.1 L9.4,12.3 L6.8,14.8 Z", evenOdd = true)
    }
    val Pen by lazy {
        icon(
            "Pen",
            "M12,2 L17.5,12.5 L15,17 L9,17 L6.5,12.5 Z M12,6.2 L8.8,12.4 L10.2,15 L11.2,15 L11.2,11.6 " +
                "A1.6,1.6 0 1 1 12.8,11.6 L12.8,15 L13.8,15 L15.2,12.4 Z M8.5,18.5 H15.5 V22 H8.5 Z",
            evenOdd = true,
        )
    }
    val Pencil by lazy {
        icon(
            "Pencil",
            "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34" +
                "c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
        )
    }
    val Eraser by lazy {
        // Eğik silgi gövdesi (sağ üst uç sert, sol alt uç yumuşak kısım) ve zemin çizgisi.
        icon(
            "Eraser",
            "M15.2,3.2 L20.8,8.8 L10.6,19 L4.9,19 L2.6,16.7 Z M15.2,6 L6,15.2 L7.2,16.4 L10.4,16.4 L18,8.8 Z M11.5,20.5 H21 V22 H11.5 Z",
            evenOdd = true,
        )
    }
    val Scissors by lazy {
        icon(
            "Scissors",
            "M9.64,7.64c0.23,-0.5 0.36,-1.05 0.36,-1.64 0,-2.21 -1.79,-4 -4,-4S2,3.79 2,6s1.79,4 4,4c0.59,0 1.14,-0.13 1.64,-0.36L10,12l-2.36,2.36" +
                "C7.14,14.13 6.59,14 6,14c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4c0,-0.59 -0.13,-1.14 -0.36,-1.64L12,14l7,7h3v-1L9.64,7.64z" +
                "M6,8c-1.1,0 -2,-0.89 -2,-2s0.9,-2 2,-2 2,0.89 2,2 -0.9,2 -2,2zM6,20c-1.1,0 -2,-0.89 -2,-2s0.9,-2 2,-2 2,0.89 2,2 -0.9,2 -2,2z" +
                "M12,12.5c-0.28,0 -0.5,-0.22 -0.5,-0.5s0.22,-0.5 0.5,-0.5 0.5,0.22 0.5,0.5 -0.22,0.5 -0.5,0.5zM19,3l-6,6 2,2 7,-7V3z",
        )
    }
    val Knife by lazy {
        icon("Knife", "M3,20.5 L14.5,3.5 C16.8,3.9 19.6,6.4 20.5,9 L6.5,21 Z M7,17.5 L8.6,18.8 L16.5,8.2 L15,6.9 Z", evenOdd = true)
    }
    val Outline by lazy {
        icon("Outline", "M4,4 H20 V20 H4 Z M7,7 V17 H17 V7 Z M10,10 H14 V14 H10 Z", evenOdd = true)
    }
    val Offset by lazy {
        icon("Offset", "M3,3 H21 V21 H3 Z M5,5 V19 H19 V5 Z M8,8 H16 V16 H8 Z M10,10 V14 H14 V10 Z", evenOdd = true)
    }
    val Divide by lazy {
        icon("Divide", "M3,3 H15 V15 H3 Z M9,9 H21 V21 H9 Z M5,5 V13 H9 V9 H13 V5 Z M11,11 V19 H19 V11 Z", evenOdd = true)
    }
    val RoundedRect by lazy {
        icon("RoundedRect", "M7,4 H17 A3,3 0 0 1 20,7 V17 A3,3 0 0 1 17,20 H7 A3,3 0 0 1 4,17 V7 A3,3 0 0 1 7,4 Z M7,6 A1,1 0 0 0 6,7 V17 A1,1 0 0 0 7,18 H17 A1,1 0 0 0 18,17 V7 A1,1 0 0 0 17,6 Z", evenOdd = true)
    }
    val Polygon by lazy {
        icon("Polygon", "M12,2.5 L20.5,8.7 L17.3,18.7 L6.7,18.7 L3.5,8.7 Z M12,5.3 L6.2,9.5 L8.4,16.2 L15.6,16.2 L17.8,9.5 Z", evenOdd = true)
    }
    val Star by lazy {
        icon("Star", "M12,2 L14.9,8.6 L22,9.3 L16.6,14 L18.2,21 L12,17.3 L5.8,21 L7.4,14 L2,9.3 L9.1,8.6 Z")
    }
    val Rectangle by lazy { icon("Rectangle", "M3,5 H21 V19 H3 Z M5,7 V17 H19 V7 Z", evenOdd = true) }
    val Ellipse by lazy {
        icon("Ellipse", "M12,4 A8,8 0 1 0 12,20 A8,8 0 1 0 12,4 Z M12,6 A6,6 0 1 1 12,18 A6,6 0 1 1 12,6 Z", evenOdd = true)
    }
    val Line by lazy { icon("Line", "M4.7,20.7 L3.3,19.3 L19.3,3.3 L20.7,4.7 Z") }
    val Text by lazy { icon("Text", "M5,4v3h5.5v12h3V7H19V4z") }
    val Eyedropper by lazy {
        icon(
            "Eyedropper",
            "M20.71,5.63l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-3.12,3.12 -1.93,-1.91 -1.41,1.41 1.42,1.42L3,16.25V21h4.75" +
                "l8.92,-8.92 1.42,1.42 1.41,-1.41 -1.92,-1.92 3.12,-3.12c0.4,-0.4 0.4,-1.03 0.01,-1.42zM6.92,19L5,17.08l8.06,-8.06 1.92,1.92L6.92,19z",
        )
    }
    val Hand by lazy {
        icon(
            "Hand",
            "M23,5.5V20c0,2.2 -1.8,4 -4,4h-7.3c-1.08,0 -2.1,-0.43 -2.85,-1.19L1,14.83s1.26,-1.23 1.3,-1.25c0.22,-0.19 0.49,-0.29 0.79,-0.29 " +
                "0.22,0 0.42,0.06 0.6,0.16 0.04,0.01 4.31,2.46 4.31,2.46V4c0,-0.83 0.67,-1.5 1.5,-1.5S11,3.17 11,4v7h1V1.5c0,-0.83 0.67,-1.5 1.5,-1.5" +
                "S15,0.67 15,1.5V11h1V2.5c0,-0.83 0.67,-1.5 1.5,-1.5s1.5,0.67 1.5,1.5V11h1V5.5c0,-0.83 0.67,-1.5 1.5,-1.5s1.5,0.67 1.5,1.5z",
        )
    }

    // Eylemler
    val Undo by lazy {
        icon(
            "Undo",
            "M12.5,8c-2.65,0 -5.05,0.99 -6.9,2.6L2,7v9h9l-3.62,-3.62c1.39,-1.16 3.16,-1.88 5.12,-1.88 " +
                "3.54,0 6.55,2.31 7.6,5.5l2.37,-0.78C21.08,11.03 17.15,8 12.5,8z",
        )
    }
    val Redo by lazy {
        icon(
            "Redo",
            "M18.4,10.6C16.55,8.99 14.15,8 11.5,8c-4.65,0 -8.58,3.03 -9.96,7.22L3.9,16c1.05,-3.19 4.05,-5.5 " +
                "7.6,-5.5 1.95,0 3.73,0.72 5.12,1.88L13,16h9V7l-3.6,3.6z",
        )
    }
    val Export by lazy { icon("Export", "M9,16h6v-6h4l-7,-7 -7,7h4v6zM5,18h14v2H5v-2z") }
    val Delete by lazy {
        icon("Delete", "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z")
    }
    val Fit by lazy {
        icon(
            "Fit",
            "M3,5v4h2V5h4V3H5C3.9,3 3,3.9 3,5zM5,15H3v4c0,1.1 0.9,2 2,2h4v-2H5V15zM19,19h-4v2h4c1.1,0 2,-0.9 " +
                "2,-2v-4h-2V19zM19,3h-4v2h4v4h2V5C21,3.9 20.1,3 19,3z",
        )
    }
    val Layers by lazy {
        icon("Layers", "M11.99,18.54l-7.37,-5.73L3,14.07l9,7 9,-7 -1.63,-1.27 -7.38,5.74zM12,16l7.36,-5.73L21,9l-9,-7 -9,7 1.63,1.27L12,16z")
    }
    val Menu by lazy { icon("Menu", "M3,18h18v-2H3v2zM3,13h18v-2H3v2zM3,6v2h18V6H3z") }
    val More by lazy {
        icon(
            "More",
            "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 " +
                "-0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z",
        )
    }
    val Add by lazy { icon("Add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }
    val Close by lazy { icon("Close", "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z") }
    val ChevronRight by lazy { icon("ChevronRight", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z") }
    val ExpandMore by lazy { icon("ExpandMore", "M16.59,8.59L12,13.17 7.41,8.59 6,10l6,6 6,-6z") }
    val Visible by lazy {
        icon(
            "Visible",
            "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73,-4.39 -6,-7.5 -11,-7.5zM12,17" +
                "c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5zM12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z",
        )
    }
    val Hidden by lazy {
        icon(
            "Hidden",
            "M12,7c2.76,0 5,2.24 5,5 0,0.65 -0.13,1.26 -0.36,1.83l2.92,2.92c1.51,-1.26 2.7,-2.89 3.43,-4.75 -1.73,-4.39 " +
                "-6,-7.5 -11,-7.5 -1.4,0 -2.74,0.25 -3.98,0.7l2.16,2.16C10.74,7.13 11.35,7 12,7zM2,4.27l2.28,2.28 0.46,0.46" +
                "C3.08,8.3 1.78,10.02 1,12c1.73,4.39 6,7.5 11,7.5 1.55,0 3.03,-0.3 4.38,-0.84l0.42,0.42L19.73,22 21,20.73 3.27,3 " +
                "2,4.27zM7.53,9.8l1.55,1.55c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.66 1.34,3 3,3 0.22,0 0.44,-0.03 0.65,-0.08l1.55,1.55" +
                "c-0.67,0.33 -1.41,0.53 -2.2,0.53 -2.76,0 -5,-2.24 -5,-5 0,-0.79 0.2,-1.53 0.53,-2.2zM11.84,9.02l3.15,3.15 0.02,-0.16" +
                "c0,-1.66 -1.34,-3 -3,-3l-0.17,0.01z",
        )
    }
    val Lock by lazy {
        icon(
            "Lock",
            "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10" +
                "c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 " +
                "3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z",
        )
    }
    val Unlock by lazy {
        icon(
            "Unlock",
            "M12,17c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6h1.9" +
                "c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10" +
                "c0,-1.1 -0.9,-2 -2,-2zM18,20H6V10h12v10z",
        )
    }
    val Up by lazy { icon("Up", "M4,12l1.41,1.41L11,7.83V20h2V7.83l5.58,5.59L20,12l-8,-8 -8,8z") }
    val Down by lazy { icon("Down", "M20,12l-1.41,-1.41L13,16.17V4h-2v12.17l-5.58,-5.59L4,12l8,8 8,-8z") }

    // ---- Seçim çubuğu ve pano simgeleri ----------------------------------------
    // Basit geometrik simgeler dikdörtgenlerden kurulur: r = dolu dikdörtgen, frame = çerçeve (dört şerit).

    private fun r(x: Number, y: Number, w: Number, h: Number) = "M$x,$y h$w v$h h-$w z "
    private fun frame(x: Double, y: Double, w: Double, h: Double, t: Double = 1.5) =
        r(x, y, w, t) + r(x, y + h - t, w, t) + r(x, y + t, t, h - 2 * t) + r(x + w - t, y + t, t, h - 2 * t)
    private fun circle(cx: Number, cy: Double, rad: Number) = "M$cx,${cy - rad.toDouble()} a$rad,$rad 0 1 0 0.01,0 z "

    val Copy by lazy {
        icon("Copy", "M16,1H4C2.9,1 2,1.9 2,3v14h2V3h12V1zM19,5H8C6.9,5 6,5.9 6,7v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7C21,5.9 20.1,5 19,5zM19,21H8V7h11V21z")
    }
    val Paste by lazy {
        icon(
            "Paste",
            "M19,2h-4.18C14.4,0.84 13.3,0 12,0c-1.3,0 -2.4,0.84 -2.82,2H5C3.9,2 3,2.9 3,4v16c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V4" +
                "C21,2.9 20.1,2 19,2zM12,2c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM19,20H5V4h2v3h10V4h2V20z",
        )
    }
    val Cut by lazy {
        icon(
            "Cut",
            "M9.64,7.64c0.23,-0.5 0.36,-1.05 0.36,-1.64 0,-2.21 -1.79,-4 -4,-4S2,3.79 2,6s1.79,4 4,4c0.59,0 1.14,-0.13 1.64,-0.36L10,12" +
                "l-2.36,2.36C7.14,14.13 6.59,14 6,14c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4c0,-0.59 -0.13,-1.14 -0.36,-1.64L12,14l7,7h3v-1" +
                "L9.64,7.64zM6,8c-1.1,0 -2,-0.89 -2,-2s0.9,-2 2,-2 2,0.89 2,2 -0.9,2 -2,2zM6,20c-1.1,0 -2,-0.89 -2,-2s0.9,-2 2,-2 2,0.89 2,2 " +
                "-0.9,2 -2,2zM12,12.5c-0.28,0 -0.5,-0.22 -0.5,-0.5s0.22,-0.5 0.5,-0.5 0.5,0.22 0.5,0.5 -0.22,0.5 -0.5,0.5zM19,3l-6,6 2,2 7,-7V3z",
        )
    }
    val Checked by lazy {
        icon("Checked", "M19,3H5C3.89,3 3,3.9 3,5v14c0,1.1 0.89,2 2,2h14c1.11,0 2,-0.9 2,-2V5C21,3.9 20.11,3 19,3zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z")
    }
    val Unchecked by lazy {
        icon("Unchecked", "M19,5v14H5V5h14m0,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2z")
    }
    val MultiSelect by lazy {
        icon("MultiSelect", r(3, 4, 4, 4) + r(9, 5, 12, 2) + r(3, 10, 4, 4) + r(9, 11, 12, 2) + frame(3.0, 16.0, 4.0, 4.0, 1.0) + r(9, 17, 12, 2))
    }
    val Check by lazy { icon("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z") }
    val Back by lazy { icon("Back", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z") }
    val Import by lazy { icon("Import", "M12,3 h1 v8.2 l3.1,-3.1 1.4,1.4 -5.5,5.5 -5.5,-5.5 1.4,-1.4 3.1,3.1 V3 z " + r(4, 17, 16, 2) + r(4, 13, 2, 4) + r(18, 13, 2, 4)) }

    val AlignLeft by lazy { icon("AlignLeft", r(3, 3, 2, 18) + r(7, 6, 13, 4) + r(7, 14, 8, 4)) }
    val AlignCenterH by lazy { icon("AlignCenterH", r(11, 3, 2, 18) + r(4, 6, 16, 4) + r(7, 14, 10, 4)) }
    val AlignRight by lazy { icon("AlignRight", r(19, 3, 2, 18) + r(4, 6, 13, 4) + r(9, 14, 8, 4)) }
    val AlignTop by lazy { icon("AlignTop", r(3, 3, 18, 2) + r(6, 7, 4, 13) + r(14, 7, 4, 8)) }
    val AlignCenterV by lazy { icon("AlignCenterV", r(3, 11, 18, 2) + r(6, 4, 4, 16) + r(14, 7, 4, 10)) }
    val AlignBottom by lazy { icon("AlignBottom", r(3, 19, 18, 2) + r(6, 4, 4, 13) + r(14, 9, 4, 8)) }
    val DistributeH by lazy { icon("DistributeH", r(3, 3, 2, 18) + r(19, 3, 2, 18) + r(9.5, 7, 5, 10)) }
    val DistributeV by lazy { icon("DistributeV", r(3, 3, 18, 2) + r(3, 19, 18, 2) + r(7, 9.5, 10, 5)) }

    val Shapes by lazy { icon("Shapes", frame(3.0, 3.0, 13.0, 13.0, 2.0) + frame(8.0, 8.0, 13.0, 13.0, 2.0)) }
    val Unite by lazy { icon("Unite", "M3,3 H16 V8 H21 V21 H8 V16 H3 Z") }
    val MinusFront by lazy { icon("MinusFront", "M3,3 H16 V8 H8 V16 H3 Z " + frame(9.5, 9.5, 11.5, 11.5, 1.5)) }
    val Intersect by lazy { icon("Intersect", frame(3.0, 3.0, 13.0, 13.0, 1.5) + frame(8.0, 8.0, 13.0, 13.0, 1.5) + r(9.5, 9.5, 5, 5)) }
    val Exclude by lazy { icon("Exclude", r(3, 3, 13, 13) + r(8, 8, 13, 13), evenOdd = true) }
    val Mask by lazy { icon("Mask", r(3, 3, 18, 18) + circle(12, 12.0, 6), evenOdd = true) }
    val Compound by lazy { icon("Compound", r(3, 3, 18, 18) + r(8, 8, 8, 8), evenOdd = true) }
    val Release by lazy { icon("Release", frame(3.0, 3.0, 18.0, 18.0, 1.5) + r(8, 8, 8, 8)) }
    val Join by lazy { icon("Join", "M3,15 L3,12 L11,12 L11,8 L16,12 L21,12 L21,15 L14,15 L14,19 L9,15 Z") }
    val Unmask by lazy { icon("Unmask", frame(3.0, 3.0, 18.0, 18.0, 1.5) + circle(12, 12.0, 6) + circle(12, 12.0, 4.5), evenOdd = true) }
    val Transform by lazy {
        icon("Transform", frame(5.0, 5.0, 14.0, 14.0, 1.5) + r(3, 3, 5, 5) + r(16, 3, 5, 5) + r(3, 16, 5, 5) + r(16, 16, 5, 5))
    }
    val Duplicate by lazy {
        icon("Duplicate", "M3,3 H15 V5 H5 V15 H3 Z " + r(8, 8, 13, 13) + "M13.5,10.5 h2 v3 h3 v2 h-3 v3 h-2 v-3 h-3 v-2 h3 z", evenOdd = true)
    }
    val Group by lazy { icon("Group", frame(3.0, 3.0, 18.0, 18.0, 1.5) + r(6.5, 6.5, 5, 5) + r(12.5, 12.5, 5, 5)) }
    val Ungroup by lazy { icon("Ungroup", frame(3.0, 3.0, 9.0, 9.0, 2.0) + frame(12.0, 12.0, 9.0, 9.0, 2.0)) }
    val Forward by lazy { icon("Forward", "M12,4 l-7,7 h4.5 v9 h5 v-9 H19 z") }
    val Backward by lazy { icon("Backward", "M12,20 l-7,-7 h4.5 v-9 h5 v9 H19 z") }
    val ToFront by lazy { icon("ToFront", r(4, 3, 16, 2) + "M12,7 l-7,7 h4.5 v7 h5 v-7 H19 z") }
    val ToBack by lazy { icon("ToBack", r(4, 19, 16, 2) + "M12,17 l-7,-7 h4.5 v-7 h5 v7 H19 z") }
    val Rename by lazy { icon("Rename", frame(2.0, 8.0, 20.0, 8.0, 1.5) + r(6, 4, 1.5, 16) + r(4.5, 4, 4.5, 1.5) + r(4.5, 18.5, 4.5, 1.5)) }
    val Nodes by lazy { icon("Nodes", r(3, 16, 5, 5) + r(16, 3, 5, 5) + "M7.4,18 L6,16.6 L16.6,6 L18,7.4 Z") }
    val Picture by lazy { icon("Picture", frame(3.0, 4.0, 18.0, 16.0, 1.5) + "M6,17 l4.5,-6 l3,4 l2,-2.5 l3.5,4.5 z " + circle(8, 8.5, 1.5)) }
    val Flat by lazy { icon("Flat", r(3, 11, 18, 2)) }
    val Arc by lazy { icon("Arc", "M3,17 A9,9 0 0 1 21,17 L18.5,17 A6.5,6.5 0 0 0 5.5,17 Z") }
    val Arch by lazy { icon("Arch", "M3,19 V13 A9,9 0 0 1 21,13 V19 H18.5 V13 A6.5,6.5 0 0 0 5.5,13 V19 Z") }
    val Wave by lazy { icon("Wave", "M3,12 C6,4 9,4 12,11 C15,18 18,18 21,10 L21,13.5 C18,21.5 15,21.5 12,14.5 C9,7.5 6,7.5 3,15.5 Z") }
    val Bulge by lazy { icon("Bulge", "M3,12 Q12,2 21,12 Q12,22 3,12 Z M6.5,12 Q12,17.5 17.5,12 Q12,6.5 6.5,12 Z", evenOdd = true) }
    val Rise by lazy { icon("Rise", "M3,16 L21,5 V8.5 L3,19.5 Z") }
    val Corner by lazy { icon("Corner", "M3,19 L12,5 L21,19 L18,19 L12,9.7 L6,19 Z") }
    val Loop by lazy { icon("Loop", circle(12, 12.0, 8) + circle(12, 12.0, 5.5), evenOdd = true) }

    val Palette by lazy {
        icon(
            "Palette",
            "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9c0.83,0 1.5,-0.67 1.5,-1.5 0,-0.39 -0.15,-0.74 -0.39,-1.01 -0.23,-0.26 -0.38,-0.61 -0.38,-0.99 " +
                "0,-0.83 0.67,-1.5 1.5,-1.5H16c2.76,0 5,-2.24 5,-5 0,-4.42 -4.03,-8 -9,-8zM6.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,9 6.5,9 8,9.67 8,10.5 " +
                "7.33,12 6.5,12zM9.5,8C8.67,8 8,7.33 8,6.5S8.67,5 9.5,5s1.5,0.67 1.5,1.5S10.33,8 9.5,8zM14.5,8c-0.83,0 -1.5,-0.67 -1.5,-1.5S13.67,5 14.5,5" +
                "s1.5,0.67 1.5,1.5S15.33,8 14.5,8zM17.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S16.67,9 17.5,9s1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5z",
        )
    }
    val TextLeft by lazy { icon("TextLeft", r(3, 5, 18, 2) + r(3, 9, 12, 2) + r(3, 13, 18, 2) + r(3, 17, 10, 2)) }
    val TextCenter by lazy { icon("TextCenter", r(3, 5, 18, 2) + r(6, 9, 12, 2) + r(3, 13, 18, 2) + r(7, 17, 10, 2)) }
    val TextRight by lazy { icon("TextRight", r(3, 5, 18, 2) + r(9, 9, 12, 2) + r(3, 13, 18, 2) + r(11, 17, 10, 2)) }
    val Legend by lazy {
        icon("Legend", r(3, 5, 6, 6) + r(11, 6, 10, 1.6) + r(11, 8.6, 7, 1.6) + r(3, 13, 6, 6) + r(11, 14, 10, 1.6) + r(11, 16.6, 7, 1.6))
    }

    // ---- Canlı efektler ----
    val RoundCorner by lazy { icon("RoundCorner", "M4,20 V4 H13 A7,7 0 0 1 20,11 V20 Z M6,18 H18 V11 A5,5 0 0 0 13,6 H6 Z", evenOdd = true) }
    val Taper by lazy { icon("Taper", "M2.5,18.5 Q10,10 21.5,3.5 Q13.5,12.5 5.5,21.5 Z") }
    val Arrow by lazy { icon("Arrow", "M3,19.6 L14.6,8 L11.5,4.9 L20,4 L19.1,12.5 L16,9.4 L4.4,21 Z") }
    val ArrowBoth by lazy { icon("ArrowBoth", "M3,21 L3.9,13.5 L6.6,16.2 L16.2,6.6 L13.5,3.9 L21,3 L20.1,10.5 L17.4,7.8 L7.8,17.4 L10.5,20.1 Z") }
    val StrokeInside by lazy { icon("StrokeInside", frame(3.0, 3.0, 18.0, 18.0, 1.0) + frame(5.0, 5.0, 14.0, 14.0, 3.0)) }
    val StrokeOutside by lazy { icon("StrokeOutside", frame(2.0, 2.0, 20.0, 20.0, 3.0) + frame(7.0, 7.0, 10.0, 10.0, 1.0)) }
    val OuterStroke by lazy { icon("OuterStroke", frame(2.5, 2.5, 19.0, 19.0, 2.0) + r(7, 7, 10, 10)) }
    val Shadow by lazy { icon("Shadow", r(3, 3, 13, 13) + "M17.5,7.5 H21 V21 H7.5 V17.5 H17.5 Z") }
    val Mesh by lazy {
        icon("Mesh", frame(3.0, 3.0, 18.0, 18.0, 1.5) + r(8.25, 3, 1.5, 18) + r(14.25, 3, 1.5, 18) + r(3, 8.25, 18, 1.5) + r(3, 14.25, 18, 1.5) + r(3, 3, 6, 6))
    }
    val OpacityMask by lazy { icon("OpacityMask", frame(3.0, 3.0, 18.0, 18.0, 1.5) + r(3, 3, 9, 18) + r(13.5, 4.5, 3, 3) + r(16.5, 7.5, 3, 3) + r(13.5, 10.5, 3, 3) + r(16.5, 13.5, 3, 3) + r(13.5, 16.5, 3, 3)) }
    val Pattern by lazy {
        icon("Pattern", (0..2).joinToString("") { y -> (0..2).joinToString("") { x -> circle(5 + x * 7, 5.0 + y * 7, 2.2) } })
    }
    val Brush by lazy {
        icon(
            "Brush",
            "M7,14c-1.66,0 -3,1.34 -3,3 0,1.31 -1.16,2 -2,2 0.92,1.22 2.49,2 4,2 2.21,0 4,-1.79 4,-4 0,-1.66 -1.34,-3 -3,-3z" +
                "M20.71,4.63l-1.34,-1.34c-0.39,-0.39 -1.02,-0.39 -1.41,0L9,12.25 11.75,15l8.96,-8.96c0.39,-0.39 0.39,-1.02 0,-1.41z",
        )
    }
    val TextPath by lazy { icon("TextPath", "M2.5,19 Q12,8 21.5,19 L21.5,21.5 Q12,10.5 2.5,21.5 Z " + r(7, 2.5, 10, 2) + r(11, 2.5, 2, 9)) }
    val Blend by lazy { icon("Blend", circle(9, 12.0, 6.5) + circle(15, 12.0, 6.5), evenOdd = true) }
    val Bake by lazy { icon("Bake", frame(3.0, 3.0, 18.0, 18.0, 1.5) + "M12,6.5 L17,11.5 H13.5 V17.5 H10.5 V11.5 H7 Z") }
    val Effects by lazy { icon("Effects", "M9,2 L10.8,7.2 L16,9 L10.8,10.8 L9,16 L7.2,10.8 L2,9 L7.2,7.2 Z M17.5,12 L18.6,15.4 L22,16.5 L18.6,17.6 L17.5,21 L16.4,17.6 L13,16.5 L16.4,15.4 Z") }
    val Edit get() = Pencil
    val CutTools get() = Scissors

    // ---- Dosya menüsü ----
    val Gallery by lazy { icon("Gallery", r(3, 3, 8, 8) + r(13, 3, 8, 8) + r(3, 13, 8, 8) + r(13, 13, 8, 8)) }
    val NewDoc by lazy {
        icon("NewDoc", "M5,2 H14 L20,8 V22 H5 Z M7,4 V20 H18 V9.5 H12.5 V4 Z M11.5,11 h2 v2.5 h2.5 v2 h-2.5 v2.5 h-2 v-2.5 h-2.5 v-2 h2.5 z", evenOdd = true)
    }
    val Open by lazy {
        icon("Open", "M20,6h-8l-2,-2H4C2.9,4 2.01,4.9 2.01,6L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM20,18H4V8h16v10z")
    }
    val Sample by lazy {
        icon("Sample", "M5,2 H14 L20,8 V22 H5 Z M7,4 V20 H18 V9.5 H12.5 V4 Z M12.5,10.5 L13.6,13.4 L16.6,13.5 L14.2,15.4 L15.1,18.4 L12.5,16.6 L9.9,18.4 L10.8,15.4 L8.4,13.5 L11.4,13.4 Z", evenOdd = true)
    }
    val Save by lazy {
        icon("Save", "M17,3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V7l-4,-4zM12,19c-1.66,0 -3,-1.34 -3,-3s1.34,-3 3,-3 3,1.34 3,3 -1.34,3 -3,3zM15,9H5V5h10v4z")
    }
    val SaveAs by lazy {
        icon("SaveAs", "M15,3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h6v-2.5l7,-7V7l-3,-4zM13,9H5V5h8v4z M21.7,13.3 l-1,-1 c-0.4,-0.4 -1,-0.4 -1.4,0 L13,18.6 V21 h2.4 l6.3,-6.3 c0.4,-0.4 0.4,-1 0,-1.4 z")
    }
    val Artboards by lazy { icon("Artboards", "M17,15h2V7c0,-1.1 -0.9,-2 -2,-2H9v2h8v8zM7,17V1H5v4H1v2h4v10c0,1.1 0.9,2 2,2h10v4h2v-4h4v-2H7z") }
    val SelectAll by lazy {
        val d = StringBuilder()
        for (x in listOf(3.0, 8.5, 14.0)) { d.append(r(x, 3, 3.5, 1.5)); d.append(r(x + 0.5, 19.5, 3.5, 1.5)) }
        for (y in listOf(6.5, 12.0)) { d.append(r(3, y, 1.5, 3.5)); d.append(r(19.5, y + 0.5, 1.5, 3.5)) }
        d.append(r(19.5, 3, 1.5, 2)); d.append(r(3, 17.5, 1.5, 2))
        icon("SelectAll", d.toString() + r(8, 8, 8, 8))
    }
    val Magnet by lazy { icon("Magnet", "M3,3 h5 v3.5 h-5 z M16,3 h5 v3.5 h-5 z M3,8 h5 v4 a4,4 0 0 0 8,0 v-4 h5 v4 a9,9 0 0 1 -18,0 z") }
    val Ruler by lazy {
        icon("Ruler", frame(2.0, 7.0, 20.0, 10.0, 1.5) + r(5.5, 7, 1.5, 4.5) + r(9.5, 7, 1.5, 6) + r(13.5, 7, 1.5, 4.5) + r(17.5, 7, 1.5, 6))
    }
    val Grid by lazy { icon("Grid", frame(3.0, 3.0, 18.0, 18.0, 1.5) + r(8.25, 3, 1.5, 18) + r(14.25, 3, 1.5, 18) + r(3, 8.25, 18, 1.5) + r(3, 14.25, 18, 1.5)) }
    val Guides by lazy { icon("Guides", r(2, 6.5, 20, 1.5) + r(16, 2, 1.5, 20) + frame(4.0, 10.0, 9.0, 9.0, 1.5)) }
    val OutlineView by lazy { icon("OutlineView", frame(3.0, 3.0, 11.0, 11.0, 1.2) + circle(15, 15.0, 6.5) + circle(15, 15.0, 5.3), evenOdd = true) }
    val Help by lazy {
        icon(
            "Help",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,19h-2v-2h2v2zM15.07,11.25l-0.9,0.92C13.45,12.9 13,13.5 13,15h-2v-0.5" +
                "c0,-1.1 0.45,-2.1 1.17,-2.83l1.24,-1.26c0.37,-0.36 0.59,-0.86 0.59,-1.41 0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2H8c0,-2.21 1.79,-4 4,-4s4,1.79 4,4" +
                "c0,0.88 -0.36,1.68 -0.93,2.25z",
        )
    }
    val Language by lazy {
        icon("Language", circle(12, 12.0, 9.5) + circle(12, 12.0, 8) + "M12,2.5 a4,9.5 0 1 0 0.01,0 z M12,4 a2.6,8 0 1 0 0.01,0 z " + r(2.5, 11.25, 19, 1.5), evenOdd = true)
    }
    val Coffee by lazy {
        icon("Coffee", "M20,3H4v10c0,2.21 1.79,4 4,4h6c2.21,0 4,-1.79 4,-4v-3h2c1.11,0 2,-0.89 2,-2V5c0,-1.11 -0.89,-2 -2,-2zM20,8h-2V5h2v3zM4,19h16v2H4z")
    }

    // ---- Seçim çubuğu ve pencereler ----
    val SpacingH by lazy { icon("SpacingH", r(3, 4, 2, 16) + r(19, 4, 2, 16) + "M7,12 l3.5,-3.5 v2.5 h3 v-2.5 l3.5,3.5 l-3.5,3.5 v-2.5 h-3 v2.5 z") }
    val SpacingV by lazy { icon("SpacingV", r(4, 3, 16, 2) + r(4, 19, 16, 2) + "M12,7 l3.5,3.5 h-2.5 v3 h2.5 l-3.5,3.5 l-3.5,-3.5 h2.5 v-3 h-2.5 z") }
    val Target by lazy {
        icon("Target", circle(12, 12.0, 8) + circle(12, 12.0, 6.5) + circle(12, 12.0, 2.5) + r(11.25, 1.5, 1.5, 4) + r(11.25, 18.5, 1.5, 4) + r(1.5, 11.25, 4, 1.5) + r(18.5, 11.25, 4, 1.5), evenOdd = true)
    }
    val FillDot by lazy { icon("FillDot", circle(12, 12.0, 8)) }
    val StrokeRing by lazy { icon("StrokeRing", circle(12, 12.0, 8.5) + circle(12, 12.0, 6), evenOdd = true) }
    val FillStroke by lazy { icon("FillStroke", circle(12, 12.0, 9) + circle(12, 12.0, 7) + circle(12, 12.0, 5), evenOdd = true) }
    val StrokeWidth by lazy { icon("StrokeWidth", r(3, 5, 18, 1) + r(3, 9, 18, 2) + r(3, 14, 18, 3.5)) }
    val Opacity by lazy { icon("Opacity", circle(12, 12.0, 9) + circle(12, 12.0, 7.5) + "M12,4.5 A7.5,7.5 0 0 0 12,19.5 Z", evenOdd = true) }
    val Minus by lazy { icon("Minus", r(5, 11, 14, 2)) }
    val Plus by lazy { icon("Plus", r(5, 11, 14, 2) + r(11, 5, 2, 14)) }
    val CornerMark by lazy { icon("CornerMark", "M24,0 L24,24 L0,24 Z") }
    val Rotate by lazy { icon("Rotate", "M12,4 A8,8 0 1 0 20,12 H17.5 A5.5,5.5 0 1 1 12,6.5 V9.5 L16.5,5.25 L12,1 Z") }
    val Scale by lazy { icon("Scale", frame(3.0, 9.0, 12.0, 12.0, 1.5) + "M13,3 H21 V11 L18.2,8.2 L13.8,12.6 L11.4,10.2 L15.8,5.8 Z") }
    val Reflect by lazy { icon("Reflect", r(11.25, 2, 1.5, 20) + "M9.5,5 V19 L2.5,19 Z M14.5,5 L21.5,19 H14.5 Z M16,11.2 V17.5 H19.1 Z", evenOdd = true) }
    val Shear by lazy { icon("Shear", "M7,5 H22 L17,19 H2 Z M8.2,6.5 L4.1,17.5 H15.8 L19.9,6.5 Z", evenOdd = true) }
}
