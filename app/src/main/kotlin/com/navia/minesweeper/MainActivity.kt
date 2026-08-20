package com.navia.minesweeper

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import java.security.SecureRandom
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

class MainActivity : Activity() {

    private data class Cell(
        var isMine: Boolean = false,
        var isRevealed: Boolean = false,
        var isFlagged: Boolean = false,
        var adjacentMines: Int = 0
    )

    private lateinit var game: MinesweeperGameView
    private lateinit var rowsInput: EditText
    private lateinit var colsInput: EditText
    private lateinit var minesInput: EditText
    private lateinit var seedInput: EditText
    private lateinit var mineCounter: TextView
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(20))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        val controlsScroll = HorizontalScrollView(this)
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        controlsScroll.addView(controls)

        rowsInput = numberInput("9")
        colsInput = numberInput("9")
        minesInput = numberInput("10")
        seedInput = EditText(this).apply {
            setTextSize(14f)
            setSingleLine(true)
            hint = "E-2528919715169309"
            setPadding(dp(8), 0, dp(8), 0)
            filters = arrayOf(android.text.InputFilter.LengthFilter(18))
            typeface = android.graphics.Typeface.MONOSPACE
        }

        controls.addView(labelled("Row", rowsInput))
        controls.addView(space(8))
        controls.addView(labelled("Column", colsInput))
        controls.addView(space(8))
        controls.addView(labelled("Mine", minesInput))
        controls.addView(space(8))
        controls.addView(labelled("Seed", seedInput))
        controls.addView(space(8))

        val newGameButton = Button(this).apply {
            text = "New Game"
            setOnClickListener { newGame() }
        }
        controls.addView(newGameButton)

        root.addView(controlsScroll, LinearLayout.LayoutParams(-1, dp(55)))

        val infoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        mineCounter = infoText("Mine: 0")
        timerText = infoText("Timer: 0")
        statusText = infoText("")
        statusText.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        infoRow.addView(mineCounter)
        infoRow.addView(space(14))
        infoRow.addView(timerText)
        infoRow.addView(space(14))
        infoRow.addView(statusText, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(infoRow)

        game = MinesweeperGameView()
        root.addView(
            game,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply { topMargin = dp(10) }
        )

        setContentView(root)
        newGame()
    }

    private fun newGame() {
        val rows = rowsInput.text.toString().toIntOrNull()?.coerceIn(2, 30) ?: 9
        val cols = colsInput.text.toString().toIntOrNull()?.coerceIn(2, 30) ?: 9
        var mines = minesInput.text.toString().toIntOrNull() ?: 10

        val maxPossible = rows * cols - min(9, rows * cols)
        if (mines > maxPossible) {
            mines = maxPossible
            minesInput.setText(mines.toString())
        }

        var seed = seedInput.text.toString().trim().uppercase()
        if (seed.isEmpty()) {
            seed = generateSeed()
            seedInput.setText(seed)
        }

        val normalized = normalizeSeed(seed)
        if (normalized == null) {
            Toast.makeText(
                this,
                "Seed tidak valid. Contoh: A-1234, E-2528919715169309, Z-FFFFFFFFFFFFFFFF",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        game.startNewGame(rows, cols, mines, normalized)
    }

    private fun generateSeed(): String {
        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val letter = letters[SecureRandom().nextInt(letters.length)]
        val bytes = ByteArray(8)
        SecureRandom().nextBytes(bytes)
        var value = 0L
        for (b in bytes) value = (value shl 8) or (b.toLong() and 0xFFL)
        val hex = java.lang.Long.toUnsignedString(value, 16).uppercase().padStart(16, '0')
        return "$letter-$hex"
    }

    private fun normalizeSeed(seed: String): String? {
        val s = seed.trim().uppercase()
        return if (Regex("^[A-Z]-[0-9A-F]{1,16}$").matches(s)) s else null
    }

    private fun numberInput(value: String): EditText = EditText(this).apply {
        setText(value)
        inputType = android.text.InputType.TYPE_CLASS_NUMBER
        setSingleLine(true)
        setTextSize(14f)
        setPadding(dp(6), 0, dp(6), 0)
        layoutParams = LinearLayout.LayoutParams(dp(80), dp(48))
    }

    private fun labelled(name: String, input: EditText): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = name
                setTextSize(14f)
            })
            addView(input)
        }

    private fun infoText(text: String) = TextView(this).apply {
        this.text = text
        setTextSize(14f)
        typeface = android.graphics.Typeface.MONOSPACE
    }

    private fun space(width: Int) = Space(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(width), 1)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    inner class MinesweeperGameView : View(this@MainActivity) {

        private var rows = 9
        private var cols = 9
        private var mineCount = 10
        private var seed = ""
        private var grid = Array(rows) { Array(cols) { Cell() } }

        private var firstClickDone = false
        private var firstClickX: Int? = null
        private var firstClickY: Int? = null
        private var gameOver = false
        private var won = false
        private var flagsPlaced = 0
        private var startTime: Long? = null
        private var timerRunning = false

        private val handler = Handler(Looper.getMainLooper())
        private var cellSize = dp(28).toFloat()
        private var boardLeft = 0f
        private var boardTop = 0f

        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        private val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }

        private val timerRunnable = object : Runnable {
            override fun run() {
                if (timerRunning) {
                    val seconds = ((System.currentTimeMillis() - (startTime ?: 0L)) / 1000L)
                    timerText.text = "Timer: $seconds"
                    handler.postDelayed(this, 250L)
                }
            }
        }

        private val gestureDetector = GestureDetector(this@MainActivity,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent) = true

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val p = cellAt(e.x, e.y) ?: return true
                    chordReveal(p.first, p.second)
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    val p = cellAt(e.x, e.y) ?: return true
                    revealCell(p.first, p.second)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    val p = cellAt(e.x, e.y) ?: return
                    toggleFlag(p.first, p.second)
                }
            })

        init {
            setBackgroundColor(0xFFFFFFFF.toInt())
            isClickable = true
        }

        fun startNewGame(newRows: Int, newCols: Int, newMines: Int, newSeed: String) {
            stopTimer()
            rows = newRows
            cols = newCols
            mineCount = newMines
            seed = newSeed
            grid = Array(rows) { Array(cols) { Cell() } }
            firstClickDone = false
            firstClickX = null
            firstClickY = null
            gameOver = false
            won = false
            flagsPlaced = 0
            startTime = null
            updateInfo()
            requestLayout()
            invalidate()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            val h = MeasureSpec.getSize(heightMeasureSpec)
            val desired = min(w, h)
            setMeasuredDimension(w, h)
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)

            val availableW = width.toFloat()
            val availableH = height.toFloat()
            cellSize = min(
                dp(28).toFloat(),
                min(availableW / cols, availableH / rows)
            ).coerceAtLeast(1f)

            val boardW = cols * cellSize
            val boardH = rows * cellSize
            boardLeft = (availableW - boardW) / 2f
            boardTop = (availableH - boardH) / 2f

            for (y in 0 until rows) {
                for (x in 0 until cols) drawCell(canvas, x, y)
            }
        }

        private fun drawCell(canvas: android.graphics.Canvas, x: Int, y: Int) {
            val c = grid[y][x]
            val l = boardLeft + x * cellSize
            val t = boardTop + y * cellSize
            val r = l + cellSize
            val b = t + cellSize

            paint.style = android.graphics.Paint.Style.FILL
            paint.color = when {
                c.isRevealed && c.isMine -> 0xFFFF9999.toInt()
                c.isRevealed -> 0xFFEEEEEE.toInt()
                c.isFlagged -> 0xFFFFDDDD.toInt()
                else -> 0xFFCCCCCC.toInt()
            }
            canvas.drawRect(l, t, r, b, paint)

            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = dp(1).toFloat()
            paint.color = 0xFF999999.toInt()
            canvas.drawRect(l, t, r, b, paint)

            if (c.isFlagged && !c.isRevealed) {
                drawCenteredText(canvas, "🚩", l, t, r, b, 16f)
            } else if (c.isRevealed && c.isMine) {
                drawCenteredText(canvas, "💣", l, t, r, b, 16f)
            } else if (c.isRevealed && c.adjacentMines > 0) {
                drawCenteredText(canvas, c.adjacentMines.toString(), l, t, r, b, 16f)
            }
        }

        private fun drawCenteredText(
            canvas: android.graphics.Canvas,
            text: String,
            l: Float, t: Float, r: Float, b: Float,
            sizeSp: Float
        ) {
            textPaint.textSize = sizeSp * resources.displayMetrics.scaledDensity
            val fm = textPaint.fontMetrics
            val cy = (t + b) / 2f - (fm.ascent + fm.descent) / 2f
            canvas.drawText(text, (l + r) / 2f, cy, textPaint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            return gestureDetector.onTouchEvent(event)
        }

        private fun cellAt(px: Float, py: Float): Pair<Int, Int>? {
            val x = floor((px - boardLeft) / cellSize).toInt()
            val y = floor((py - boardTop) / cellSize).toInt()
            return if (x in 0 until cols && y in 0 until rows) x to y else null
        }

        private fun getNeighbors(x: Int, y: Int): List<Pair<Int, Int>> {
            val result = ArrayList<Pair<Int, Int>>(8)
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until cols && ny in 0 until rows) result.add(nx to ny)
            }
            return result
        }

        private fun isSafeZone(x: Int, y: Int, safeX: Int, safeY: Int) =
            abs(x - safeX) <= 1 && abs(y - safeY) <= 1

        private fun seedTo64(value: String): Long {
            var hash = -3750763034362895579L // 0xCBF29CE484222325
            val prime = 1099511628211L
            for (ch in value) {
                hash = hash xor ch.code.toLong()
                hash *= prime
            }
            return hash
        }

        private fun splitMix64(input: Long): Pair<Long, Long> {
            var state = input - 7046029254386353131L
            var z = state
            z = (z xor (z ushr 30)) * -4658895280553007687L
            z = (z xor (z ushr 27)) * -7723592293110705685L
            z = z xor (z ushr 31)
            return state to z
        }

        private fun createRng64(seedValue: String): () -> Double {
            var state = seedTo64(seedValue)
            if (state == 0L) state = -7046029254386353131L
            state = splitMix64(state).second

            return {
                state = state xor (state ushr 12)
                state = state xor (state shl 25)
                state = state xor (state ushr 27)
                val result = state * 2685821657736338717L
                (result ushr 11).toDouble() / 9007199254740992.0
            }
        }

        private fun generateMinePositions(
            safeX: Int,
            safeY: Int
        ): List<Pair<Int, Int>> {
            val positions = ArrayList<Pair<Int, Int>>()
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    if (!isSafeZone(x, y, safeX, safeY)) positions.add(x to y)
                }
            }

            val generationSeed = "$seed|$rows|$cols|$mineCount|$safeX|$safeY"
            val random = createRng64(generationSeed)

            for (i in positions.lastIndex downTo 1) {
                val j = floor(random() * (i + 1)).toInt()
                val tmp = positions[i]
                positions[i] = positions[j]
                positions[j] = tmp
            }
            return positions.take(mineCount)
        }

        private fun placeMines(safeX: Int, safeY: Int) {
            for ((x, y) in generateMinePositions(safeX, safeY)) {
                grid[y][x].isMine = true
            }
            calculateAdjacentMines()
        }

        private fun calculateAdjacentMines() {
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    if (!grid[y][x].isMine) {
                        grid[y][x].adjacentMines =
                            getNeighbors(x, y).count { (nx, ny) -> grid[ny][nx].isMine }
                    }
                }
            }
        }

        private fun revealCell(x: Int, y: Int) {
            if (gameOver) return
            val cell = grid[y][x]
            if (cell.isRevealed || cell.isFlagged) return

            if (!firstClickDone) {
                firstClickDone = true
                firstClickX = x
                firstClickY = y
                placeMines(x, y)
                startTimer()
            }

            if (cell.isMine) {
                cell.isRevealed = true
                endGame(false)
                invalidate()
                return
            }

            floodFillReveal(x, y)
            invalidate()
            checkWinCondition()
        }

        private fun floodFillReveal(startX: Int, startY: Int) {
            val stack = ArrayDeque<Pair<Int, Int>>()
            val visited = HashSet<String>()
            stack.addLast(startX to startY)

            while (stack.isNotEmpty()) {
                val (x, y) = stack.removeLast()
                val key = "$x,$y"
                if (!visited.add(key)) continue

                val cell = grid[y][x]
                if (cell.isRevealed || cell.isFlagged || cell.isMine) continue

                cell.isRevealed = true
                if (cell.adjacentMines == 0) {
                    for ((nx, ny) in getNeighbors(x, y)) {
                        val n = grid[ny][nx]
                        if (!n.isRevealed && !n.isFlagged && !n.isMine) {
                            stack.addLast(nx to ny)
                        }
                    }
                }
            }
        }

        private fun toggleFlag(x: Int, y: Int) {
            if (gameOver) return
            val cell = grid[y][x]
            if (cell.isRevealed) return
            if (!cell.isFlagged && flagsPlaced >= mineCount) return

            cell.isFlagged = !cell.isFlagged
            flagsPlaced += if (cell.isFlagged) 1 else -1
            updateInfo()
            invalidate()
        }

        private fun chordReveal(x: Int, y: Int) {
            if (gameOver) return
            val cell = grid[y][x]
            if (!cell.isRevealed || cell.adjacentMines == 0) return

            val neighbors = getNeighbors(x, y)
            val flaggedCount = neighbors.count { (nx, ny) -> grid[ny][nx].isFlagged }
            if (flaggedCount != cell.adjacentMines) return

            for ((nx, ny) in neighbors) {
                val neighbor = grid[ny][nx]
                if (neighbor.isFlagged || neighbor.isRevealed) continue
                if (neighbor.isMine) {
                    neighbor.isRevealed = true
                    endGame(false)
                    break
                }
                floodFillReveal(nx, ny)
            }
            invalidate()
            checkWinCondition()
        }

        private fun checkWinCondition() {
            if (gameOver) return
            for (y in 0 until rows) for (x in 0 until cols) {
                val c = grid[y][x]
                if (!c.isMine && !c.isRevealed) return
            }
            endGame(true)
        }

        private fun endGame(isWon: Boolean) {
            gameOver = true
            won = isWon
            stopTimer()

            if (!isWon) {
                for (y in 0 until rows) for (x in 0 until cols) {
                    if (grid[y][x].isMine) grid[y][x].isRevealed = true
                }
            }
            updateInfo()
            invalidate()
        }

        private fun startTimer() {
            stopTimer()
            startTime = System.currentTimeMillis()
            timerRunning = true
            handler.post(timerRunnable)
        }

        private fun stopTimer() {
            timerRunning = false
            handler.removeCallbacks(timerRunnable)
        }

        private fun updateInfo() {
            mineCounter.text = "Mine: ${mineCount - flagsPlaced}"
            statusText.text = if (gameOver) {
                if (won) "You Won" else "You Lose"
            } else {
                "Seed: $seed"
            }
        }
    }
}
