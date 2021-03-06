package spatial.codegen.chiselgen

import argon.codegen.chiselgen.ChiselCodegen
import spatial.api.HostTransferExp
import spatial.SpatialConfig

trait ChiselGenHostTransfer extends ChiselCodegen  {
  val IR: HostTransferExp
  import IR._


  // Does not belong in chisel
  // override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
  //   case _ => super.emitNode(lhs, rhs)
  // }



}
