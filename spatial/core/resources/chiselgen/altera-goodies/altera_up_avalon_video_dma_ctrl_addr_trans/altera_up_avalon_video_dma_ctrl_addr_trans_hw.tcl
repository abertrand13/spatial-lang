# (C) 2001-2016 Intel Corporation. All rights reserved.
# Your use of Intel Corporation's design tools, logic functions and other 
# software and tools, and its AMPP partner logic functions, and any output 
# files any of the foregoing (including device programming or simulation 
# files), and any associated documentation or information are expressly subject 
# to the terms and conditions of the Intel Program License Subscription 
# Agreement, Intel MegaCore Function License Agreement, or other applicable 
# license agreement, including, without limitation, that your use is for the 
# sole purpose of programming logic devices manufactured by Intel and sold by 
# Intel or its authorized distributors.  Please refer to the applicable 
# agreement for further details.


# +----------------------------------------------------------------------------+
# | License Agreement                                                          |
# |                                                                            |
# | Copyright (c) 1991-2014 Altera Corporation, San Jose, California, USA.     |
# | All rights reserved.                                                       |
# |                                                                            |
# | Any megafunction design, and related net list (encrypted or decrypted),    |
# |  support information, device programming or simulation file, and any other |
# |  associated documentation or information provided by Altera or a partner   |
# |  under Altera's Megafunction Partnership Program may be used only to       |
# |  program PLD devices (but not masked PLD devices) from Altera.  Any other  |
# |  use of such megafunction design, net list, support information, device    |
# |  programming or simulation file, or any other related documentation or     |
# |  information is prohibited for any other purpose, including, but not       |
# |  limited to modification, reverse engineering, de-compiling, or use with   |
# |  any other silicon devices, unless such use is explicitly licensed under   |
# |  a separate agreement with Altera or a megafunction partner.  Title to     |
# |  the intellectual property, including patents, copyrights, trademarks,     |
# |  trade secrets, or maskworks, embodied in any such megafunction design,    |
# |  net list, support information, device programming or simulation file, or  |
# |  any other related documentation or information provided by Altera or a    |
# |  megafunction partner, remains with Altera, the megafunction partner, or   |
# |  their respective licensors.  No other licenses, including any licenses    |
# |  needed under any third party's intellectual property, are provided herein.|
# |  Copying or modifying any file, or portion thereof, to which this notice   |
# |  is attached violates this copyright.                                      |
# |                                                                            |
# | THIS FILE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    |
# | IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,   |
# | FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL    |
# | THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER |
# | LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING    |
# | FROM, OUT OF OR IN CONNECTION WITH THIS FILE OR THE USE OR OTHER DEALINGS  |
# | IN THIS FILE.                                                              |
# |                                                                            |
# | This agreement shall be governed in all respects by the laws of the State  |
# |  of California and by the laws of the United States of America.            |
# |                                                                            |
# +----------------------------------------------------------------------------+

# TCL File Generated by Altera University Program
# DO NOT MODIFY

set aup_version 16.1

package require -exact qsys 14.1

# +-----------------------------------
# | module altera_up_avalon_video_pixel_buffer_dma
# | 
set_module_property DESCRIPTION "Translates the front and back buffer addresses"
set_module_property NAME altera_up_avalon_video_dma_ctrl_addr_trans
set_module_property VERSION $aup_version
set_module_property GROUP "University Program/Audio & Video/Video"
set_module_property AUTHOR "Altera University Program"
set_module_property DISPLAY_NAME "DMA's Front and Back Buffer Address Translator"
#set_module_property TOP_LEVEL_HDL_FILE altera_up_avalon_video_dma_ctrl_address_translation.v
#set_module_property TOP_LEVEL_HDL_MODULE altera_up_avalon_video_dma_ctrl_address_translation
set_module_property INSTANTIATE_IN_SYSTEM_MODULE true
set_module_property HIDE_FROM_QUARTUS true
set_module_property HIDE_FROM_SOPC true
set_module_property EDITABLE false
#set_module_property ANALYZE_HDL false
# | 
# +-----------------------------------

# +-----------------------------------
# | file sets
# | 
add_fileset QUARTUS_SYNTH QUARTUS_SYNTH "" ""
set_fileset_property QUARTUS_SYNTH TOP_LEVEL altera_up_avalon_video_dma_ctrl_addr_trans
set_fileset_property QUARTUS_SYNTH ENABLE_RELATIVE_INCLUDE_PATHS false
set_fileset_property QUARTUS_SYNTH ENABLE_FILE_OVERWRITE_MODE false
add_fileset_file altera_up_avalon_video_dma_ctrl_addr_trans.v VERILOG PATH altera_up_avalon_video_dma_ctrl_addr_trans.v TOP_LEVEL_FILE

add_fileset SIM_VERILOG SIM_VERILOG "" ""
set_fileset_property SIM_VERILOG TOP_LEVEL altera_up_avalon_video_dma_ctrl_addr_trans
set_fileset_property SIM_VERILOG ENABLE_RELATIVE_INCLUDE_PATHS false
set_fileset_property SIM_VERILOG ENABLE_FILE_OVERWRITE_MODE false
add_fileset_file altera_up_avalon_video_dma_ctrl_addr_trans.v VERILOG PATH altera_up_avalon_video_dma_ctrl_addr_trans.v

