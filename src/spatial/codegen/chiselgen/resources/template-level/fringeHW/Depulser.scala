package fringe

import chisel3._

/**
 * Depulser: 1-cycle pulse to a steady high signal
 */
class Depulser() extends Module {
  val io = IO(new Bundle {
    val in = Input(Bool())
    val rst = Input(Bool())
    val out = Output(Bool())
  })

  val r = Module(new FF(1))
  r.io.in := Mux(io.rst, UInt(0), io.in)
  r.io.init := UInt(0)
  r.io.enable := io.in | io.rst
  io.out := r.io.out
}