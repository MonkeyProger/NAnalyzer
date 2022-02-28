package nullProject

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue

object Main {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val cr = ClassReader(args[0])
        println(cr.className.trimIndent())
        val classNode = ClassNode()
        cr.accept(classNode, 0)
        val methods = classNode.methods
        val mdsParMap = mutableMapOf<String,Map<Int, String>>()
        for (mn in methods) {
            analyzeMethod(classNode,mn,mdsParMap)
        }
    }

    /**
     * This function analyze each case of usage null/not-null/not-stated values,
     *
     */
    private fun analyzeMethod (cn: ClassNode, mn: MethodNode,
        mdsParMap: MutableMap<String, Map<Int, String>>) {
        //  Getting variables info block
        val analyzer: Analyzer<BasicValue> = Analyzer(BasicInterpreter())
        analyzer.analyze(cn.name, mn)
        val insnList = mn.instructions
        val varList = mn.localVariables
        val varAnn = mn.invisibleLocalVariableAnnotations
        val parAnn = mn.invisibleParameterAnnotations
        if (parAnn != null) {
            val advParMap = createAdvParMap(parAnn) //  Map of parameters for each method in class
            mdsParMap[mn.name] = advParMap
        }
        val advVarList = createAdvVarList(varList, parAnn, varAnn)  //  List of variables for each method

        /*println("Method '" + mn.name +"' contains variables:")
        advVarList.forEach {
            println(it)
        }*/

        var reportNullmd = 0
        if (mdsParMap[mn.name]!=null)
        mdsParMap[mn.name]!!.forEach { if (it.value == "NotNull")
            reportNullmd++  //  Avoiding redundant check of $$$reportNull$$$0(@NotNull variable)
        }
        var skipNxtLabel: Label? = null
        var resetLabel: Label? = null
        var modVarList = advVarList  //  List of modified variables, e.g in nested if's
        var lineNumber = 0

        //  Starting analyze
        for ( i in 0 until insnList.size()) {
            val insn = insnList.get(i)
             if (insn.toString().contains("LabelNode")) {
                 if (skipNxtLabel != null)
                    if ((insn as LabelNode).label != skipNxtLabel) continue else
                        skipNxtLabel = null
                 if ((insn as LabelNode).label == resetLabel) {
                     resetLabel = null
                     modVarList = advVarList
                 }
             }
             else if (insn.toString().contains("LineNumberNode")){
                lineNumber = (insn as LineNumberNode).line
            }
            else {
                //  Checking insn opcode
                when (insn.opcode) {
                    Opcodes.IFNONNULL -> {
                        //  Avoiding redundant check of $$$reportNull$$$0(@NotNull variable)
                        if (reportNullmd > 0) reportNullmd--
                        else {
                            val labels = ifNullCheck(advVarList, modVarList, insn, lineNumber)
                            skipNxtLabel = labels.first
                            resetLabel = labels.second
                            modVarList = modifyVarList(modVarList, insn, true)
                        }
                    }
                    Opcodes.IFNULL -> {
                        val labels = ifNonNullCheck(modVarList, insn, lineNumber)
                        skipNxtLabel = labels.first
                        resetLabel = labels.second
                        modVarList = modifyVarList(modVarList, insn, false)
                    }
                    Opcodes.INVOKESTATIC, Opcodes.INVOKEVIRTUAL -> {
                        val mdName = (insn as MethodInsnNode).name
                        if (mdsParMap.containsKey(mdName)){
                            val parMap = mdsParMap[mdName]
                            functionCallCheck(modVarList, parMap!!, insn, mdName, lineNumber)
                        }
                    }
                    Opcodes.GETFIELD -> getFieldCheck(modVarList,insn,lineNumber)
                    }
                }
            }
        }
    }

    /**
     * This function creates an advanced method list of variables,
     * which contains besides correct indexes of variables,
     * also its names and annotations(if given). Have fun :)
     *
     * advVarList{IndexOfVariableUsedInMethod} = (VarName, VarAnnotation)
     */
    private fun createAdvVarList(
        varList: MutableList<LocalVariableNode>, parAnn: Array<MutableList<AnnotationNode>>?,
        varAnn: MutableList<LocalVariableAnnotationNode>?): List<Pair<String, String>> {
        val advVarList = mutableListOf<Pair<String,String>>()
        for (i in 0 until varList.size)
            advVarList.add(Pair(varList[i].name,""))
        if (parAnn != null && varList.size > 0)
            for (i in parAnn.indices) {
                if (parAnn[i] != null)  {
                    val ann = parAnn[i][0].desc.split("/").last().dropLast(1)
                    val nPair = Pair(advVarList[i+1].first, ann)
                    advVarList[i+1] = nPair
                }
            }
        if (varAnn != null && varList.size > 0)
            for (i in varAnn.indices) {
                if (varAnn[i] != null)  {
                    val ann = varAnn[i].desc.split("/").last().dropLast(1)
                    val index = varAnn[i].index[0]
                    val nPair = Pair(advVarList[index].first, ann)
                    advVarList[index] = nPair
                }
            }
        return advVarList
    }

    /**
     * This function creates an advanced method parameters map,
     * name of annotated parameters and their annotation.
     * (It's used to create a full map of methods and their annotated parameters) Take joy :>
     *
     * parMap{nameOfAnnotatedPar} = "Provided Annotation"
     */
    private fun createAdvParMap(parAnn: Array<MutableList<AnnotationNode>>): Map<Int,String> {
        val parMap = mutableMapOf<Int,String>()
        if (parAnn.isNotEmpty())
        for (i in parAnn.indices)
            if (parAnn[i] != null) {
                parMap[i+1] = parAnn[i][0].desc.split("/").last().dropLast(1)
            }
        return parMap
    }

    /**
     * This method allows modifying variable annotations, e.g in nested if.
     * It's used in the temporary modified variables list,
     * so don't worry, it won't destroy everything, only my last neuron cell. :@
     */
    private fun modifyVarList(modVarList: List<Pair<String, String>>,
            insn: AbstractInsnNode, nullModifier: Boolean ):List<Pair<String, String>>{
            val newVarList = modVarList.toMutableList()
            val varInd = (insn.previous as VarInsnNode).`var`
            val vInfo = modVarList[varInd]
            if (vInfo.second != "NotNull")
                if (nullModifier) newVarList[varInd] = vInfo.first to "Null" else
                    if (!nullModifier) newVarList[varInd] = vInfo.first to "NotNull"
            return newVarList
        }

    /**
     * This method checks the case of (var == null). Have a good time :3
     */
    private fun ifNullCheck(advVarList: List<Pair<String, String>>, modVarList: List<Pair<String, String>>,
        insn: AbstractInsnNode, lineNumber: Int): Pair<Label?,Label?> {
        val labelInfo = (insn as JumpInsnNode).label.label
        val varInd = (insn.previous as VarInsnNode).`var`
        val vInfo = advVarList[varInd]
        if (vInfo.second == "NotNull") {
            println("Line:$lineNumber   Redundant null check: variable '" + vInfo.first + "' is NotNull\n")
            return labelInfo to labelInfo
        }
        return null to labelInfo
    }

    /**
     * This method checks the case of (var != null). Take delight ;)
     */
    private fun ifNonNullCheck(modVarList: List<Pair<String, String>>,
        insn: AbstractInsnNode, lineNumber: Int): Pair<Label?,Label?> {
        val varInd = (insn.previous as VarInsnNode).`var`
        val labelInfo = (insn as JumpInsnNode).label.label
        val vInfo = modVarList[varInd]
        if (vInfo.second != "NotNull") return labelInfo to labelInfo
        else println("Line:$lineNumber   Condition is always true here, as '"+vInfo.first+"' is always NotNull\n")
        return null to labelInfo

    }

    /**
     * This method checks if given to the function parameter has correct annotation. Have a nice day :]
     */
    private fun functionCallCheck(modVarList: List<Pair<String, String>>,
        parMap: Map<Int,String>, insn: AbstractInsnNode, mdName: String, lineNumber: Int) {
        if (parMap.isNotEmpty()) {
            var prevInsn = insn.previous
            for (i in 1..parMap.size) {
                if (prevInsn.opcode == Opcodes.ALOAD) {
                    val varInd = (prevInsn as VarInsnNode).`var`
                    val vInfo = modVarList[varInd]
                    val pInfo = parMap[i]
                    val parName = vInfo.first
                    prevInsn = prevInsn.previous
                    if (vInfo.second != pInfo)
                        println("Line:$lineNumber   Function '$mdName' requires not null parameter, but '$parName' may be null\n")
                }
            }
        }
    }

    /**
     * This method checks accessing the field.      Don't let things upset you :}
     */
    private fun getFieldCheck(modVarList: List<Pair<String, String>>,
            insn: AbstractInsnNode, lineNumber: Int){
            val varInd = (insn.previous as VarInsnNode).`var`
            val vInfo = modVarList[varInd]
            val vName = vInfo.first
            when (vInfo.second){
                "Nullable" -> println("Line:$lineNumber   '$vName' may be null here\n")
                "Null" -> println("Line:$lineNumber   '$vName' is always null here\n")
            }
        }