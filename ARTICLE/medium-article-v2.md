# La matemática detrás de las firmas digitales suaves

*Cómo transformar trazos pixelados en firmas profesionales con curvas de Bézier y física simulada*

![SignaturePad Demo](../ART/sign.gif)

Capturar firmas digitales parece trivial: conecta puntos touch con líneas. El resultado: un dibujo de preescolar.

El primer problema es obvio: **esquinas afiladas**. Pero hay uno más sutil que arruina la experiencia: **grosor constante**. Una firma real varía con la velocidad de tu mano: lenta = gruesa, rápida = delgada.

La solución no es más código. Es **mejor matemática**.

---

## El problema: líneas rectas y grosor constante

```
Touch events: •     •    •      •   •
Connected:    •-----•----•------•---•
```

Dos problemas fundamentales:
1. **Esquinas afiladas** - El ojo humano detecta cada cambio de dirección brusco
2. **Grosor uniforme** - Una firma real es orgánica, no robótica

---

## La solución: interpolación con curvas de Bézier

La clave está en **interpolar** entre los puntos capturados usando curvas de Bézier cúbicas. En lugar de líneas rectas, creamos curvas suaves.

### ¿Qué es una curva de Bézier?

Una curva de Bézier cúbica se define con 4 puntos:
- **P₀**: Punto inicial
- **P₁, P₂**: Puntos de control (no están en la curva, la "guían")
- **P₃**: Punto final

La fórmula paramétrica es:

```
B(t) = (1-t)³·P₀ + 3(1-t)²t·P₁ + 3(1-t)t²·P₂ + t³·P₃
```

Donde `t` va de 0 a 1. Cuando `t=0` estamos en P₀, cuando `t=1` estamos en P₃, y entre medio la curva fluye suavemente.

### Calculando los puntos de control

El truco está en **cómo calculamos P₁ y P₂**. No podemos elegirlos arbitrariamente o la curva podría hacer loops extraños. Necesitamos que la transición entre curvas sea suave.

Aquí está el algoritmo (basado en Catmull-Rom):

```kotlin
fun calculateControlPoints(
    s1: Point,  // Punto anterior
    s2: Point,  // Punto actual
    s3: Point   // Punto siguiente
): Pair<Point, Point> {
    // Vectores entre puntos
    val dx1 = s1.x - s2.x
    val dy1 = s1.y - s2.y
    val dx2 = s2.x - s3.x
    val dy2 = s2.y - s3.y
    
    // Puntos medios
    val m1 = Point((s1.x + s2.x) / 2f, (s1.y + s2.y) / 2f)
    val m2 = Point((s2.x + s3.x) / 2f, (s2.y + s3.y) / 2f)
    
    // Distancias (para ponderar)
    val l1 = sqrt(dx1 * dx1 + dy1 * dy1)
    val l2 = sqrt(dx2 * dx2 + dy2 * dy2)
    
    // Razón de distancias
    val k = l2 / (l1 + l2)
    
    // Centro móvil ponderado
    val cm = Point(
        m2.x + (m1.x - m2.x) * k,
        m2.y + (m1.y - m2.y) * k
    )
    
    // Desplazamiento para tangente suave
    val tx = s2.x - cm.x
    val ty = s2.y - cm.y
    
    // Puntos de control finales
    return Pair(
        Point(m1.x + tx, m1.y + ty),  // P₁
        Point(m2.x + tx, m2.y + ty)   // P₂
    )
}
```

**¿Qué hace este código?**

1. **Calcula puntos medios** entre segmentos consecutivos
2. **Pondera por distancia** (k factor) para que curvas cortas y largas se mezclen bien
3. **Desplaza los puntos de control** para que la curva pase suavemente por s2

El resultado: **transiciones C1-continuas** (la primera derivada es continua = sin esquinas).

---

## Grosor variable: simulando física real

Ahora que tenemos curvas suaves, queremos que el grosor varíe con la velocidad. Esto requiere:

### 1. Capturar timestamps y calcular velocidad

```kotlin
data class TimedPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

fun velocityFrom(start: TimedPoint): Float {
    val deltaTime = (timestamp - start.timestamp).toFloat()
    if (deltaTime == 0f) return 0f
    
    val distance = sqrt((x - start.x).pow(2) + (y - start.y).pow(2))
    return distance / deltaTime  // px/ms
}
```

