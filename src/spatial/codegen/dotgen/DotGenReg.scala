package spatial.codegen.dotgen

import argon.codegen.dotgen._
import spatial.api.RegExp
import spatial.SpatialConfig
import spatial.SpatialExp

trait DotGenReg extends DotCodegen {
  val IR: RegExp with SpatialExp
  import IR._

  override def attr(n:Exp[_]) = n match {
    case n if isArgIn(n) | isArgOut(n) => super.attr(n).shape(box).style(filled).color(indianred)
    case n if isReg(n) => super.attr(n).shape(box).style(filled).color(limegreen)
    case n => super.attr(n)
  }

  def emitMemRead(reader:Sym[_]) = {
    val LocalReader(reads) = reader
    reads.foreach { case (mem, ind, en) =>
      readersOf(mem).foreach { case read =>
        if (read.node==reader) {
          emitEdge(mem, read.ctrlNode, DotAttr().label(s"${quote(reader)}"))
        }
      }
    }
  }

  def emitMemWrite(writer:Sym[_]) = {
    val LocalWriter(writes) = writer
    writes.foreach { case (mem, value, _, _) =>
      writersOf(mem).foreach { case write =>
        if (write.node==writer) {
          emitEdge(write.ctrlNode, mem, DotAttr().label(s"${quote(writer)}"))
        }
      }
    }
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ArgInNew(init)  => emitVert(lhs)
    case ArgOutNew(init) => emitVert(lhs)
    case RegNew(init)    => emitVert(lhs)
    case RegRead(reg)    => emitMemRead(lhs)
    case RegWrite(reg,v,en) => emitMemWrite(lhs)
    case _ => super.emitNode(lhs, rhs)
  }

  override protected def emitFileFooter() {
    super.emitFileFooter()
  }
}