add_fileset SIM_VHDL SIM_VHDL "" ""
set_fileset_property SIM_VHDL TOP_LEVEL altera_up_avalon_video_dma_ctrl_addr_trans
set_fileset_property SIM_VHDL ENABLE_RELATIVE_INCLUDE_PATHS false
set_fileset_property SIM_VHDL ENABLE_FILE_OVERWRITE_MODE false
add_fileset_file altera_up_avalon_video_dma_ctrl_addr_trans.v VERILOG PATH altera_up_avalon_video_dma_ctrl_addr_trans.v
# | 
# +-----------------------------------

# +-----------------------------------
# | parameters
# | 
add_parameter ADDRESS_TRANSLATION_MASK STD_LOGIC_VECTOR 3221225472 "" 
set_parameter_property ADDRESS_TRANSLATION_MASK DEFAULT_VALUE 3221225472
set_parameter_property ADDRESS_TRANSLATION_MASK DISPLAY_NAME "Address Translation Mask"
set_parameter_property ADDRESS_TRANSLATION_MASK HDL_PARAMETER true
set_parameter_property ADDRESS_TRANSLATION_MASK UNITS None
set_parameter_property ADDRESS_TRANSLATION_MASK VISIBLE true
set_parameter_property ADDRESS_TRANSLATION_MASK ENABLED true
set_parameter_property ADDRESS_TRANSLATION_MASK WIDTH 32
# | 
# +-----------------------------------

# +-----------------------------------
# | connection point clock
# | 
add_interface clock clock end
set_interface_property clock clockRate 0
set_interface_property clock ENABLED true
set_interface_property clock EXPORT_OF ""
set_interface_property clock PORT_NAME_MAP ""
set_interface_property clock CMSIS_SVD_VARIABLES ""
set_interface_property clock SVD_ADDRESS_GROUP ""

add_interface_port clock clk clk Input 1
# | 
# +-----------------------------------

# +-----------------------------------
# | connection point reset
# | 
add_interface reset reset end
set_interface_property reset associatedClock clock
set_interface_property reset synchronousEdges DEASSERT
set_interface_property reset ENABLED true
set_interface_property reset EXPORT_OF ""
set_interface_property reset PORT_NAME_MAP ""
set_interface_property reset CMSIS_SVD_VARIABLES ""
set_interface_property reset SVD_ADDRESS_GROUP ""

add_interface_port reset reset reset Input 1
# | 
# +-----------------------------------

# +-----------------------------------
# | connection point slave
# | 
add_interface slave avalon end 
set_interface_property slave addressUnits WORDS
set_interface_property slave associatedClock clock
set_interface_property slave associatedReset reset
set_interface_property slave bitsPerSymbol 8
set_interface_property slave burstOnBurstBoundariesOnly false
set_interface_property slave burstcountUnits WORDS
set_interface_property slave explicitAddressSpan 0
set_interface_property slave holdTime 0
set_interface_property slave linewrapBursts false
set_interface_property slave maximumPendingReadTransactions 0
set_interface_property slave maximumPendingWriteTransactions 0
set_interface_property slave readLatency 0
set_interface_property slave readWaitTime 0
set_interface_property slave setupTime 0
set_interface_property slave timingUnits Cycles
set_interface_property slave writeWaitTime 0
set_interface_property slave ENABLED true
set_interface_property slave EXPORT_OF ""
set_interface_property slave PORT_NAME_MAP ""
set_interface_property slave CMSIS_SVD_VARIABLES ""
set_interface_property slave SVD_ADDRESS_GROUP ""

add_interface_port slave slave_address address Input 2
add_interface_port slave slave_byteenable byteenable Input 4
add_interface_port slave slave_read read Input 1
add_interface_port slave slave_write write Input 1
add_interface_port slave slave_writedata writedata Input 32
add_interface_port slave slave_readdata readdata Output 32
add_interface_port slave slave_waitrequest waitrequest Output 1

set_interface_assignment slave embeddedsw.configuration.isFlash 0
set_interface_assignment slave embeddedsw.configuration.isMemoryDevice 0
set_interface_assignment slave embeddedsw.configuration.isNonVolatileStorage 0
set_interface_assignment slave embeddedsw.configuration.isPrintableDevice 0
# | 
# +-----------------------------------

# +-----------------------------------
# | connection point master
# | 
add_interface master avalon start 
set_interface_property master addressUnits WORDS
set_interface_property master associatedClock clock
set_interface_property master associatedReset reset
set_interface_property master bitsPerSymbol 8
set_interface_property master burstOnBurstBoundariesOnly false
set_interface_property master burstcountUnits WORDS
set_interface_property master doStreamReads false
set_interface_property master doStreamWrites false
set_interface_property master holdTime 0
set_interface_property master linewrapBursts false
set_interface_property master maximumPendingReadTransactions 0
set_interface_property master maximumPendingWriteTransactions 0
set_interface_property master readLatency 0
set_interface_property master readWaitTime 0
set_interface_property master setupTime 0
set_interface_property master timingUnits Cycles
set_interface_property master writeWaitTime 0
set_interface_property master ENABLED true
set_interface_property master EXPORT_OF ""
set_interface_property master PORT_NAME_MAP ""
set_interface_property master CMSIS_SVD_VARIABLES ""
set_interface_property master SVD_ADDRESS_GROUP ""

add_interface_port master master_readdata readdata Input 32
add_interface_port master master_waitrequest waitrequest Input 1
add_interface_port master master_address address Output 2
add_interface_port master master_byteenable byteenable Output 4
add_interface_port master master_read read Output 1
add_interface_port master master_write write Output 1
add_interface_port master master_writedata writedata Output 32
# | 
# +-----------------------------------


## Add documentation links for user guide and/or release notes
add_documentation_link "Release Notes" https://documentation.altera.com/#/link/hco1421698042087/hco1421698013408