### 2. Transformar velocidad → grosor con curva gamma

La relación no es lineal. Usamos una **curva de potencia** (gamma) para controlar el contraste:

```kotlin
fun calculateStrokeWidth(
    velocity: Float,
    minWidth: Float,
    maxWidth: Float,
    minVelocity: Float,      // Nueva: umbral mínimo
    maxVelocity: Float,      // Nueva: umbral máximo
    widthVariation: Float    // Nueva: gamma (1.0 = lineal, 1.5 = más contraste)
): Float {
    // Normalizar velocidad entre 0 y 1
    val normalizedV = ((velocity - minVelocity) / (maxVelocity - minVelocity))
        .coerceIn(0f, 1f)
    
    // Aplicar curva gamma
    val widthFactor = (1f - normalizedV).pow(widthVariation)
    
    return minWidth + (maxWidth - minWidth) * widthFactor
}
```

**Parámetros clave:**
- `widthVariation = 1.5` (fountain pen) → Alto contraste, dramático
- `widthVariation = 1.2` (BIC pen) → Bajo contraste, casi uniforme
- `widthVariation = 1.1` (marker) → Grosor muy consistente

### 3. Suavizado EMA: Eliminando Jitter

El sensor touch tiene ruido. Si aplicamos el grosor directamente, obtenemos variaciones bruscas. Solución: **Exponential Moving Average**:

```kotlin
var smoothedVelocity = 0f
var smoothedWidth = 0f

// Dos niveles de suavizado independientes
val velocitySmoothness = 0.85f  // Para la velocidad
val widthSmoothness = 0.7f      // Para el grosor

fun updateVelocity(newVelocity: Float) {
    smoothedVelocity = velocitySmoothness * smoothedVelocity + 
                       (1 - velocitySmoothness) * newVelocity
}

fun updateWidth(newWidth: Float) {
    smoothedWidth = widthSmoothness * smoothedWidth + 
                    (1 - widthSmoothness) * newWidth
}
```

**Por qué dos filtros:**
- `velocitySmoothness` (0.85) → Suaviza el trazo general
- `widthSmoothness` (0.7) → Transiciones graduales de grosor
- Valores más altos = más suave pero menos responsivo

### 4. Filtrado de ruido de input

Un último detalle: el dedo tiembla. Filtramos puntos demasiado cercanos:

```kotlin
val inputNoiseThreshold = 0.8f  // px

if (distance(lastPoint, newPoint) > inputNoiseThreshold) {
    processPoint(newPoint)
}
```

---

## Renderizando la Curva: De Matemática a Píxeles

Tenemos la curva matemática, pero `Canvas.drawPath()` de Android no soporta grosor variable directamente. Necesitamos **dividir la curva en segmentos pequeños** y dibujar cada uno con su grosor:

```kotlin
fun drawBezierCurve(
    canvas: Canvas,
    curve: Bezier,
    startWidth: Float,
    endWidth: Float,
    paint: Paint
) {
    val steps = ceil(curve.length()).toInt()
    val widthDelta = endWidth - startWidth
    
    repeat(steps) { step ->
        val t = step.toFloat() / steps
        
        // Interpolación cúbica del grosor (t³ para suavidad)
        val width = startWidth + widthDelta * t * t * t
        
        // Punto en la curva (fórmula de Bézier)
        val t1 = 1f - t
        val t1_2 = t1 * t1
        val t1_3 = t1_2 * t1
        val t_2 = t * t
        val t_3 = t_2 * t
        
        val x = t1_3 * curve.p0.x +
                3f * t1_2 * t * curve.p1.x +
                3f * t1 * t_2 * curve.p2.x +
                t_3 * curve.p3.x
                
        val y = t1_3 * curve.p0.y +
                3f * t1_2 * t * curve.p1.y +
                3f * t1 * t_2 * curve.p2.y +
                t_3 * curve.p3.y
        
        paint.strokeWidth = width
        canvas.drawPoint(x, y, paint)
    }
}
```

**Detalles clave**:

