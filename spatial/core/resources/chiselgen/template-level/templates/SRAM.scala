package templates

import chisel3._
import chisel3.util
import scala.collection.mutable.HashMap

sealed trait BankingMode
object DiagonalMemory extends BankingMode
object BankedMemory extends BankingMode

class flatW(val w: Int) extends Bundle {
  val addr = UInt(32.W)
  val data = UInt(w.W)
  val en = Bool()

  override def cloneType = (new flatW(w)).asInstanceOf[this.type] // See chisel3 bug 358
}
class flatR(val w: Int) extends Bundle {
  val addr = UInt(32.W)
  val en = Bool()

  override def cloneType = (new flatR(w)).asInstanceOf[this.type] // See chisel3 bug 358
}
class multidimW(val N: Int, val w: Int) extends Bundle {
  val addr = Vec(N, UInt(w.W))
  val data = UInt(w.W)
  val en = Bool()

  override def cloneType = (new multidimW(N, w)).asInstanceOf[this.type] // See chisel3 bug 358
}
class multidimR(val N: Int, val w: Int) extends Bundle {
  val addr = Vec(N, UInt(w.W))
  val en = Bool()
  
  override def cloneType = (new multidimR(N, w)).asInstanceOf[this.type] // See chisel3 bug 358
}

class Mem1D(val size: Int, val isFifo: Boolean, bitWidth: Int = 32) extends Module { // Unbanked, inner 1D mem
  def this(size: Int) = this(size, true)

  val io = IO( new Bundle {
    val w = Input(new flatW(bitWidth))
    val r = Input(new flatR(bitWidth))
    val output = new Bundle {
      val data  = Output(UInt(bitWidth.W))
    }
    val debug = new Bundle {
      val invalidRAddr = Output(Bool())
      val invalidWAddr = Output(Bool())
      val rwOn = Output(Bool())
      val error = Output(Bool())
      // val addrProbe = Output(UInt(bitWidth.W))
    }
  })

  // We can do better than MaxJ by forcing mems to be single-ported since
  //   we know how to properly schedule reads and writes
  val m = Mem(size, UInt(bitWidth.W) /*, seqRead = true deprecated? */)
  val wInBound = io.w.addr < (size).U
  val rInBound = io.r.addr < (size).U

  if (isFifo) { // Fifos need to be dual port to avoid strangeness
    when (io.w.en & wInBound) {m(io.w.addr) := io.w.data}
    io.output.data := m(io.r.addr)
  } else {
    val reg_rAddr = Reg(UInt())
    when (io.w.en & wInBound) {m(io.w.addr) := io.w.data}
    .elsewhen (io.r.en & rInBound) {reg_rAddr := io.r.addr}
    io.output.data := m(reg_rAddr)
  }

  io.debug.invalidRAddr := ~rInBound
  io.debug.invalidWAddr := ~wInBound
  io.debug.rwOn := io.w.en & io.r.en
  io.debug.error := ~rInBound | ~wInBound | (io.w.en & io.r.en)
  // io.debug.addrProbe := m(0.U)

}


// Last dimension is the leading-dim
class MemND(val dims: List[Int], bitWidth: Int = 32) extends Module { 
  val depth = dims.reduce{_*_} // Size of memory
  val N = dims.length // Number of dimensions

  val io = IO( new Bundle {
    val w = Input(new multidimW(N, bitWidth))
    val wMask = Input(Bool())
    val r = Input(new multidimR(N, bitWidth))
    val rMask = Input(Bool())
    val output = new Bundle {
      val data  = Output(UInt(bitWidth.W))
    }
    val debug = new Bundle {
      val invalidRAddr = Output(Bool())
      val invalidWAddr = Output(Bool())
      val rwOn = Output(Bool())
      val error = Output(Bool())
    }
  })

  // Instantiate 1D mem
  val m = Module(new Mem1D(depth))

  // Address flattening
  m.io.w.addr := io.w.addr.zipWithIndex.map{ case (addr, i) =>
    addr * (dims.drop(i).reduce{_*_}/dims(i)).U
  }.reduce{_+_}
  m.io.r.addr := io.r.addr.zipWithIndex.map{ case (addr, i) =>
    addr * (dims.drop(i).reduce{_*_}/dims(i)).U
  }.reduce{_+_}

