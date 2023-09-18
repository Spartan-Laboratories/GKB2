package com.spartanlabs.bottools.plugins.math

import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.commands.Option
import net.dv8tion.jda.internal.utils.tuple.MutablePair
import net.dv8tion.jda.internal.utils.tuple.Pair
import kotlin.math.pow

class MathCommand() : Command("math") {
    override val brief = "Calculates the answer to any basic mathematical problem"
    override val details = """This command is able to understand and process: operators, parenthesis, logarithms, basic trigonometric functions, and more
           |Examples:
           |    `/math 1+3*2`
           |    `/math (4(5-3))^2`
           |    `/math sin(ln(2.718))`
           |    `/math log(2,16)`"""
    private val chars = charArrayOf('^', '*', '/', '+', '-')
    private val operators = List<Char>(chars.size){ index->chars[index] }
    init {
        makeInteractive()
        this + Option(type = "string", name = "problem", description = "the problem that you want to be solved", true)
    }

    override fun invoke(args: Array<String>){
        try {
            `reply with`(solve(concatArgs(args).replace(" ", "").lowercase()).toString())
        } catch (e: Exception) {
            say("The problem was not formed correctly.\n" + e.message)
            e.printStackTrace()
        }
    }

    @Throws(MalformedProblemException::class)
    private fun solve(problem: String): Double {
        val parts = split(problem)
        // Single operation
        if (parts.size == 1) {
            val firstChar = problem.toCharArray()[0]
            if (Character.isAlphabetic(firstChar.code) || firstChar == '!')
                return parseFunction(problem)
            else if (firstChar == '(')
                return solve(problem.substring(1, problem.length - 1))
        }
        // Populate
        var simplifiedProblem: Pair<ArrayList<Double>, ArrayList<Char>> = populate(parts)

        // Solve
        simplifiedProblem = sweepThrough(
            sweepThrough(
                sweepThrough(simplifiedProblem, '^'), '*', '/'
            ), '+', '-'
        )
        return simplifiedProblem.left[0]
    }

    @Throws(MalformedProblemException::class)
    private fun sweepThrough(
        problem: Pair<ArrayList<Double>, ArrayList<Char>>, vararg operatorTypes: Char
    ): Pair<ArrayList<Double>, ArrayList<Char>> {
        println("Sweeping through operators: " + operatorTypes[0])
        val numbers = problem.left
        val operators = problem.right
        var i = 0
        while (i < operators.size) {
            for (operatorType: Char in operatorTypes) {
                println("Looking for operator: $operatorType")
                if ((operators[i] == operatorType)) {
                    println("Found matching operator: " + operators[i])
                    numbers[i] = doOperation(numbers[i], numbers[i + 1], operatorType)
                    numbers.removeAt(i + 1)
                    operators.removeAt(i--)
                    break
                }
            }
            i++
        }
        return MutablePair(numbers, operators)
    }

    @Throws(MalformedProblemException::class)
    private fun populate(parts: ArrayList<MutablePair<ProblemPieceType, String>>): MutablePair<ArrayList<Double>, ArrayList<Char>> {
        val simplifiedProblem = MutablePair<ArrayList<Double>, ArrayList<Char>>()
        simplifiedProblem.left = ArrayList()
        simplifiedProblem.right = ArrayList()
        var d: Double
        for (part: MutablePair<ProblemPieceType, String> in parts) {
            when (part.left) {
                ProblemPieceType.NUMBER -> {
                    d = part.right!!.toDouble()
                    simplifiedProblem.left.add(d)
                    println("Part $d added")
                }

                ProblemPieceType.OPERATION -> {
                    val c = part.right!!.toCharArray()[0]
                    simplifiedProblem.right.add(c)
                    println("Part $c added")
                }

                ProblemPieceType.COMPLEX -> {
                    d = solve(part.right)
                    simplifiedProblem.left.add(d)
                    println("Part $d added")
                }
            }
        }
        return simplifiedProblem
    }

    @Throws(MalformedProblemException::class)
    private fun parseFunction(problem: String?): Double {
        if (problem!!.startsWith("sin")) return Math.sin(Math.toRadians(getX(3, problem)))
        if (problem.startsWith("cos")) return Math.cos(Math.toRadians(getX(3, problem)))
        if (problem.startsWith("sqrt")) return Math.sqrt(getX(4, problem))
        if (problem.startsWith("ln")) return Math.log(getX(2, problem))
        if (problem.startsWith("log")) {
            val xy: Pair<Double, Double> = getXY(3, problem)
            return Math.log(xy.right) / Math.log(xy.left)
        }
        throw MalformedProblemException("Unrecognised function: " + problem.substring(0, problem.indexOf("(")))
    }

    @Throws(MalformedProblemException::class)
    private fun getX(functionNameSize: Int, problem: String?): Double {
        return solve(problem!!.substring(functionNameSize + 1, problem.length - 1))
    }

    @Throws(MalformedProblemException::class)
    private fun getXY(functionNameSize: Int, problem: String?): MutablePair<Double, Double> {
        return MutablePair(
            solve(problem!!.substring(functionNameSize + 1, problem.indexOf(","))), solve(
                problem.substring(
                    problem.indexOf(",") + 1, problem.length - 1
                )
            )
        )
    }