1. **Número de steps** basado en la longitud de la curva: Curvas largas necesitan más puntos
2. **Interpolación cúbica del grosor** (`t³`): Hace que el cambio sea gradual
3. **Dibujamos puntos**, no líneas: Con `strokeCap = ROUND` los puntos se superponen suavemente

---


## Optimización: 60 FPS o Nada

Dibujar curvas Bézier en cada frame puede ser costoso. Tres optimizaciones críticas:

### 1. Object Pooling

```kotlin
// ❌ Crea objetos en hot path
fun onTouchMove(point: Point) {
    val controls = calculateControlPoints(...)  // Nueva instancia
    val curve = Bezier(...)  // Nueva instancia
}

// ✅ Reutiliza objetos
private val controlPointsCache = ControlPoints()
private val curveCache = Bezier()

fun onTouchMove(point: Point) {
    calculateControlPoints(..., cache = controlPointsCache)
    curveCache.set(...)  // Reutiliza instancia existente
}
```

### 2. Canvas Nativo vs Compose Canvas

```kotlin
// Compose Canvas es declarativo, pero queremos imperativo para performance
Canvas(modifier) {
    // ❌ Lento: Recompone todo en cada frame
    strokes.forEach { stroke ->
        drawPath(stroke.path, ...)
    }
}

// ✅ Rápido: Dibuja en bitmap mutable
val bitmap = remember { Bitmap.createBitmap(...) }
val nativeCanvas = remember { Canvas(bitmap) }

// Dibuja nuevas curvas en el bitmap (imperativo)
LaunchedEffect(newCurve) {
    drawBezierCurve(nativeCanvas, newCurve, ...)
}

// Compose solo renderiza el bitmap (cheap)
Canvas(modifier) {
    drawIntoCanvas { it.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null) }
}
```

### 3. derivedStateOf para Cálculos

```kotlin
// ❌ Recalcula en cada recomposición
val penWidthPx = with(LocalDensity.current) { penWidth.toPx() }

// ✅ Solo recalcula cuando penWidth cambia
val penWidthPx by remember {
    derivedStateOf { with(density) { penWidth.toPx() } }
}
```

**Resultado**: 60 FPS consistente incluso en trazos rápidos.

---

## Presets de Instrumentos: No Todos los Bolígrafos Son Iguales

Con todos estos parámetros, ¿cómo los configuramos? La respuesta: **presets calibrados** que simulan instrumentos reales.

### Fountain Pen (Pluma Estilográfica)
```kotlin
SignaturePadConfig.fountainPen()

// Configuración:
penMinWidth = 1.0.dp          // Trazos finos en velocidad
penMaxWidth = 4.0.dp          // Acumulación de tinta
velocitySmoothness = 0.85     // Flujo orgánico
widthSmoothness = 0.7         // Transiciones graduales
widthVariation = 1.5          // Alto contraste
```
**Resultado**: Firma elegante y expresiva. Ideal para documentos formales.

### BIC Pen (Bolígrafo Estándar)
```kotlin
SignaturePadConfig.pen()

// Configuración:
penMinWidth = 1.8.dp          // Grosor casi constante
penMaxWidth = 2.8.dp          // Poca variación
velocitySmoothness = 0.95     // Muy suave
widthVariation = 1.2          // Bajo contraste
```
**Resultado**: Grosor uniforme con sutil acumulación de tinta en curvas. Como un BIC real.

### Marker (Rotulador)
```kotlin
SignaturePadConfig.marker()

// Configuración:
penMinWidth = 5.0.dp          // Trazo grueso
penMaxWidth = 6.5.dp          // Muy consistente
widthVariation = 1.1          // Casi sin variación
```
**Resultado**: Trazo bold y visible. Perfecto para firmas en tablets grandes.

### Comparación de Parámetros

| Parámetro | Fountain Pen | BIC Pen | Marker | Propósito |
|-----------|--------------|---------|--------|-----------|
| Width Range | 1-4dp | 1.8-2.8dp | 5-6.5dp | Rango de grosor |
| Velocity Smoothness | 0.85 | 0.95 | 0.93 | Suavidad del trazo |
| Width Variation | 1.5 | 1.2 | 1.1 | Contraste (gamma) |
| Max Velocity | 8 px/ms | 8 px/ms | 18 px/ms | Umbral de velocidad |

