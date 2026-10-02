package com.rodrigo.tracker

import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // colores del heatmap
    private val colores = intArrayOf(
        0xFF333333.toInt(), 0xFFA8E6A1.toInt(), 0xFF7FD47A.toInt(),
        0xFF4CAF50.toInt(), 0xFF2E8B3A.toInt(), 0xFF00E64D.toInt()
    )

    private val semanas = 26
    private val totalCeldas = semanas * 7    // celdas 7 por semana
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private lateinit var prefs: SharedPreferences
    private lateinit var grid: GridLayout
    private lateinit var tvProgreso: TextView
    private lateinit var cards: List<MaterialCardView>
    private lateinit var celdaHoy: View

    private val estado = BooleanArray(5)     // estado de habito
    private val hoy: String = fmt.format(Calendar.getInstance().time)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("habitflow", MODE_PRIVATE)
        grid = findViewById(R.id.gridHeatmap)
        tvProgreso = findViewById(R.id.tvProgreso)
        cards = listOf(
            findViewById(R.id.habit1), findViewById(R.id.habit2),
            findViewById(R.id.habit3), findViewById(R.id.habit4),
            findViewById(R.id.habit5)
        )

        cargarEstadoDeHoy()
        construirLeyenda()
        construirHeatmap()

        // click en habito
        cards.forEachIndexed { i, card ->
            card.setOnClickListener {
                estado[i] = !estado[i]
                guardarEstadoDeHoy()
                actualizarUI(animar = true)
            }
        }

        actualizarUI(animar = false)
    }


    private fun nivelHoy(): Int = estado.count { it }

    private fun actualizarUI(animar: Boolean) {
        // tarjeta de habito
        cards.forEachIndexed { i, card ->
            if (estado[i]) {
                card.strokeColor = 0xFF2ECC55.toInt()
                card.setCardBackgroundColor(0xFF0F2015.toInt())
            } else {
                card.strokeColor = 0xFF3A3A3A.toInt()
                card.setCardBackgroundColor(0xFF1A1A1A.toInt())
            }
        }

        // contador
        val n = nivelHoy()
        tvProgreso.text = "$n/5"
        tvProgreso.setTextColor(if (n == 5) 0xFF2ECC55.toInt() else 0xFF8A8A8A.toInt())

        // cuadro del ultimo dia en heatmap
        celdaHoy.background = fondoCelda(n)
        if (animar) {
            celdaHoy.animate().scaleX(1.5f).scaleY(1.5f).setDuration(120)
                .withEndAction {
                    celdaHoy.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
        }
    }

    // ---------- persistencia (un entero por día, cada bit = un hábito) ----------

    private fun cargarEstadoDeHoy() {
        val mascara = prefs.getInt("estado_$hoy", 0)
        for (i in 0 until 5) estado[i] = (mascara shr i) and 1 == 1
    }

    private fun guardarEstadoDeHoy() {
        var mascara = 0
        for (i in 0 until 5) if (estado[i]) mascara = mascara or (1 shl i)
        prefs.edit().putInt("estado_$hoy", mascara).apply()
    }


    // la ultima columna termina en el día de hoy, los días futuros quedan invisibles
    private fun construirHeatmap() {
        grid.removeAllViews()

        val hoyCal = Calendar.getInstance()
        val diaSemanaHoy = (hoyCal.get(Calendar.DAY_OF_WEEK) + 5) % 7   // Lun=0 ... Dom=6

        // lunes de la primera semana mostrada
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(diaSemanaHoy + (semanas - 1) * 7))

        val indiceHoy = (semanas - 1) * 7 + diaSemanaHoy

        for (i in 0 until totalCeldas) {
            if (i > indiceHoy) {
                grid.addView(crearCelda(0, visible = false))   // futuro: solo ocupa espacio
            } else {
                val fecha = fmt.format(cal.time)
                val nivel = Integer.bitCount(prefs.getInt("estado_$fecha", 0))
                grid.addView(crearCelda(nivel))
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        celdaHoy = grid.getChildAt(indiceHoy)
    }

    private fun crearCelda(nivel: Int, visible: Boolean = true): View {
        val d = resources.displayMetrics.density
        val lado = (12 * d).toInt()
        val margen = (2 * d).toInt()
        return View(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = lado; height = lado
                setMargins(margen, margen, margen, margen)
            }
            background = fondoCelda(nivel)
            visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun fondoCelda(nivel: Int) = GradientDrawable().apply {
        cornerRadius = 4f
        setColor(colores[nivel])
    }

    private fun construirLeyenda() {
        listOf(R.id.leg0, R.id.leg1, R.id.leg2, R.id.leg3, R.id.leg4, R.id.leg5)
            .forEachIndexed { n, id -> findViewById<View>(id).background = fondoCelda(n) }
    }
}