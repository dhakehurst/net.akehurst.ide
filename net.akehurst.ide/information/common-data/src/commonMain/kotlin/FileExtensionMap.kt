package net.akehurst.ide.common.data.fileextensionmap

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.expressions.processor.ObjectGraphByReflection
import net.akehurst.language.agl.expressions.processor.TypedObjectByReflection
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.agl.simple.ContextFromGrammarAndTypeModel
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserFromAsmTransformAbstract
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.TransformString
import net.akehurst.language.api.syntaxAnalyser.AsmFactory
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.processor.TypedObject
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.asm.TransformDomainDefault
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault

object FileExtensionMap {
    val grammar = """
        namespace ide
        grammar FileExtensionMap {
            skip leaf WS = "\s+" ;
            
            unit = entry* ;
            entry = EXT ':' value ;
            value = PATH | QUALIFIED_NAME ;
            leaf EXT = ID ;
            QUALIFIED_NAME =  [ID / '.']+ ;
            PATH = '/' [ID / '/']+ ;
            leaf ID = "[a-zA-Z._][a-zA-Z0-9._-]+" ;
        }
    """
    val asmTransform = """
        #create-missing-types
        #override-default-transform
        
        namespace ide
        transform FileExtensionMap {
           unit : children.asMap as Map<String,String>
           entry : tuple { key:=child[0] value:=child[2] }
           PATH: (child[0] + child[1].children.elements.join) as String
           QUALIFIED_NAME: children.elements.join as String
        }
    """
    val processor: LanguageProcessor<List<MapStringString>, ContextAsmSimple> by lazy {
        val res = Agl.processorFromString<List<MapStringString>, ContextAsmSimple>(
            grammarDefinitionStr = grammar,
            configuration = Agl.configuration<List<MapStringString>, ContextAsmSimple>(base = Agl.configurationBase()) {
                asmTransformResolver { p ->
                    TransformDomainDefault.fromString(ContextFromGrammarAndTypeModel(p.grammarModel!!, p.baseTypeModel), TransformString(asmTransform))
                }
                syntaxAnalyserResolver { p ->
                    ProcessResultDefault(
                        FileExtensionMapSyntaxAnalyser(
                            p.typeModel,
                            p.asmTransformModel,
                            p.targetAsmTransformRuleSet.qualifiedName
                        ),
                        IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)
                    )
                }
            }
        )
        check(res.issues.errors.isEmpty()) { res.issues.toString() }
        res.processor!!
    }

    fun process(sentence: String): MapStringString {
        val res = processor.process(sentence)
        check(res.issues.errors.isEmpty()) { res.issues.toString() }
        val asm = res.asm!!
        return asm[0]
    }
}

typealias MapStringString = MutableMap<String, String>

class FileExtensionMapFactory(
    typeModel: TypeModel,
    issues: IssueHolder,
) : AsmFactory<List<MapStringString>, Any>, ObjectGraphByReflection(typeModel, issues) {

    override fun constructAsm() = mutableListOf<MapStringString>()

    override fun rootList(asm: List<MapStringString>): List<Any> {
        return asm
    }

    override fun addRoot(asm: List<MapStringString>, root: Any) {
        (asm as MutableList<MapStringString>).add(root as MapStringString)
    }

    override fun removeRoot(asm: List<MapStringString>, root: Any) {
        Unit
    }

    override fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<Any>>): TypedObject<Any> {
        return when (possiblyQualifiedTypeName.value) {
            "MapEntry" -> {
                val pairType = StdLibDefault.Pair.type(listOf())
                TypedObjectByReflection(pairType, Pair(constructorArgs[""],constructorArgs[""]))
            }
            else -> super.createStructureValue(possiblyQualifiedTypeName, constructorArgs)
        }
    }
}

class FileExtensionMapSyntaxAnalyser(
    typeModel: TypeModel,
    asmTransformModel: TransformModel,
    relevantTrRuleSet: QualifiedName
) : SyntaxAnalyserFromAsmTransformAbstract<List<MapStringString>, Any>(
    typeModel,
    asmTransformModel,
    relevantTrRuleSet,
    FileExtensionMapFactory(typeModel, IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)),
) {

}