  // Check if read/write is in bounds
  val rInBound = io.r.addr.zip(dims).map { case (addr, bound) => addr < bound.U }.reduce{_&_}
  val wInBound = io.w.addr.zip(dims).map { case (addr, bound) => addr < bound.U }.reduce{_&_}

  // Connect the other ports
  m.io.w.data := io.w.data
  m.io.w.en := io.w.en & io.wMask
  m.io.r.en := io.r.en & io.rMask
  io.output.data := m.io.output.data
  io.debug.invalidWAddr := ~wInBound
  io.debug.invalidRAddr := ~rInBound
  io.debug.rwOn := io.w.en & io.wMask & io.r.en & io.rMask
  io.debug.error := ~wInBound | ~rInBound | (io.w.en & io.r.en)
}


/*
                            
                                                           __________             ___SRAM__
         _        _           _______                     |          |--bundleND-|   MemND |               
        | |------| |---------|       |                    |          |           |_________|                        
   IO(Vec(bundleSRAM))-------| Mux1H |-----bundleSRAM-----|   VAT    |--bundleND-|   MemND |    
        |_|------|_|---------|_______|                    |          |           |_________|                        
                               | | |                      |__________|--bundleND-|   MemND |               
                             stageEnables                                        |_________|
                                                                        
                                                                    
*/
class SRAM(val logicalDims: List[Int], val bitWidth: Int, 
           val banks: List[Int], val strides: List[Int], 
           val wPar: List[Int], val rPar: List[Int], val bankingMode: BankingMode) extends Module { 

  // Overloaded construters
  // Tuple unpacker
  def this(tuple: (List[Int], Int, List[Int], List[Int], 
           List[Int], List[Int], BankingMode)) = this(tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7)
  // Bankmode-less
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: List[Int], strides: List[Int], 
           wPar: List[Int], rPar: List[Int]) = this(logicalDims, bitWidth, banks, strides, wPar, rPar, BankedMemory)
  // If 1D, spatial will make banks and strides scalars instead of lists
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: Int, strides: Int, 
           wPar: List[Int], rPar: List[Int]) = this(logicalDims, bitWidth, List(banks), List(strides), wPar, rPar, BankedMemory)

  val depth = logicalDims.reduce{_*_} // Size of memory
  val N = logicalDims.length // Number of dimensions

  val io = IO( new Bundle {
    // TODO: w bundle gets forcefully generated as output in verilog
    //       so the only way to make it an input seems to flatten the
    //       Vec(numWriters, Vec(wPar, _)) to a 1D vector and then reconstruct it
    val w = Vec(wPar.reduce{_+_}, Input(new multidimW(N, bitWidth)))
    val r = Vec(rPar.reduce{_+_},Input(new multidimR(N, bitWidth))) // TODO: Spatial allows only one reader per mem
    val output = new Bundle {
      val data  = Vec(rPar.reduce{_+_}, Output(UInt(bitWidth.W)))
    }
    val debug = new Bundle {
      val invalidRAddr = Output(Bool())
      val invalidWAddr = Output(Bool())
      val rwOn = Output(Bool())
      val readCollision = Output(Bool())
      val writeCollision = Output(Bool())
      val error = Output(Bool())
    }
  })

  // Get info on physical dims
  // TODO: Upcast dims to evenly bank
  val physicalDims = bankingMode match {
    case DiagonalMemory => logicalDims.zipWithIndex.map { case (dim, i) => if (i == N - 1) math.ceil(dim.toDouble/banks.head).toInt else dim}
    case BankedMemory => logicalDims.zip(banks).map { case (dim, b) => math.ceil(dim.toDouble/b).toInt}
  }
  val numMems = bankingMode match {
    case DiagonalMemory => banks.head
    case BankedMemory => banks.reduce{_*_}
  }

  // Create physical mems
  val m = (0 until numMems).map{ i => Module(new MemND(physicalDims))}

  // Reconstruct io.w as 2d vector


  // TODO: Should connect multidimW's directly to their banks rather than all-to-all connections
  // Convert selectedWVec to translated physical addresses
  val wConversions = io.w.map{ wbundle => 
    // Writer conversion
    val convertedW = Wire(new multidimW(N,bitWidth))
    val physicalAddrs = bankingMode match {
      case DiagonalMemory => wbundle.addr.zipWithIndex.map {case (logical, i) => if (i == N - 1) logical / banks.head.U else logical}
      case BankedMemory => wbundle.addr.zip(banks).map{ case (logical, b) => logical / b.U }
    }
    physicalAddrs.zipWithIndex.foreach { case (calculatedAddr, i) => convertedW.addr(i) := calculatedAddr}
    convertedW.data := wbundle.data
    convertedW.en := wbundle.en
    val flatBankId = bankingMode match {
      case DiagonalMemory => wbundle.addr.reduce{_+_} % banks.head.U
      case BankedMemory => 
        val bankCoords = wbundle.addr.zip(banks).map{ case (logical, b) => logical % b.U }
        bankCoords.zipWithIndex.map{ case (c, i) => c*(banks.drop(i).reduce{_*_}/banks(i)).U }.reduce{_+_}
    }

    (convertedW, flatBankId)
  }
  val convertedWVec = wConversions.map{_._1}
  val bankIdW = wConversions.map{_._2}

  val rConversions = io.r.map{ rbundle => 
    // Reader conversion
    val convertedR = Wire(new multidimR(N,bitWidth))
    val physicalAddrs = bankingMode match {
      case DiagonalMemory => rbundle.addr.zipWithIndex.map {case (logical, i) => if (i == N - 1) logical / banks.head.U else logical}
      case BankedMemory => rbundle.addr.zip(banks).map{ case (logical, b) => logical / b.U }
    }
    physicalAddrs.zipWithIndex.foreach { case (calculatedAddr, i) => convertedR.addr(i) := calculatedAddr}
    convertedR.en := rbundle.en
    val flatBankId = bankingMode match {
      case DiagonalMemory => rbundle.addr.reduce{_+_} % banks.head.U
      case BankedMemory => 
        val bankCoords = rbundle.addr.zip(banks).map{ case (logical, b) => logical % b.U }
        bankCoords.zipWithIndex.map{ case (c, i) => c*(banks.drop(i).reduce{_*_}/banks(i)).U }.reduce{_+_}
    }
    (convertedR, flatBankId)
  }
  val convertedRVec = rConversions.map{_._1}
  val bankIdR = rConversions.map{_._2}

  // TODO: Doing inefficient thing here of all-to-all connection between bundlesNDs and MemNDs
  // Convert bankCoords for each bundle to a bit vector
  // TODO: Probably need to have a dummy multidimW port to default to for unused banks so we don't overwrite anything
  m.zipWithIndex.foreach{ case (mem, i) => 
    val bundleSelect = bankIdW.zip(convertedWVec).map{ case(bid, wvec) => bid === i.U & wvec.en }
    mem.io.wMask := bundleSelect.reduce{_|_}
    mem.io.w := chisel3.util.PriorityMux(bundleSelect, convertedWVec)
  }

  // TODO: Doing inefficient thing here of all-to-all connection between bundlesNDs and MemNDs
  // Convert bankCoords for each bundle to a bit vector
  m.zipWithIndex.foreach{ case (mem, i) => 
    val bundleSelect = bankIdR.zip(convertedRVec).map{ case(bid, rvec) => (bid === i.U) & rvec.en }
    mem.io.rMask := bundleSelect.reduce{_|_}
    mem.io.r := chisel3.util.PriorityMux(bundleSelect, convertedRVec)
  }

  // Connect read data to output
  io.output.data.zip(bankIdR).foreach { case (wire, id) => 
    val sel = (0 until numMems).map{ i => (id === i.U)}
    val datas = m.map{ _.io.output.data }
    val d = chisel3.util.PriorityMux(sel, datas)
    wire := d
  }

  var wInUse = Array.fill(wPar.length) {false} // Array for tracking which wPar sections are in use
  def connectWPort(wBundle: Vec[multidimW], ports: List[Int]) {
    // Figure out which wPar section this wBundle fits in by finding first false index with same wPar
    val potentialFits = wPar.zipWithIndex.filter(_._1 == wBundle.length).map(_._2)
    val wId = potentialFits(potentialFits.map(wInUse(_)).indexWhere(_ == false))
    val port = ports(0) // Should never have more than 1 for SRAM
    // Get start index of this section
    val base = if (wId > 0) {wPar.take(wId).reduce{_+_}} else 0
    // Connect to wPar(wId) elements from base
    (0 until wBundle.length).foreach{ i => 
      io.w(base + i) := wBundle(i) 
    }
    // Set this section in use
    wInUse(wId) = true
  }

  var rId = 0
  def connectRPort(rBundle: Vec[multidimR], port: Int): Int = {
    // Get start index of this section
    val base = rId
    // Connect to rPar(rId) elements from base
    (0 until rBundle.length).foreach{ i => 
      io.r(base + i) := rBundle(i) 
    }
    rId = rId + rBundle.length
    base
  }


  // Connect debug signals
  val wInBound = io.w.map{ v => v.addr.zip(logicalDims).map { case (addr, bound) => addr < bound.U }.reduce{_&_}}.reduce{_&_}
  val rInBound = io.r.map{ v => v.addr.zip(logicalDims).map { case (addr, bound) => addr < bound.U }.reduce{_&_}}.reduce{_&_}
  val writeOn = io.w.map{ v => v.en }
  val readOn = io.r.map{ v => v.en }
  val rwOn = writeOn.zip(readOn).map{ case(a,b) => a&b}.reduce{_|_}
  val rCollide = bankIdR.zip( readOn).map{ case(id1,en1) => bankIdR.zip( readOn).map{ case(id2,en2) => Mux((id1 === id2) & en1 & en2, 1.U, 0.U)}.reduce{_+_} }.reduce{_+_} !=  readOn.map{Mux(_, 1.U, 0.U)}.reduce{_+_}
  val wCollide = bankIdW.zip(writeOn).map{ case(id1,en1) => bankIdW.zip(writeOn).map{ case(id2,en2) => Mux((id1 === id2) & en1 & en2, 1.U, 0.U)}.reduce{_+_} }.reduce{_+_} != writeOn.map{Mux(_, 1.U, 0.U)}.reduce{_+_}
  io.debug.invalidWAddr := ~wInBound
  io.debug.invalidRAddr := ~rInBound
  io.debug.rwOn := rwOn
  io.debug.readCollision := rCollide
  io.debug.writeCollision := wCollide
  io.debug.error := ~wInBound | ~rInBound | rwOn | rCollide | wCollide

}