    @Throws(MalformedProblemException::class)
    private fun split(problem: String): ArrayList<MutablePair<ProblemPieceType, String>> {
        val parts = ArrayList<MutablePair<ProblemPieceType, String>>()
        val characters = problem.toCharArray()

        //setFirstPiece(parts, characters[0]);
        if (problem.length < 1) throw MalformedProblemException("no problem given")
        if (Character.isDigit(characters[0]) || characters[0] == '.')
            parts.add(MutablePair(ProblemPieceType.NUMBER, characters[0].toString()))
        else if (Character.isAlphabetic(characters[0].code) || characters[0] == '(') {
            val pieceLength = findCorrespondingParenthesis(problem.substring(0))
            parts.add(MutablePair(ProblemPieceType.COMPLEX, problem.substring(0, pieceLength + 1)))
        }
        else if (characters[0] == '!') {
            val pieceLength = firstNumLength(problem.substring(1)) + 1
            parts.add(MutablePair(ProblemPieceType.COMPLEX, problem.substring(0, pieceLength)))
        }
        else if (operators.contains(characters[0]))
            if (characters[0] == '-' && characters.size > 1)
                if (Character.isDigit(characters[1]) || characters[1] == '.')
                    parts.add(MutablePair(ProblemPieceType.NUMBER, characters[0].toString()))
                else throw MalformedProblemException("Cannot start with an operator")
        val partOneLength = parts[0].right!!.length
        if (problem.length > partOneLength) {
            var i = partOneLength
            while (i < problem.length) {
                val lastPart: Pair<ProblemPieceType?, String?> =
                    parts[parts.size - 1]
                if (Character.isDigit(characters[i]) || characters[i] == '.')
                    when (lastPart.left as ProblemPieceType) {
                        ProblemPieceType.NUMBER -> parts[parts.size - 1].right = lastPart.right + characters[i]
                        ProblemPieceType.COMPLEX -> {
                            parts.add(MutablePair(ProblemPieceType.OPERATION, "*"))
                            parts.add(MutablePair(ProblemPieceType.NUMBER, characters[i].toString()))
                        }
                        ProblemPieceType.OPERATION -> parts.add(
                            MutablePair(ProblemPieceType.NUMBER, characters[i].toString()))
                    }
                else if (operators.contains( characters[i] ))
                    when (lastPart.left as ProblemPieceType) {
                        ProblemPieceType.NUMBER, ProblemPieceType.COMPLEX ->
                            parts.add(MutablePair(ProblemPieceType.OPERATION, characters[i].toString()))
                        ProblemPieceType.OPERATION ->
                            if (characters[i] == '-')
                                parts.add(MutablePair(ProblemPieceType.NUMBER, characters[i].toString()))
                            else throw MalformedProblemException("Cannot have two operators in a row")
                }
                else if (Character.isAlphabetic(characters[i].code) || characters[i] == '(') {
                    val pieceLength = findCorrespondingParenthesis(problem.substring(i))
                    when (lastPart.left as ProblemPieceType) {
                        ProblemPieceType.NUMBER, ProblemPieceType.COMPLEX -> {
                            parts.add(MutablePair(ProblemPieceType.OPERATION, "*"))
                            parts.add(MutablePair(ProblemPieceType.COMPLEX, problem.substring(i, pieceLength + i + 1)))
                        }
                        ProblemPieceType.OPERATION ->
                            parts.add(MutablePair(ProblemPieceType.COMPLEX, problem.substring(i, pieceLength + i + 1)))
                    }
                    i += pieceLength
                } else if (characters[i] == '!') {
                    val pieceLength = firstNumLength(problem.substring(i + 1)) + 1
                    parts.add(MutablePair(ProblemPieceType.COMPLEX, problem.substring(i, i + pieceLength)))
                    i += pieceLength
                } else throw MalformedProblemException("Unknown character: " + characters[i])
                i++
            }
        }
        return parts
    }

    @Throws(MalformedProblemException::class)
    private fun firstNumLength(problem: String): Int {
        val chars = problem.toCharArray()
        if (!Character.isDigit(chars[0])) throw MalformedProblemException("Expected digit instead of " + chars[0])
        for (i in 1..problem.length) if (!Character.isDigit(chars[i - 1])) return i
        return problem.length
    }

    @Throws(MalformedProblemException::class)
    private fun setFirstPiece(parts: ArrayList<MutablePair<ProblemPieceType, String>>, firstChar: Char) {
        parts.add(MutablePair(ProblemPieceType.NUMBER, "0"))
        if (Character.isDigit(firstChar)) return
        else if (Character.isAlphabetic(firstChar.code) || (firstChar == '!') || (firstChar == '('))
            parts.add(MutablePair(ProblemPieceType.OPERATION, "+"))
        else throw MalformedProblemException("Cannot start with an operator")
    }

    private fun findCorrespondingParenthesis(problem: String): Int {
        var level = 0
        val characters = problem.toCharArray()
        for (i in 0 until problem.length) {
            if (characters[i] == '(') level++ else if (characters[i] == ')') level--
            if (!Character.isAlphabetic(characters[i].code) && level == 0) return i
        }
        throw IllegalArgumentException()
    }

    private enum class ProblemPieceType {
        NUMBER, OPERATION, COMPLEX
    }

    private inner class MalformedProblemException(message: String) :
        Exception("The given problem is not formed correctly:\n$message")

    private fun factorial(i: Int): Int {
        return if (i == 1) 1 else i * factorial(i - 1)
    }

    private fun doOperation(a: Double, b: Double, operator: Char): Double {
        println("$a $operator $b")
        when (operator) {
            '^' -> return a.pow(b)
            '*' -> return a * b
            '/' -> return a / b
            '+' -> return a + b
            '-' -> return a - b
            else -> throw MalformedProblemException("unrecognised operator: $operator")
        }
    }
}