**Insight clave**: No es solo el rango de grosor. Es la **combinación** de suavizado y curva gamma lo que crea la sensación táctil correcta.

---

## El resultado: firmas que se sienten reales

La combinación de estas técnicas produce firmas que:

- ✅ **Fluyen naturalmente** (Catmull-Rom → Bézier)
- ✅ **Varían en grosor** (velocidad + curva gamma)
- ✅ **Responden instantáneamente** (optimizaciones de performance)
- ✅ **Se sienten correctas** (presets calibrados)

![SignaturePad Demo](../ART/sign.gif)

### 9 parámetros, infinitas posibilidades

```kotlin
SignaturePadConfig(
    penMinWidth = 2.dp,              // Grosor mínimo
    penMaxWidth = 8.dp,              // Grosor máximo
    penColor = Color.Blue,           // Color del trazo
    velocitySmoothness = 0.8f,       // Suavidad (0.0-1.0)
    widthSmoothness = 0.7f,          // Transiciones de grosor
    minVelocity = 0f,                // Umbral inferior velocidad
    maxVelocity = 10f,               // Umbral superior velocidad
    widthVariation = 1.5f,           // Curva gamma (contraste)
    inputNoiseThreshold = 1.0f       // Filtro de temblor
)
```

Cada parámetro responde a una pregunta del usuario:
- *"¿Qué tan suave quiero el trazo?"* → `velocitySmoothness`
- *"¿Cuánto contraste necesito?"* → `widthVariation`
- *"¿Filtro el temblor de la mano?"* → `inputNoiseThreshold`

**Nota de diseño**: Los presets cubren el 95% de casos de uso. Pero si necesitas simular un sharpie desgastado o una pluma de caligrafía, todos los parámetros están expuestos.

---

## El código está disponible

Implementé todas estas técnicas en una librería open source: [**android-signaturepad**](https://github.com/rulogarcillan/android-signaturepad)

**Fork mejorado**: Esta librería es un fork del excelente [android-signaturepad](https://github.com/gcacace/android-signaturepad) de Gianluca Cacace, pero reescrito desde cero con **superpoderes**:
- 🔄 100% Kotlin + Jetpack Compose (el original era Java + Views)
- ⚙️ 9 parámetros configurables vs. 4 del original
- 🎨 3 presets calibrados de instrumentos reales
- 📐 Algoritmo Catmull-Rom mejorado con suavizado dual
- 🚀 Optimizaciones de performance (object pooling, bitmap caching)

```kotlin
// La API es simple...
@Composable
fun MySignature() {
    val state = rememberSignaturePadState()
    
    SignaturePad(
        state = state,
        config = SignaturePadConfig.fountainPen()
    )
}

// ...pero la implementación es matemática pura
```

**Características destacadas:**
- ✅ 3 presets optimizados (fountain pen, BIC, marker)
- ✅ 9 parámetros configurables para casos avanzados
- ✅ Undo/Redo completo con granularidad correcta
- ✅ Export a SVG y Bitmap (con/sin crop)
- ✅ 100% Jetpack Compose, API declarativa

Si quieres profundizar, el código más interesante está en:
- `BezierMath.kt` - Algoritmo Catmull-Rom
- `SignaturePad.kt` - Renderizado y optimizaciones
- `BezierRenderer.kt` - Dibujo con grosor variable
- `SignaturePadConfig.kt` - Configuración y presets

---

## Reflexiones finales

Capturar firmas digitales parece trivial hasta que lo intentas. La diferencia entre "funcional" y "excelente" está en los detalles matemáticos:

- **Interpolación correcta** elimina esquinas
- **Grosor variable** añade naturalidad
- **Suavizado dual** (velocidad + grosor) previene jitter
- **Presets calibrados** cubren casos comunes
- **Optimizaciones** mantienen fluidez

La próxima vez que firmes en una tablet y el trazo se vea perfecto, recuerda: hay curvas de Bézier, splines Catmull-Rom, filtros exponenciales y curvas gamma trabajando en segundo plano.

---

*Si implementas captura de firmas (o cualquier dibujo táctil), ¿qué técnicas te funcionan? 💬*

---

**Rulo Garcillan** | [GitHub](https://github.com/rulogarcillan) | Android & Kotlin