class NBufSRAM(val logicalDims: List[Int], val numBufs: Int, val bitWidth: Int, /*TODO: width, get rid of this!*/
           val banks: List[Int], val strides: List[Int], 
           val wPar: List[Int], val rPar: List[Int], val rBundling: List[Int], val bPar: Int, val bankingMode: BankingMode) extends Module { 

  // Overloaded construters
  // Tuple unpacker
  def this(tuple: (List[Int], Int, Int, List[Int], List[Int], 
           List[Int], List[Int], List[Int], Int, BankingMode)) = this(tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7,tuple._8,tuple._9,tuple._10)
  // Bankmode-less
  def this(logicalDims: List[Int], numBufs: Int, bitWidth: Int, 
           banks: List[Int], strides: List[Int], 
           wPar: List[Int], rPar: List[Int], rBundling: List[Int], bPar: Int) = this(logicalDims, numBufs, bitWidth, banks, strides, wPar, rPar, rBundling, bPar, BankedMemory)
  // If 1D, spatial will make banks and strides scalars instead of lists
  def this(logicalDims: List[Int], numBufs: Int, bitWidth: Int, 
           banks: Int, strides: Int, 
           wPar: List[Int], rPar: List[Int], rBundling: List[Int], bPar: Int) = this(logicalDims, numBufs, bitWidth, List(banks), List(strides), wPar, rPar, rBundling, bPar, BankedMemory)

  val depth = logicalDims.reduce{_*_} // Size of memory
  val N = logicalDims.length // Number of dimensions

  val rHashmap = rPar.zip(rBundling).groupBy{_._2}
  val maxR = rHashmap.map{_._2.map{_._1}.reduce{_+_}}.max
  val io = IO( new Bundle {
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val w = Vec(wPar.reduce{_+_}, Input(new multidimW(N, bitWidth)))
    val broadcast = Vec(bPar, Input(new multidimW(N, bitWidth)))
    val r = Vec(rPar.reduce{_+_},Input(new multidimR(N, bitWidth))) // TODO: Spatial allows only one reader per mem
    val output = new Bundle {
      val data  = Vec(numBufs * maxR, Output(UInt(bitWidth.W)))  
    }
    val debug = new Bundle {
      val invalidRAddr = Output(Bool())
      val invalidWAddr = Output(Bool())
      val rwOn = Output(Bool())
      val readCollision = Output(Bool())
      val writeCollision = Output(Bool())
      val error = Output(Bool())
    }
  })

  // // Chisel3 broke this on 3/24/2017...
  // val reconstructedOut = (0 until numBufs).map{ h =>
  //   Vec((0 until rPar).map {
  //     j => io.output.data(h*rPar + j)
  //   })
  // }

  // Get info on physical dims
  // TODO: Upcast dims to evenly bank
  val physicalDims = logicalDims.zip(banks).map { case (dim, b) => dim/b}
  val numMems = banks.reduce{_*_}

  // Create physical mems
  val srams = (0 until numBufs).map{ i => Module(
    new SRAM(logicalDims,
            bitWidth, banks, strides, 
            List(wPar, List(bPar)).flatten, List(maxR), bankingMode)
  )}

  val sEn_latch = (0 until numBufs).map{i => Module(new SRFF())}
  val sDone_latch = (0 until numBufs).map{i => Module(new SRFF())}

  val swap = Wire(Bool())

  // Latch whether each buffer's stage is enabled and when they are done
  (0 until numBufs).foreach{ i => 
    sEn_latch(i).io.input.set := io.sEn(i)
    sEn_latch(i).io.input.reset := swap
    sEn_latch(i).io.input.asyn_reset := reset
    sDone_latch(i).io.input.set := io.sDone(i)
    sDone_latch(i).io.input.reset := swap
    sDone_latch(i).io.input.asyn_reset := reset
  }
  val anyEnabled = sEn_latch.map{ en => en.io.output.data }.reduce{_|_}
  swap := sEn_latch.zip(sDone_latch).map{ case (en, done) => en.io.output.data === done.io.output.data }.reduce{_&_} & anyEnabled

  val stateIn = Module(new NBufCtr())
  stateIn.io.input.start := 0.U
  stateIn.io.input.max := numBufs.U
  stateIn.io.input.enable := swap
  stateIn.io.input.countUp := false.B
  val statesInR = (0 until numBufs).map{  i => 
    val c = Module(new NBufCtr())
    c.io.input.start := i.U 
    c.io.input.max := numBufs.U
    c.io.input.enable := swap
    c.io.input.countUp := true.B
    c
  }

  val statesOut = (0 until numBufs).map{  i => 
    val c = Module(new NBufCtr())
    c.io.input.start := i.U 
    c.io.input.max := numBufs.U
    c.io.input.enable := swap
    c.io.input.countUp := false.B
    c
  }

  srams.zipWithIndex.foreach{ case (f,i) => 
    val wMask = stateIn.io.output.count === i.U
    (0 until wPar.reduce{_+_}).foreach { k =>
      val masked_w = Wire(new multidimW(N, bitWidth))
      masked_w.en := io.w(k).en & wMask
      masked_w.data := io.w(k).data
      masked_w.addr := io.w(k).addr
      f.io.w(k) := masked_w
    }
    (0 until bPar).foreach {k =>
      f.io.w(wPar.reduce{_+_} + k) := io.broadcast(k)
    }

    var idx = 0 
    var idx_meaningful = 0 
    val rSel = (0 until numBufs).map{ statesInR(i).io.output.count === _.U}
    (0 until maxR).foreach {lane => // Technically only need per read and not per buf but oh well
      // Assemble buffet of read ports
      val buffet = (0 until numBufs).map {p => 
        val size = rHashmap.getOrElse(p, List((0,0))).map{_._1}.reduce{_+_}
        val base = if (p > 0) {(0 until p).map{ q =>
          rHashmap.getOrElse(q,List((0,0))).map{_._1}.reduce{_+_}
          }.reduce{_+_}
          } else {0}
        val dummy_r = Wire(new multidimR(N,bitWidth))
        dummy_r.en := false.B
        if (lane < size) {io.r(base + lane)} else dummy_r
      }
      f.io.r(lane) := chisel3.util.Mux1H(rSel, buffet)
    }
  }

  (0 until numBufs).foreach {i =>
    val sel = (0 until numBufs).map{ statesOut(i).io.output.count === _.U }
    (0 until maxR).foreach{ j => 
      io.output.data(i*maxR + j) := chisel3.util.Mux1H(sel, srams.map{f => f.io.output.data(j)})
    }
  }

  var wInUse = Array.fill(wPar.length) {false} // Array for tracking which wPar sections are in use
  def connectWPort(wBundle: Vec[multidimW], ports: List[Int]) {
    if (ports.length == 1) {
      // Figure out which wPar section this wBundle fits in by finding first false index with same wPar
      val potentialFits = wPar.zipWithIndex.filter(_._1 == wBundle.length).map(_._2)
      val wId = potentialFits(potentialFits.map(wInUse(_)).indexWhere(_ == false))
      val port = ports(0) // Should never have more than 1 for SRAM
      // Get start index of this section
      val base = if (wId > 0) {wPar.take(wId).reduce{_+_}} else 0
      (0 until wBundle.length).foreach{ i => 
        io.w(base + i) := wBundle(i) 
      }
      // Set this section in use
      wInUse(wId) = true
    } else { // broadcast
      (0 until wPar.max).foreach{ i => 
        if (i < wBundle.length) {
          io.broadcast(i) := wBundle(i) 
        } else { // Unused broadcast ports
          io.broadcast(i).en := false.B
        }
      }
    }
  }

  var rInUse = rHashmap.map{(_._1 -> 0)} // Tracking connect write lanes per port
  def connectRPort(rBundle: Vec[multidimR], port: Int): Int = {
    // Figure out which rPar section this wBundle fits in by finding first false index with same rPar
    val rId = rInUse(port)
    // Get start index of this section
    val base = port * maxR + rId
    val packbase = if (port > 0) {
      (0 until port).map{p => 
        rHashmap.getOrElse(p, List((0,0))).map{_._1}.reduce{_+_}
      }.reduce{_+_}
    } else {0}
    // Connect to rPar(rId) elements from base
    (0 until rBundle.length).foreach{ i => 
      io.r(packbase + rId + i) := rBundle(i) 
    }
    rInUse += (port -> {rId + rBundle.length})
    base
  }


  def connectStageCtrl(done: Bool, en: Bool, ports: List[Int]) {
    ports.foreach{ port => 
      io.sEn(port) := en
      io.sDone(port) := done
    }
  }

  def connectUnwrittenPorts(ports: List[Int]) { // TODO: Remnant from maxj?
    // ports.foreach{ port => 
    //   io.input(port).enable := false.B
    // }
  }
 
  // def readTieDown(port: Int) { 
  //   (0 until numReaders).foreach {i => 
  //     io.rSel(port * numReaders + i) := false.B
  //   }
  // }

  def connectUntouchedPorts(ports: List[Int]) {
    ports.foreach{ port => 
      io.sEn(port) := false.B
      io.sDone(port) := false.B
    }
  }

  def connectDummyBroadcast() {
    (0 until rPar.max).foreach { i =>
      io.broadcast(i).en := false.B
    }
  }



}
