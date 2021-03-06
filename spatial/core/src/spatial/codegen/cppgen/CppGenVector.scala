package spatial.codegen.cppgen

import argon.codegen.cppgen.CppCodegen
import spatial.api.VectorExp
import spatial.SpatialConfig

trait CppGenVector extends CppCodegen {
  val IR: VectorExp
  import IR._

  override def quote(s: Exp[_]): String = {
    if (SpatialConfig.enableNaming) {
      s match {
        case lhs: Sym[_] =>
          lhs match {
            case Def(ListVector(_)) => s"x${lhs.id}_vecified"
            case Def(VectorApply(_,i:Int)) => s"x${lhs.id}_elem${i}"
            case Def(VectorSlice(_,s:Int,e:Int)) => s"x${lhs.id}_slice${s}to${e}"
            case _ => super.quote(s)
          }
        case _ => super.quote(s)
      }
    } else {
      super.quote(s)
    }
  } 

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: VectorType[_] => src"Array[${tp.child}]"
    case _ => super.remap(tp)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ListVector(elems)      => emit(src"val $lhs = Array(" + elems.map(quote).mkString(",") + ")")
    case VectorApply(vector, i) => emit(src"val $lhs = $vector.apply($i)")
    case VectorSlice(vector, start, end) => emit(src"val $lhs = $vector.slice($start, $end)")
    case _ => super.emitNode(lhs, rhs)
  }
}