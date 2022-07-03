package manticore.compiler

// The constructor is declared private so users cannot call it without going
// through the Color companion object (which checks various properties).
// Note that putting private *after* the name of the class makes the constructor
// private. The class itself is still public.
// If we had put private before the name of the class, then the whole class
// would be private and we wouldn't be able to return an object of this type
// even from the companion object.
class Color private (
    val r: Int,
    val g: Int,
    val b: Int
) {
  def toHexString(): String = {
    val rHexGHexBHex = List(r, g, b).map(value => "%02x".format(value))
    rHexGHexBHex.mkString
  }

  def toCssHexString(): String = {
    // CSS colors start with a '#'.
    s"#${toHexString()}"
  }

  def toRgbString(): String = {
    s"rgb(${r},${g},${b})"
  }
}

object Color {
  def apply(hexStr: String) = {
    val hexStrNoHash = hexStr.dropWhile(c => c == '#')

    // 2 chars for red, 2 chars for green, 2 chars for blue
    assert(hexStrNoHash.length() == 6)

    val rHex = hexStrNoHash.slice(0, 2)
    val gHex = hexStrNoHash.slice(2, 4)
    val bHex = hexStrNoHash.slice(4, 6)

    val rInt = Integer.parseInt(rHex, 16)
    val gInt = Integer.parseInt(gHex, 16)
    val bInt = Integer.parseInt(bHex, 16)

    new Color(rInt, gInt, bInt)
  }

  def apply(r: Int, g: Int, b: Int) = {
    val valuesWithinRange = List(r, g, b).forall(value => value < 256)
    assert(valuesWithinRange, s"(${r}, ${g}, ${b}) is not a valid RGB color.")

    new Color(r, g, b)
  }
}

object CyclicColorGenerator {
  // These colors were generated using the following website:
  //
  //    https://medialab.github.io/iwanthue/
  //
  private val colors = Vector(
    Color("#d45079"), Color("#4fc656"), Color("#b969e9"), Color("#86be36"), Color("#835dd8"), Color("#c4ba32"), Color("#436ae3"), Color("#92ab38"),
    Color("#9e40b5"), Color("#3c8f29"), Color("#d336a7"), Color("#3ac37d"), Color("#f82387"), Color("#298443"), Color("#e672d5"), Color("#77b55d"),
    Color("#3351bd"), Color("#e28f30"), Color("#7e84ed"), Color("#d2a43f"), Color("#6649ae"), Color("#628124"), Color("#c37fde"), Color("#968721"),
    Color("#5e64c1"), Color("#e2672a"), Color("#428ae4"), Color("#ce452b"), Color("#4cc3e3"), Color("#e53578"), Color("#56cbb0"), Color("#d14791"),
    Color("#80c687"), Color("#ac4197"), Color("#4b9e74"), Color("#d5404f"), Color("#39a8a4"), Color("#884b9f"), Color("#89a660"), Color("#5b4d99"),
    Color("#c2bb6f"), Color("#826cb5"), Color("#596416"), Color("#b297e1"), Color("#52783a"), Color("#de8bc2"), Color("#367042"), Color("#983f69"),
    Color("#277257"), Color("#e8846d"), Color("#4695c7"), Color("#b4682b"), Color("#4365a3"), Color("#886624"), Color("#809bdb"), Color("#9c4c2c"),
    Color("#754f8c"), Color("#968c4b"), Color("#a66495"), Color("#60642d"), Color("#e08698"), Color("#d19968"), Color("#914a57"), Color("#ad4b51")
  )

  def apply(numColors: Int): IterableOnce[Color] = {
    Seq.tabulate(numColors) { idx => colors(idx % colors.size) }
  }
}

object HeatmapColor {
  // https://stackoverflow.com/questions/12875486/what-is-the-algorithm-to-create-colors-for-a-heatmap
  def getHeatmapColorHSL(value: Double): (Double, Double, Double) = {
    assert(
      (0 <= value) && (value <= 1),
      s"Error: input value (${value}) must be normalized to [0, 1] for heatmap color generation."
    )
    // Hue is a "circle" such that 0° and 360° have the same color (red).
    // Instead I want cold colors to be mapped to dark blue, and warm colors to red.
    //
    //   Hue(dark blue) ~ 255
    //   Hue(red) = 0
    //
    val h = (1 - value) * 255 // [0, 360], but I restrict it to avoid the wrap-around behavior.
    val s = 1 // [0, 1]
    val l = 0.5 // [0, 1]
    // println(s"h = ${h}")
    // println(s"s = ${s}")
    // println(s"l = ${l}")
    (h, s, l)
  }

  // Adapted from Wikipedia article:
  // https://en.wikipedia.org/wiki/HSL_and_HSV#HSL_to_RGB
  def hslToRgb(h: Double, s: Double, l: Double): (Int, Int, Int) = {
    val c = (1 - Math.abs(2 * l - 1)) * s
    val hPrime = h / 60
    val x = c * (1 - Math.abs((hPrime % 2) - 1))
    val (rPrime, gPrime, bPrime) = if ((0 <= hPrime) && (hPrime < 1)) {
      (c, x, 0.0)
    } else if ((1 <= hPrime) && (hPrime < 2)) {
      (x, c, 0.0)
    } else if ((2 <= hPrime) && (hPrime < 3)) {
      (0.0, c, x)
    } else if ((3 <= hPrime) && (hPrime < 4)) {
      (0.0, x, c)
    } else if ((4 <= hPrime) && (hPrime < 5)) {
      (x, 0.0, c)
    } else if ((5 <= hPrime) && (hPrime < 6)) {
      (c, 0.0, x)
    } else {
      assert(false, s"hPrime (${hPrime}) did not fall in any valid range.")
      (0.0, 0.0, 0.0)
    }
    val m = l - c/2
    // println(s"c = ${c}")
    // println(s"hPrime = ${hPrime}")
    // println(s"x = ${x}")
    // println(s"rPrime = ${rPrime}")
    // println(s"gPrime = ${gPrime}")
    // println(s"bPrime = ${bPrime}")
    val Seq(r, g, b) = Seq(rPrime, gPrime, bPrime).map(v => ((v + m) * 255).toInt)
    (r, g, b)
  }

  def apply(value: Double): Color = {
    // Color is RGB, but heatmaps are generated in HSL. We must therefore convert the HSL value back to RGB.
    val (h, s, l) = getHeatmapColorHSL(value)
    val (r, g, b) = hslToRgb(h, s, l)
    Color(r, g, b)
  }
}
