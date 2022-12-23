# JynxWasm32

  This reads a wasm file and produces a jynx file (wasm macro library required to assemble)

## Usage

Usage:

  {options} .wasm-file
  `  (produces a .jx file)`

Options are:

*	--DEBUG changes log-level to FINEST
*	--NON_STANDARD stops changing first character of class name to upper case
*	--COMMENT add wasm ops as comments to Jynx output
*	--NAME class_name  ; default is module-name else filename without the .wasm extension
* --PACKAGE add package name

## Notice

The source code contains some documentation snippets from 
[WebAssembly BinaryEncoding](https://github.com/WebAssembly/design/blob/main/BinaryEncoding.md)
