# JynxWasm32

  This reads a wasm file and produces a jynx file (JynxMacro library required to assemble)

The supported extensions to the  mvp are

*	Sign extension instructions
*	Non-trapping float-to-int conversions
*	memory copy and memory fill
*	section 12
*	extended name section

## Usage

Usage:

  {options} .wasm-file
    (produces a .jx file)

Options are:

*	--DEBUG changes log-level to FINEST
*	--CLASS_NAME_AS_IS stops changing first character of class name to upper case
*	--COMMENT add wasm ops as comments to Jynx output
*	--NAME class_name  ; default is module-name else filename without the .wasm extension

## Notice

The source code contains some documentation snippets from 
[WebAssembly BinaryEncoding](https://github.com/WebAssembly/design/blob/main/BinaryEncoding.md)
